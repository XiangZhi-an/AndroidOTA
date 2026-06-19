package com.xza.ota.callback;

import java.io.File;

/**
 * OTA 下载更新回调
 */
public interface OtaDownloadCallback {

    /**
     * 下载进度
     *
     * @param percent  下载百分比 (0-100)
     * @param downloaded 已下载字节数
     * @param total     总字节数
     */
    void onProgress(int percent, long downloaded, long total);

    /**
     * 下载完成且校验通过
     *
     * @param apkFile 下载的 APK 文件
     */
    void onCompleted(File apkFile);

    /**
     * 下载或校验失败
     */
    void onError(Exception e);
}
