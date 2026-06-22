# AndroidOTA

Android OTA 更新库，支持检查更新、下载 APK、校验完整性、触发安装。

## 功能

- 检查服务端版本，自动对比本地版本号
- 下载 APK 并实时回调进度
- 支持 SHA256 等校验，确保文件完整性
- 支持强制更新标识
- 支持取消下载
- 零外部依赖，纯 Java 实现

## 引入

### 1. 添加 JitPack 仓库

在 `settings.gradle.kts` 的 `dependencyResolutionManagement.repositories` 中添加：

```kotlin
maven { setUrl("https://jitpack.io") }
```

### 2. 添加依赖

```kotlin
implementation("com.github.XiangZhi-an:AndroidOTA:v1.0.1")
```

## 服务端接口

库需要服务端提供一个版本查询接口，返回 JSON 格式如下：

```json
{
  "software": "AndroidOTA",
  "version_name": "快捷新建",
  "version_code": [1, 0, 0],
  "version_no": "1.0.0",
  "download_url": "http://example.com/app.apk",
  "file_size": 4647118,
  "checksum": "0cc3891926d6fe8e17b78e4ec185e1d4...",
  "checksum_type": "sha256",
  "force_update": false,
  "release_notes": ["修复了若干问题", "优化性能"],
  "published_at": "2026-06-19 13:56:39"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| software | String | 软件标识 |
| version_name | String | 版本名称 |
| version_code | Array\<Integer\> | 版本号数组，用于版本比较 |
| version_no | String | 版本号字符串 |
| download_url | String | APK 下载地址 |
| file_size | Integer | 文件大小（字节） |
| checksum | String | 文件校验值 |
| checksum_type | String | 校验类型（如 sha256） |
| force_update | Boolean | 是否强制更新 |
| release_notes | Array\<String\> | 更新说明 |
| published_at | String | 发布时间 |

## 使用

### 1. 添加权限

在 `AndroidManifest.xml` 中添加：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

在 `<application>` 标签中添加：

```xml
android:usesCleartextTraffic="true"
```

### 2. 初始化

```java
OtaManager otaManager = new OtaManager.Builder()
    .context(this)
    .checkUrl("http://your-server/api/version?name=YourAppName")
    .currentVersionName(BuildConfig.VERSION_NAME)
    .build();
```

也可以直接传版本号数组：

```java
.currentVersionCode(new int[]{1, 0, 0})
```

### 3. 检查更新

```java
otaManager.checkUpdate(new OtaCheckCallback() {
    @Override
    public void onUpdateAvailable(OtaUpdateInfo updateInfo) {
        // 发现新版本
    }

    @Override
    public void onNoUpdate() {
        // 已是最新版本
    }

    @Override
    public void onError(Exception e) {
        // 检查失败
    }
});
```

### 4. 下载并安装

```java
otaManager.downloadUpdate(updateInfo, new OtaDownloadCallback() {
    @Override
    public void onProgress(int percent, long downloaded, long total) {
        // 下载进度：percent(0-100), downloaded(已下载字节), total(总字节)
    }

    @Override
    public void onCompleted(File apkFile) {
        // 下载完成且校验通过，触发安装
        otaManager.installUpdate(apkFile);
    }

    @Override
    public void onError(Exception e) {
        // 下载失败或校验不通过
    }
});
```

### 5. 取消下载

```java
otaManager.cancelDownload();
```

## API 参考

### OtaManager.Builder

| 方法 | 说明 |
|------|------|
| `context(Context)` | 上下文（必填） |
| `checkUrl(String)` | 检查更新的完整 URL（必填） |
| `currentVersionName(String)` | 当前版本名，如 `"1.0.0"`，自动解析为数组 |
| `currentVersionCode(int[])` | 当前版本号数组，如 `new int[]{1, 0, 0}` |

### OtaManager

| 方法 | 说明 |
|------|------|
| `checkUpdate(OtaCheckCallback)` | 检查更新 |
| `downloadUpdate(OtaUpdateInfo, OtaDownloadCallback)` | 下载更新 |
| `installUpdate(File)` | 安装 APK |
| `cancelDownload()` | 取消当前下载 |

### OtaUpdateInfo

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getSoftware()` | String | 软件标识 |
| `getVersionName()` | String | 版本名称 |
| `getVersionCode()` | int[] | 版本号数组 |
| `getVersionNo()` | String | 版本号字符串 |
| `getDownloadUrl()` | String | 下载地址 |
| `getFileSize()` | long | 文件大小（字节） |
| `getChecksum()` | String | 校验值 |
| `getChecksumType()` | String | 校验类型 |
| `isForceUpdate()` | boolean | 是否强制更新 |
| `getReleaseNotes()` | List\<String\> | 更新说明 |
| `getPublishedAt()` | String | 发布时间 |
| `isNewerThan(int[])` | boolean | 是否比指定版本号更新 |

## 注意事项

- 回调在子线程执行，UI 操作需切换到主线程
- 下载的 APK 保存在应用缓存目录，安装后可手动清理
- 需要用户手动授予安装未知来源应用权限（Android 8.0+）

## Demo

项目中的 `app` 模块是一个完整的演示示例，展示了检查更新、显示更新信息、下载进度、安装的完整流程。

## License

MIT
