package org.telegram.ui.Components.QuickShare;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import static org.telegram.ui.Components.QuickShare.CircleClipInterpolator.maxCrop;
import static org.telegram.ui.Components.QuickShare.CircleGradientInterpolator.gradientFirstStage;
import static org.telegram.ui.Components.QuickShare.CircleGradientInterpolator.sizeMultiplier;
import org.telegram.ui.Components.QuickShareContainerLayout;

public class QuickShareEnterDrawable extends Drawable {

    public static final float containerDelayProgress = 0.0f;
    private final QuickShareContainerLayout targetView;
    private final BounceInterpolator bounceInterpolator;
    private final RotationInterpolator rotationInterpolator;
    private final ContainerYInterpolator containerYInterpolator;
    private final ContainerXInterpolator containerXInterpolator;
    private final ContainerAlphaInterpolator containerAlphaInterpolator;
    private final CircleGradientInterpolator circleGradientInterpolator;
    private final CircleAlphaInterpolator circleAlphaInterpolator;
    private final CircleClipInterpolator circleClipInterpolator;
    private final QuickShareBtnAttributes quickShareBtnAttributes;
    public RectF menuBackgroundRect = new RectF();
    public float dismissedProgress = 1f;
    LinearGradient linearGradient;
    BitmapShader bitmapShader;
    ComposeShader composeShader;
    Matrix linearGradientShaderMatrix = new Matrix();
    Matrix bitmapShaderMatrix = new Matrix();
    Matrix angleMatrix = new Matrix();
    RectF buttonRect = new RectF();
    Path shareButtonPath = new Path();
    Path menuClipPath = new Path();
    Path shareButtonAnglesClipPath = new Path();
    RectF croppingRectLeft = new RectF();
    RectF croppingRectRight = new RectF();
    Point realScreenSize;
    private int maxX;
    private int maxY;
    private int startX;
    private int startY;
    private int finalContainerLayoutOffsetX;
    private int finalContainerLayoutOffsetY;
    private float progress = 1f;
    private RectF angleRect = new RectF();
    private Path anglePath = new Path();
    private float lastClipLeftTop = 0f;
    private float lastClipRightTop = 0f;

    private float lastClipLeftBottom = 0f;
    private float lastClipRightBottom = 0f;

