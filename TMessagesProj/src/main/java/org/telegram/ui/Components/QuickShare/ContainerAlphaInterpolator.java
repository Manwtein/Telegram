package org.telegram.ui.Components.QuickShare;

import android.view.animation.Interpolator;
import static org.telegram.ui.Components.QuickShare.QuickShareEnterDrawable.containerDelayProgress;

public class ContainerAlphaInterpolator implements Interpolator {

    public float maxPercent = 1f;

    public float getInterpolation(float t) {
        float initT = t;
        float result;
        float secondStage = 0.1f;
        float firstStage = containerDelayProgress;
        if (t <= firstStage) {
            result = 0;
        } else if (t <= secondStage + firstStage) {
            t -= firstStage;
            result = maxPercent * (t / secondStage);
        } else {
            result = 1;
        }
//        System.out.println("Incoming alpha container progression = " + initT + ", result = " + result);
        return result;
    }
}
