package com.xza.ota.internal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * OTA 安装器，负责触发 APK 安装
 */
public class OtaInstaller {

    /**
     * 安装 APK 文件
     *
     * @param context 上下文
     * @param apkFile APK 文件
     */
    public static void install(Context context, File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Uri uri;
        uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".ota.fileprovider", apkFile);

        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}
