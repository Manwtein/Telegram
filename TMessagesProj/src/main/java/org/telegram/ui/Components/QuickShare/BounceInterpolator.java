package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class BounceInterpolator implements Interpolator {
    public static final float secondBounceStage = 0.21f;
    private static final float firstBounceStage = 0.33f;
    public static float maxBounceStage = firstBounceStage;
    public float maxPercent = 1f;

    public float getInterpolation(float t) {
        float result;

        if (t <= firstBounceStage) {
            result = maxPercent * (t / firstBounceStage);
        } else if (t <= firstBounceStage + secondBounceStage) {
            t -= firstBounceStage;
            result = maxPercent - 1.25f * (t / (secondBounceStage));
        } else {
            t -= firstBounceStage + secondBounceStage;
            result = -0.25f + 0.25f * (t / (1 - firstBounceStage - secondBounceStage));
        }
//        System.out.println("Incoming bounce progression = " + initT + ", result = " + result);
        return result;
    }
}
