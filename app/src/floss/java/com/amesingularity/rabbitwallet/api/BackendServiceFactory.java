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
import com.amesingularity.rabbitwallet.api.saf.SAFBackendService;
import com.amesingularity.rabbitwallet.api.saf.SAFBackendServiceAPI;
import com.amesingularity.rabbitwallet.model.BackupService;
import com.amesingularity.rabbitwallet.model.IFile;
import com.amesingularity.rabbitwallet.model.SAFFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andrea on 21/11/18.
 */
public class BackendServiceFactory {

    public static final String SERVICE_ID_SAF = "storage_access_framework";

    public static AbstractBackendServiceDelegate getServiceById(String backendId, AbstractBackendServiceDelegate.BackendServiceStatusListener listener) {
        if (SERVICE_ID_SAF.equals(backendId)) {
            return new SAFBackendService(listener);
        }
        return null;
    }

    public static IBackendServiceAPI getServiceAPIById(Context context, String backendId) throws BackendException {
        if (SERVICE_ID_SAF.equals(backendId)) {
            return new SAFBackendServiceAPI(context);
        }
        throw new BackendException("Invalid backend");
    }

    public static List<BackupService> getBackupServices() {
        List<BackupService> services = new ArrayList<>();
        services.add(new BackupService(SERVICE_ID_SAF, R.drawable.ic_storage_black_24dp, R.string.service_backup_storage_access_framework));
        return services;
    }

    public static IFile getFile(String backendId, String encoded) {
        if (encoded != null && SERVICE_ID_SAF.equals(backendId)) {
            return new SAFFile(encoded);
        }
        return null;
    }
}
