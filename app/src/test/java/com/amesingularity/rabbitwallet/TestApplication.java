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

package com.amesingularity.rabbitwallet;

import android.app.Application;

import com.amesingularity.rabbitwallet.storage.preference.PreferenceManager;
import com.amesingularity.rabbitwallet.ui.view.theme.ThemeEngine;

/**
 * A minimal {@link Application} used by the Robolectric unit tests. The real {@link App} performs
 * a lot of side effects in {@code onCreate} (worker scheduling, alarm timers, notification
 * channels) that would make the plain unit tests slow and hardly deterministic, so only the
 * managers needed by the code under test are initialized here.
 */
public class TestApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.initialize(this);
        ThemeEngine.initialize(this);
    }
}
