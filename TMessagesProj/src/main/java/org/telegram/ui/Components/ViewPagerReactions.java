package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.ReactionInteractor;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSearchCell;

import java.util.ArrayList;

public class ViewPagerReactions extends FrameLayout {

    public static int reactionTabMarginOffset = AndroidUtilities.dp(32);
    public static int reactionTabMargin = AndroidUtilities.dp(5);
    int currentPosition;
    int nextPosition;
    private View[] viewPages;
    private int[] viewTypes;

    protected SparseArray<View> viewsByType = new SparseArray<>();

    private int startedTrackingPointerId;
    private int startedTrackingX;
    private int startedTrackingY;
    private VelocityTracker velocityTracker;

    private AnimatorSet tabsAnimation;
    private boolean tabsAnimationInProgress;
    private boolean animatingForward;
    private float additionalOffset;
    private boolean backAnimation;
    private int maximumVelocity;
    private boolean startedTracking;
    private boolean maybeStartTracking;
    private static final Interpolator interpolator = t -> {
        --t;
        return t * t * t * t * t + 1.0F;
    };

    private final float touchSlop;

    private Adapter adapter;
    TabsView tabsView;

    ValueAnimator.AnimatorUpdateListener updateTabProgress = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (tabsAnimationInProgress) {
                float scrollProgress = Math.abs(viewPages[0].getTranslationX()) / (float) viewPages[0].getMeasuredWidth();
                if (tabsView != null) {
                    tabsView.selectTab(nextPosition, currentPosition, 1f - scrollProgress);
                }
            }
        }
    };
    private Rect rect = new Rect();

    public ViewPagerReactions(@NonNull Context context) {
        super(context);

        touchSlop = AndroidUtilities.getPixelsInCM(0.3f, true);
        maximumVelocity = ViewConfiguration.get(context).getScaledMaximumFlingVelocity();

        viewTypes = new int[2];
        viewPages = new View[2];
        setClipChildren(true);
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        viewTypes[0] = adapter.getItemViewType(currentPosition);
        viewPages[0] = adapter.createView(viewTypes[0]);
        adapter.bindView(viewPages[0], currentPosition, viewTypes[0]);
        addView(viewPages[0]);
        viewPages[0].setVisibility(View.VISIBLE);
        fillTabs();
    }

    public void invalidateItem(int position) {
        updateViewForIndex(position);
    }

    private ArrayList<TLRPC.TL_reactionCount> results;

    public TabsView createTabsView(ArrayList<TLRPC.TL_reactionCount> results, Theme.ResourcesProvider resourcesProvider) {
        this.results = results;
        tabsView = new TabsView(getContext(), resourcesProvider);
        tabsView.setDelegate(new TabsView.TabsViewDelegate() {
            @Override
            public void onPageSelected(int page, boolean forward) {
                animatingForward = forward;
                nextPosition = page;
                updateViewForIndex(1);

                if (forward) {
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
                } else {
                    viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth());
                }
            }

            @Override
            public void onPageScrolled(float progress) {
                if (progress == 1f) {
                    if (viewPages[1] != null) {
                        swapViews();
                        viewsByType.put(viewTypes[1], viewPages[1]);
                        removeView(viewPages[1]);
                        viewPages[0].setTranslationX(0);
                        viewPages[1] = null;
                    }
                    return;
                }
                if (viewPages[1] == null) {
                    return;
                }
                if (animatingForward) {
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() * (1f - progress));
                    viewPages[0].setTranslationX(-viewPages[0].getMeasuredWidth() * progress);
                } else {
                    viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth() * (1f - progress));
                    viewPages[0].setTranslationX(viewPages[0].getMeasuredWidth() * progress);
                }
            }

            @Override
            public void onSamePageSelected() {

            }

            @Override
            public boolean canPerformActions() {
                return !tabsAnimationInProgress && !startedTracking;
            }
        });
        fillTabs();
        return tabsView;
    }

    private void updateViewForIndex(int index) {
        int adapterPosition = index == 0 ? currentPosition : nextPosition;
        if (viewPages.length -1 < index) { // TODO 05/12/2021 Fuji team, RIDER-: fix
            return;
        }
        if (viewPages[index] == null) {
            viewTypes[index] = adapter.getItemViewType(adapterPosition);
            View v = viewsByType.get(viewTypes[index]);
            if (v == null) {
                v = adapter.createView(viewTypes[index]);
            } else {
                viewsByType.remove(viewTypes[index]);
            }
            if (v.getParent() != null) {
                ViewGroup parent = (ViewGroup) v.getParent();
                parent.removeView(v);
            }
            addView(v);
            viewPages[index] = v;
            adapter.bindView(viewPages[index], adapterPosition, viewTypes[index]);
            viewPages[index].setVisibility(View.VISIBLE);
        } else {
            if (viewTypes[index] == adapter.getItemViewType(adapterPosition)) {
                adapter.bindView(viewPages[index], adapterPosition, viewTypes[index]);
                viewPages[index].setVisibility(View.VISIBLE);
            } else {
                viewsByType.put(viewTypes[index], viewPages[index]);
                viewPages[index].setVisibility(View.GONE);
                removeView(viewPages[index]);
                viewTypes[index] = adapter.getItemViewType(adapterPosition);
                View v = viewsByType.get(viewTypes[index]);
                if (v == null) {
                    v = adapter.createView(viewTypes[index]);
                } else {
                    viewsByType.remove(viewTypes[index]);
                }
                addView(v);
                viewPages[index] = v;
                viewPages[index].setVisibility(View.VISIBLE);
                adapter.bindView(viewPages[index], adapterPosition, adapter.getItemViewType(adapterPosition));
            }
        }
    }

    private void fillTabs() {
        if (adapter != null && tabsView != null) {
            tabsView.removeTabs();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                tabsView.addTab(adapter.getItemId(i), results.get(i));
            }
        }
    }

    private boolean prepareForMoving(MotionEvent ev, boolean forward) {
        if ((!forward && currentPosition == 0) || (forward && currentPosition == adapter.getItemCount() - 1)) {
            return false;
        }

        getParent().requestDisallowInterceptTouchEvent(true);
        maybeStartTracking = false;
        startedTracking = true;
        startedTrackingX = (int) (ev.getX() + additionalOffset);
        if (tabsView != null) {
            tabsView.setEnabled(false);
        }

        animatingForward = forward;
        nextPosition = currentPosition + (forward ? 1 : -1);
        updateViewForIndex(1);
        if (forward) {
            viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
        } else {
            viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth());
        }
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (tabsView != null && tabsView.isAnimatingIndicator()) {
            return false;
        }
        if (checkTabsAnimationInProgress()) {
            return true;
        }
        onTouchEvent(ev);
        return startedTracking;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (maybeStartTracking && !startedTracking) {
            onTouchEvent(null);
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private View backButton;

    public void setBackButton(View backButton) {
        this.backButton = backButton;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev != null && ev.getY() < AndroidUtilities.dp(44) && backButton != null) {
            backButton.onTouchEvent(ev);
        }
        if (tabsView != null && tabsView.animatingIndicator) {
            return false;
        }
        if (ev != null) {
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(ev);
        }
        if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && checkTabsAnimationInProgress()) {
            startedTracking = true;
            startedTrackingPointerId = ev.getPointerId(0);
            startedTrackingX = (int) ev.getX();
            if (animatingForward) {
                if (startedTrackingX < viewPages[0].getMeasuredWidth() + viewPages[0].getTranslationX()) {
                    additionalOffset = viewPages[0].getTranslationX();
                } else {
                    swapViews();
                    animatingForward = false;
                    additionalOffset = viewPages[0].getTranslationX();
                }
            } else {
                if (startedTrackingX < viewPages[1].getMeasuredWidth() + viewPages[1].getTranslationX()) {
                    swapViews();
                    animatingForward = true;
                    additionalOffset = viewPages[0].getTranslationX();
                } else {
                    additionalOffset = viewPages[0].getTranslationX();
                }
            }
            tabsAnimation.removeAllListeners();
            tabsAnimation.cancel();
            tabsAnimationInProgress = false;
        } else if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN) {
            additionalOffset = 0;
        }

        if (!startedTracking && ev != null) {
            View child = findScrollingChild(this, ev.getX(), ev.getY());
            if (child != null && (child.canScrollHorizontally(1) || child.canScrollHorizontally(-1))) {
                return false;
            }
        }
        if (ev != null && ev.getAction() == MotionEvent.ACTION_DOWN && !startedTracking && !maybeStartTracking) {
            startedTrackingPointerId = ev.getPointerId(0);
            maybeStartTracking = true;
            startedTrackingX = (int) ev.getX();
            startedTrackingY = (int) ev.getY();
        } else if (ev != null && ev.getAction() == MotionEvent.ACTION_MOVE && ev.getPointerId(0) == startedTrackingPointerId) {
            int dx = (int) (ev.getX() - startedTrackingX + additionalOffset);
            int dy = Math.abs((int) ev.getY() - startedTrackingY);
            if (startedTracking && (animatingForward && dx > 0 || !animatingForward && dx < 0)) {
                if (!prepareForMoving(ev, dx < 0)) {
                    maybeStartTracking = true;
                    startedTracking = false;
                    viewPages[0].setTranslationX(0);
                    viewPages[1].setTranslationX(animatingForward ? viewPages[0].getMeasuredWidth() : -viewPages[0].getMeasuredWidth());
                    if (tabsView != null) {
                        tabsView.selectTab(currentPosition, 0, 0);
                    }
                }
            }
            if (maybeStartTracking && !startedTracking) {
                int dxLocal = (int) (ev.getX() - startedTrackingX);
                if (Math.abs(dxLocal) >= touchSlop && Math.abs(dxLocal) > dy) {
                    prepareForMoving(ev, dx < 0);
                }
            } else if (startedTracking) {
                viewPages[0].setTranslationX(dx);
                if (animatingForward) {
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() + dx);
                } else {
                    viewPages[1].setTranslationX(dx - viewPages[0].getMeasuredWidth());
                }
                float scrollProgress = Math.abs(dx) / (float) viewPages[0].getMeasuredWidth();
                if (tabsView != null) {
                    tabsView.selectTab(nextPosition, currentPosition, 1f - scrollProgress);
                }
            }
        } else if (ev == null || ev.getPointerId(0) == startedTrackingPointerId && (ev.getAction() == MotionEvent.ACTION_CANCEL || ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_POINTER_UP)) {
            velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
            float velX;
            float velY;
            if (ev != null && ev.getAction() != MotionEvent.ACTION_CANCEL) {
                velX = velocityTracker.getXVelocity();
                velY = velocityTracker.getYVelocity();
                if (!startedTracking) {
                    if (Math.abs(velX) >= 3000 && Math.abs(velX) > Math.abs(velY)) {
                        prepareForMoving(ev, velX < 0);
                    }
                }
            } else {
                velX = 0;
                velY = 0;
            }
            if (startedTracking) {
                float x = viewPages[0].getX();
                tabsAnimation = new AnimatorSet();
                if (additionalOffset != 0) {
                    if (Math.abs(velX) > 1500) {
                        backAnimation = animatingForward ? velX > 0 : velX < 0;
                    } else {
                        if (animatingForward) {
                            backAnimation = (viewPages[1].getX() > (viewPages[0].getMeasuredWidth() >> 1));
                        } else {
                            backAnimation = (viewPages[0].getX() < (viewPages[0].getMeasuredWidth() >> 1));
                        }
                    }
                } else {
                    backAnimation = Math.abs(x) < viewPages[0].getMeasuredWidth() / 3.0f && (Math.abs(velX) < 3500 || Math.abs(velX) < Math.abs(velY));
                }
                float distToMove;
                float dx;
                if (backAnimation) {
                    dx = Math.abs(x);
                    if (animatingForward) {
                        tabsAnimation.playTogether(
                                ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, 0),
                                ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, viewPages[1].getMeasuredWidth())
                        );
                    } else {
                        tabsAnimation.playTogether(
                                ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, 0),
                                ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, -viewPages[1].getMeasuredWidth())
                        );
                    }
                } else {
                    dx = viewPages[0].getMeasuredWidth() - Math.abs(x);
                    if (animatingForward) {
                        tabsAnimation.playTogether(
                                ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, -viewPages[0].getMeasuredWidth()),
                                ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, 0)
                        );
                    } else {
                        tabsAnimation.playTogether(
                                ObjectAnimator.ofFloat(viewPages[0], View.TRANSLATION_X, viewPages[0].getMeasuredWidth()),
                                ObjectAnimator.ofFloat(viewPages[1], View.TRANSLATION_X, 0)
                        );
                    }
                }
                ValueAnimator animator = ValueAnimator.ofFloat(0,1f);
                animator.addUpdateListener(updateTabProgress);
                tabsAnimation.playTogether(animator);
                tabsAnimation.setInterpolator(interpolator);

                int width = getMeasuredWidth();
                int halfWidth = width / 2;
                float distanceRatio = Math.min(1.0f, 1.0f * dx / (float) width);
                float distance = (float) halfWidth + (float) halfWidth * distanceInfluenceForSnapDuration(distanceRatio);
                velX = Math.abs(velX);
                int duration;
                if (velX > 0) {
                    duration = 4 * Math.round(1000.0f * Math.abs(distance / velX));
                } else {
                    float pageDelta = dx / getMeasuredWidth();
                    duration = (int) ((pageDelta + 1.0f) * 100.0f);
                }
                duration = Math.max(150, Math.min(duration, 600));

                tabsAnimation.setDuration(duration);
                tabsAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        tabsAnimation = null;
                        if (viewPages[1] != null) {
                            if (!backAnimation) {
                                swapViews();
                            }

                            viewsByType.put(viewTypes[1], viewPages[1]);
                            removeView(viewPages[1]);
                            viewPages[1].setVisibility(View.GONE);
                            viewPages[1] = null;
                        }
                        tabsAnimationInProgress = false;
                        maybeStartTracking = false;
                        if (tabsView != null) {
                            tabsView.setEnabled(true);
                        }
                    }
                });
                tabsAnimation.start();
                tabsAnimationInProgress = true;
                startedTracking = false;
            } else {
                maybeStartTracking = false;
                if (tabsView != null) {
                    tabsView.setEnabled(true);
                }
            }
            if (velocityTracker != null) {
                velocityTracker.recycle();
                velocityTracker = null;
            }
        }
        return startedTracking || maybeStartTracking;
    }

    private void swapViews() {
        View page = viewPages[0];
        viewPages[0] = viewPages[1];
        viewPages[1] = page;
        int p = currentPosition;
        currentPosition = nextPosition;
        nextPosition = p;
        p = viewTypes[0];
        viewTypes[0] = viewTypes[1];
        viewTypes[1] = p;

        onItemSelected(viewPages[0], viewPages[1], currentPosition, nextPosition);
    }


    public boolean checkTabsAnimationInProgress() {
        if (tabsAnimationInProgress) {
            boolean cancel = false;
            if (backAnimation) {
                if (Math.abs(viewPages[0].getTranslationX()) < 1) {
                    viewPages[0].setTranslationX(0);
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() * (animatingForward ? 1 : -1));
                    cancel = true;
                }
            } else if (Math.abs(viewPages[1].getTranslationX()) < 1) {
                viewPages[0].setTranslationX(viewPages[0].getMeasuredWidth() * (animatingForward ? -1 : 1));
                viewPages[1].setTranslationX(0);
                cancel = true;
            }
            if (cancel) {
                //showScrollbars(true);
                if (tabsAnimation != null) {
                    tabsAnimation.cancel();
                    tabsAnimation = null;
                }
                tabsAnimationInProgress = false;
            }
            return tabsAnimationInProgress;
        }
        return false;
    }

    public static float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5F;
        f *= 0.47123894F;
        return (float) Math.sin(f);
    }

    public void setPosition(int position) {
        if (tabsAnimation != null) {
            tabsAnimation.cancel();
        }
        if (viewPages[1] != null) {
            viewsByType.put(viewTypes[1], viewPages[1]);
            removeView(viewPages[1]);
            viewPages[1] = null;
        }
        if (currentPosition != position) {
            int oldPosition = currentPosition;
            currentPosition = position;
            View oldView = viewPages[0];
            updateViewForIndex(0);
            onItemSelected(viewPages[0], oldView, currentPosition, oldPosition);
            viewPages[0].setTranslationX(0);
            if (tabsView != null) {
                tabsView.selectTab(position, 0, 1f);
            }
        }
    }

    protected void onItemSelected(View currentPage, View oldPage, int position, int oldPosition) {

    }

    public abstract static class Adapter {
        public abstract int getItemCount();
        public abstract View createView(int viewType);
        public abstract void bindView(View view, int position, int viewType);

        public int getItemId(int position) {
            return position;
        }

        public String getItemTitle(int position) {
            return "";
        }

        public int getItemViewType(int position) {
            return 0;
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (direction == 0) {
            return false;
        }
        if (tabsAnimationInProgress || startedTracking) {
            return true;
        }
        boolean forward = direction > 0;
        if ((!forward && currentPosition == 0) || (forward && currentPosition == adapter.getItemCount() - 1)) {
            return false;
        }
        return true;
    }

    public View getCurrentView() {
        return viewPages[0];
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public static class TabsView extends FrameLayout {

        public interface TabsViewDelegate {
            void onPageSelected(int page, boolean forward);
            void onPageScrolled(float progress);
            void onSamePageSelected();
            boolean canPerformActions();
        }

        private static class Tab {
            public int id;
            public TLRPC.TL_reactionCount reactionCount;
            public int width;
            public int counter;
            public StaticLayout textLayout;

            public Tab(int i, TLRPC.TL_reactionCount reactionCount) {
                id = i;
                this.reactionCount = reactionCount;
            }
            public class VerticalImageSpan extends ImageSpan {

                public VerticalImageSpan(Drawable drawable) {
                    super(drawable);
                }

                @Override
                public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fontMetricsInt) {
                    Drawable drawable = getDrawable();
                    Rect rect = drawable.getBounds();
                    if (fontMetricsInt != null) {
                        Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
                        int fontHeight = fmPaint.descent - fmPaint.ascent;
                        int drHeight = rect.bottom - rect.top;
                        int centerY = fmPaint.ascent + fontHeight / 2;

                        fontMetricsInt.ascent = centerY - drHeight / 2;
                        fontMetricsInt.top = fontMetricsInt.ascent;
                        fontMetricsInt.bottom = centerY + drHeight / 2;
                        fontMetricsInt.descent = fontMetricsInt.bottom;
                    }
                    return rect.right;
                }

                @Override
                public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
                    Drawable drawable = getDrawable();
                    canvas.save();
                    Paint.FontMetricsInt fmPaint = paint.getFontMetricsInt();
                    int fontHeight = fmPaint.descent - fmPaint.ascent;
                    int centerY = y + fmPaint.descent - fontHeight / 2;
                    int transY = centerY - (drawable.getBounds().bottom - drawable.getBounds().top) / 2;
                    canvas.translate(x, transY);
                    if (LocaleController.isRTL) {
                        canvas.scale(-1, 1, drawable.getIntrinsicWidth() / 2, drawable.getIntrinsicHeight() / 2);
                    }
                    drawable.draw(canvas);
                    canvas.restore();
                }
            }

            public int getWidth(boolean store, TextPaint textPaint, Context context) {
                String formattedText = String.format("%s", LocaleController.formatShortNumber(Math.max(1, reactionCount.count), null));
                final String finalText = String.format("%s %s", reactionCount.reaction, formattedText);
                CharSequence buttonText;
                float additionalWidth = AndroidUtilities.dp(6);
                if (reactionCount.reaction.equals(ReactionInteractor.nonFilteredMessageKey)) {
                    SpannableString spannableString = SpannableString.valueOf(finalText);
                    Drawable drawable = context.getResources().getDrawable(R.drawable.ic_reaction_button_menu).mutate();
                    drawable.setBounds(0, 0, AndroidUtilities.dp(20), AndroidUtilities.dp(20));
                    drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_profile_tabSelectedText), PorterDuff.Mode.MULTIPLY));
                    spannableString.setSpan(new VerticalImageSpan(drawable), 0, ReactionInteractor.nonFilteredMessageKey.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    buttonText = spannableString;
                } else {
                    buttonText = Emoji.replaceEmoji(finalText, textPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false);
                }
                textLayout = new StaticLayout(buttonText, textPaint, (int) Math.ceil(textPaint.measureText(finalText)), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                float width = textLayout.getLineWidth(0) + additionalWidth;
//                float left = title.getLineLeft(0);
//                if (left < width) {
//                    width -= left;
//                }
                int buttonWidth = (int) Math.ceil(Math.max(width + AndroidUtilities.dp(20), AndroidUtilities.dp(46)));
                return buttonWidth;
            }

//            public boolean setTitle(String newTitle) {
//                if (TextUtils.equals(title, newTitle)) {
//                    return false;
//                }
//                title = newTitle;
//                return true;
//            }
        }

        public class ReactionTabView extends View {

            private Tab currentTab;
            private int textHeight;
            private int tabWidth;
            private int currentPosition;
            private RectF rect = new RectF();
            private String currentText;
            private StaticLayout textLayout;
            private int textOffsetX;

            public ReactionTabView(Context context) {
                super(context);
            }

            public void setTab(Tab tab, int position) {
                currentTab = tab;
                currentPosition = position;
//                setContentDescription(tab.title);
                requestLayout();
            }

            public int getId() {
                return currentTab.id;
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int w = currentTab.getWidth(false, textPaint, getContext()) + reactionTabMargin + additionalTabWidth; // TODO 05/12/2021 Fuji team, RIDER-fix: when 1 element
                tabWidth = w;
                setMeasuredDimension(w, MeasureSpec.getSize(heightMeasureSpec));
                final int dp = AndroidUtilities.dp(5);
//                selectorDrawable.setBounds(dp, 0, getMeasuredWidth() , getMeasuredHeight());;
            }

            @SuppressLint("DrawAllocation")
            @Override
            protected void onDraw(Canvas canvas) {
                backgroundPaint.setAlpha(100);
                final int bottom = getMeasuredHeight() - reactionTabMargin;
                final int right = getMeasuredWidth() - reactionTabMargin;
                RectF rectf = new RectF(0f + reactionTabMargin,
                        0 + reactionTabMargin,
                        right,
                        bottom);
                canvas.drawRoundRect(
                        rectf,
                        AndroidUtilities.dp(72),
                        AndroidUtilities.dp(72),
                        backgroundPaint
                );
                textPaint.setAlpha(255);
                canvas.save();
                canvas.translate(
                        right / 2- currentTab.textLayout.getWidth() / 2 ,
                        bottom / 2 - currentTab.textLayout.getHeight() / 2 + AndroidUtilities.dp(1)
                );
                currentTab.textLayout.draw(canvas);
                canvas.restore();
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                info.setSelected(currentTab != null && selectedTabId != -1 && currentTab.id == selectedTabId);
            }
        }

        private Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        {
            textPaint.setTextSize(AndroidUtilities.dp(15));
            textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        }

        private ArrayList<Tab> tabs = new ArrayList<>();

        private Bitmap crossfadeBitmap;
        private Paint crossfadePaint = new Paint();
        private float crossfadeAlpha;
        private boolean commitCrossfade;

        private boolean isEditing;
        private long lastEditingAnimationTime;
        private boolean editingForwardAnimation;
        private float editingAnimationProgress;
        private float editingStartAnimationProgress;

        private boolean orderChanged;

        private boolean ignoreLayout;

        private RecyclerListView listView;
        private LinearLayoutManager layoutManager;
        private ListAdapter adapter;

        private TabsViewDelegate delegate;

        private int currentPosition;
        private int selectedTabId = -1;
        private int allTabsWidth;

        private int additionalTabWidth;

        private boolean animatingIndicator;
        private float animatingIndicatorProgress;
        private int manualScrollingToPosition = -1;
        private int manualScrollingToId = -1;

        private int scrollingToChild = -1;
        private GradientDrawable selectorDrawable;

        private String tabLineColorKey = Theme.key_profile_tabSelectedLine;
        private String activeTextColorKey = Theme.key_profile_tabSelectedText;
        private String unactiveTextColorKey = Theme.key_profile_tabText;
        private String selectorColorKey = Theme.key_profile_tabSelector;
        private String backgroundColorKey = Theme.key_actionBarDefault;
        private String textColorKey = Theme.key_dialogSearchText;

        private int prevLayoutWidth;

        private boolean invalidated;

        private boolean isInHiddenMode;
        private float hideProgress;

        private CubicBezierInterpolator interpolator = CubicBezierInterpolator.EASE_OUT_QUINT;

        private SparseIntArray positionToId = new SparseIntArray(5);
        private SparseIntArray idToPosition = new SparseIntArray(5);
        private SparseIntArray positionToWidth = new SparseIntArray(5);
        private SparseIntArray positionToX = new SparseIntArray(5);

        private boolean animationRunning;
        private long lastAnimationTime;
        private float animationTime;
        private int previousPosition;
        private int previousId;
        private Runnable animationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!animatingIndicator) {
                    return;
                }
                long newTime = SystemClock.elapsedRealtime();
                long dt = (newTime - lastAnimationTime);
                if (dt > 17) {
                    dt = 17;
                }
                animationTime += dt / 200.0f;
                setAnimationIdicatorProgress(interpolator.getInterpolation(animationTime));
                if (animationTime > 1.0f) {
                    animationTime = 1.0f;
                }
                if (animationTime < 1.0f) {
                    AndroidUtilities.runOnUIThread(animationRunnable);
                } else {
                    animatingIndicator = false;
                    setEnabled(true);
                    if (delegate != null) {
                        delegate.onPageScrolled(1.0f);
                    }
                }
            }
        };
        ValueAnimator tabsAnimator;
        private float animationValue;

        public TabsView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
