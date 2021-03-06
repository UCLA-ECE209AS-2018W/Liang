package android.support.v4.app;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.Build.VERSION;

public final class AppOpsManagerCompat {
    private static final AppOpsManagerImpl IMPL;

    private static class AppOpsManagerImpl {
        AppOpsManagerImpl() {
        }

        public String permissionToOp(String permission) {
            return null;
        }

        public int noteProxyOp(Context context, String op, String proxiedPackageName) {
            return 1;
        }
    }

    private static class AppOpsManager23 extends AppOpsManagerImpl {
        AppOpsManager23() {
        }

        public final String permissionToOp(String permission) {
            return AppOpsManager.permissionToOp(permission);
        }

        public final int noteProxyOp(Context context, String op, String proxiedPackageName) {
            return ((AppOpsManager) context.getSystemService(AppOpsManager.class)).noteProxyOp(op, proxiedPackageName);
        }
    }

    static {
        if (VERSION.SDK_INT >= 23) {
            IMPL = new AppOpsManager23();
        } else {
            IMPL = new AppOpsManagerImpl();
        }
    }

    public static String permissionToOp(String permission) {
        return IMPL.permissionToOp(permission);
    }

    public static int noteProxyOp(Context context, String op, String proxiedPackageName) {
        return IMPL.noteProxyOp(context, op, proxiedPackageName);
    }
}
