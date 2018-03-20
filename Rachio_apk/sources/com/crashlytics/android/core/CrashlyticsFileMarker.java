package com.crashlytics.android.core;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.persistence.FileStore;
import java.io.File;
import java.io.IOException;

final class CrashlyticsFileMarker {
    private final FileStore fileStore;
    private final String markerName;

    public CrashlyticsFileMarker(String markerName, FileStore fileStore) {
        this.markerName = markerName;
        this.fileStore = fileStore;
    }

    public final boolean create() {
        boolean wasCreated = false;
        try {
            wasCreated = getMarkerFile().createNewFile();
        } catch (IOException e) {
            Fabric.getLogger().e("CrashlyticsCore", "Error creating marker: " + this.markerName, e);
        }
        return wasCreated;
    }

    public final boolean isPresent() {
        return getMarkerFile().exists();
    }

    public final boolean remove() {
        return getMarkerFile().delete();
    }

    private File getMarkerFile() {
        return new File(this.fileStore.getFilesDir(), this.markerName);
    }
}