    public QuickShareEnterDrawable(
            QuickShareContainerLayout targetView,
            QuickShareBtnAttributes quickShareBtnAttributes
    ) {
        this.targetView = targetView;
        this.bounceInterpolator = new BounceInterpolator();
        this.rotationInterpolator = new RotationInterpolator();
        this.containerYInterpolator = new ContainerYInterpolator();
        this.containerXInterpolator = new ContainerXInterpolator();
        this.containerAlphaInterpolator = new ContainerAlphaInterpolator();
        this.circleGradientInterpolator = new CircleGradientInterpolator();
        this.circleAlphaInterpolator = new CircleAlphaInterpolator();
        this.circleClipInterpolator = new CircleClipInterpolator();
        this.quickShareBtnAttributes = quickShareBtnAttributes;
        normaliseButtonInitRect();
        if (quickShareBtnAttributes.paint.getShader() instanceof BitmapShader) {
            bitmapShader = (BitmapShader) quickShareBtnAttributes.paint.getShader();
        }
        linearGradient = getLinearGradient();
        if (bitmapShader != null && Build.VERSION.SDK_INT > 22) { // TODO: 28.10.2024 check
            composeShader = new ComposeShader(linearGradient, bitmapShader, PorterDuff.Mode.DST_OVER);
        }
        realScreenSize = AndroidUtilities.getRealScreenSize();
        maxX = quickShareBtnAttributes.maxParentX;
        maxY = quickShareBtnAttributes.maxParentY;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        normaliseButtonInitRect();
        shareButtonPath.rewind();
        shareButtonAnglesClipPath.rewind();
        menuClipPath.rewind();
        anglePath.rewind();
        int count = canvas.save();
        if (dismissedProgress != 1f) {
//            float bounceProgress = bounceInterpolator.getInterpolation(progress);
//
//            float maxBounceX = 0;
//            float maxBounceY;
//            if (quickShareBtnAttributes.bottomDirection) {
//                maxBounceY = (finalContainerLayoutOffsetY + (targetView.textViewHeight + targetView.getPaddingTop()));
//            } else {
//                maxBounceY = finalContainerLayoutOffsetY + (targetView.getMeasuredHeight() - targetView.getPaddingBottom());
//            }
//
//            float buttonDx = bounceProgress * maxBounceX;
//            float buttonDy = bounceProgress * maxBounceY;
//
//
//            int dx = (int) (quickShareBtnAttributes.rectF.left + finalContainerLayoutOffsetX);
//            int dy = startY + finalContainerLayoutOffsetY;
//            if (dx + targetView.getMeasuredWidth() > maxX) {
//                dx = maxX - targetView.getMeasuredWidth();
//            }
//            if (dy < maxY) {
//                dy = maxY + targetView.getMeasuredHeight();
//            }
//
//            float buttonFinalY = startY + buttonDy;
//            float buttonFinalX = startX + buttonDx;
//
////        if (progress < 1f) {
//            int top = targetView.getPaddingTop();
//            int menuParentGroupRightMargin = AndroidUtilities.dp(8f);
//            int left = targetView.getPaddingLeft() - menuParentGroupRightMargin;
//            int bottom = targetView.getBackgroundHeight() - targetView.getPaddingBottom();
//            int right = targetView.getMeasuredWidth() - targetView.getPaddingRight() - menuParentGroupRightMargin;
//            float yProgress = containerYInterpolator.getInterpolation(progress);
//            float xProgress = containerXInterpolator.getInterpolation(progress);
//
//            int finalLeft = left + dx;
//            int finalTop = top + dy;
//            int finalRight = right + dx;
//            int finalBottom = bottom + dy;
//            if (targetView.relativeBackgroundRect.isEmpty()) {
//                targetView.relativeBackgroundRect.set(
//                        finalLeft,
//                        finalTop,
//                        finalRight,
//                        finalBottom
//                );
//            }
//            float progressedLeft = finalLeft + ((buttonFinalX - finalLeft) * (1 - xProgress));
//            float buttonTopYForMenu = buttonFinalY - targetView.textViewHeight;
//            float buttonBottomYForMenu = buttonTopYForMenu + quickShareBtnAttributes.rectF.height();
//            float buttonRightXForMenu = buttonFinalX + quickShareBtnAttributes.rectF.width();
//            float progressedTop = finalTop + (buttonTopYForMenu - finalTop) * (1 - yProgress);
//            float progressedRight = finalRight + ((buttonRightXForMenu - finalRight) * (1 - xProgress));
//            float progressedBottom = finalBottom + (buttonBottomYForMenu - finalBottom) * (1 - yProgress);
//
//            int oldAlpha = targetView.bgPaint.getAlpha();
//            menuBackgroundRect.set(
//                    progressedLeft,
//                    progressedTop + targetView.textViewHeight,
//                    progressedRight,
//                    progressedBottom + targetView.textViewHeight
//            );
//            targetView.bgPaint.setAlpha((int) (255 * (1 - dismissedProgress)));
//            addPathElements(canvas, buttonFinalX, buttonFinalY);
//            drawMenu(canvas);
//            drawShareButton(canvas);
//            if (oldAlpha != 0) {
//                targetView.bgPaint.setAlpha(oldAlpha);
//            }
//            drawMenuContacts(canvas, xProgress);
        } else {
            float bounceProgress = bounceInterpolator.getInterpolation(progress);

            float maxBounceX = 0;
            float maxBounceY;
            if (quickShareBtnAttributes.bottomDirection) {
                maxBounceY = (finalContainerLayoutOffsetY + (targetView.textViewHeight + targetView.getPaddingTop()) - quickShareBtnAttributes.rectF.height());
            } else {
                maxBounceY = finalContainerLayoutOffsetY + (targetView.getMeasuredHeight() - targetView.getPaddingBottom());
            }

            float buttonDx = bounceProgress * maxBounceX;
            float buttonDy = bounceProgress * maxBounceY;


            int dx = (int) (quickShareBtnAttributes.rectF.left + finalContainerLayoutOffsetX);
            int dy = startY + finalContainerLayoutOffsetY;
            if (dx + targetView.getMeasuredWidth() > maxX) {
                dx = maxX - targetView.getMeasuredWidth();
            }
            if (dy < maxY) {
                dy = maxY + targetView.getMeasuredHeight();
            }

            float buttonFinalY = startY + buttonDy;
            float buttonFinalX = startX + buttonDx;

            int top = targetView.getPaddingTop();
            int menuParentGroupRightMargin = AndroidUtilities.dp(8f);
            int left = targetView.getPaddingLeft() - menuParentGroupRightMargin;
            int bottom = targetView.getBackgroundHeight() - targetView.getPaddingBottom();
            int right = targetView.getMeasuredWidth() - targetView.getPaddingRight() - menuParentGroupRightMargin;
            float yProgress = containerYInterpolator.getInterpolation(progress);
            float xProgress = containerXInterpolator.getInterpolation(progress);

            int finalLeft = left + dx;
            int finalTop = top + dy;
            int finalRight = right + dx;
            int finalBottom = bottom + dy;
            if (targetView.relativeBackgroundRect.isEmpty()) {
                targetView.relativeBackgroundRect.set(
                        finalLeft,
                        finalTop,
                        finalRight,
                        finalBottom
                );
            }
            float progressedLeft = finalLeft + ((buttonFinalX - finalLeft) * (1 - xProgress));
            float buttonTopYForMenu = buttonFinalY - targetView.textViewHeight;
            if (quickShareBtnAttributes.bottomDirection) {
                buttonTopYForMenu -= quickShareBtnAttributes.rectF.height();
            }
            float buttonBottomYForMenu = buttonTopYForMenu + quickShareBtnAttributes.rectF.height();
            float buttonRightXForMenu = buttonFinalX + quickShareBtnAttributes.rectF.width();
            float progressedTop = finalTop + (buttonTopYForMenu - finalTop) * (1 - yProgress);
            float progressedRight = finalRight + ((buttonRightXForMenu - finalRight) * (1 - xProgress));
            float progressedBottom = finalBottom + (buttonBottomYForMenu - finalBottom) * (1 - yProgress);

            int oldAlpha = targetView.bgPaint.getAlpha();
            menuBackgroundRect.set(
                    progressedLeft,
                    progressedTop + targetView.textViewHeight,
                    progressedRight,
                    progressedBottom + targetView.textViewHeight
            );
            addPathElements(canvas, buttonFinalX, buttonFinalY);
            drawMenu(canvas);
            drawShareButton(canvas);
            if (oldAlpha != 0) {
                targetView.bgPaint.setAlpha(oldAlpha);
            }
            drawMenuContacts(canvas, xProgress);
        }
        canvas.restoreToCount(count);
//        drawDebugMiddleLines(canvas);
    }

