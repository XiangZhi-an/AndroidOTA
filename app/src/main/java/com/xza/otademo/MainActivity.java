package com.xza.otademo;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xza.otademo.BuildConfig;
import com.xza.ota.OtaManager;
import com.xza.ota.callback.OtaCheckCallback;
import com.xza.ota.callback.OtaDownloadCallback;
import com.xza.ota.model.OtaUpdateInfo;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String CHECK_URL = "https://api.56dz.com/SOFC/api/version?name=AndroidOTADEMO";

    private OtaManager otaManager;
    private OtaUpdateInfo pendingUpdate;

    private TextView tvCurrentVersion;
    private TextView tvStatus;
    private TextView tvUpdateInfo;
    private TextView tvProgress;
    private Button btnCheckUpdate;
    private Button btnDownload;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCurrentVersion = findViewById(R.id.tvCurrentVersion);
        tvStatus = findViewById(R.id.tvStatus);
        tvUpdateInfo = findViewById(R.id.tvUpdateInfo);
        tvProgress = findViewById(R.id.tvProgress);
        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        btnDownload = findViewById(R.id.btnDownload);
        progressBar = findViewById(R.id.progressBar);

        tvCurrentVersion.setText("当前版本: " + BuildConfig.VERSION_NAME);

        otaManager = new OtaManager.Builder()
                .context(this)
                .checkUrl(CHECK_URL)
                .currentVersionName(BuildConfig.VERSION_NAME)
                .build();

        btnCheckUpdate.setOnClickListener(v -> checkUpdate());
        btnDownload.setOnClickListener(v -> downloadAndInstall());
    }

    private void checkUpdate() {
        btnCheckUpdate.setEnabled(false);
        tvStatus.setText("状态: 正在检查更新...");
        tvUpdateInfo.setVisibility(View.GONE);
        btnDownload.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvProgress.setVisibility(View.GONE);

        otaManager.checkUpdate(new OtaCheckCallback() {
            @Override
            public void onUpdateAvailable(OtaUpdateInfo updateInfo) {
                runOnUiThread(() -> {
                    pendingUpdate = updateInfo;
                    tvStatus.setText("状态: 发现新版本");
                    String info = "新版本: " + updateInfo.getVersionNo()
                            + " (" + updateInfo.getVersionName() + ")\n"
                            + "文件大小: " + formatFileSize(updateInfo.getFileSize()) + "\n"
                            + "校验方式: " + updateInfo.getChecksumType() + "\n"
                            + "强制更新: " + (updateInfo.isForceUpdate() ? "是" : "否") + "\n"
                            + "发布时间: " + updateInfo.getPublishedAt();
                    List<String> notes = updateInfo.getReleaseNotes();
                    if (!notes.isEmpty()) {
                        info += "\n更新说明:\n";
                        for (String note : notes) {
                            info += "  - " + note + "\n";
                        }
                    }
                    tvUpdateInfo.setText(info);
                    tvUpdateInfo.setVisibility(View.VISIBLE);
                    btnDownload.setVisibility(View.VISIBLE);
                    btnCheckUpdate.setEnabled(true);
                });
            }

            @Override
            public void onNoUpdate() {
                runOnUiThread(() -> {
                    tvStatus.setText("状态: 已是最新版本");
                    btnCheckUpdate.setEnabled(true);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText("状态: 检查失败 - " + e.getMessage());
                    btnCheckUpdate.setEnabled(true);
                });
            }
        });
    }

    private void downloadAndInstall() {
        if (pendingUpdate == null) return;

        btnDownload.setEnabled(false);
        btnCheckUpdate.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        tvProgress.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvStatus.setText("状态: 正在下载...");

        otaManager.downloadUpdate(pendingUpdate, new OtaDownloadCallback() {
            @Override
            public void onProgress(int percent, long downloaded, long total) {
                runOnUiThread(() -> {
                    progressBar.setProgress(percent);
                    tvProgress.setText(formatFileSize(downloaded) + " / " + formatFileSize(total)
                            + " (" + percent + "%)");
                });
            }

            @Override
            public void onCompleted(File apkFile) {
                runOnUiThread(() -> {
                    tvStatus.setText("状态: 下载完成，校验通过，正在安装...");
                    progressBar.setProgress(100);
                    otaManager.installUpdate(apkFile);
                    btnDownload.setEnabled(true);
                    btnCheckUpdate.setEnabled(true);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    tvStatus.setText("状态: 下载失败 - " + e.getMessage());
                    btnDownload.setEnabled(true);
                    btnCheckUpdate.setEnabled(true);
                });
            }
        });
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
