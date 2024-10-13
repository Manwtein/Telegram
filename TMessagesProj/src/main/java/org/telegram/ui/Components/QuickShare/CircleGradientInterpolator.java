package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;
import static org.telegram.ui.Components.QuickShare.BounceInterpolator.maxBounceStage;
import static org.telegram.ui.Components.QuickShare.BounceInterpolator.secondBounceStage;

public class CircleGradientInterpolator implements Interpolator {

    public static int sizeMultiplier = 4;
    public static float gradientFirstStage = 0.1f;

    public float getInterpolation(float t) {
        float result;
        if (t <= gradientFirstStage) {
            result = -(sizeMultiplier - 1);
        } else {
            if (t <= maxBounceStage) {
                t -= gradientFirstStage;
                result = -0.3f * (t / (maxBounceStage - gradientFirstStage));
            } else if (t <= maxBounceStage + secondBounceStage) {
                t -= maxBounceStage;
                result = -0.3f - 0.7f * (t / secondBounceStage);
            } else {
                result = -1f;
            }
        }

//        result = -3f;
//        System.out.println("Incoming circle gradient progression = " + initT + ", result = " + result);
        return result;
    }
}
