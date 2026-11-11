/*
 * Copyright (c) 2026.
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

package com.amesingularity.rabbitwallet.storage.database.json;

import com.amesingularity.rabbitwallet.TestApplication;
import com.amesingularity.rabbitwallet.storage.database.model.Attachment;
import com.amesingularity.rabbitwallet.storage.database.model.Budget;
import com.amesingularity.rabbitwallet.storage.database.model.BudgetWallet;
import com.amesingularity.rabbitwallet.storage.database.model.Category;
import com.amesingularity.rabbitwallet.storage.database.model.Currency;
import com.amesingularity.rabbitwallet.storage.database.model.Debt;
import com.amesingularity.rabbitwallet.storage.database.model.DebtPerson;
import com.amesingularity.rabbitwallet.storage.database.model.Event;
import com.amesingularity.rabbitwallet.storage.database.model.EventPerson;
import com.amesingularity.rabbitwallet.storage.database.model.Person;
import com.amesingularity.rabbitwallet.storage.database.model.Place;
import com.amesingularity.rabbitwallet.storage.database.model.RecurrentTransaction;
import com.amesingularity.rabbitwallet.storage.database.model.RecurrentTransfer;
import com.amesingularity.rabbitwallet.storage.database.model.Saving;
import com.amesingularity.rabbitwallet.storage.database.model.Transaction;
import com.amesingularity.rabbitwallet.storage.database.model.TransactionAttachment;
import com.amesingularity.rabbitwallet.storage.database.model.TransactionModel;
import com.amesingularity.rabbitwallet.storage.database.model.TransactionPerson;
import com.amesingularity.rabbitwallet.storage.database.model.Transfer;
import com.amesingularity.rabbitwallet.storage.database.model.TransferAttachment;
import com.amesingularity.rabbitwallet.storage.database.model.TransferModel;
import com.amesingularity.rabbitwallet.storage.database.model.TransferPerson;
import com.amesingularity.rabbitwallet.storage.database.model.Wallet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Golden-file test for the JSON backup format produced by the {@code storage.database.json}
 * package. A fixed set of model objects (deterministic ids, UUIDs and last-edit timestamps)
 * is serialized with the same factory and stream writer used by {@link JSONDatabaseExporter}
 * and the result is compared section by section against the golden file
 * {@code test/resources/backup/backup_v2_golden.json}. Null fields are omitted from the
 * generated objects (org.json removes null values) so the golden file documents the exact
 * key set that is expected for every record type.
 *
 * <p>A second test feeds the serialized bytes back through {@link JSONDataStreamReader} and
 * {@link JSONDataInputFactory} to make sure that the import side still understands the
 * exported keys and resolves the UUID cross-references correctly.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class)
public class JSONBackupGoldenTest {

    /** Same order the exporter writes the top level sections in. */
    private static final String[] TOP_LEVEL_SECTIONS = {
            JSONDatabase.Header.OBJECT,
            JSONDatabase.Currency.ARRAY,
            JSONDatabase.Wallet.ARRAY,
            JSONDatabase.Category.ARRAY,
            JSONDatabase.Event.ARRAY,
            JSONDatabase.Place.ARRAY,
            JSONDatabase.Person.ARRAY,
            JSONDatabase.EventPeople.ARRAY,
            JSONDatabase.Debt.ARRAY,
            JSONDatabase.DebtPeople.ARRAY,
            JSONDatabase.Budget.ARRAY,
            JSONDatabase.BudgetWallet.ARRAY,
            JSONDatabase.Saving.ARRAY,
            JSONDatabase.RecurrentTransaction.ARRAY,
            JSONDatabase.RecurrentTransfer.ARRAY,
            JSONDatabase.Transaction.ARRAY,
            JSONDatabase.TransactionPeople.ARRAY,
            JSONDatabase.TransactionModel.ARRAY,
            JSONDatabase.Transfer.ARRAY,
            JSONDatabase.TransferPeople.ARRAY,
            JSONDatabase.TransferModel.ARRAY,
            JSONDatabase.Attachment.ARRAY,
            JSONDatabase.TransactionAttachment.ARRAY,
            JSONDatabase.TransferAttachment.ARRAY,
    };

    private static final long LAST_EDIT_BASE = 1600000000000L;

    private final JSONDataOutputFactory mFactory = new JSONDataOutputFactory();

    private byte[] mJson;

    @Before
    public void setUp() throws Exception {
        mJson = exportFixedDataset();
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// GOLDEN COMPARISON ///////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void exportedBackupMatchesTheGoldenFile() throws Exception {
        JSONObject actual = parse(new String(mJson, StandardCharsets.UTF_8));
        JSONObject expected = parse(readResource("/backup/backup_v2_golden.json"));

        String differences = diff(expected, actual);
        if (differences != null) {
            fail("backup JSON does not match the golden file:\n" + differences);
        }
    }

    @Test
    public void goldenFileCoversAllTheTopLevelSections() throws Exception {
        JSONObject actual = parse(new String(mJson, StandardCharsets.UTF_8));
        for (String section : TOP_LEVEL_SECTIONS) {
            assertTrue("missing section: " + section, actual.has(section));
        }
        assertEquals(TOP_LEVEL_SECTIONS.length, countKeys(actual));
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// ROUND TRIP ////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void streamReaderCanReadBackTheExportedSections() throws Exception {
        JSONDataStreamReader reader = new JSONDataStreamReader(new ByteArrayInputStream(mJson));

        // header first, it is an object and not an array
        assertEquals(JSONDatabase.Header.OBJECT, reader.readName());
        JSONObject header = reader.readObject();
        assertEquals(JSONDatabase.VERSION, header.getInt(JSONDatabase.Header.VERSION_CODE));

        for (int i = 1; i < TOP_LEVEL_SECTIONS.length; i++) {
            assertEquals(TOP_LEVEL_SECTIONS[i], reader.readName());
            int objects = 0;
            reader.beginArray();
            while (reader.hasArrayAnotherObject()) {
                reader.readObject();
                objects++;
            }
            reader.endArray();
            assertTrue("section should not be empty: " + TOP_LEVEL_SECTIONS[i], objects > 0);
        }
        reader.close();
    }

    @Test
    public void inputFactoryRestoresModelsAndResolvesReferences() throws Exception {
        JSONObject document = parse(new String(mJson, StandardCharsets.UTF_8));
        JSONDataInputFactory factory = new JSONDataInputFactory();

        Currency currency = factory.getCurrency(objectAt(document, JSONDatabase.Currency.ARRAY, 0));
        assertEquals("EUR", currency.mIso);
        assertEquals(2, currency.mDecimals);
        assertTrue(currency.mFavourite);
        assertEquals("currency_EUR", currency.mUUID);

        Wallet wallet1 = factory.getWallet(objectAt(document, JSONDatabase.Wallet.ARRAY, 0));
        assertEquals(uuid(1), wallet1.mUUID);
        assertEquals("Cash wallet", wallet1.mName);
        assertEquals(100000L, wallet1.mStartMoney);
        assertNull("null note should stay null after the round trip", wallet1.mNote);
        factory.cacheWallet(wallet1.mUUID, 1L);

        Wallet wallet2 = factory.getWallet(objectAt(document, JSONDatabase.Wallet.ARRAY, 1));
        assertEquals(-500000L, wallet2.mStartMoney);
        assertTrue(wallet2.mArchived);
        assertEquals("second wallet", wallet2.mNote);
        factory.cacheWallet(wallet2.mUUID, 2L);

        Category parent = factory.getCategory(objectAt(document, JSONDatabase.Category.ARRAY, 0));
        assertNull(parent.mParent);
        factory.cacheCategory(parent.mUUID, 3L);

        Category child = factory.getCategory(objectAt(document, JSONDatabase.Category.ARRAY, 1));
        assertEquals(Long.valueOf(3L), child.mParent);
        factory.cacheCategory(child.mUUID, 4L);

        Event event = factory.getEvent(objectAt(document, JSONDatabase.Event.ARRAY, 0));
        assertEquals("2020-01-31", event.mEndDate);
        factory.cacheEvent(event.mUUID, 5L);

        Place place = factory.getPlace(objectAt(document, JSONDatabase.Place.ARRAY, 0));
        assertEquals(45.4642d, place.mLatitude, 0.00001d);
        assertEquals(9.19d, place.mLongitude, 0.00001d);
        factory.cachePlace(place.mUUID, 6L);

        Person person = factory.getPerson(objectAt(document, JSONDatabase.Person.ARRAY, 0));
        assertEquals("Alice", person.mName);
        factory.cachePerson(person.mUUID, 7L);

        EventPerson eventPerson = factory.getEventPerson(objectAt(document, JSONDatabase.EventPeople.ARRAY, 0));
        assertEquals(Long.valueOf(5L), eventPerson.mEvent);
        assertEquals(Long.valueOf(7L), eventPerson.mPerson);

        Debt debt = factory.getDebt(objectAt(document, JSONDatabase.Debt.ARRAY, 0));
        assertEquals(Long.valueOf(1L), debt.mWallet);
        assertNull("null place should stay null after the round trip", debt.mPlace);
        assertEquals(250000L, debt.mMoney);
        factory.cacheDebt(debt.mUUID, 9L);

        DebtPerson debtPerson = factory.getDebtPerson(objectAt(document, JSONDatabase.DebtPeople.ARRAY, 0));
        assertEquals(Long.valueOf(9L), debtPerson.mDebt);
        assertEquals(Long.valueOf(7L), debtPerson.mPerson);

        Budget budget = factory.getBudget(objectAt(document, JSONDatabase.Budget.ARRAY, 0));
        assertEquals(Long.valueOf(4L), budget.mCategory);
        assertEquals("EUR", budget.mCurrency);
        factory.cacheBudget(budget.mUUID, 11L);

        BudgetWallet budgetWallet = factory.getBudgetWallet(objectAt(document, JSONDatabase.BudgetWallet.ARRAY, 0));
        assertEquals(Long.valueOf(11L), budgetWallet.mBudget);
        assertEquals(Long.valueOf(2L), budgetWallet.mWallet);

        Saving saving = factory.getSaving(objectAt(document, JSONDatabase.Saving.ARRAY, 0));
        assertEquals(Long.valueOf(1L), saving.mWallet);
        assertEquals(10000000L, saving.mEndMoney);
        factory.cacheSaving(saving.mUUID, 13L);

        RecurrentTransaction recurrent = factory.getRecurrentTransaction(
                objectAt(document, JSONDatabase.RecurrentTransaction.ARRAY, 0));
        assertEquals(Long.valueOf(3L), recurrent.mCategory);
        assertNull("null last_occurrence should stay null after the round trip", recurrent.mLastOccurrence);
        assertEquals("FREQ=MONTHLY;INTERVAL=1", recurrent.mRule);
        factory.cacheRecurrentTransaction(recurrent.mUUID, 14L);

        RecurrentTransfer recurrentTransfer = factory.getRecurrentTransfer(
                objectAt(document, JSONDatabase.RecurrentTransfer.ARRAY, 0));
        assertEquals(Long.valueOf(1L), recurrentTransfer.mFromWallet);
        assertEquals(Long.valueOf(2L), recurrentTransfer.mToWallet);
        assertEquals(1000L, recurrentTransfer.mTaxMoney);
        factory.cacheRecurrentTransfer(recurrentTransfer.mUUID, 15L);

        Transaction transaction = factory.getTransaction(objectAt(document, JSONDatabase.Transaction.ARRAY, 0));
        assertEquals(Long.valueOf(4L), transaction.mCategory);
        assertEquals(Long.valueOf(1L), transaction.mWallet);
        assertEquals(Long.valueOf(6L), transaction.mPlace);
        assertEquals(Long.valueOf(5L), transaction.mEvent);
        assertEquals(Long.valueOf(14L), transaction.mRecurrence);
        assertNull("null saving should stay null after the round trip", transaction.mSaving);
        factory.cacheTransaction(transaction.mUUID, 16L);

        factory.cacheTransaction(factory.getTransaction(
                objectAt(document, JSONDatabase.Transaction.ARRAY, 1)).mUUID, 17L);
        factory.cacheTransaction(factory.getTransaction(
                objectAt(document, JSONDatabase.Transaction.ARRAY, 2)).mUUID, 18L);

        TransactionPerson transactionPerson = factory.getTransactionPerson(
                objectAt(document, JSONDatabase.TransactionPeople.ARRAY, 0));
        assertEquals(Long.valueOf(16L), transactionPerson.mTransaction);
        assertEquals(Long.valueOf(7L), transactionPerson.mPerson);

        TransactionModel transactionModel = factory.getTransactionModel(
                objectAt(document, JSONDatabase.TransactionModel.ARRAY, 0));
        assertEquals(Long.valueOf(4L), transactionModel.mCategory);
        assertEquals(12000L, transactionModel.mMoney);

        Transfer transfer = factory.getTransfer(objectAt(document, JSONDatabase.Transfer.ARRAY, 0));
        assertEquals(Long.valueOf(17L), transfer.mTransactionFrom);
        assertEquals(Long.valueOf(18L), transfer.mTransactionTo);
        assertNull("null tax transaction should stay null after the round trip", transfer.mTransactionTax);
        assertEquals(Long.valueOf(15L), transfer.mRecurrence);
        factory.cacheTransfer(transfer.mUUID, 21L);

        TransferPerson transferPerson = factory.getTransferPerson(
                objectAt(document, JSONDatabase.TransferPeople.ARRAY, 0));
        assertEquals(Long.valueOf(21L), transferPerson.mTransfer);
        assertEquals(Long.valueOf(7L), transferPerson.mPerson);

        TransferModel transferModel = factory.getTransferModel(
                objectAt(document, JSONDatabase.TransferModel.ARRAY, 0));
        assertEquals(Long.valueOf(1L), transferModel.mFromWallet);
        assertEquals(Long.valueOf(2L), transferModel.mToWallet);
        assertEquals(500L, transferModel.mTaxMoney);

        Attachment attachment = factory.getAttachment(objectAt(document, JSONDatabase.Attachment.ARRAY, 0));
        assertEquals("receipt.png", attachment.mFile);
        assertEquals(2048L, attachment.mSize);
        factory.cacheAttachment(attachment.mUUID, 24L);

        TransactionAttachment transactionAttachment = factory.getTransactionAttachment(
                objectAt(document, JSONDatabase.TransactionAttachment.ARRAY, 0));
        assertEquals(Long.valueOf(16L), transactionAttachment.mTransaction);
        assertEquals(Long.valueOf(24L), transactionAttachment.mAttachment);

        TransferAttachment transferAttachment = factory.getTransferAttachment(
                objectAt(document, JSONDatabase.TransferAttachment.ARRAY, 0));
        assertEquals(Long.valueOf(21L), transferAttachment.mTransfer);
        assertEquals(Long.valueOf(24L), transferAttachment.mAttachment);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// DATASET ///////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Serializes the fixed dataset exactly the way {@link JSONDatabaseExporter} does: a
     * header object followed by one array per record type, in the very same order. The
     * output factory keeps a per-type id-to-UUID cache that is filled on the fly, so the
     * objects must be converted in the same order they are written in.
     */
    private byte[] exportFixedDataset() throws Exception {
        Currency currency = currency();
        Wallet wallet1 = wallet1();
        Wallet wallet2 = wallet2();
        Category parent = categoryParent();
        Category child = categoryChild();
        Event event = event();
        Place place = place();
        Person person = person();
        EventPerson eventPerson = eventPerson();
        Debt debt = debt();
        DebtPerson debtPerson = debtPerson();
        Budget budget = budget();
        BudgetWallet budgetWallet = budgetWallet();
        Saving saving = saving();
        RecurrentTransaction recurrentTransaction = recurrentTransaction();
        RecurrentTransfer recurrentTransfer = recurrentTransfer();
        Transaction transaction = transaction();
        Transaction transactionFrom = transactionFrom();
        Transaction transactionTo = transactionTo();
        TransactionPerson transactionPerson = transactionPerson();
        TransactionModel transactionModel = transactionModel();
        Transfer transfer = transfer();
        TransferPerson transferPerson = transferPerson();
        TransferModel transferModel = transferModel();
        Attachment attachment = attachment();
        TransactionAttachment transactionAttachment = transactionAttachment();
        TransferAttachment transferAttachment = transferAttachment();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JSONDataStreamWriter writer = new JSONDataStreamWriter(out);

        writer.writeName(JSONDatabase.Header.OBJECT);
        JSONObject header = new JSONObject();
        header.put(JSONDatabase.Header.VERSION_CODE, JSONDatabase.VERSION);
        writer.writeJSONObject(header);

        writeSection(writer, JSONDatabase.Currency.ARRAY, list(mFactory.getObject(currency)));
        writeSection(writer, JSONDatabase.Wallet.ARRAY, list(mFactory.getObject(wallet1), mFactory.getObject(wallet2)));
        writeSection(writer, JSONDatabase.Category.ARRAY, list(mFactory.getObject(parent), mFactory.getObject(child)));
        writeSection(writer, JSONDatabase.Event.ARRAY, list(mFactory.getObject(event)));
        writeSection(writer, JSONDatabase.Place.ARRAY, list(mFactory.getObject(place)));
        writeSection(writer, JSONDatabase.Person.ARRAY, list(mFactory.getObject(person)));
        writeSection(writer, JSONDatabase.EventPeople.ARRAY, list(mFactory.getObject(eventPerson)));
        writeSection(writer, JSONDatabase.Debt.ARRAY, list(mFactory.getObject(debt)));
        writeSection(writer, JSONDatabase.DebtPeople.ARRAY, list(mFactory.getObject(debtPerson)));
        writeSection(writer, JSONDatabase.Budget.ARRAY, list(mFactory.getObject(budget)));
        writeSection(writer, JSONDatabase.BudgetWallet.ARRAY, list(mFactory.getObject(budgetWallet)));
        writeSection(writer, JSONDatabase.Saving.ARRAY, list(mFactory.getObject(saving)));
        writeSection(writer, JSONDatabase.RecurrentTransaction.ARRAY, list(mFactory.getObject(recurrentTransaction)));
        writeSection(writer, JSONDatabase.RecurrentTransfer.ARRAY, list(mFactory.getObject(recurrentTransfer)));
        writeSection(writer, JSONDatabase.Transaction.ARRAY,
                list(mFactory.getObject(transaction), mFactory.getObject(transactionFrom), mFactory.getObject(transactionTo)));
        writeSection(writer, JSONDatabase.TransactionPeople.ARRAY, list(mFactory.getObject(transactionPerson)));
        writeSection(writer, JSONDatabase.TransactionModel.ARRAY, list(mFactory.getObject(transactionModel)));
        writeSection(writer, JSONDatabase.Transfer.ARRAY, list(mFactory.getObject(transfer)));
        writeSection(writer, JSONDatabase.TransferPeople.ARRAY, list(mFactory.getObject(transferPerson)));
        writeSection(writer, JSONDatabase.TransferModel.ARRAY, list(mFactory.getObject(transferModel)));
        writeSection(writer, JSONDatabase.Attachment.ARRAY, list(mFactory.getObject(attachment)));
        writeSection(writer, JSONDatabase.TransactionAttachment.ARRAY, list(mFactory.getObject(transactionAttachment)));
        writeSection(writer, JSONDatabase.TransferAttachment.ARRAY, list(mFactory.getObject(transferAttachment)));

        writer.close();
        return out.toByteArray();
    }

    private static void writeSection(JSONDataStreamWriter writer, String name, List<JSONObject> objects)
            throws Exception {
        writer.writeName(name);
        writer.beginArray();
        for (JSONObject object : objects) {
            writer.writeJSONObject(object);
        }
        writer.endArray();
    }

    @SafeVarargs
    private static <T> List<T> list(T... items) {
        return Arrays.asList(items);
    }

    private static String uuid(int n) {
        return String.format("00000000-0000-0000-0000-%012X", n);
    }

    private static long lastEdit(int n) {
        return LAST_EDIT_BASE + n;
    }

    private static Currency currency() {
        Currency currency = new Currency();
        currency.mIso = "EUR";
        currency.mName = "Euro";
        currency.mSymbol = "\u20ac";
        currency.mDecimals = 2;
        currency.mFavourite = true;
        currency.mLastEdit = lastEdit(1);
        currency.mDeleted = false;
        return currency;
    }

    private static Wallet wallet1() {
        Wallet wallet = new Wallet();
        wallet.mId = 1L;
        wallet.mUUID = uuid(1);
        wallet.mName = "Cash wallet";
        wallet.mIcon = "ic_wallet";
        wallet.mCurrency = "EUR";
        wallet.mStartMoney = 100000L;
        wallet.mCountInTotal = true;
        wallet.mArchived = false;
        wallet.mNote = null;
        wallet.mTag = null;
        wallet.mIndex = 0;
        wallet.mLastEdit = lastEdit(2);
        return wallet;
    }

    private static Wallet wallet2() {
        Wallet wallet = wallet1();
        wallet.mId = 2L;
        wallet.mUUID = uuid(2);
        wallet.mName = "Bank";
        wallet.mIcon = "ic_bank";
        wallet.mStartMoney = -500000L;
        wallet.mArchived = true;
        wallet.mNote = "second wallet";
        wallet.mTag = "wallet-tag";
        wallet.mIndex = 1;
        wallet.mLastEdit = lastEdit(3);
        return wallet;
    }

    private static Category categoryParent() {
        Category category = new Category();
        category.mId = 3L;
        category.mUUID = uuid(3);
        category.mName = "Food";
        category.mIcon = "ic_food";
        category.mType = 1;
        category.mParent = null;
        category.mTag = null;
        category.mShowReport = true;
        category.mIndex = 0;
        category.mLastEdit = lastEdit(4);
        return category;
    }

    private static Category categoryChild() {
        Category category = categoryParent();
        category.mId = 4L;
        category.mUUID = uuid(4);
        category.mName = "Restaurant";
        category.mIcon = "ic_restaurant";
        category.mParent = 3L;
        category.mIndex = 1;
        category.mLastEdit = lastEdit(5);
        return category;
    }

    private static Event event() {
        Event event = new Event();
        event.mId = 5L;
        event.mUUID = uuid(5);
        event.mName = "Birthday";
        event.mIcon = "ic_event";
        event.mNote = null;
        event.mStartDate = "2020-01-01";
        event.mEndDate = "2020-01-31";
        event.mTag = "event-tag";
        event.mLastEdit = lastEdit(6);
        return event;
    }

    private static Place place() {
        Place place = new Place();
        place.mId = 6L;
        place.mUUID = uuid(6);
        place.mName = "Shop";
        place.mIcon = "ic_place";
        place.mAddress = "Via Roma 1";
        place.mLatitude = 45.4642d;
        place.mLongitude = 9.19d;
        place.mTag = null;
        place.mLastEdit = lastEdit(7);
        return place;
    }

    private static Person person() {
        Person person = new Person();
        person.mId = 7L;
        person.mUUID = uuid(7);
        person.mName = "Alice";
        person.mIcon = "ic_person";
        person.mNote = "colleague";
        person.mTag = null;
        person.mLastEdit = lastEdit(8);
        return person;
    }

    private static EventPerson eventPerson() {
        EventPerson eventPerson = new EventPerson();
        eventPerson.mUUID = uuid(8);
        eventPerson.mEvent = 5L;
        eventPerson.mPerson = 7L;
        eventPerson.mLastEdit = lastEdit(9);
        return eventPerson;
    }

    private static Debt debt() {
        Debt debt = new Debt();
        debt.mId = 9L;
        debt.mUUID = uuid(9);
        debt.mType = 0;
        debt.mIcon = "ic_debt";
        debt.mDescription = "Loan from Alice";
        debt.mDate = "2020-02-01";
        debt.mExpirationDate = "2021-02-01";
        debt.mWallet = 1L;
        debt.mNote = null;
        debt.mPlace = null;
        debt.mMoney = 250000L;
        debt.mArchived = false;
        debt.mTag = null;
        debt.mLastEdit = lastEdit(10);
        return debt;
    }

    private static DebtPerson debtPerson() {
        DebtPerson debtPerson = new DebtPerson();
        debtPerson.mUUID = uuid(10);
        debtPerson.mDebt = 9L;
        debtPerson.mPerson = 7L;
        debtPerson.mLastEdit = lastEdit(11);
        return debtPerson;
    }

    private static Budget budget() {
        Budget budget = new Budget();
        budget.mId = 11L;
        budget.mUUID = uuid(11);
        budget.mType = 1;
        budget.mCategory = 4L;
        budget.mStartDate = "2020-01-01";
        budget.mEndDate = "2020-12-31";
        budget.mMoney = 500000L;
        budget.mCurrency = "EUR";
        budget.mTag = null;
        budget.mLastEdit = lastEdit(12);
        return budget;
    }

    private static BudgetWallet budgetWallet() {
        BudgetWallet budgetWallet = new BudgetWallet();
        budgetWallet.mUUID = uuid(12);
        budgetWallet.mBudget = 11L;
        budgetWallet.mWallet = 2L;
        budgetWallet.mLastEdit = lastEdit(13);
        return budgetWallet;
    }

    private static Saving saving() {
        Saving saving = new Saving();
        saving.mId = 13L;
        saving.mUUID = uuid(13);
        saving.mDescription = "New car";
        saving.mIcon = "ic_saving";
        saving.mStartMoney = 0L;
        saving.mEndMoney = 10000000L;
        saving.mWallet = 1L;
        saving.mEndDate = "2025-12-31";
        saving.mComplete = false;
        saving.mNote = null;
        saving.mTag = null;
        saving.mLastEdit = lastEdit(14);
        return saving;
    }

    private static RecurrentTransaction recurrentTransaction() {
        RecurrentTransaction transaction = new RecurrentTransaction();
        transaction.mId = 14L;
        transaction.mUUID = uuid(14);
        transaction.mMoney = 5000L;
        transaction.mDescription = "Rent";
        transaction.mCategory = 3L;
        transaction.mDirection = 0;
        transaction.mWallet = 1L;
        transaction.mPlace = null;
        transaction.mNote = null;
        transaction.mEvent = null;
        transaction.mConfirmed = true;
        transaction.mCountInTotal = true;
        transaction.mStartDate = "2020-01-01";
        transaction.mLastOccurrence = null;
        transaction.mNextOccurrence = "2020-02-01";
        transaction.mRule = "FREQ=MONTHLY;INTERVAL=1";
        transaction.mTag = null;
        transaction.mLastEdit = lastEdit(15);
        return transaction;
    }

    private static RecurrentTransfer recurrentTransfer() {
        RecurrentTransfer transfer = new RecurrentTransfer();
        transfer.mId = 15L;
        transfer.mUUID = uuid(15);
        transfer.mDescription = "Monthly saving";
        transfer.mFromWallet = 1L;
        transfer.mToWallet = 2L;
        transfer.mFromMoney = 100000L;
        transfer.mToMoney = 99000L;
        transfer.mTaxMoney = 1000L;
        transfer.mNote = null;
        transfer.mEvent = null;
        transfer.mPlace = null;
        transfer.mConfirmed = true;
        transfer.mCountInTotal = false;
        transfer.mStartDate = "2020-01-15";
        transfer.mLastOccurrence = null;
        transfer.mNextOccurrence = "2020-02-15";
        transfer.mRule = "FREQ=MONTHLY;INTERVAL=1";
        transfer.mTag = null;
        transfer.mLastEdit = lastEdit(16);
        return transfer;
    }

    private static Transaction transaction() {
        Transaction transaction = new Transaction();
        transaction.mId = 16L;
        transaction.mUUID = uuid(16);
        transaction.mMoney = 4560L;
        transaction.mDate = "2020-01-05";
        transaction.mDescription = "Pizza";
        transaction.mCategory = 4L;
        transaction.mDirection = 0;
        transaction.mType = 2;
        transaction.mWallet = 1L;
        transaction.mPlace = 6L;
        transaction.mNote = null;
        transaction.mSaving = null;
        transaction.mDebt = null;
        transaction.mEvent = 5L;
        transaction.mRecurrence = 14L;
        transaction.mConfirmed = true;
        transaction.mCountInTotal = true;
        transaction.mTag = null;
        transaction.mLastEdit = lastEdit(17);
        return transaction;
    }

    private static Transaction transactionFrom() {
        Transaction transaction = transaction();
        transaction.mId = 17L;
        transaction.mUUID = uuid(17);
        transaction.mMoney = 100000L;
        transaction.mDate = "2020-01-06";
        transaction.mDescription = "From wallet";
        transaction.mCategory = 3L;
        transaction.mType = 3;
        transaction.mPlace = null;
        transaction.mEvent = null;
        transaction.mRecurrence = null;
        transaction.mLastEdit = lastEdit(18);
        return transaction;
    }

    private static Transaction transactionTo() {
        Transaction transaction = transactionFrom();
        transaction.mId = 18L;
        transaction.mUUID = uuid(18);
        transaction.mDescription = "To wallet";
        transaction.mDirection = 1;
        transaction.mType = 4;
        transaction.mWallet = 2L;
        transaction.mLastEdit = lastEdit(19);
        return transaction;
    }

    private static TransactionPerson transactionPerson() {
        TransactionPerson transactionPerson = new TransactionPerson();
        transactionPerson.mUUID = uuid(19);
        transactionPerson.mTransaction = 16L;
        transactionPerson.mPerson = 7L;
        transactionPerson.mLastEdit = lastEdit(20);
        return transactionPerson;
    }

    private static TransactionModel transactionModel() {
        TransactionModel transactionModel = new TransactionModel();
        transactionModel.mId = 20L;
        transactionModel.mUUID = uuid(20);
        transactionModel.mMoney = 12000L;
        transactionModel.mDescription = "Groceries";
        transactionModel.mCategory = 4L;
        transactionModel.mDirection = 0;
        transactionModel.mWallet = 1L;
        transactionModel.mPlace = null;
        transactionModel.mNote = null;
        transactionModel.mEvent = null;
        transactionModel.mConfirmed = true;
        transactionModel.mCountInTotal = true;
        transactionModel.mTag = null;
        transactionModel.mLastEdit = lastEdit(21);
        return transactionModel;
    }

    private static Transfer transfer() {
        Transfer transfer = new Transfer();
        transfer.mId = 21L;
        transfer.mUUID = uuid(21);
        transfer.mDescription = "To bank";
        transfer.mDate = "2020-01-06";
        transfer.mTransactionFrom = 17L;
        transfer.mTransactionTo = 18L;
        transfer.mTransactionTax = null;
        transfer.mNote = null;
        transfer.mPlace = null;
        transfer.mEvent = null;
        transfer.mRecurrence = 15L;
        transfer.mConfirmed = true;
        transfer.mCountInTotal = true;
        transfer.mTag = null;
        transfer.mLastEdit = lastEdit(22);
        return transfer;
    }

    private static TransferPerson transferPerson() {
        TransferPerson transferPerson = new TransferPerson();
        transferPerson.mUUID = uuid(22);
        transferPerson.mTransfer = 21L;
        transferPerson.mPerson = 7L;
        transferPerson.mLastEdit = lastEdit(23);
        return transferPerson;
    }

    private static TransferModel transferModel() {
        TransferModel transferModel = new TransferModel();
        transferModel.mId = 23L;
        transferModel.mUUID = uuid(23);
        transferModel.mDescription = "Savings model";
        transferModel.mFromWallet = 1L;
        transferModel.mToWallet = 2L;
        transferModel.mFromMoney = 50000L;
        transferModel.mToMoney = 49500L;
        transferModel.mTaxMoney = 500L;
        transferModel.mNote = null;
        transferModel.mEvent = null;
        transferModel.mPlace = null;
        transferModel.mConfirmed = true;
        transferModel.mCountInTotal = true;
        transferModel.mTag = null;
        transferModel.mLastEdit = lastEdit(24);
        return transferModel;
    }

    private static Attachment attachment() {
        Attachment attachment = new Attachment();
        attachment.mId = 24L;
        attachment.mUUID = uuid(24);
        attachment.mFile = "receipt.png";
        attachment.mName = "receipt.png";
        attachment.mType = "image/png";
        attachment.mSize = 2048L;
        attachment.mTag = null;
        attachment.mLastEdit = lastEdit(25);
        return attachment;
    }

    private static TransactionAttachment transactionAttachment() {
        TransactionAttachment transactionAttachment = new TransactionAttachment();
        transactionAttachment.mUUID = uuid(25);
        transactionAttachment.mTransaction = 16L;
        transactionAttachment.mAttachment = 24L;
        transactionAttachment.mLastEdit = lastEdit(26);
        return transactionAttachment;
    }

    private static TransferAttachment transferAttachment() {
        TransferAttachment transferAttachment = new TransferAttachment();
        transferAttachment.mUUID = uuid(26);
        transferAttachment.mTransfer = 21L;
        transferAttachment.mAttachment = 24L;
        transferAttachment.mLastEdit = lastEdit(27);
        return transferAttachment;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////// HELPERS ///////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    private static JSONObject parse(String json) throws JSONException {
        Object value = new JSONTokener(json).nextValue();
        assertTrue("golden content should be a JSON object", value instanceof JSONObject);
        return (JSONObject) value;
    }

    private static String readResource(String path) throws Exception {
        InputStream in = JSONBackupGoldenTest.class.getResourceAsStream(path);
        assertNotNull("golden file not found on the test classpath: " + path, in);
        try {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            in.close();
        }
    }

    /** Returns the section as a section name so the failure messages stay readable. */
    private static JSONObject objectAt(JSONObject document, String section, int index) throws JSONException {
        return document.getJSONArray(section).getJSONObject(index);
    }

    private static int countKeys(JSONObject object) {
        int count = 0;
        for (Iterator<String> it = object.keys(); it.hasNext(); ) {
            it.next();
            count++;
        }
        return count;
    }

    private static TreeSet<String> keyNames(JSONObject object) {
        TreeSet<String> names = new TreeSet<>();
        for (Iterator<String> it = object.keys(); it.hasNext(); ) {
            names.add(it.next());
        }
        return names;
    }

    /**
     * Compares the two documents section by section so that a failure only reports the
     * records that actually changed instead of dumping the whole document. The values are
     * normalized to plain Java maps and lists because Robolectric's org.json objects do
     * not implement value equality.
     */
    private static String diff(JSONObject expected, JSONObject actual) throws JSONException {
        StringBuilder builder = new StringBuilder();
        for (String section : keyNames(expected)) {
            if (!actual.has(section)) {
                builder.append("missing section: ").append(section).append('\n');
            } else if (!toPlainValue(expected.get(section)).equals(toPlainValue(actual.get(section)))) {
                builder.append("section '").append(section).append("' differs\n")
                        .append("  expected: ").append(expected.get(section)).append('\n')
                        .append("  actual:   ").append(actual.get(section)).append('\n');
            }
        }
        for (String section : keyNames(actual)) {
            if (!expected.has(section)) {
                builder.append("unexpected section: ").append(section).append('\n');
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    /** Converts a parsed org.json tree into maps/lists/primitives so that equals() works by value. */
    private static Object toPlainValue(Object value) throws JSONException {
        if (value == JSONObject.NULL) {
            return null;
        } else if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Map<String, Object> plain = new LinkedHashMap<>();
            for (Iterator<String> it = object.keys(); it.hasNext(); ) {
                String key = it.next();
                plain.put(key, toPlainValue(object.get(key)));
            }
            return plain;
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            List<Object> plain = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                plain.add(toPlainValue(array.get(i)));
            }
            return plain;
        }
        return value;
    }
}