    private void normaliseButtonInitRect() {
        if (quickShareBtnAttributes.rectF.left != 0) {
            float topForNormalisation = quickShareBtnAttributes.rectF.top;
            quickShareBtnAttributes.rectF.top = 0;
            quickShareBtnAttributes.rectF.bottom -= topForNormalisation;
            startY = quickShareBtnAttributes.buttonParentTopY;
            startX = (int) quickShareBtnAttributes.rectF.left;
            float leftForNormalisation = quickShareBtnAttributes.rectF.left;
            quickShareBtnAttributes.rectF.left = 0;
            quickShareBtnAttributes.rectF.right -= leftForNormalisation;
        }
    }

//    private void drawDebugMiddleLines(@NonNull Canvas canvas) {
//        canvas.drawLine(
//                menuBackgroundRect.left,
//                menuBackgroundRect.centerY(),
//                menuBackgroundRect.right,
//                menuBackgroundRect.centerY(),
//                testPaint
//        );
//        canvas.drawLine(
//                menuBackgroundRect.centerX(),
//                menuBackgroundRect.top,
//                menuBackgroundRect.centerX(),
//                menuBackgroundRect.bottom,
//                testPaint
//        );
//    }

    private void drawMenuContacts(@NonNull Canvas canvas, float xProgress) {
        float startDrawingChildCutOff = 0.2f;
        if (progress > startDrawingChildCutOff) {
            canvas.translate(
                    (menuBackgroundRect.centerX()) - targetView.getMeasuredWidth() / 2f,
                    ((menuBackgroundRect.centerY()) - targetView.getPaddingTop()) - targetView.textViewHeight - targetView.contactSize / 2f - targetView.marginInBackground
            );
            targetView.drawChildren(
                    canvas,
                    (progress - startDrawingChildCutOff) / (1f - startDrawingChildCutOff),
                    menuBackgroundRect,
                    xProgress
            );
        }
    }

