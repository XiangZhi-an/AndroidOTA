package com.xza.ota.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * OTA 更新信息模型，对应服务端 API 响应
 */
public class OtaUpdateInfo {

    private String software;
    private String versionName;
    private int[] versionCode;
    private String versionNo;
    private String downloadUrl;
    private long fileSize;
    private String checksum;
    private String checksumType;
    private boolean forceUpdate;
    private List<String> releaseNotes;
    private String publishedAt;

    public OtaUpdateInfo() {
        releaseNotes = new ArrayList<>();
    }

    /**
     * 从 JSON 对象解析 OtaUpdateInfo
     */
    public static OtaUpdateInfo fromJson(JSONObject json) throws JSONException {
        OtaUpdateInfo info = new OtaUpdateInfo();
        info.software = json.optString("software", "");
        info.versionName = json.optString("version_name", "");
        info.versionNo = json.optString("version_no", "");

        JSONArray codeArray = json.optJSONArray("version_code");
        if (codeArray != null) {
            info.versionCode = new int[codeArray.length()];
            for (int i = 0; i < codeArray.length(); i++) {
                info.versionCode[i] = codeArray.getInt(i);
            }
        } else {
            info.versionCode = new int[0];
        }

        info.downloadUrl = json.optString("download_url", "");
        info.fileSize = json.optLong("file_size", 0);
        info.checksum = json.optString("checksum", "");
        info.checksumType = json.optString("checksum_type", "");
        info.forceUpdate = json.optBoolean("force_update", false);

        JSONArray notesArray = json.optJSONArray("release_notes");
        if (notesArray != null) {
            for (int i = 0; i < notesArray.length(); i++) {
                info.releaseNotes.add(notesArray.getString(i));
            }
        }

        info.publishedAt = json.optString("published_at", "");
        return info;
    }

    /**
     * 比较版本号，判断是否需要更新
     *
     * @param currentVersionCode 当前版本号数组
     * @return true 表示有新版本可更新
     */
    public boolean isNewerThan(int[] currentVersionCode) {
        if (currentVersionCode == null || versionCode == null) return false;
        int len = Math.min(currentVersionCode.length, versionCode.length);
        for (int i = 0; i < len; i++) {
            if (versionCode[i] > currentVersionCode[i]) return true;
            if (versionCode[i] < currentVersionCode[i]) return false;
        }
        return versionCode.length > currentVersionCode.length;
    }

    // Getters
    public String getSoftware() { return software; }
    public String getVersionName() { return versionName; }
    public int[] getVersionCode() { return versionCode; }
    public String getVersionNo() { return versionNo; }
    public String getDownloadUrl() { return downloadUrl; }
    public long getFileSize() { return fileSize; }
    public String getChecksum() { return checksum; }
    public String getChecksumType() { return checksumType; }
    public boolean isForceUpdate() { return forceUpdate; }
    public List<String> getReleaseNotes() { return releaseNotes; }
    public String getPublishedAt() { return publishedAt; }

    @Override
    public String toString() {
        return "OtaUpdateInfo{" +
                "software='" + software + '\'' +
                ", versionName='" + versionName + '\'' +
                ", versionNo='" + versionNo + '\'' +
                ", fileSize=" + fileSize +
                ", forceUpdate=" + forceUpdate +
                '}';
    }
}
