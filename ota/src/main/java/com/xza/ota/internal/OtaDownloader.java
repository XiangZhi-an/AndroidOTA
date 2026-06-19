package com.xza.ota.internal;

import android.util.Log;

import com.xza.ota.callback.OtaDownloadCallback;
import com.xza.ota.model.OtaUpdateInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * OTA 下载器，负责下载 APK 文件并校验
 */
public class OtaDownloader {

    private static final String TAG = "OtaDownloader";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int BUFFER_SIZE = 8192;

    private volatile boolean cancelled = false;

    /**
     * 在当前线程执行下载（调用方需在子线程调用）
     */
    public void download(OtaUpdateInfo updateInfo, File destFile, OtaDownloadCallback callback) {
        HttpURLConnection connection = null;
        try {
            String downloadUrl = updateInfo.getDownloadUrl();
            Log.d(TAG, "开始下载: " + downloadUrl);
            Log.d(TAG, "保存路径: " + destFile.getAbsolutePath());

            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "下载响应码: " + responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "下载失败, HTTP " + responseCode);
                callback.onError(new Exception("HTTP error: " + responseCode));
                return;
            }

            long totalSize = updateInfo.getFileSize();
            if (totalSize <= 0) {
                String contentLength = connection.getHeaderField("Content-Length");
                if (contentLength != null) {
                    totalSize = Long.parseLong(contentLength);
                }
            }
            Log.d(TAG, "文件大小: " + totalSize + " bytes");

            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 下载到临时文件
            File tempFile = new File(destFile.getParent(), destFile.getName() + ".tmp");

            MessageDigest digest = null;
            String checksumType = updateInfo.getChecksumType();
            if (checksumType != null && !checksumType.isEmpty()) {
                digest = MessageDigest.getInstance(checksumType.toUpperCase());
            }

            InputStream input = connection.getInputStream();
            FileOutputStream output = new FileOutputStream(tempFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            int lastPercent = -1;

            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                if (cancelled) {
                    input.close();
                    output.close();
                    tempFile.delete();
                    callback.onError(new Exception("Download cancelled"));
                    return;
                }

                output.write(buffer, 0, bytesRead);
                if (digest != null) {
                    digest.update(buffer, 0, bytesRead);
                }
                downloaded += bytesRead;

                int percent = totalSize > 0 ? (int) (downloaded * 100 / totalSize) : 0;
                if (percent != lastPercent) {
                    lastPercent = percent;
                    callback.onProgress(percent, downloaded, totalSize);
                }
            }

            output.flush();
            output.close();
            input.close();

            // 校验文件
            if (digest != null && updateInfo.getChecksum() != null && !updateInfo.getChecksum().isEmpty()) {
                String computedChecksum = bytesToHex(digest.digest());
                Log.d(TAG, "校验类型: " + checksumType + ", 期望: " + updateInfo.getChecksum() + ", 实际: " + computedChecksum);
                if (!computedChecksum.equalsIgnoreCase(updateInfo.getChecksum())) {
                    tempFile.delete();
                    Log.e(TAG, "校验失败!");
                    callback.onError(new Exception("Checksum mismatch: expected " + updateInfo.getChecksum()
                            + ", got " + computedChecksum));
                    return;
                }
                Log.i(TAG, "校验通过");
            }

            // 重命名临时文件为目标文件
            if (destFile.exists()) {
                destFile.delete();
            }
            if (!tempFile.renameTo(destFile)) {
                // 如果重命名失败，尝试复制
                copyFile(tempFile, destFile);
                tempFile.delete();
            }

            callback.onCompleted(destFile);
            Log.i(TAG, "下载完成: " + destFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "下载异常", e);
            callback.onError(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 取消下载
     */
    public void cancel() {
        cancelled = true;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void copyFile(File src, File dst) throws Exception {
        java.io.FileInputStream fis = new java.io.FileInputStream(src);
        java.io.FileOutputStream fos = new java.io.FileOutputStream(dst);
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }
        fos.flush();
        fos.close();
        fis.close();
    }
}
