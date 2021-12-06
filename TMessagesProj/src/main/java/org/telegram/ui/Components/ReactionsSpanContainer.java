package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayList;

public class ReactionsSpanContainer extends ViewGroup {

    private AnimatorSet currentAnimation;
    private boolean animationStarted;
    private ArrayList<Animator> animators = new ArrayList<>();
    private View addingSpan;
    private View removingSpan;
    private int containerHeight = MeasureSpec.AT_MOST;
    private ArrayList<ReactionsSpan> allSpans = new ArrayList<>();
    private int selectedCount = 0;

    public ReactionsSpanContainer(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int maxWidth = width - AndroidUtilities.dp(26);
        int currentLineWidth = 0;
        int y = AndroidUtilities.dp(10);
        int allCurrentLineWidth = 0;
        int allY = AndroidUtilities.dp(10);
        int x;
        for (int a = 0; a < count; a++) {
            View child = getChildAt(a);
            if (!(child instanceof ReactionsSpan)) {
                continue;
            }
            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
            if (child != removingSpan && currentLineWidth + child.getMeasuredWidth() > maxWidth) {
                y += child.getMeasuredHeight() + AndroidUtilities.dp(8);
                currentLineWidth = 0;
            }
            if (allCurrentLineWidth + child.getMeasuredWidth() > maxWidth) {
                allY += child.getMeasuredHeight() + AndroidUtilities.dp(8);
                allCurrentLineWidth = 0;
            }
            x = AndroidUtilities.dp(13) + currentLineWidth;
            if (!animationStarted) {
                if (child == removingSpan) {
                    child.setTranslationX(AndroidUtilities.dp(13) + allCurrentLineWidth);
                    child.setTranslationY(allY);
                } else if (removingSpan != null) {
                    if (child.getTranslationX() != x) {
                        animators.add(ObjectAnimator.ofFloat(child, View.TRANSLATION_X, x));
                    }
                    if (child.getTranslationY() != y) {
                        animators.add(ObjectAnimator.ofFloat(child, View.TRANSLATION_Y, y));
                    }
                } else {
                    child.setTranslationX(x);
                    child.setTranslationY(y);
                }
            }
            if (child != removingSpan) {
                currentLineWidth += child.getMeasuredWidth() + AndroidUtilities.dp(9);
            }
            allCurrentLineWidth += child.getMeasuredWidth() + AndroidUtilities.dp(9);
        }
        int minWidth;
        if (AndroidUtilities.isTablet()) {
            minWidth = AndroidUtilities.dp(530 - 26 - 18 - 57 * 2) / 3;
        } else {
            minWidth = (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(26 + 18 + 57 * 2)) / 3;
        }
        if (maxWidth - currentLineWidth < minWidth) {
            currentLineWidth = 0;
            y += AndroidUtilities.dp(32 + 8);
        }
        if (maxWidth - allCurrentLineWidth < minWidth) {
            allY += AndroidUtilities.dp(32 + 8);
        }
//        editText.measure(MeasureSpec.makeMeasureSpec(maxWidth - currentLineWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
//        if (!animationStarted) {
//            int currentHeight = allY + AndroidUtilities.dp(32 + 10);
//            int fieldX = currentLineWidth + AndroidUtilities.dp(16);
//            fieldY = y;
//            if (currentAnimation != null) {
//                int resultHeight = y + AndroidUtilities.dp(32 + 10);
//                if (containerHeight != resultHeight) {
//                    animators.add(ObjectAnimator.ofInt(FilterUsersActivity.this, "containerHeight", resultHeight));
//                }
//                if (editText.getTranslationX() != fieldX) {
//                    animators.add(ObjectAnimator.ofFloat(editText, View.TRANSLATION_X, fieldX));
//                }
//                if (editText.getTranslationY() != fieldY) {
//                    animators.add(ObjectAnimator.ofFloat(editText, View.TRANSLATION_Y, fieldY));
//                }
//                editText.setAllowDrawCursor(false);
//                currentAnimation.playTogether(animators);
//                currentAnimation.start();
//                animationStarted = true;
//            } else {
//                containerHeight = currentHeight;
//                editText.setTranslationX(fieldX);
//                editText.setTranslationY(fieldY);
//            }
//        } else if (currentAnimation != null) {
//            if (!ignoreScrollEvent && removingSpan == null) {
//                editText.bringPointIntoView(editText.getSelectionStart());
//            }
//        }
        setMeasuredDimension(width, containerHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View child = getChildAt(a);
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    public void addSpan(final ReactionsSpan span, boolean animated) {
        allSpans.add(span);
        long uid = span.getUid();
        if (uid > Integer.MIN_VALUE + 7) {
            selectedCount++;
        }
//        selectedContacts.put(uid, span);

//        editText.setHintVisible(false);
        if (currentAnimation != null) {
            currentAnimation.setupEndValues();
            currentAnimation.cancel();
        }
        animationStarted = false;
        if (animated) {
            currentAnimation = new AnimatorSet();
            currentAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    addingSpan = null;
                    currentAnimation = null;
                    animationStarted = false;
//                    editText.setAllowDrawCursor(true);
                }
            });
            currentAnimation.setDuration(150);
            addingSpan = span;
            animators.clear();
            animators.add(ObjectAnimator.ofFloat(addingSpan, View.SCALE_X, 0.01f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(addingSpan, View.SCALE_Y, 0.01f, 1.0f));
            animators.add(ObjectAnimator.ofFloat(addingSpan, View.ALPHA, 0.0f, 1.0f));
        }
        addView(span);
    }

    public void removeSpan(final ReactionsSpan span) {
//        ignoreScrollEvent = true;
        long uid = span.getUid();
        if (uid > Integer.MIN_VALUE + 7) {
            selectedCount--;
        }
//        selectedContacts.remove(uid);
        allSpans.remove(span);
        span.setOnClickListener(null);

        if (currentAnimation != null) {
            currentAnimation.setupEndValues();
            currentAnimation.cancel();
        }
        animationStarted = false;
        currentAnimation = new AnimatorSet();
        currentAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                removeView(span);
                removingSpan = null;
                currentAnimation = null;
                animationStarted = false;
//                editText.setAllowDrawCursor(true);
                if (allSpans.isEmpty()) {
//                    editText.setHintVisible(true);
                }
            }
        });
        currentAnimation.setDuration(150);
        removingSpan = span;
        animators.clear();
        animators.add(ObjectAnimator.ofFloat(removingSpan, View.SCALE_X, 1.0f, 0.01f));
        animators.add(ObjectAnimator.ofFloat(removingSpan, View.SCALE_Y, 1.0f, 0.01f));
        animators.add(ObjectAnimator.ofFloat(removingSpan, View.ALPHA, 1.0f, 0.0f));
        requestLayout();
    }
}

