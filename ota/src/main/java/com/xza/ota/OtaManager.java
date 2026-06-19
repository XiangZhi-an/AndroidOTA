package com.xza.ota;

import android.content.Context;

import com.xza.ota.callback.OtaCheckCallback;
import com.xza.ota.callback.OtaDownloadCallback;
import com.xza.ota.internal.OtaChecker;
import com.xza.ota.internal.OtaDownloader;
import com.xza.ota.internal.OtaInstaller;
import com.xza.ota.model.OtaUpdateInfo;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OTA 管理器，提供检查更新、下载更新、安装更新的能力
 * <p>
 * 使用示例：
 * <pre>
 * OtaManager manager = new OtaManager.Builder()
 *     .context(context)
 *     .checkUrl("http://127.0.0.1:8008/api/version?name=AndroidOTA")
 *     .currentVersionName(BuildConfig.VERSION_NAME)
 *     .build();
 *
 * manager.checkUpdate(callback);
 * </pre>
 */
public class OtaManager {

    private final Context context;
    private final String checkUrl;
    private final int[] currentVersionCode;
    private final ExecutorService executor;
    private OtaDownloader currentDownloader;

    private OtaManager(Builder builder) {
        this.context = builder.context.getApplicationContext();
        this.checkUrl = builder.checkUrl;
        this.currentVersionCode = builder.currentVersionCode;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * 检查更新
     */
    public void checkUpdate(OtaCheckCallback callback) {
        OtaChecker checker = new OtaChecker(checkUrl, currentVersionCode);
        executor.execute(() -> checker.check(callback));
    }

    /**
     * 下载更新
     *
     * @param updateInfo 更新信息
     * @param callback   下载回调
     */
    public void downloadUpdate(OtaUpdateInfo updateInfo, OtaDownloadCallback callback) {
        File destFile = new File(context.getExternalCacheDir(), "ota_update.apk");
        currentDownloader = new OtaDownloader();
        executor.execute(() -> currentDownloader.download(updateInfo, destFile, callback));
    }

    /**
     * 取消当前下载
     */
    public void cancelDownload() {
        if (currentDownloader != null) {
            currentDownloader.cancel();
        }
    }

    /**
     * 安装 APK
     *
     * @param apkFile APK 文件
     */
    public void installUpdate(File apkFile) {
        OtaInstaller.install(context, apkFile);
    }

    /**
     * 便捷方法：检查更新并自动下载安装
     *
     * @param checkCallback  检查回调
     * @param downloadCallback 下载回调
     */
    public void checkAndDownload(OtaCheckCallback checkCallback, OtaDownloadCallback downloadCallback) {
        checkUpdate(new OtaCheckCallback() {
            @Override
            public void onUpdateAvailable(OtaUpdateInfo updateInfo) {
                checkCallback.onUpdateAvailable(updateInfo);
                downloadUpdate(updateInfo, downloadCallback);
            }

            @Override
            public void onNoUpdate() {
                checkCallback.onNoUpdate();
            }

            @Override
            public void onError(Exception e) {
                checkCallback.onError(e);
            }
        });
    }

    /**
     * OtaManager 构建器
     */
    public static class Builder {
        private Context context;
        private String checkUrl;
        private int[] currentVersionCode;

        /**
         * 设置上下文
         */
        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        /**
         * 设置检查更新的完整 URL，如 "http://127.0.0.1:8008/api/version?name=AndroidOTA"
         */
        public Builder checkUrl(String checkUrl) {
            this.checkUrl = checkUrl;
            return this;
        }

        /**
         * 设置当前版本号数组，如 new int[]{1, 0, 0}
         */
        public Builder currentVersionCode(int[] currentVersionCode) {
            this.currentVersionCode = currentVersionCode;
            return this;
        }

        /**
         * 设置当前版本名称，如 "1.0.0"，将自动解析为版本号数组
         * 可直接传入 BuildConfig.VERSION_NAME
         */
        public Builder currentVersionName(String versionName) {
            this.currentVersionCode = parseVersionName(versionName);
            return this;
        }

        private static int[] parseVersionName(String versionName) {
            if (versionName == null || versionName.isEmpty()) {
                return new int[0];
            }
            String[] parts = versionName.split("\\.");
            int[] code = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    code[i] = Integer.parseInt(parts[i]);
                } catch (NumberFormatException e) {
                    code[i] = 0;
                }
            }
            return code;
        }

        public OtaManager build() {
            if (context == null) {
                throw new IllegalStateException("context is required");
            }
            if (checkUrl == null || checkUrl.isEmpty()) {
                throw new IllegalStateException("checkUrl is required");
            }
            if (currentVersionCode == null || currentVersionCode.length == 0) {
                throw new IllegalStateException("currentVersionCode is required");
            }
            return new OtaManager(this);
        }
    }
}
