package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class ContainerYInterpolator implements Interpolator {

    public float getInterpolation(float t) {
        float initT = t;
        float result;
        float secondStage = 0.33f;
//        float firstStage = containerDelayProgress;
        float firstStage = 0.01f;

        if (t <= firstStage) {
            result = 0.5f * (t / firstStage);
        } else if (t <= secondStage + firstStage) {
            t -= firstStage;
//            result = 0.5f + 0.52f * (t / secondStage);
            result = 0.5f + 0.5f * (t / secondStage);
        } else {
            t -= secondStage + firstStage;
//            result = -0.01f + maxPercent + 0.01f * (t / secondStage);
//            result = 1.02f - 0.2f * (t / (1 - secondStage - firstStage));
            result = 1f;
        }
//        result = 0.5f;
//        System.out.println("Incoming Y progression = " + initT + ", result = " + result);
        return result;
    }
}