    private void drawMenu(@NonNull Canvas canvas) {
        int oldAlpha = targetView.bgPaint.getAlpha();
        float alpha = containerAlphaInterpolator.getInterpolation(progress);
        targetView.bgPaint.setAlpha((int) (alpha * 255));
        if (menuBackgroundRect.width() > 0 && menuBackgroundRect.height() > 0) {
            targetView.shadow.setAlpha((int) (alpha * 255));
            targetView.shadow.setBounds(
                    (int) menuBackgroundRect.left - targetView.shadowSize,
                    (int) menuBackgroundRect.top - targetView.shadowSize,
                    (int) menuBackgroundRect.right + targetView.shadowSize,
                    (int) menuBackgroundRect.bottom + targetView.shadowSize
            );
            targetView.shadow.draw(canvas);
        }
        canvas.drawRoundRect(
                menuBackgroundRect,
                menuBackgroundRect.width() / 2f,
                menuBackgroundRect.width() / 2f,
                targetView.bgPaint
        );
        menuClipPath.addRoundRect(
                menuBackgroundRect,
                menuBackgroundRect.width() / 2f,
                menuBackgroundRect.width() / 2f,
                Path.Direction.CW
        );
        if (oldAlpha != 0) {
            targetView.bgPaint.setAlpha(oldAlpha);
        }
    }

    private void drawShareButton(Canvas canvas) {
        clipShareButtonAngles();
        float gradientProgress = circleGradientInterpolator.getInterpolation(progress);
        if (quickShareBtnAttributes.bottomDirection) {
            if (gradientProgress > gradientFirstStage) {
                gradientProgress = -gradientProgress;
            }
        }
        float alphaProgress = circleAlphaInterpolator.getInterpolation(progress);
        linearGradientShaderMatrix.reset();
        angleMatrix.reset();
        bitmapShaderMatrix.reset();
        float rotationProgress = rotationInterpolator.getInterpolation(progress);
        linearGradientShaderMatrix.setTranslate(0, buttonRect.top + buttonRect.height() * gradientProgress);

        linearGradientShaderMatrix.postRotate(
                45 * rotationProgress,
                buttonRect.centerX(),
                buttonRect.centerY()
        );

        bitmapShaderMatrix.postRotate(
                45 * rotationProgress,
                buttonRect.centerX(),
                buttonRect.centerY()
        );
        angleMatrix.setRotate(
                45 * rotationProgress,
                buttonRect.centerX(),
                buttonRect.centerY()
        );
        linearGradient.setLocalMatrix(linearGradientShaderMatrix);
        int count = canvas.save();
//        if (isAngleClippingDisabled()) {
//            canvas.clipPath(menuClipPath, Region.Op.DIFFERENCE);
//        }
        canvas.rotate(
                -45 * rotationProgress,
                buttonRect.centerX(),
                buttonRect.centerY()
        );
        anglePath.transform(angleMatrix);
        shareButtonPath.op(anglePath, Path.Op.UNION);
//        quickShareBtnAttributes.paint.setAlpha(255);
        if (composeShader != null) {
            updateBitmapShader();
            quickShareBtnAttributes.paint.setShader(composeShader);
        } else if (bitmapShader != null) {
            updateBitmapShader();
            quickShareBtnAttributes.paint.setShader(bitmapShader);
        } else {
            quickShareBtnAttributes.paint.setShader(linearGradient);
        }
//        if (gradientProgress == 0 && quickShareBtnAttributes.paint.getColorFilter() != null) {
//            quickShareBtnAttributes.paint.setColorFilter(null);
//        } else if (gradientProgress != 0 && quickShareBtnAttributes.paint.getColorFilter() != buttonColorFilter) {
//            quickShareBtnAttributes.paint.setColorFilter(buttonColorFilter);
//        }
        int oldShareBtnPaintAlpha = quickShareBtnAttributes.paint.getAlpha();
        quickShareBtnAttributes.paint.setAlpha((int) (255 * alphaProgress));
        canvas.drawPath(shareButtonPath, quickShareBtnAttributes.paint);
        canvas.restoreToCount(count);
        int count2 = canvas.save();
        canvas.rotate(
                -45 * rotationProgress,
                buttonRect.centerX(),
                buttonRect.centerY()
        );
        int oldAlpha = quickShareBtnAttributes.icon.getAlpha();
        quickShareBtnAttributes.icon.setAlpha(targetView.bgPaint.getAlpha());
        canvas.translate(
                buttonRect.centerX() - quickShareBtnAttributes.icon.getBounds().centerX(),
                buttonRect.centerY() - quickShareBtnAttributes.icon.getBounds().centerY()
        );
        quickShareBtnAttributes.icon.draw(canvas);
        if (oldAlpha != 0) {
            quickShareBtnAttributes.icon.setAlpha(oldAlpha);
        }
        if (oldShareBtnPaintAlpha != 0) {
            quickShareBtnAttributes.paint.setAlpha(oldAlpha);
        }
        canvas.restoreToCount(count2);
    }

