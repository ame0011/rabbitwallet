package com.amesingularity.rabbitwallet.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.amesingularity.rabbitwallet.broadcast.LocalAction;
import com.amesingularity.rabbitwallet.model.DataFormat;
import com.amesingularity.rabbitwallet.model.Wallet;
import com.amesingularity.rabbitwallet.storage.database.Contract;
import com.amesingularity.rabbitwallet.storage.database.DataContentProvider;
import com.amesingularity.rabbitwallet.storage.database.data.AbstractDataExporter;
import com.amesingularity.rabbitwallet.storage.database.data.AbstractDataImporter;
import com.amesingularity.rabbitwallet.storage.database.data.csv.CSVDataExporter;
import com.amesingularity.rabbitwallet.storage.database.data.csv.CSVDataImporter;
import com.amesingularity.rabbitwallet.storage.database.data.pdf.PDFDataExporter;
import com.amesingularity.rabbitwallet.storage.database.data.xls.XLSDataExporter;
import com.amesingularity.rabbitwallet.utils.DateUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by andrea on 20/12/18.
 */
public class ImportExportIntentService extends IntentService {

    public static final String MODE = "ImportExportIntentService::Arguments::Mode";
    public static final String FORMAT = "ImportExportIntentService::Arguments::Format";
    public static final String START_DATE = "ImportExportIntentService::Arguments::StartDate";
    public static final String END_DATE = "ImportExportIntentService::Arguments::EndDate";
    public static final String WALLETS = "ImportExportIntentService::Arguments::Wallets";
    /**
     * Document to read for the import operation (a content uri).
     */
    public static final String FILE_URI = "ImportExportIntentService::Arguments::FileUri";
    /**
     * Display name of the document to import, used to keep the file extension.
     */
    public static final String FILE_NAME = "ImportExportIntentService::Arguments::FileName";
    /**
     * Document where the export operation must write the result (a content uri).
     */
    public static final String OUTPUT_URI = "ImportExportIntentService::Arguments::OutputUri";
    public static final String UNIQUE_WALLET = "ImportExportIntentService::Arguments::UniqueWallet";
    public static final String OPTIONAL_COLUMNS = "ImportExportIntentService::Arguments::OptionalColumns";

    public static final String RESULT_FILE_URI = "ImportExportIntentService::Results::FileUri";
    public static final String RESULT_FILE_TYPE = "ImportExportIntentService::Results::FileType";
    public static final String EXCEPTION = "ImportExportIntentService::Results::Exception";

    public static final int MODE_EXPORT = 0;
    public static final int MODE_IMPORT = 1;

    private static final String IMPORT_CACHE_FOLDER = "import";
    private static final String EXPORT_CACHE_FOLDER = "exports";

    private static final String DEFAULT_IMPORT_FILE_NAME = "import.csv";

    private LocalBroadcastManager mBroadcastManager;

    public ImportExportIntentService() {
        super("ImportExportIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            mBroadcastManager = LocalBroadcastManager.getInstance(this);
            int mode = intent.getIntExtra(MODE, MODE_EXPORT);
            switch (mode) {
                case MODE_EXPORT:
                    handleExport(intent);
                    break;
                case MODE_IMPORT:
                    handleImport(intent);
                    break;
            }
        }
    }

    private void handleImport(@NonNull Intent intent) {
        notifyTaskStarted(LocalAction.ACTION_IMPORT_SERVICE_STARTED);
        File file = null;
        try {
            // extract parameters from the intent
            DataFormat dataFormat = (DataFormat) intent.getSerializableExtra(FORMAT);
            Uri uri = intent.getParcelableExtra(FILE_URI);
            String fileName = intent.getStringExtra(FILE_NAME);
            if (dataFormat == null) {
                throw new IllegalArgumentException("parameter is null [FORMAT]");
            }
            if (uri == null) {
                throw new IllegalArgumentException("parameter is null [FILE_URI]");
            }
            // the importer works on a local file: copy the document selected by the user
            // inside the application cache
            file = copyDocumentToCache(uri, fileName);
            // initialize the correct data importer
            AbstractDataImporter dataImporter = getDataImporter(dataFormat, file);
            try {
                dataImporter.importData();
            } finally {
                dataImporter.close();
            }
            notifyTaskFinished(LocalAction.ACTION_IMPORT_SERVICE_FINISHED);
        } catch (Exception e) {
            notifyTaskFailed(LocalAction.ACTION_IMPORT_SERVICE_FAILED, e);
        } finally {
            if (file != null) {
                FileUtils.deleteQuietly(file.getParentFile());
            }
        }
    }

