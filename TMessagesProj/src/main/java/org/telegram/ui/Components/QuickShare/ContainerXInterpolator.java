package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;

public class ContainerXInterpolator implements Interpolator {

    public float getInterpolation(float t) {
        float initT = t;
        float result;
        float firstStage = 0.01f;
        float secondStage = 0.2f;
        float thirdStage = 0.23f;
        float fourthStage = 0.5f;
        if (t <= firstStage) {
            result = 0.2f * (t / firstStage);
        } else if (t <= firstStage + secondStage) {
            t -= firstStage;
            result = 0.2f + 0.1f * (t / secondStage);
        } else if (t <= firstStage + secondStage + thirdStage) {
            t -= firstStage + secondStage;
            result = 0.3f + 0.75f * (t / (thirdStage));
        } else if (t <= firstStage + secondStage + thirdStage + fourthStage) {
            t -= firstStage + secondStage + thirdStage;
            result = 1.05f - 0.05f * (t / (fourthStage));
        } else {
            result = 1f;
        }
//        System.out.println("Incoming X progression = " + initT + ", result = " + result);
        return result;
    }
}
