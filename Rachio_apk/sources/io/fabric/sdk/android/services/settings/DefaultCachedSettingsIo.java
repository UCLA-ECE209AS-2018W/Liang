package io.fabric.sdk.android.services.settings;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;
import io.fabric.sdk.android.services.common.CommonUtils;
import io.fabric.sdk.android.services.persistence.FileStoreImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import org.json.JSONObject;

final class DefaultCachedSettingsIo implements CachedSettingsIo {
    private final Kit kit;

    public DefaultCachedSettingsIo(Kit kit) {
        this.kit = kit;
    }

    public final JSONObject readCachedSettings() {
        Exception e;
        Throwable th;
        Fabric.getLogger().d("Fabric", "Reading cached settings...");
        FileInputStream fis = null;
        JSONObject toReturn = null;
        try {
            File settingsFile = new File(new FileStoreImpl(this.kit).getFilesDir(), "com.crashlytics.settings.json");
            if (settingsFile.exists()) {
                FileInputStream fis2 = new FileInputStream(settingsFile);
                try {
                    toReturn = new JSONObject(CommonUtils.streamToString(fis2));
                    fis = fis2;
                } catch (Exception e2) {
                    e = e2;
                    fis = fis2;
                    try {
                        Fabric.getLogger().e("Fabric", "Failed to fetch cached settings", e);
                        CommonUtils.closeOrLog(fis, "Error while closing settings cache file.");
                        return toReturn;
                    } catch (Throwable th2) {
                        th = th2;
                        CommonUtils.closeOrLog(fis, "Error while closing settings cache file.");
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    fis = fis2;
                    CommonUtils.closeOrLog(fis, "Error while closing settings cache file.");
                    throw th;
                }
            }
            Fabric.getLogger().d("Fabric", "No cached settings found.");
            CommonUtils.closeOrLog(fis, "Error while closing settings cache file.");
        } catch (Exception e3) {
            e = e3;
            Fabric.getLogger().e("Fabric", "Failed to fetch cached settings", e);
            CommonUtils.closeOrLog(fis, "Error while closing settings cache file.");
            return toReturn;
        }
        return toReturn;
    }

    public final void writeCachedSettings(long expiresAtMillis, JSONObject settingsJson) {
        Exception e;
        Throwable th;
        Fabric.getLogger().d("Fabric", "Writing settings to cache file...");
        if (settingsJson != null) {
            FileWriter writer = null;
            try {
                settingsJson.put("expires_at", expiresAtMillis);
                FileWriter writer2 = new FileWriter(new File(new FileStoreImpl(this.kit).getFilesDir(), "com.crashlytics.settings.json"));
                try {
                    writer2.write(settingsJson.toString());
                    writer2.flush();
                    CommonUtils.closeOrLog(writer2, "Failed to close settings writer.");
                } catch (Exception e2) {
                    e = e2;
                    writer = writer2;
                    try {
                        Fabric.getLogger().e("Fabric", "Failed to cache settings", e);
                        CommonUtils.closeOrLog(writer, "Failed to close settings writer.");
                    } catch (Throwable th2) {
                        th = th2;
                        CommonUtils.closeOrLog(writer, "Failed to close settings writer.");
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    writer = writer2;
                    CommonUtils.closeOrLog(writer, "Failed to close settings writer.");
                    throw th;
                }
            } catch (Exception e3) {
                e = e3;
                Fabric.getLogger().e("Fabric", "Failed to cache settings", e);
                CommonUtils.closeOrLog(writer, "Failed to close settings writer.");
            }
        }
    }
}