    private void updateBitmapShader() {
        if (quickShareBtnAttributes.shaderBitmap == null) {
            Theme.applyServiceShaderMatrix(
                    bitmapShader,
                    bitmapShaderMatrix,
                    realScreenSize.x,
                    realScreenSize.y,
                    0,
                    0
            );
        } else {
            Theme.applyServiceShaderMatrix(
                    quickShareBtnAttributes.shaderBitmap,
                    bitmapShader,
                    bitmapShaderMatrix,
                    realScreenSize.x,
                    realScreenSize.y,
                    0,
                    quickShareBtnAttributes.shaderOffset
            );
        }
    }

    private void clipShareButtonAngles() {
        if (progress > maxCrop) {
            return;
        }
        if (isAngleClippingDisabled()) {
            return;
        }
        shareButtonAnglesClipPath.addPath(menuClipPath);

        if (quickShareBtnAttributes.bottomDirection) {
            angleRect.set(
                    menuBackgroundRect.left,
                    buttonRect.centerY(),
                    menuBackgroundRect.right,
                    menuBackgroundRect.centerY()
            );
        } else {
            angleRect.set(
                    menuBackgroundRect.left,
                    menuBackgroundRect.centerY(),
                    menuBackgroundRect.right,
                    buttonRect.centerY()
            );
        }
        anglePath.addRect(angleRect, Path.Direction.CW);
        anglePath.op(shareButtonAnglesClipPath, Path.Op.DIFFERENCE);
    }

    private float getYForMenuLeftBottomDirectionBottom(float x, float y, float bottom) {
        float menuRadius = (menuBackgroundRect.height() / 2f);
        float relativeX = x - menuBackgroundRect.left;
        float relativeY = menuBackgroundRect.top - bottom;
        float dx = relativeX - menuRadius;
        float dy = relativeY - menuRadius;
        boolean inCorner = x * x + y * y > menuRadius * menuRadius && dx < 0 && dy < 0;
        if (!inCorner) {
            return menuBackgroundRect.top;
        }
        float resultDy = (float) Math.sqrt(menuRadius * menuRadius - dx * dx);
        float result = Math.min(
                menuBackgroundRect.top + (menuRadius - resultDy) /*- menuRadius*/,
                menuBackgroundRect.centerY()
        );
        return Float.isNaN(result) ? y : result;
    }

    private float getYForMenuRightBottomDirectionBottom(float x, float y, float bottom) {
        float menuRadius = (menuBackgroundRect.height() / 2f);
        float relativeX = x - menuBackgroundRect.left;
        float relativeY = menuBackgroundRect.top - bottom;
        float dx = menuBackgroundRect.width() - relativeX - menuRadius;
        float dy = relativeY - menuRadius;
        boolean inCorner = x * x + y * y > menuRadius * menuRadius && dx < 0 && dy < 0;
        if (!inCorner) {
            return menuBackgroundRect.top;
        }
        float resultDy = (float) Math.sqrt(menuRadius * menuRadius - dx * dx);
        float result = Math.min(
                menuBackgroundRect.top + (menuRadius - resultDy) /*- menuRadius*/,
                menuBackgroundRect.centerY()
        );
        return Float.isNaN(result) ? y : result;
    }

