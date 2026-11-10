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

package com.amesingularity.rabbitwallet.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.amesingularity.rabbitwallet.api.BackendServiceFactory;
import com.amesingularity.rabbitwallet.model.IFile;
import com.amesingularity.rabbitwallet.storage.preference.BackendManager;
import com.amesingularity.rabbitwallet.storage.preference.PreferenceManager;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Checks which automatic backups are due and enqueues them. The next check is scheduled again
 * every time the worker runs, so that the chain never stops.
 */
public class AutoBackupWorker extends Worker {

    private static final String WORK_NAME = "auto_backup";
    private static final int MILLIS_IN_HOUR = 1000 * 60 * 60;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * Cancel the pending check and schedule a new one according to the current settings.
     */
    public static void schedule(Context context) {
        WorkManager workManager = WorkManager.getInstance(context);
        workManager.cancelUniqueWork(WORK_NAME);
        enqueueNextCheck(context, workManager, ExistingWorkPolicy.REPLACE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Set<String> backendIdSet = BackendManager.getAutoBackupEnabledServices();
        if (backendIdSet != null) {
            for (String backendId : backendIdSet) {
                long lastTimestamp = BackendManager.getAutoBackupLastTime(backendId);
                long nextOccurrence = getNextOccurrence(backendId, lastTimestamp);
                if (nextOccurrence <= System.currentTimeMillis()) {
                    if (!BackendManager.isAutoBackupWhenDataIsChangedOnly(backendId)
                            || PreferenceManager.getLastTimeDataIsChanged() > lastTimestamp) {
                        IFile folder = BackendServiceFactory.getFile(backendId, BackendManager.getAutoBackupFolder(backendId));
                        // build the intent to start the backup
                        Intent intent = new Intent();
                        intent.putExtra(BackupWorker.ACTION, BackupWorker.ACTION_BACKUP);
                        intent.putExtra(BackupWorker.BACKEND_ID, backendId);
                        intent.putExtra(BackupWorker.AUTO_BACKUP, true);
                        intent.putExtra(BackupWorker.ONLY_ON_WIFI, BackendManager.isAutoBackupOnWiFiOnly(backendId));
                        intent.putExtra(BackupWorker.PARENT_FOLDER, folder);
                        intent.putExtra(BackupWorker.PASSWORD, BackendManager.getAutoBackupPassword(backendId));
                        BackupWorker.enqueueInForeground(context, intent);
                    }
                    // register the next occurrence as the last time the auto backup
                    // for this specific backend has been executed: if an error occur
                    // and the backup process is interrupted, the error is shown in
                    // a specific notification (no auto rescheduling because we don't
                    // know if the error is a recoverable error or a critical one)
                    BackendManager.setAutoBackupLastTime(backendId, nextOccurrence);
                }
            }
        }
        // schedule the next check as a new work appended to the current chain: the work that is
        // running now must not be cancelled, otherwise its result would be discarded
        enqueueNextCheck(context, WorkManager.getInstance(context), ExistingWorkPolicy.APPEND_OR_REPLACE);
        return Result.success();
    }

    private static void enqueueNextCheck(Context context, WorkManager workManager, ExistingWorkPolicy policy) {
        Set<String> backendIdSet = BackendManager.getAutoBackupEnabledServices();
        if (backendIdSet == null || backendIdSet.isEmpty()) {
            return;
        }
        Long nextTimestamp = null;
        for (String backendId : backendIdSet) {
            long nextOccurrence = getNextOccurrence(backendId, BackendManager.getAutoBackupLastTime(backendId));
            if (nextTimestamp == null || nextOccurrence < nextTimestamp) {
                nextTimestamp = nextOccurrence;
            }
        }
        if (nextTimestamp == null) {
            return;
        }
        long delay = Math.max(0L, nextTimestamp - System.currentTimeMillis());
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AutoBackupWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();
        workManager.enqueueUniqueWork(WORK_NAME, policy, request);
    }

    private static long getNextOccurrence(String backendId, long lastTimestamp) {
        int hourOffset = BackendManager.getAutoBackupHoursOffset(backendId);
        return lastTimestamp + ((long) hourOffset * MILLIS_IN_HOUR);
    }
}