//            textPaint.setColor(Theme.getColor(selectorColorKey));


//            textPaint.setColor(Theme.getColor(tabLineColorKey));
//            textPaint.setColor(Theme.getColor(selectorColorKey));
            textPaint.setColor(Theme.getColor(textColorKey));
            backgroundPaint.setColor(Theme.getColor(activeTextColorKey));

            selectorDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, null);
            float rad = AndroidUtilities.dp(72);
            selectorDrawable.setCornerRadii(new float[]{rad, rad, rad, rad, 0, 0, 0, 0});
            backgroundPaint.setAlpha(50);
            selectorDrawable.setColor(backgroundPaint.getColor());

            setHorizontalScrollBarEnabled(false);
            listView = new RecyclerListView(context) {

                @Override
                public void addView(View child, int index, ViewGroup.LayoutParams params) {
                    super.addView(child, index, params);
                    if (isInHiddenMode) {
                        child.setScaleX(0.3f);
                        child.setScaleY(0.3f);
                        child.setAlpha(0);
                    } else {
                        child.setScaleX(1f);
                        child.setScaleY(1f);
                        child.setAlpha(1f);
                    }
                }

                @Override
                public void setAlpha(float alpha) {
                    super.setAlpha(alpha);
                    TabsView.this.invalidate();
                }

                @Override
                protected boolean canHighlightChildAt(View child, float x, float y) {
                    if (isEditing) {
                        ReactionTabView reactionTabView = (ReactionTabView) child;
                        int side = AndroidUtilities.dp(6);
                        if (reactionTabView.rect.left - side < x && reactionTabView.rect.right + side > x) {
                            return false;
                        }
                    }
                    return super.canHighlightChildAt(child, x, y);
                }
            };
            ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
            listView.setSelectorType(7);
