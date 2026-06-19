package com.xza.ota.callback;

import com.xza.ota.model.OtaUpdateInfo;

/**
 * OTA 检查更新回调
 */
public interface OtaCheckCallback {

    /**
     * 发现新版本
     */
    void onUpdateAvailable(OtaUpdateInfo updateInfo);

    /**
     * 当前已是最新版本
     */
    void onNoUpdate();

    /**
     * 检查更新失败
     */
    void onError(Exception e);
}
