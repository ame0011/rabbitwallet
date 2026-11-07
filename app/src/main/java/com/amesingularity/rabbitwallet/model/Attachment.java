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

package com.amesingularity.rabbitwallet.model;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.UUID;

/**
 * Created by andrea on 27/03/18.
 */
public class Attachment implements Parcelable, Identifiable {

    public static final String FOLDER_NAME = "attachments";

    private long mId;
    private final String mFile;
    private final String mName;
    private final String mType;
    private final long mSize;

    private Status mStatus;

    public Attachment(long id, String file, String name, String type, long size) {
        mId = id;
        mFile = file;
        mName = name;
        mType = type;
        mSize = size;
        mStatus = Status.READY;
    }

    public Attachment(long id, String file, String name, String type, long size, Status status) {
        mId = id;
        mFile = file;
        mName = name;
        mType = type;
        mSize = size;
        mStatus = status;
    }

    private Attachment(Parcel in) {
        mId = in.readLong();
        mFile = in.readString();
        mName = in.readString();
        mType = in.readString();
        mSize = in.readLong();
        mStatus = (Status) in.readSerializable();
    }

    @Override
    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getFile() {
        return mFile;
    }

    public String getName() {
        return mName;
    }

    public String getType() {
        return mType;
    }

    public long getSize() {
        return mSize;
    }

    public Status getStatus() {
        return mStatus;
    }

    public void setStatus(Status status, String error) {
        mStatus = status;
        // TODO handle error
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {

        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mFile);
        dest.writeString(mName);
        dest.writeString(mType);
        dest.writeLong(mSize);
        dest.writeSerializable(mStatus);
    }

    public enum Status {
        PENDING,
        READY,
        ERROR
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Attachment && mFile.equals(((Attachment) object).mFile);
    }

    @Override
    public int hashCode() {
        return mFile.hashCode();
    }

    public Uri getUri(Context context) {
        String authority = context.getPackageName() + ".storage.file";
        File folder = new File(context.getExternalFilesDir(null), FOLDER_NAME);
        File attachment = new File(folder, mFile);
        return FileProvider.getUriForFile(context, authority, attachment);
    }

    public Intent getActionViewIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(getUri(context), mType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return intent;
    }

    public static String generateFileUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}