    private void handleExport(@NonNull Intent intent) {
        notifyTaskStarted(LocalAction.ACTION_EXPORT_SERVICE_STARTED);
        try {
            // extract parameters from the intent
            DataFormat dataFormat = (DataFormat) intent.getSerializableExtra(FORMAT);
            Date startDate = (Date) intent.getSerializableExtra(START_DATE);
            Date endDate = (Date) intent.getSerializableExtra(END_DATE);
            Wallet[] wallets = getWalletList(intent, WALLETS);
            Uri outputUri = intent.getParcelableExtra(OUTPUT_URI);
            boolean uniqueWallet = intent.getBooleanExtra(UNIQUE_WALLET, false);
            String[] optionalColumns = intent.getStringArrayExtra(OPTIONAL_COLUMNS);
            // check necessary parameters
            if (dataFormat == null) {
                throw new IllegalArgumentException("parameter is null [FORMAT]");
            }
            if (wallets == null || wallets.length == 0) {
                throw new IllegalArgumentException("parameter is null or empty [WALLETS]");
            }
            if (outputUri == null) {
                throw new IllegalArgumentException("parameter is null [OUTPUT_URI]");
            }
            // the exporters work on a local file: write the result inside the application cache
            // and copy it inside the document selected by the user at the end
            File folder = prepareExportCacheFolder();
            // initialize the correct data exporter
            AbstractDataExporter dataExporter = getDataExporter(dataFormat, folder);
            ContentResolver contentResolver = getContentResolver();
            Uri uri = DataContentProvider.CONTENT_TRANSACTIONS;
            // initialize the selection builder with common variables
            StringBuilder selectionBuilder = new StringBuilder();
            List<String> selectionArguments = new ArrayList<>();
            // append rule to limit to the end date or to the current date
            selectionBuilder.append("DATE (" + Contract.Transaction.DATE + ") <= DATE(?)");
            selectionArguments.add(DateUtils.getSQLDateString(getFixedEndDate(endDate)));
            // if provided, apply a rule to the start date
            if (startDate != null) {
                selectionBuilder.append(" AND DATE (" + Contract.Transaction.DATE + ") >= DATE(?)");
                selectionArguments.add(DateUtils.getSQLDateString(startDate));
            }
            String sortOrder = Contract.Transaction.DATE + " DESC";
            // check if we should create a unique wallet or if we can export each wallet
            // in a separate way
            boolean multiWallet = wallets.length > 1 && dataExporter.isMultiWalletSupported() && !uniqueWallet;
            String[] columns = dataExporter.getColumns(!multiWallet, optionalColumns);
            // before starting with the export logic, check if the exporter should
            // store the people names into his internal cache to speedup the procedure
            if (dataExporter.shouldLoadPeople()) {
                Cursor cursor = contentResolver.query(DataContentProvider.CONTENT_PEOPLE, null, null, null, null);
                if (cursor != null) {
                    dataExporter.cachePeople(cursor);
                    cursor.close();
                }
            }
            // handle the export logic differently
            if (multiWallet) {
                // execute a query for each wallet: we should clone the original builder
                // to avoid mistakes during each successive query
                for (Wallet wallet : wallets) {
                    String selection = selectionBuilder + " AND " + Contract.Transaction.WALLET_ID + " = ?";
                    String[] arguments = selectionArguments.toArray(new String[selectionArguments.size() + 1]);
                    arguments[arguments.length - 1] = String.valueOf(wallet.getId());
                    Cursor cursor = contentResolver.query(uri, null, selection, arguments, sortOrder);
                    if (cursor != null) {
                        dataExporter.exportData(cursor, columns, wallet);
                        cursor.close();
                    }
                }
            } else {
                // execute only a large query: we can modify the original builder here
                selectionBuilder.append(" AND (");
                for (int i = 0; i < wallets.length; i++) {
                    if (i != 0) {
                        selectionBuilder.append(" OR ");
                    }
                    selectionBuilder.append(Contract.Transaction.WALLET_ID + " = ?");
                    selectionArguments.add(String.valueOf(wallets[i].getId()));
                }
                selectionBuilder.append(")");
                Cursor cursor = contentResolver.query(uri, null, selectionBuilder.toString(), selectionArguments.toArray(new String[selectionArguments.size()]), sortOrder);
                if (cursor != null) {
                    dataExporter.exportData(cursor, columns, wallets);
                    cursor.close();
                }
            }
            // close the exporter to flush and close all the open streams
            dataExporter.close();
            // if no exception has been thrown so far, we can ask the exporter for the output
            // file: it is copied inside the document selected by the user and its uri is
            // passed back to the caller, so it can be opened with an external application
            String authority = getPackageName() + ".storage.file";
            File outputFile = dataExporter.getOutputFile();
            copyFileToDocument(outputFile, outputUri);
            Uri resultUri = FileProvider.getUriForFile(this, authority, outputFile);
            String resultType = dataExporter.getResultType();
            // send a successful intent to inform the receivers that the operation succeed
            notifyTaskFinished(LocalAction.ACTION_EXPORT_SERVICE_FINISHED, resultUri, resultType);
        } catch (Exception e) {
            e.printStackTrace();
            notifyTaskFailed(LocalAction.ACTION_EXPORT_SERVICE_FAILED, e);
        }
    }