    private float getYForMenuLeftBottom(float x, float y, float top) {
        float menuRadius = (menuBackgroundRect.height() / 2f);
        float relativeX = x - menuBackgroundRect.left;
        float relativeY = top - menuBackgroundRect.top;
        float dx = relativeX - menuRadius;
        float dy = menuBackgroundRect.height() - relativeY - menuRadius;
        boolean inCorner = x * x + y * y > menuRadius * menuRadius && dx < 0 && dy < 0;
        if (!inCorner) {
            return menuBackgroundRect.bottom;
        }
        float resultDy = (float) Math.sqrt(menuRadius * menuRadius - dx * dx);
        float result = Math.max(
                menuBackgroundRect.bottom - (menuBackgroundRect.height() - menuRadius - resultDy),
                menuBackgroundRect.centerY()
        );
        return Float.isNaN(result) ? y : result;
    }

    private float getYForMenuRightBottom(float x, float y, float top) {
        float menuRadius = (menuBackgroundRect.height() / 2f);
        float relativeX = x - menuBackgroundRect.left;
        float relativeY = top - menuBackgroundRect.top;
        float dx = menuBackgroundRect.width() - relativeX - menuRadius;
        float dy = menuBackgroundRect.height() - relativeY - menuRadius;
        boolean inCorner = x * x + y * y > menuRadius * menuRadius && dx < 0 && dy < 0;
        if (!inCorner) {
            return menuBackgroundRect.bottom;
        }
        float resultDy = (float) Math.sqrt(menuRadius * menuRadius - dx * dx);
        float result = Math.max(
                menuBackgroundRect.bottom - (menuBackgroundRect.height() - menuRadius - resultDy),
                menuBackgroundRect.centerY()
        );
        return Float.isNaN(result) ? y : result;
    }

    private boolean isAngleClippingDisabled() {
        if (quickShareBtnAttributes.bottomDirection) {
            return menuBackgroundRect.top < buttonRect.centerY();
        } else {
            return menuBackgroundRect.bottom > buttonRect.top + buttonRect.height() / 2;
        }
    }

    private @NonNull LinearGradient getLinearGradient() {
        int[] colors;
        float[] positions;
        if (bitmapShader == null) {
            if (quickShareBtnAttributes.bottomDirection) {
                colors = new int[]{
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        quickShareBtnAttributes.paint.getColor(),

                        quickShareBtnAttributes.paint.getColor(),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        quickShareBtnAttributes.paint.getColor(),
                };
                positions = new float[]{
                        0.0f,
                        0.0f + (0.1f * (1f / sizeMultiplier)),
                        1f / sizeMultiplier,

                        2f / sizeMultiplier,

                        2f / sizeMultiplier,

                        3f / sizeMultiplier,
                        3f / sizeMultiplier + (0.1f * (1f / sizeMultiplier)),
                        4f / sizeMultiplier,
                };
            } else {
                colors = new int[]{
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        quickShareBtnAttributes.paint.getColor(),

                        quickShareBtnAttributes.paint.getColor(),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        quickShareBtnAttributes.paint.getColor(),
                };
                positions = new float[]{
                        0.0f,
                        0.0f + (0.1f * (1f / sizeMultiplier)),
                        1f / sizeMultiplier,

                        2f / sizeMultiplier,

                        2f / sizeMultiplier,

                        3f / sizeMultiplier,
                        3f / sizeMultiplier + (0.1f * (1f / sizeMultiplier)),
                        4f / sizeMultiplier,
                };
            }
        } else {
            if (quickShareBtnAttributes.bottomDirection) {
                colors = new int[]{
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        Color.TRANSPARENT,

                        Color.TRANSPARENT,
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        Color.TRANSPARENT,
                };
                positions = new float[]{
                        0.0f,
                        0.0f + (0.1f * (1f / sizeMultiplier)),
                        1f / sizeMultiplier,

                        2f / sizeMultiplier,

                        2f / sizeMultiplier,

                        3f / sizeMultiplier,
                        3f / sizeMultiplier + (0.1f * (1f / sizeMultiplier)),
                        4f / sizeMultiplier,
                };
            } else {
                colors = new int[]{
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        Color.TRANSPARENT,

                        Color.TRANSPARENT,
                        getColorWithNewAlpha(255, targetView.bgPaint),

                        getColorWithNewAlpha(255, targetView.bgPaint),
                        getColorWithNewAlpha(255, targetView.bgPaint),
                        Color.TRANSPARENT,
                };
                positions = new float[]{
                        0.0f,
                        0.0f + (0.1f * (1f / sizeMultiplier)),
                        1f / sizeMultiplier,

                        2f / sizeMultiplier,

                        2f / sizeMultiplier,

                        3f / sizeMultiplier,
                        3f / sizeMultiplier + (0.1f * (1f / sizeMultiplier)),
                        4f / sizeMultiplier,
                };
            }
        }
        return new LinearGradient(
                0,
                0,
                0,
                quickShareBtnAttributes.rectF.height() * sizeMultiplier,
                colors,
                positions,
                Shader.TileMode.CLAMP
        );
    }

