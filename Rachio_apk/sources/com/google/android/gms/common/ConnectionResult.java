package com.google.android.gms.common;

import android.app.PendingIntent;
import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.google.android.gms.common.internal.safeparcel.zza;
import com.google.android.gms.common.internal.safeparcel.zzd;
import com.google.android.gms.common.internal.zzbe;
import java.util.Arrays;

public final class ConnectionResult extends zza {
    public static final Creator<ConnectionResult> CREATOR = new zzb();
    public static final ConnectionResult zzazX = new ConnectionResult(0);
    private final PendingIntent mPendingIntent;
    private int zzaku;
    private final int zzaxu;
    private final String zzazY;

    public ConnectionResult(int i) {
        this(i, null, null);
    }

    ConnectionResult(int i, int i2, PendingIntent pendingIntent, String str) {
        this.zzaku = i;
        this.zzaxu = i2;
        this.mPendingIntent = pendingIntent;
        this.zzazY = str;
    }

    public ConnectionResult(int i, PendingIntent pendingIntent) {
        this(i, pendingIntent, null);
    }

    private ConnectionResult(int i, PendingIntent pendingIntent, String str) {
        this(1, i, pendingIntent, null);
    }

    static String getStatusString(int i) {
        switch (i) {
            case -1:
                return "UNKNOWN";
            case 0:
                return "SUCCESS";
            case 1:
                return "SERVICE_MISSING";
            case 2:
                return "SERVICE_VERSION_UPDATE_REQUIRED";
            case 3:
                return "SERVICE_DISABLED";
            case 4:
                return "SIGN_IN_REQUIRED";
            case 5:
                return "INVALID_ACCOUNT";
            case 6:
                return "RESOLUTION_REQUIRED";
            case 7:
                return "NETWORK_ERROR";
            case 8:
                return "INTERNAL_ERROR";
            case 9:
                return "SERVICE_INVALID";
            case 10:
                return "DEVELOPER_ERROR";
            case 11:
                return "LICENSE_CHECK_FAILED";
            case 13:
                return "CANCELED";
            case 14:
                return "TIMEOUT";
            case 15:
                return "INTERRUPTED";
            case 16:
                return "API_UNAVAILABLE";
            case 17:
                return "SIGN_IN_FAILED";
            case 18:
                return "SERVICE_UPDATING";
            case 19:
                return "SERVICE_MISSING_PERMISSION";
            case 20:
                return "RESTRICTED_PROFILE";
            case 21:
                return "API_VERSION_UPDATE_REQUIRED";
            case 99:
                return "UNFINISHED";
            case 1500:
                return "DRIVE_EXTERNAL_STORAGE_REQUIRED";
            default:
                return "UNKNOWN_ERROR_CODE(" + i + ")";
        }
    }

    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ConnectionResult)) {
            return false;
        }
        ConnectionResult connectionResult = (ConnectionResult) obj;
        return this.zzaxu == connectionResult.zzaxu && zzbe.equal(this.mPendingIntent, connectionResult.mPendingIntent) && zzbe.equal(this.zzazY, connectionResult.zzazY);
    }

    public final int getErrorCode() {
        return this.zzaxu;
    }

    public final String getErrorMessage() {
        return this.zzazY;
    }

    public final PendingIntent getResolution() {
        return this.mPendingIntent;
    }

    public final boolean hasResolution() {
        return (this.zzaxu == 0 || this.mPendingIntent == null) ? false : true;
    }

    public final int hashCode() {
        return Arrays.hashCode(new Object[]{Integer.valueOf(this.zzaxu), this.mPendingIntent, this.zzazY});
    }

    public final boolean isSuccess() {
        return this.zzaxu == 0;
    }

    public final String toString() {
        return zzbe.zzt(this).zzg("statusCode", getStatusString(this.zzaxu)).zzg("resolution", this.mPendingIntent).zzg("message", this.zzazY).toString();
    }

    public final void writeToParcel(Parcel parcel, int i) {
        int zze = zzd.zze(parcel);
        zzd.zzc(parcel, 1, this.zzaku);
        zzd.zzc(parcel, 2, this.zzaxu);
        zzd.zza(parcel, 3, this.mPendingIntent, i, false);
        zzd.zza(parcel, 4, this.zzazY, false);
        zzd.zzI(parcel, zze);
    }
}