    private Wallet[] getWalletList(Intent intent, String key) {
        Parcelable[] parcelableArray = intent.getParcelableArrayExtra(key);
        Wallet[] wallets = new Wallet[parcelableArray.length];
        for (int i = 0; i < parcelableArray.length; i++) {
            wallets[i] = (Wallet) parcelableArray[i];
        }
        return wallets;
    }

    private void notifyTaskStarted(String action) {
        Intent intent = new Intent(action);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void notifyTaskFinished(String action) {
        Intent intent = new Intent(action);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void notifyTaskFinished(String action, Uri resultUri, String resultType) {
        Intent intent = new Intent(action);
        intent.putExtra(RESULT_FILE_URI, resultUri);
        intent.putExtra(RESULT_FILE_TYPE, resultType);
        mBroadcastManager.sendBroadcast(intent);
    }

    private void notifyTaskFailed(String action, Exception exception) {
        Intent intent = new Intent(action);
        intent.putExtra(EXCEPTION, exception);
        mBroadcastManager.sendBroadcast(intent);
    }

    /**
     * Copy the document selected by the user inside a temporary folder of the application cache.
     * The importers work on plain files, so this is the only way to read a document that has been
     * granted through the storage access framework.
     */
    private File copyDocumentToCache(@NonNull Uri uri, @Nullable String fileName) throws IOException {
        File folder = new File(getCacheDir(), IMPORT_CACHE_FOLDER + File.separator + UUID.randomUUID().toString());
        FileUtils.forceMkdir(folder);
        File file = new File(folder, getSafeFileName(fileName));
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {
            if (inputStream == null) {
                throw new IOException("Unable to open the selected document");
            }
            IOUtils.copy(inputStream, outputStream);
        }
        return file;
    }

    /**
     * Copy the exported file inside the document created by the user through the storage access
     * framework.
     */
    private void copyFileToDocument(@NonNull File file, @NonNull Uri uri) throws IOException {
        try (InputStream inputStream = new FileInputStream(file);
             OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                throw new IOException("Unable to open the destination document");
            }
            IOUtils.copy(inputStream, outputStream);
        }
    }

    /**
     * @return the folder (inside the application cache) where the exporters can write the result.
     * The content of the folder is deleted before each export to avoid to fill the cache.
     */
    private File prepareExportCacheFolder() throws IOException {
        File folder = new File(getCacheDir(), EXPORT_CACHE_FOLDER);
        FileUtils.deleteQuietly(folder);
        FileUtils.forceMkdir(folder);
        return folder;
    }

    @NonNull
    private String getSafeFileName(@Nullable String fileName) {
        if (fileName == null) {
            return DEFAULT_IMPORT_FILE_NAME;
        }
        // the display name may contain path separators: keep only the file name
        String safeName = new File(fileName).getName();
        return safeName.isEmpty() ? DEFAULT_IMPORT_FILE_NAME : safeName;
    }

    private AbstractDataImporter getDataImporter(DataFormat dataFormat, File file) throws IOException {        switch (dataFormat) {
            case CSV:
                return new CSVDataImporter(this, file);
            default:
                throw new RuntimeException("DataFormat not supported");
        }
    }

    private AbstractDataExporter getDataExporter(DataFormat dataFormat, File folder) throws IOException {
        switch (dataFormat) {
            case CSV:
                return new CSVDataExporter(this, folder);
            case XLS:
                return new XLSDataExporter(this, folder);
            case PDF:
                return new PDFDataExporter(this, folder);
            default:
                throw new RuntimeException("DataFormat not supported");
        }
    }

    private Date getFixedEndDate(Date endDate) {
        Date now = new Date();
        if (endDate != null) {
            long minMillis = Math.min(now.getTime(), endDate.getTime());
            return new Date(minMillis);
        }
        return now;
    }
}