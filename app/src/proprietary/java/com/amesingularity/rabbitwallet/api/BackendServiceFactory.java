/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amesingularity.rabbitwallet.api;

import android.content.Context;

import com.amesingularity.rabbitwallet.R;
import com.amesingularity.rabbitwallet.api.dropbox.DropboxBackendService;
import com.amesingularity.rabbitwallet.api.dropbox.DropboxBackendServiceAPI;
import com.amesingularity.rabbitwallet.api.saf.SAFBackendService;
import com.amesingularity.rabbitwallet.api.saf.SAFBackendServiceAPI;
import com.amesingularity.rabbitwallet.model.BackupService;
import com.amesingularity.rabbitwallet.model.DropBoxFile;
import com.amesingularity.rabbitwallet.model.IFile;
import com.amesingularity.rabbitwallet.model.SAFFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 21/11/18.
 */
public class BackendServiceFactory {

    public static final String SERVICE_ID_DROPBOX = "dropbox";
    public static final String SERVICE_ID_SAF = "storage_access_framework";

    public static AbstractBackendServiceDelegate getServiceById(String backendId, AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
        switch (backendId) {
            case SERVICE_ID_DROPBOX:
                return new DropboxBackendService(listener);
            case SERVICE_ID_SAF:
                return new SAFBackendService(listener);
        }
        return null;
    }

    public static IBackendServiceAPI getServiceAPIById(Context context, String backendId) throws BackendException {
        switch (backendId) {
            case SERVICE_ID_DROPBOX:
                return new DropboxBackendServiceAPI(context);
            case SERVICE_ID_SAF:
                return new SAFBackendServiceAPI(context);
            default:
                throw new BackendException("Invalid backend");
        }
    }

    public static List<BackupService> getBackupServices() {
        List<BackupService> services = new ArrayList<>();
        services.add(new BackupService(SERVICE_ID_DROPBOX, R.drawable.ic_dropbox_24dp, R.string.service_backup_drop_box));
        services.add(new BackupService(SERVICE_ID_SAF, R.drawable.ic_storage_black_24dp, R.string.service_backup_storage_access_framework));
        return services;
    }

    public static IFile getFile(String backendId, String encoded) {
        if (encoded != null) {
            switch (backendId) {
                case SERVICE_ID_DROPBOX:
                    return new DropBoxFile(encoded);
                case SERVICE_ID_SAF:
                    return new SAFFile(encoded);
            }
        }
        return null;
    }
}