    private int getColorWithNewAlpha(int alpha, Paint paint) {
        return Color.argb(alpha,
                Color.red(paint.getColor()),
                Color.green(paint.getColor()),
                Color.blue(paint.getColor())
        );
    }

    private void addPathElements(Canvas canvas, float buttonFinalX, float buttonFinalY) {
        buttonRect.set(quickShareBtnAttributes.rectF);
        buttonRect.offset(buttonFinalX, buttonFinalY);
        shareButtonPath.addRoundRect(
                buttonRect,
                quickShareBtnAttributes.radius,
                quickShareBtnAttributes.radius,
                Path.Direction.CW
        );

        if (quickShareBtnAttributes.bottomDirection) {
            addClippingElementsBottomDirection(canvas);
        } else {
            addClippingElementsTopDirection();
        }
    }

    private void addClippingElementsBottomDirection(Canvas canvas) {
        float clipProgress = circleClipInterpolator.getInterpolation(progress);
//        float bottom = Math.min(menuBackgroundRect.top, buttonRect.bottom);
        float bottom = menuBackgroundRect.top;
//        float top = menuBackgroundRect.bottom;
        float ratio = 0.5f;
        float widthProgress = buttonRect.width() * clipProgress;
        float buttonLeftX = buttonRect.left;
        float buttonRightX = buttonRect.right;
        float croppingHeight = buttonRect.height();
        croppingRectLeft.set(
                buttonLeftX - croppingHeight * ratio + widthProgress,
                bottom - croppingHeight,
                buttonLeftX + widthProgress,
                bottom
        );
        if (croppingRectLeft.centerX() < menuBackgroundRect.left) {
            float rectRightWidth = croppingRectLeft.width();
            croppingRectLeft.right = menuBackgroundRect.left + rectRightWidth / 2;
            croppingRectLeft.left = croppingRectLeft.right - croppingHeight * ratio;
        }
        croppingRectLeft.bottom = getYForMenuLeftBottomDirectionBottom(croppingRectLeft.centerX(), lastClipLeftBottom, bottom);
        lastClipLeftBottom = croppingRectLeft.bottom;
        float radi = croppingRectLeft.width() / 2;
        shareButtonAnglesClipPath.addRoundRect(
                croppingRectLeft,
                radi,
                radi,
                Path.Direction.CW
        );
        croppingRectRight.set(
                buttonRightX - widthProgress,
                bottom - croppingHeight,
                buttonRightX + croppingHeight * ratio - widthProgress,
                bottom
        );
        if (croppingRectRight.centerX() > menuBackgroundRect.right) {
            float rectRightWidth = croppingRectRight.width();
            croppingRectRight.left = menuBackgroundRect.right - rectRightWidth / 2;
            croppingRectRight.right = croppingRectRight.left + croppingHeight * ratio;
        }
        croppingRectRight.bottom = getYForMenuRightBottomDirectionBottom(croppingRectRight.centerX(), lastClipRightBottom, bottom);
        lastClipRightBottom = croppingRectRight.bottom;
        radi = croppingRectRight.width() / 2;
        shareButtonAnglesClipPath.addRoundRect(
                croppingRectRight,
                radi,
                radi,
                Path.Direction.CW
        );

//        testPaint.setColor(Color.RED);
//        testPaint.setAlpha(255);
//        canvas.drawPath(shareButtonAnglesClipPath, testPaint);
//        shareButtonAnglesClipPath.rewind();
//        testPaint.setColor(Color.BLUE);
//        testPaint.setAlpha(50);
        shareButtonAnglesClipPath.addRect(
                menuBackgroundRect.left,
                buttonRect.top,
                croppingRectLeft.centerX(),
                menuBackgroundRect.centerY(),
                Path.Direction.CW
        );
        shareButtonAnglesClipPath.addRect(
                croppingRectRight.centerX(),
                buttonRect.top,
                menuBackgroundRect.right,
                menuBackgroundRect.centerY(),
                Path.Direction.CW
        );
//        canvas.drawPath(shareButtonAnglesClipPath, testPaint);
    }

//    Paint testPaint = new Paint();

