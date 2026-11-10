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

package com.amesingularity.rabbitwallet;

import android.app.Application;
import android.content.Context;

import com.amesingularity.rabbitwallet.service.AutoBackupWorker;
import com.amesingularity.rabbitwallet.broadcast.DailyBroadcastReceiver;
import com.amesingularity.rabbitwallet.broadcast.RecurrenceBroadcastReceiver;
import com.amesingularity.rabbitwallet.storage.preference.BackendManager;
import com.amesingularity.rabbitwallet.storage.preference.PreferenceManager;
import com.amesingularity.rabbitwallet.ui.notification.NotificationContract;
import com.amesingularity.rabbitwallet.ui.view.theme.ThemeEngine;
import com.amesingularity.rabbitwallet.utils.CurrencyManager;

import me.weishu.reflection.Reflection;

/**
 * Created by andrea on 17/01/18.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.initialize(this);
        BackendManager.initialize(this);
        ThemeEngine.initialize(this);
        CurrencyManager.initialize(this);
        NotificationContract.initializeNotificationChannels(this);
        initializeScheduledTimers();
    }

    private void initializeScheduledTimers() {
        // The application may be killed by the OS when resources are needed or by the user for
        // every kind of reasons. When the application is killed all the scheduled operations are
        // canceled by the OS. This is the best place where all those things can be scheduled again.
        DailyBroadcastReceiver.scheduleDailyNotification(this);
        RecurrenceBroadcastReceiver.scheduleRecurrenceTask(this);
        AutoBackupWorker.schedule(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);
    }
}