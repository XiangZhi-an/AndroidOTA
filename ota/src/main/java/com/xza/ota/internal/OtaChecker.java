package com.xza.ota.internal;

import android.util.Log;

import com.xza.ota.callback.OtaCheckCallback;
import com.xza.ota.model.OtaUpdateInfo;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * OTA 更新检查器，负责请求服务端版本接口
 */
public class OtaChecker {

    private static final String TAG = "OtaChecker";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;

    private final String checkUrl;
    private final int[] currentVersionCode;

    public OtaChecker(String checkUrl, int[] currentVersionCode) {
        this.checkUrl = checkUrl;
        this.currentVersionCode = currentVersionCode;
    }

    /**
     * 在当前线程执行检查（调用方需在子线程调用）
     */
    public void check(OtaCheckCallback callback) {
        HttpURLConnection connection = null;
        try {
            Log.d(TAG, "请求地址: " + checkUrl);
            URL url = new URL(checkUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "响应码: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                // 读取错误响应体
                String errorBody = "";
                try {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder esb = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        esb.append(line);
                    }
                    errorReader.close();
                    errorBody = esb.toString();
                } catch (Exception ignored) {}
                Log.e(TAG, "请求失败, HTTP " + responseCode + ", 响应: " + errorBody);
                callback.onError(new Exception("HTTP error: " + responseCode + ", 响应: " + errorBody));
                return;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String responseBody = sb.toString();
            Log.d(TAG, "响应内容: " + responseBody);

            JSONObject json = new JSONObject(responseBody);
            OtaUpdateInfo updateInfo = OtaUpdateInfo.fromJson(json);
            Log.d(TAG, "解析结果: " + updateInfo);

            if (updateInfo.isNewerThan(currentVersionCode)) {
                Log.i(TAG, "发现新版本: " + updateInfo.getVersionNo());
                callback.onUpdateAvailable(updateInfo);
            } else {
                Log.i(TAG, "已是最新版本");
                callback.onNoUpdate();
            }
        } catch (Exception e) {
            Log.e(TAG, "检查更新异常", e);
            callback.onError(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
