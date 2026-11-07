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

package com.amesingularity.rabbitwallet.ui.view;

import android.content.ActivityNotFoundException;
import android.content.Context;

import android.text.TextUtils;

import com.amesingularity.rabbitwallet.R;
import com.amesingularity.rabbitwallet.model.Attachment;
import com.amesingularity.rabbitwallet.ui.view.theme.ThemedDialog;

/**
 * Attachment の UI 操作(ビューア起動とアイコン選択)を扱うヘルパー。
 * model 層が UI 層に依存しないよう、Attachment から切り出しました。
 */
public class AttachmentActions {

    private AttachmentActions() {
        // static-only
    }

    /**
     * 添付ファイルを起動します。対応する Activity がない場合はエラーダイアログを表示します。
     */
    public static void openAttachment(Context context, Attachment attachment) {
        try {
            context.startActivity(attachment.getActionViewIntent(context));
        } catch (ActivityNotFoundException e) {
            ThemedDialog.buildMaterialDialog(context)
                    .title(R.string.title_error)
                    .content(R.string.message_error_activity_not_found)
                    .positiveText(android.R.string.ok)
                    .show();
        }
    }

    /**
     * MIME タイプに応じたアイコンのリソース ID を返します。
     */
    public static int getIconResByType(String type) {
        if (!TextUtils.isEmpty(type)) {
            switch (type) {
                case "application/pdf":
                    return R.drawable.ic_file_pdf_24dp;
                case "text/plain":
                    return R.drawable.ic_file_document_24dp;
                case "image/png":
                case "image/jpg":
                case "image/jpeg":
                case "image/gif":
                case "image/bmp":
                    return R.drawable.ic_file_image_24dp;
                case "video/mp4":
                    return R.drawable.ic_file_video_24dp;
            }
        }
        return R.drawable.ic_file_outline_24dp;
    }
}
