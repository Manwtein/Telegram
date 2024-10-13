package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class CircleAlphaInterpolator implements Interpolator {

    public float getInterpolation(float t) {
        float result;
        float firstStage = 0.1f;
        if (t <= firstStage) {
            result = 0.3f + 0.7f * (t / firstStage);
        } else {
            result = 1f;
        }

//        result = 1f;
//        System.out.println("Incoming circle gradient progression = " + initT + ", result = " + result);
        return result;
    }
}
