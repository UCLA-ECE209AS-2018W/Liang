package com.google.android.gms.maps.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.dynamic.IObjectWrapper.zza;
import com.google.android.gms.internal.zzed;
import com.google.android.gms.internal.zzef;
import com.google.android.gms.maps.GoogleMapOptions;

public final class zzj extends zzed implements IMapFragmentDelegate {
    zzj(IBinder iBinder) {
        super(iBinder, "com.google.android.gms.maps.internal.IMapFragmentDelegate");
    }

    public final void getMapAsync(zzap zzap) throws RemoteException {
        Parcel zzZ = zzZ();
        zzef.zza(zzZ, (IInterface) zzap);
        zzb(12, zzZ);
    }

    public final void onCreate(Bundle bundle) throws RemoteException {
        Parcel zzZ = zzZ();
        zzef.zza(zzZ, (Parcelable) bundle);
        zzb(3, zzZ);
    }

    public final IObjectWrapper onCreateView(IObjectWrapper iObjectWrapper, IObjectWrapper iObjectWrapper2, Bundle bundle) throws RemoteException {
        Parcel zzZ = zzZ();
        zzef.zza(zzZ, (IInterface) iObjectWrapper);
        zzef.zza(zzZ, (IInterface) iObjectWrapper2);
        zzef.zza(zzZ, (Parcelable) bundle);
        zzZ = zza(4, zzZ);
        IObjectWrapper zzM = zza.zzM(zzZ.readStrongBinder());
        zzZ.recycle();
        return zzM;
    }

    public final void onDestroy() throws RemoteException {
        zzb(8, zzZ());
    }

    public final void onDestroyView() throws RemoteException {
        zzb(7, zzZ());
    }

    public final void onInflate(IObjectWrapper iObjectWrapper, GoogleMapOptions googleMapOptions, Bundle bundle) throws RemoteException {
        Parcel zzZ = zzZ();
        zzef.zza(zzZ, (IInterface) iObjectWrapper);
        zzef.zza(zzZ, (Parcelable) googleMapOptions);
        zzef.zza(zzZ, (Parcelable) bundle);
        zzb(2, zzZ);
    }

    public final void onLowMemory() throws RemoteException {
        zzb(9, zzZ());
    }

    public final void onPause() throws RemoteException {
        zzb(6, zzZ());
    }

    public final void onResume() throws RemoteException {
        zzb(5, zzZ());
    }

    public final void onSaveInstanceState(Bundle bundle) throws RemoteException {
        Parcel zzZ = zzZ();
        zzef.zza(zzZ, (Parcelable) bundle);
        zzZ = zza(10, zzZ);
        if (zzZ.readInt() != 0) {
            bundle.readFromParcel(zzZ);
        }
        zzZ.recycle();
    }

    public final void onStart() throws RemoteException {
        zzb(15, zzZ());
    }

    public final void onStop() throws RemoteException {
        zzb(16, zzZ());
    }
}
