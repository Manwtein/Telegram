package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class CircleClipInterpolator implements Interpolator {

    public static float firstStage = 0.30f;
    public static float secondStage = 0.06f;
    public static float thirdStage = 0.02f;
    public static float maxCrop = firstStage + secondStage + thirdStage;

    public float getInterpolation(float t) {
        float initT = t;
        float result;
        if (t <= firstStage) {
            result = 0f;
        } else if (t <= firstStage + secondStage) {
            t -= firstStage;
            result = 0.6f * (t / secondStage);
        } else if (t <= firstStage + secondStage + thirdStage) {
            t -= firstStage + secondStage;
            result = 0.6f + 0.4f * (t / (thirdStage));
        } else {
            result = 1;
        }
//        System.out.println("Incoming circle clip progression = " + initT + ", result = " + result);
        return result;
    }
}