//            listView.selectorDrawable.setBounds(0, 0, 100, 0);
            listView.setSelectorRadius(AndroidUtilities.dp(72));
            listView.setSelectorDrawableColor(Theme.getColor(activeTextColorKey));
            listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {
                @Override
                public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                    LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                        @Override
                        protected void onTargetFound(View targetView, RecyclerView.State state, Action action) {
                            int dx = calculateDxToMakeVisible(targetView, getHorizontalSnapPreference());
                            if (dx > 0 || dx == 0 && targetView.getLeft() - AndroidUtilities.dp(21) < 0) {
                                dx += AndroidUtilities.dp(60);
                            } else if (dx < 0 || dx == 0 && targetView.getRight() + AndroidUtilities.dp(21) > getMeasuredWidth()) {
                                dx -= AndroidUtilities.dp(60);
                            }

                            final int dy = calculateDyToMakeVisible(targetView, getVerticalSnapPreference());
                            final int distance = (int) Math.sqrt(dx * dx + dy * dy);
                            final int time = Math.max(180, calculateTimeForDeceleration(distance));
                            if (time > 0) {
                                action.update(-dx, -dy, time, mDecelerateInterpolator);
                            }
                        }
                    };
                    linearSmoothScroller.setTargetPosition(position);
                    startSmoothScroll(linearSmoothScroller);
                }

                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull RecyclerView.Recycler recycler, @NonNull RecyclerView.State state, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(recycler, state, info);
                    if (isInHiddenMode) {
                        info.setVisibleToUser(false);
                    }
                }
            });
            listView.setPadding(AndroidUtilities.dp(7), 0, AndroidUtilities.dp(7), 0);
            listView.setClipToPadding(false);
            listView.setDrawSelectorBehind(true);
            listView.setAdapter(adapter = new ListAdapter(context));
            listView.setOnItemClickListener((view, position, x, y) -> {
                if (!delegate.canPerformActions()) {
                    return;
                }
                ReactionTabView reactionTabView = (ReactionTabView) view;
                if (position == currentPosition && delegate != null) {
                    delegate.onSamePageSelected();
                    return;
                }
                scrollToTab(reactionTabView.currentTab.id, position);
            });
            listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    invalidate();
                }
            });
            addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        }

        public void setDelegate(TabsViewDelegate filterTabsViewDelegate) {
            delegate = filterTabsViewDelegate;
        }

        public boolean isAnimatingIndicator() {
            return animatingIndicator;
        }

        public void scrollToTab(int id, int position) {
            boolean scrollingForward = currentPosition < position;
            scrollingToChild = -1;
            previousPosition = currentPosition;
            previousId = selectedTabId;
            currentPosition = position;
            selectedTabId = id;

            if (tabsAnimator != null) {
                tabsAnimator.cancel();
            }
            if (animatingIndicator) {
                animatingIndicator = false;
            }

            animationTime = 0;
            animatingIndicatorProgress = 0;
            animatingIndicator = true;
            setEnabled(false);


            if (delegate != null) {
                delegate.onPageSelected(id, scrollingForward);
            }
            scrollToChild(position);
            tabsAnimator = ValueAnimator.ofFloat(0,1f);
            tabsAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float progress = (float) valueAnimator.getAnimatedValue();
                    setAnimationIdicatorProgress(progress);
                    if (delegate != null) {
                        delegate.onPageScrolled(progress);
                    }
                }
            });
            tabsAnimator.setDuration(250);
            tabsAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            tabsAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animatingIndicator = false;
                    setEnabled(true);
                    if (delegate != null) {
                        delegate.onPageScrolled(1.0f);
                    }
                    invalidate();
                }
            });
            tabsAnimator.start();
        }

        public void setAnimationIdicatorProgress(float value) {
            animatingIndicatorProgress = value;
            listView.invalidateViews();
            invalidate();
            if (delegate != null) {
                delegate.onPageScrolled(value);
            }
        }

        public Drawable getSelectorDrawable() {
            return selectorDrawable;
        }

        public RecyclerListView getTabsContainer() {
            return listView;
        }

        public int getNextPageId(boolean forward) {
            return positionToId.get(currentPosition + (forward ? 1 : -1), -1);
        }

        public void addTab(int id, TLRPC.TL_reactionCount reactionCount) {
            int position = tabs.size();
            if (position == 0 && selectedTabId == -1) {
                selectedTabId = id;
            }
            positionToId.put(position, id);
            idToPosition.put(id, position);
            if (selectedTabId != -1 && selectedTabId == id) {
                currentPosition = position;
            }
            Tab tab = new Tab(id, reactionCount);
            allTabsWidth += tab.getWidth(true, textPaint, getContext()) + reactionTabMarginOffset;
            tabs.add(tab);
        }

        public void removeTabs() {
            tabs.clear();
            positionToId.clear();
            idToPosition.clear();
            positionToWidth.clear();
            positionToX.clear();
            allTabsWidth = 0;
        }

        public void finishAddingTabs() {
            adapter.notifyDataSetChanged();
        }

        public int getCurrentTabId() {
            return selectedTabId;
        }

        public int getFirstTabId() {
            return positionToId.get(0, 0);
        }

        private void updateTabsWidths() {
            positionToX.clear();
            positionToWidth.clear();
            int xOffset = AndroidUtilities.dp(5);
            for (int a = 0, N = tabs.size(); a < N; a++) {
                int tabWidth = tabs.get(a).getWidth(false, textPaint, getContext());
                positionToWidth.put(a, tabWidth);
                positionToX.put(a, xOffset + additionalTabWidth / 2);
                xOffset += tabWidth + reactionTabMargin + additionalTabWidth;
            }
        }

        @Override
        protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
            boolean result = super.drawChild(canvas, child, drawingTime);
            if (child == listView) {
                final int height = getMeasuredHeight();
                boolean invalidate = false;
                if (isInHiddenMode && hideProgress != 1f) {
                    hideProgress += 0.1f;
                    if (hideProgress > 1f) {
                        hideProgress = 1f;
                    }
                    invalidate();
                } else if (!isInHiddenMode && hideProgress != 0) {
                    hideProgress -= 0.12f;
                    if (hideProgress < 0) {
                        hideProgress = 0;
                    }
                    invalidate();
                }
                int indicatorX = 0;
                int indicatorWidth = 0;
                if (animatingIndicator || manualScrollingToPosition != -1) {
                    int position = layoutManager.findFirstVisibleItemPosition();
                    if (position != RecyclerListView.NO_POSITION) {
                        RecyclerListView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            int idx1;
                            int idx2;
                            if (animatingIndicator) {
                                idx1 = previousPosition;
                                idx2 = currentPosition;
                            } else {
                                idx1 = currentPosition;
                                idx2 = manualScrollingToPosition;
                            }
                            int prevX = positionToX.get(idx1);
                            int newX = positionToX.get(idx2);
                            int prevW = positionToWidth.get(idx1);
                            int newW = positionToWidth.get(idx2);
                            if (additionalTabWidth != 0) {
                                indicatorX = (int) (prevX + (newX - prevX) * animatingIndicatorProgress) + reactionTabMargin / 2;
                            } else {
                                int x = positionToX.get(position);
                                indicatorX = (int) (prevX + (newX - prevX) * animatingIndicatorProgress) - (x - holder.itemView.getLeft()) + reactionTabMargin / 2;
                            }
                            indicatorWidth = (int) (prevW + (newW - prevW) * animatingIndicatorProgress);
                        }
                    }
                } else {
                    RecyclerListView.ViewHolder holder = listView.findViewHolderForAdapterPosition(currentPosition);
                    if (holder != null) {
                        ReactionTabView reactionTabView = (ReactionTabView) holder.itemView;
                        indicatorWidth = reactionTabView.tabWidth;
                        indicatorX = (int) (reactionTabView.getX() + (reactionTabView.getMeasuredWidth() - indicatorWidth) / 2);
                    }
                }