    private void addClippingElementsTopDirection() {
        float clipProgress = circleClipInterpolator.getInterpolation(progress);
        float top = Math.min(menuBackgroundRect.bottom, buttonRect.top);
//        float top = menuBackgroundRect.bottom;
        float ratio = 0.5f;
        float widthProgress = buttonRect.width() * clipProgress;
        float buttonLeftX = buttonRect.left;
        float buttonRightX = buttonRect.right;
        float croppingHeight = buttonRect.height();
        croppingRectLeft.set(
                buttonLeftX - croppingHeight * ratio + widthProgress,
                top,
                buttonLeftX + widthProgress,
                top + croppingHeight
        );
        if (croppingRectLeft.centerX() < menuBackgroundRect.left) {
            float rectRightWidth = croppingRectLeft.width();
            croppingRectLeft.right = menuBackgroundRect.left + rectRightWidth / 2;
            croppingRectLeft.left = croppingRectLeft.right - croppingHeight * ratio;
        }
        croppingRectLeft.top = getYForMenuLeftBottom(croppingRectLeft.centerX(), lastClipLeftTop, top);
        lastClipLeftTop = croppingRectLeft.top;
        float radi = croppingRectLeft.width() / 2;
        shareButtonAnglesClipPath.addRoundRect(
                croppingRectLeft,
                radi,
                radi,
                Path.Direction.CW
        );
        croppingRectRight.set(
                buttonRightX - widthProgress,
                top,
                buttonRightX + croppingHeight * ratio - widthProgress,
                top + croppingHeight
        );
        if (croppingRectRight.centerX() > menuBackgroundRect.right) {
            float rectRightWidth = croppingRectRight.width();
            croppingRectRight.left = menuBackgroundRect.right - rectRightWidth / 2;
            croppingRectRight.right = croppingRectRight.left + croppingHeight * ratio;
        }
        croppingRectRight.top = getYForMenuRightBottom(croppingRectRight.centerX(), lastClipRightTop, top);
        lastClipRightTop = croppingRectRight.top;
        radi = croppingRectRight.width() / 2;
        shareButtonAnglesClipPath.addRoundRect(
                croppingRectRight,
                radi,
                radi,
                Path.Direction.CW
        );

//        testPaint.setColor(Color.RED);
//        testPaint.setAlpha(255);
//        canvas.drawPath(shareButtonAnglesClipPath, testPaint);
//        shareButtonAnglesClipPath.rewind();
//        testPaint.setColor(Color.BLUE);
//        testPaint.setAlpha(50);
        shareButtonAnglesClipPath.addRect(
                menuBackgroundRect.left,
                menuBackgroundRect.centerY(),
                croppingRectLeft.centerX(),
                buttonRect.bottom,
                Path.Direction.CW
        );
        shareButtonAnglesClipPath.addRect(
                croppingRectRight.centerX(),
                menuBackgroundRect.centerY(),
                menuBackgroundRect.right,
                buttonRect.bottom,
                Path.Direction.CW
        );
//        canvas.drawPath(shareButtonAnglesClipPath, testPaint);
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setContainerFinalPositions(int finalContainerLayoutX, int finalContainerLayoutY) {
        this.finalContainerLayoutOffsetX = (int) (finalContainerLayoutX - quickShareBtnAttributes.rectF.left); // TODO: 27.10.2024
        this.finalContainerLayoutOffsetY = finalContainerLayoutY - startY;
    }

    @Override
    public void setAlpha(int alpha) {
// TODO: 24.10.2024
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
// TODO: 24.10.2024
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
