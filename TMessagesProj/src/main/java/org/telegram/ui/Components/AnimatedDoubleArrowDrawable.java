package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import org.telegram.messenger.AndroidUtilities;

public class AnimatedDoubleArrowDrawable extends Drawable {

    private final Paint paint;
    private final Path path = new Path();
    private float animPassedTime;

    public AnimatedDoubleArrowDrawable(int color) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(AndroidUtilities.dp(1.5f));
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        updatePath();
    }

    private float getArrowTranslateYProgress(float t) {
        float maxPercent = 1f;

        float firstStage = 0.6f;
        float secondStage = 0.1f;
        float thirdStage = 0.1f;
        float fourthStage = 0.1f;
        float fifthStage = 0.1f;
        float result;
        if (t <= firstStage) {
            result = 0;
        } else if (t <= firstStage + secondStage) {
            t -= firstStage;
            result = maxPercent * (t / (secondStage));
        } else if (t <= firstStage + secondStage + thirdStage) {
            t -= firstStage + secondStage;
            result = 1 - 1 * (t / (thirdStage));
        } else if (t <= firstStage + secondStage + thirdStage + fourthStage) {
            t -= firstStage + secondStage + thirdStage;
            result = 0 + 1 * (t / (fourthStage));
        } else {
            t -= firstStage + secondStage + thirdStage + fourthStage;
            result = 1 - 1 * (t / (fifthStage));
        }

        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        int count = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        updatePath();
        canvas.drawPath(path, paint);
        canvas.restoreToCount(count);
        invalidateSelf();
    }

    private void updatePath() {
        path.rewind();
        animPassedTime += 16f;
        int animDuration = 3500;
        float progress = animPassedTime / animDuration;
        float yProgress = getArrowTranslateYProgress(progress);
        float yOffset = AndroidUtilities.dp(7) * yProgress;
        int lineWidth = AndroidUtilities.dp(6);
        int lineHeight = AndroidUtilities.dp(6);
        int left = (getBounds().width() / 2) - lineWidth;
        int secondArrowTopMargin = AndroidUtilities.dp(7);
        int top = (getBounds().height() - lineHeight - secondArrowTopMargin) / 2;
        path.moveTo(left, top + yOffset);
        path.rLineTo(lineWidth, lineHeight);
        path.rLineTo(lineWidth, -lineHeight);

        float yOffset2 = AndroidUtilities.dp(4) * yProgress;
        path.moveTo(left, top + secondArrowTopMargin + yOffset2);
        path.rLineTo(lineWidth, lineHeight);
        path.rLineTo(lineWidth, -lineHeight);
        if (animPassedTime >= animDuration) {
            animPassedTime = 0;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return AndroidUtilities.dp(26);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(26);
    }
}