//                if (indicatorWidth != 0) {
//                    selectorDrawable.setBounds(indicatorX, (int) (height - AndroidUtilities.dpr(4) + hideProgress * AndroidUtilities.dpr(4)), indicatorX + indicatorWidth, (int) (height + hideProgress * AndroidUtilities.dpr(4)));
//                    selectorDrawable.draw(canvas);
//                }


                if (crossfadeBitmap != null) {
                    crossfadePaint.setAlpha((int) (crossfadeAlpha * 255));
//                    canvas.drawBitmap(crossfadeBitmap, 0, 0, crossfadePaint);
                }
                RectF rect = new RectF(
                        indicatorX + reactionTabMargin,
//                        (int) (/*AndroidUtilities.dpr(4) + *//*hideProgress * AndroidUtilities.dpr(4)*/),
                        reactionTabMargin,
                        indicatorX + indicatorWidth - reactionTabMargin,
                        (int) (height + hideProgress * AndroidUtilities.dpr(4)) - reactionTabMargin
                );
                listView.selectorDrawable.setBounds(
                        (int)rect.left + AndroidUtilities.dp(5),
                        (int)rect.top + AndroidUtilities.dp(5),
                        (int)rect.right - AndroidUtilities.dp(5),
                        (int)(rect.bottom) - AndroidUtilities.dp(5));
                listView.selectorDrawable.setAlpha((int) (50 * listView.getAlpha()));
                textPaint.setAlpha(255);
                final Path path = new Path();
                path.addRoundRect(
                        rect.left + AndroidUtilities.dp(2),
                        rect.top + AndroidUtilities.dp(2),
                        rect.right - AndroidUtilities.dp(2),
                        rect.bottom - AndroidUtilities.dp(2),
                        AndroidUtilities.dp(72),
                        AndroidUtilities.dp(72),
                        Path.Direction.CW);
                canvas.save();
                canvas.clipOutPath(path);
                backgroundPaint.setAlpha(255);
                canvas.drawRoundRect(
                        rect,
                        AndroidUtilities.dp(72),
                        AndroidUtilities.dp(72),
//                        deletePaint.getThemedPaint(Theme.key_paint_chatActionBackground)
                        backgroundPaint
                );
                canvas.restore();
            }

            return result;
        }
