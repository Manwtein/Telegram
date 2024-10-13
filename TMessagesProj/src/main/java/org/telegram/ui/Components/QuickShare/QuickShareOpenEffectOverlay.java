package org.telegram.ui.Components.QuickShare;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.core.view.ViewCompat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.QuickShareContainerLayout;

public class QuickShareOpenEffectOverlay {

    @SuppressLint("StaticFieldLeak")
    public static QuickShareOpenEffectOverlay currentOverlay;
    public static QuickShareBtnAttributes quickShareBtnAttributes;
    private static long lastHapticTime;
    public final QuickShareEnterDrawable quickShareEnterDrawable;
    private final Context context;
    private final QuickShareContainerLayout targetView;
    private final View fromView;
    public FrameLayout windowView;
    public boolean started;
    public long startTime;
    public boolean isStories;
    float animationDuration = 600; // TODO: 25.10.2024 FINAL
    float refreshRate = 60f;
    float animateProgress;
    private boolean dismissed;
    private float dismissProgress;
    private ViewGroup decorView;

    public QuickShareOpenEffectOverlay(Context context,
                                       QuickShareContainerLayout targetView,
                                       View fromView,
                                       boolean isStories,
                                       int containerXPosition,
                                       int containerYPosition,
                                       QuickShareBtnAttributes quickShareBtnAttributes
    ) {
        this.isStories = isStories;
        this.context = context;
        this.targetView = targetView;
        this.fromView = fromView;
        animateProgress = 0f;
        windowView = new FrameLayout(context) {
            float v = 300;

            @Override
            protected void dispatchDraw(Canvas canvas) {
                float oneFrame = 1000 / refreshRate;
                if (targetView.firstDrawn) {
                    if (!dismissed) {
                        dismissed = true;
                        decorView.post(() -> removeCurrentView());
                    }
                    return;
                }
                if (dismissed) {
//                    quickShareEnterDrawable.dismissedProgress = dismissProgress;
                    if (dismissProgress != 1f) {
                        dismissProgress += oneFrame / v;
                        if (dismissProgress > 1f) {
                            dismissProgress = 1f;
                            decorView.post(() -> removeCurrentView());
                        }
                    }
                    if (dismissProgress != 1f) {
                        setAlpha(1f - dismissProgress);
                        quickShareEnterDrawable.draw(canvas);
                    }
                    invalidate();
                    return;
                }
                if (!started) {
                    invalidate();
                    return;
                }
                if (targetView.getMeasuredWidth() == 0) {
                    invalidate();
                    return;
                }

//                System.out.println("Manwtein, fromX = " + fromX + ", fromY = " + fromY + ", animateInProgressX = " + animateInProgressX + ", animateInProgressY = " + animateInProgressY);
//                System.out.println("Manwtein, x = " + x + ", y = " + y + ", alpha = " + (1f - animateOutProgress) + ", scale = " + scale);

                super.dispatchDraw(canvas);

                if (animateProgress != 1f) {
                    animateProgress += oneFrame / animationDuration;
                    if (animateProgress > 1f) {
                        animateProgress = 1f;
                    }
                }
                quickShareEnterDrawable.setProgress(animateProgress);
                quickShareEnterDrawable.draw(canvas);

                if (QuickShareOpenEffectOverlay.this.animateProgress == 1f) {
                    removeCurrentView();
                    currentOverlay = null;
                }
                invalidate();
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                // TODO: 28.10.2024
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                // TODO: 27.10.2024
            }
        };
        Display display = ViewCompat.getDisplay(fromView);
        if (display != null) {
            refreshRate = display.getRefreshRate();
        }
        quickShareEnterDrawable = new QuickShareEnterDrawable(
                targetView,
                quickShareBtnAttributes
        );
        quickShareEnterDrawable.setContainerFinalPositions(
                containerXPosition,
                containerYPosition
        );
    }


    public static void show(
            QuickShareContainerLayout targetView,
            View fromAnimationView,
            ChatActivity baseFragment,
            int containerXOffset,
            int containerYOffset,
            QuickShareBtnAttributes btnAttributes
    ) {
        if (targetView == null || fromAnimationView == null || btnAttributes == null || baseFragment == null) {
            return;
        }
        quickShareBtnAttributes = btnAttributes;

        QuickShareOpenEffectOverlay reactionsEffectOverlay = new QuickShareOpenEffectOverlay(
                targetView.getContext(),
                /*reactionsLayout,*/ targetView,
                fromAnimationView,
                false,
                containerXOffset,
                containerYOffset,
                btnAttributes
        );
        currentOverlay = reactionsEffectOverlay;
        reactionsEffectOverlay.decorView = (FrameLayout) baseFragment.getParentActivity().getWindow().getDecorView();
        reactionsEffectOverlay.decorView.addView(reactionsEffectOverlay.windowView);
        targetView.invalidate();
        if (targetView.getParent() != null) {
            ((View) targetView.getParent()).invalidate();
        }
    }

    public static void startAnimation(View fromView) {
        if (fromView == null) {
            dismissAll();
            return;
        }
        if (currentOverlay != null) {
            currentOverlay.started = true;
            currentOverlay.targetView.disableDefaultDrawing = true;
            currentOverlay.targetView.invalidate();
            quickShareBtnAttributes.drawDisabled = true;
            ((View) fromView.getParent()).invalidate();
            currentOverlay.startTime = System.currentTimeMillis();
            if (System.currentTimeMillis() - lastHapticTime > 200) {
                lastHapticTime = System.currentTimeMillis();
                currentOverlay.windowView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        }
    }


    public static void removeCurrent(boolean instant) {
        if (currentOverlay != null) {
            if (instant) {
                currentOverlay.removeCurrentView();
            } else {
                currentOverlay.dismissed = true;
            }
        }
        currentOverlay = null;
    }

    public static void dismissAll() {
        if (currentOverlay != null) {
            currentOverlay.dismissed = true;
        }
    }

    private void removeCurrentView() {
        try {
            targetView.disableDefaultDrawing = false;
            targetView.invalidate();
            quickShareBtnAttributes.drawDisabled = false;
            fromView.invalidate();
            View parent = (View) (fromView.getParent());
            if (parent != null) {
                parent.invalidate();
            }
            windowView.postDelayed(() -> AndroidUtilities.removeFromParent(windowView), 300);
        } catch (Exception e) {

        }
    }
}
