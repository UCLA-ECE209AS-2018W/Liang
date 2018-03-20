package com.google.android.gms.common.internal;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.google.android.gms.common.stats.zza;
import java.util.HashSet;
import java.util.Set;

final class zzah implements ServiceConnection {
    private int mState = 2;
    private ComponentName zzaHO;
    private final Set<ServiceConnection> zzaHT = new HashSet();
    private boolean zzaHU;
    private final zzaf zzaHV;
    private /* synthetic */ zzag zzaHW;
    private IBinder zzaHj;

    public zzah(zzag zzag, zzaf zzaf) {
        this.zzaHW = zzag;
        this.zzaHV = zzaf;
    }

    public final IBinder getBinder() {
        return this.zzaHj;
    }

    public final ComponentName getComponentName() {
        return this.zzaHO;
    }

    public final int getState() {
        return this.mState;
    }

    public final boolean isBound() {
        return this.zzaHU;
    }

    public final void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        synchronized (this.zzaHW.zzaHP) {
            this.zzaHW.mHandler.removeMessages(1, this.zzaHV);
            this.zzaHj = iBinder;
            this.zzaHO = componentName;
            for (ServiceConnection onServiceConnected : this.zzaHT) {
                onServiceConnected.onServiceConnected(componentName, iBinder);
            }
            this.mState = 1;
        }
    }

    public final void onServiceDisconnected(ComponentName componentName) {
        synchronized (this.zzaHW.zzaHP) {
            this.zzaHW.mHandler.removeMessages(1, this.zzaHV);
            this.zzaHj = null;
            this.zzaHO = componentName;
            for (ServiceConnection onServiceDisconnected : this.zzaHT) {
                onServiceDisconnected.onServiceDisconnected(componentName);
            }
            this.mState = 2;
        }
    }

    public final boolean zza(ServiceConnection serviceConnection) {
        return this.zzaHT.contains(serviceConnection);
    }

    public final void zza$2d8eac7(ServiceConnection serviceConnection) {
        this.zzaHW.zzaHQ;
        this.zzaHW.mApplicationContext;
        this.zzaHV.zzrB();
        this.zzaHT.add(serviceConnection);
    }

    public final void zzb$2d8eac7(ServiceConnection serviceConnection) {
        this.zzaHW.zzaHQ;
        this.zzaHW.mApplicationContext;
        this.zzaHT.remove(serviceConnection);
    }

    public final void zzcB(String str) {
        this.mState = 3;
        this.zzaHW.zzaHQ;
        this.zzaHU = zza.zza$58d5677d(this.zzaHW.mApplicationContext, this.zzaHV.zzrB(), this, 129);
        if (this.zzaHU) {
            this.zzaHW.mHandler.sendMessageDelayed(this.zzaHW.mHandler.obtainMessage(1, this.zzaHV), 300000);
            return;
        }
        this.mState = 2;
        try {
            this.zzaHW.zzaHQ;
            this.zzaHW.mApplicationContext.unbindService(this);
        } catch (IllegalArgumentException e) {
        }
    }

    public final void zzcC$552c4e01() {
        this.zzaHW.mHandler.removeMessages(1, this.zzaHV);
        this.zzaHW.zzaHQ;
        this.zzaHW.mApplicationContext.unbindService(this);
        this.zzaHU = false;
        this.mState = 2;
    }

    public final boolean zzrC() {
        return this.zzaHT.isEmpty();
    }
}