//        private void drawReactionButtons(Canvas canvas, ArrayList<ChatMessageCell.ReactionButton> reactionButtons, float alpha) {
//            float height = 0;
//            for (int a = 0; a < reactionButtons.size(); a++) {
//                ChatMessageCell.ReactionButton button = reactionButtons.get(a);
//                int bottom = button.y + button.height;
//                if (bottom > height) {
//                    height = bottom;
//                }
//            }
//            float top = layoutHeight - AndroidUtilities.dp(2) + transitionParams.deltaBottom;
//            final boolean isUnderContent = reactionButtons.get(0).isUnderContent;
//            int addX;
//            if (isUnderContent) {
//                if (currentMessageObject.isOutOwner()) {
//                    addX = getMeasuredWidth() - widthForReactionButtons - AndroidUtilities.dp(10);
//                } else {
//                    addX = backgroundDrawableLeft + AndroidUtilities.dp(mediaBackground || drawPinnedBottom ? 1 : 7);
//                }
//            } else {
//                top -= contentReactionsHeight;
//                if (drawCommentButton) {
//                    top -= AndroidUtilities.dp(shouldDrawTimeOnMedia() ? 41.3f : 43);
//                }
//                if (currentMessageObject.isOutOwner()) {
//                    addX = getMeasuredWidth() - widthForReactionButtons;
//                } else {
//                    addX = backgroundDrawableLeft + AndroidUtilities.dp(mediaBackground || drawPinnedBottom ? 1 : 7);
//                }
//            }
//
//            rect.set(0, top, getMeasuredWidth(), top + height);
//            if (alpha != 1f) {
//                canvas.saveLayerAlpha(rect, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);
//            } else {
//                canvas.save();
//            }
//
//            for (int a = 0; a < reactionButtons.size(); a++) {
//                ChatMessageCell.ReactionButton button = reactionButtons.get(a);
//                float y = button.y + top;
//                rect.set(
//                        button.x + addX,
//                        y,
//                        button.x + addX + button.width,
//                        y + button.height
//                );
//                if (!button.isUnderContent) {
////                Paint debugPain = new Paint();
//////                debugPain.setAlpha(120);
//////                debugPain.setColor(Color.RED);
//////                canvas.drawRect(0,0, layoutHeight, layoutWidth,debugPain );
////                debugPain.setColor(Color.BLUE);
////                debugPain.setAlpha(50);
////                canvas.drawRect(rect, debugPain);
//                }
//                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
//                canvas.drawRoundRect(
//                        rect,
//                        AndroidUtilities.dp(72),
//                        AndroidUtilities.dp(72),
//                        getThemedPaint(Theme.key_paint_chatActionBackground)
//                );
//
//                if (hasGradientService()) {
//                    canvas.drawRoundRect(
//                            rect,
//                            AndroidUtilities.dp(72),
//                            AndroidUtilities.dp(72),
//                            Theme.chat_actionBackgroundGradientDarkenPaint
//                    );
//                }
////                if (Build.VERSION.SDK_INT >= 21 && a == pressedBotReactionButton) {
////                    canvas.save();
////                    Path path = new Path();
////                    path.addRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), Path.Direction.CW);
////                    canvas.clipPath(path);
////                    selectorDrawableMaskType[0] = 0;
////                    selectorDrawable[0].setBounds(
////                            (int)rect.left,
////                            (int)rect.top,
////                            (int)rect.right,
////                            (int)rect.bottom
////                    );
////                    selectorDrawable[0].draw(canvas);
////                    canvas.restore();
////                }
//                if (button.reaction.chosen) {
//                    Paint backPaint = getThemedPaint(Theme.key_paint_chatReactionButton);
//
////                if (currentMessageObject.isOutOwner()) {
////                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
////                } else {
////                    instantDrawable = Theme.chat_msgInInstantDrawable;
////                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
//
//                    if (button.isUnderContent) {
//                        backPaint.setColor(getThemedColor(Theme.key_chat_reactionUnderContentSelected));
//                    } else {
//                        backPaint.setColor(getThemedColor(Theme.key_chat_reactionInContentSelected));
//                    }
//                    canvas.drawRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), backPaint);
//                }
//                canvas.save();
//                canvas.translate(
//                        button.x + addX + AndroidUtilities.dp(5),
//                        y + (button.height - button.title.getLineBottom(button.title.getLineCount() - 1)) / 2
//                );
//                button.title.draw(canvas);
//                canvas.restore();
//            }
//            canvas.restore();
//        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if (!tabs.isEmpty()) {
                int width = MeasureSpec.getSize(widthMeasureSpec) - AndroidUtilities.dp(7) - AndroidUtilities.dp(7);
                int prevWidth = additionalTabWidth;
