package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class RotationInterpolator implements Interpolator {

    public float maxPercent = 1f;

    public float getInterpolation(float t) {
        float initT = t;
        float result;
        float firstStage = 0.33f;
        float secondStage = 0.21f;
        if (t <= firstStage) {
            result = maxPercent * (t / firstStage);
        } else if (t <= firstStage + secondStage) {
            t -= firstStage;
            result = maxPercent - 2f * (t / secondStage);
        } else {
            t -= firstStage + secondStage;
            result = -1f + 1f * (t / (1 - firstStage - secondStage));
        }
        System.out.println("Incoming rotation progression = " + initT + ", result = " + result);
        return result;
    }
}
