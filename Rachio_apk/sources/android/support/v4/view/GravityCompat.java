package android.support.v4.view;

import android.graphics.Rect;
import android.os.Build.VERSION;
import android.view.Gravity;

public final class GravityCompat {
    static final GravityCompatImpl IMPL;

    interface GravityCompatImpl {
        void apply(int i, int i2, int i3, Rect rect, Rect rect2, int i4);

        int getAbsoluteGravity(int i, int i2);
    }

    static class GravityCompatImplBase implements GravityCompatImpl {
        GravityCompatImplBase() {
        }

        public final int getAbsoluteGravity(int gravity, int layoutDirection) {
            return -8388609 & gravity;
        }

        public final void apply(int gravity, int w, int h, Rect container, Rect outRect, int layoutDirection) {
            Gravity.apply(gravity, w, h, container, outRect);
        }
    }

    static class GravityCompatImplJellybeanMr1 implements GravityCompatImpl {
        GravityCompatImplJellybeanMr1() {
        }

        public final int getAbsoluteGravity(int gravity, int layoutDirection) {
            return Gravity.getAbsoluteGravity(gravity, layoutDirection);
        }

        public final void apply(int gravity, int w, int h, Rect container, Rect outRect, int layoutDirection) {
            Gravity.apply(gravity, w, h, container, outRect, layoutDirection);
        }
    }

    static {
        if (VERSION.SDK_INT >= 17) {
            IMPL = new GravityCompatImplJellybeanMr1();
        } else {
            IMPL = new GravityCompatImplBase();
        }
    }

    public static void apply(int gravity, int w, int h, Rect container, Rect outRect, int layoutDirection) {
        IMPL.apply(gravity, w, h, container, outRect, layoutDirection);
    }

    public static int getAbsoluteGravity(int gravity, int layoutDirection) {
        return IMPL.getAbsoluteGravity(gravity, layoutDirection);
    }
}