//                additionalTabWidth = allTabsWidth < width ? (width - allTabsWidth) / tabs.size() : 0;
                if (prevWidth != additionalTabWidth) {
                    ignoreLayout = true;
                    adapter.notifyDataSetChanged();
                    ignoreLayout = false;
                }
                updateTabsWidths();
                invalidated = false;
            }
            super.onMeasure(widthMeasureSpec, AndroidUtilities.dp(36));
        }

        public void updateColors() {
            selectorDrawable.setColor(Theme.getColor(activeTextColorKey));
            selectorDrawable.setAlpha(50);
            listView.invalidateViews();
            listView.invalidate();
            invalidate();
        }

        @Override
        public void requestLayout() {
            if (ignoreLayout) {
                return;
            }
            super.requestLayout();
        }

        private void scrollToChild(int position) {
            if (tabs.isEmpty() || scrollingToChild == position || position < 0 || position >= tabs.size()) {
                return;
            }
            scrollingToChild = position;
            listView.smoothScrollToPosition(position);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);

            if (prevLayoutWidth != r - l) {
                prevLayoutWidth = r - l;
                scrollingToChild = -1;
                if (animatingIndicator) {
                    AndroidUtilities.cancelRunOnUIThread(animationRunnable);
                    animatingIndicator = false;
                    setEnabled(true);
                    if (delegate != null) {
                        delegate.onPageScrolled(1.0f);
                    }
                }
            }
        }
        public void selectTab(int currentPosition, int nextPosition, float progress) {
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1.0f) {
                progress = 1.0f;
            }

            this.currentPosition = currentPosition;
            selectedTabId = positionToId.get(currentPosition);

            if (progress > 0) {
                manualScrollingToPosition = nextPosition;
                manualScrollingToId = positionToId.get(nextPosition);
            } else {
                manualScrollingToPosition = -1;
                manualScrollingToId = -1;
            }
            animatingIndicatorProgress = progress;
            listView.invalidateViews();
            invalidate();
            scrollToChild(currentPosition);

            if (progress >= 1.0f) {
                manualScrollingToPosition = -1;
                manualScrollingToId = -1;
                this.currentPosition = nextPosition;
                selectedTabId = positionToId.get(nextPosition);
            }
        }

        public void selectTabWithId(int id, float progress) {
            int position = idToPosition.get(id, -1);
            if (position < 0) {
                return;
            }
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1.0f) {
                progress = 1.0f;
            }

            if (progress > 0) {
                manualScrollingToPosition = position;
                manualScrollingToId = id;
            } else {
                manualScrollingToPosition = -1;
                manualScrollingToId = -1;
            }
            animatingIndicatorProgress = progress;
            listView.invalidateViews();
            invalidate();
            scrollToChild(position);

            if (progress >= 1.0f) {
                manualScrollingToPosition = -1;
                manualScrollingToId = -1;
                currentPosition = position;
                selectedTabId = id;
            }
        }

        private int getChildWidth(TextView child) {
            Layout layout = child.getLayout();
            if (layout != null) {
                int w = (int) Math.ceil(layout.getLineWidth(0)) + AndroidUtilities.dp(2);
                if (child.getCompoundDrawables()[2] != null) {
                    w += child.getCompoundDrawables()[2].getIntrinsicWidth() + AndroidUtilities.dp(6);
                }
                return w;
            } else {
                return child.getMeasuredWidth();
            }
        }

        public void onPageScrolled(int position, int first) {
            if (currentPosition == position) {
                return;
            }
            currentPosition = position;
            if (position >= tabs.size()) {
                return;
            }
            if (first == position && position > 1) {
                scrollToChild(position - 1);
            } else {
                scrollToChild(position);
            }
            invalidate();
        }

        public boolean isEditing() {
            return isEditing;
        }

        public void setIsEditing(boolean value) {
            isEditing = value;
            editingForwardAnimation = true;
            listView.invalidateViews();
            invalidate();
            if (!isEditing && orderChanged) {
                MessagesStorage.getInstance(UserConfig.selectedAccount).saveDialogFiltersOrder();
                TLRPC.TL_messages_updateDialogFiltersOrder req = new TLRPC.TL_messages_updateDialogFiltersOrder();
                ArrayList<MessagesController.DialogFilter> filters = MessagesController.getInstance(UserConfig.selectedAccount).dialogFilters;
                for (int a = 0, N = filters.size(); a < N; a++) {
                    MessagesController.DialogFilter filter = filters.get(a);
                    req.order.add(filters.get(a).id);
                }
                ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> {

                });
                orderChanged = false;
            }
        }

        private class ListAdapter extends RecyclerListView.SelectionAdapter {

            private Context mContext;

            public ListAdapter(Context context) {
                mContext = context;
            }

            @Override
            public int getItemCount() {
                return tabs.size();
            }

            @Override
            public long getItemId(int i) {
                return i;
            }

            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return true;
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerListView.Holder(new ReactionTabView(mContext));
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ReactionTabView reactionTabView = (ReactionTabView) holder.itemView;
                reactionTabView.setTab(tabs.get(position), position);
            }

            @Override
            public int getItemViewType(int i) {
                return 0;
            }

        }

        public void hide(boolean hide, boolean animated) {
            isInHiddenMode = hide;

            if (animated) {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    listView.getChildAt(i).animate().alpha(hide ? 0 : 1f).scaleX(hide ? 0 : 1f).scaleY(hide ? 0 : 1f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(220).start();
                }
            } else {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View v = listView.getChildAt(i);
                    v.setScaleX(hide ? 0 : 1f);
                    v.setScaleY(hide ? 0 : 1f);
                    v.setAlpha(hide ? 0 : 1f);
                }
                hideProgress = hide ? 1 : 0;
            }
            invalidate();
        }
    }

    private View findScrollingChild(ViewGroup parent, float x, float y) {
        int n = parent.getChildCount();
        for (int i = 0; i < n; i++) {
            View child = parent.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE) {
                continue;
            }
            child.getHitRect(rect);
            if (rect.contains((int) x, (int) y)) {
                if (child.canScrollHorizontally(-1)) {
                    return child;
                } else if (child instanceof ViewGroup) {
                    View v = findScrollingChild((ViewGroup) child, x - rect.left, y - rect.top);
                    if (v != null) {
                        return v;
                    }
                }
            }
        }
        return null;
    }


}
