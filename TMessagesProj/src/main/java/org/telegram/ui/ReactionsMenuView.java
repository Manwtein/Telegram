package org.telegram.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.ReactionsAdapter;
import org.telegram.ui.Cells.ReactionCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Random;

public class ReactionsMenuView extends FrameLayout {

    final Runnable animRunnable;
    boolean ignoreLayout;
    private RecyclerListView reactionListView;
    private ReactionsAdapter reactionsAdapter;
    private ImageView stickersPanelArrow;
    private AnimatorSet runningAnimation;
    private ChatActivity.ThemeDelegate themeDelegate;
    private OnClickListener onClickListener;
    private final float maxVisibleReactionsCount = 5f;
    Drawable singleMenuDrawable;
    Drawable manyMenuDrawable;
//    Drawable bubbleDrawable;

    public ReactionsMenuView(ArrayList<TLRPC.TL_availableReaction> reactions, @NonNull Context context, ChatActivity.ThemeDelegate themeDelegate, MessagesController messagesController, OnClickListener onClickListener) {
        super(context);
//        menuDrawable = ContextCompat.getDrawable(getContext(), R.drawable.reactions_menu).mutate(); // TODO 02/12/2021 Fuji team, RIDER-: check theme
        singleMenuDrawable = ContextCompat.getDrawable(getContext(), R.drawable.single_react_menu).mutate(); // TODO 02/12/2021 Fuji team, RIDER-: check theme
//                Drawable shadowDrawable2 = ContextCompat.getDrawable(contentView.getContext(), R.drawable.reactions_popup_menu_new).mutate();
        singleMenuDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
        manyMenuDrawable = ContextCompat.getDrawable(getContext(), R.drawable.many_react_menu).mutate(); // TODO 02/12/2021 Fuji team, RIDER-: check theme
//                Drawable shadowDrawable2 = ContextCompat.getDrawable(contentView.getContext(), R.drawable.reactions_popup_menu_new).mutate();
        manyMenuDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));

//        bubbleDrawable = ContextCompat.getDrawable(getContext(), R.drawable.react_menu_bubble).mutate(); // TODO 02/12/2021 Fuji team, RIDER-: check theme
//                Drawable shadowDrawable2 = ContextCompat.getDrawable(contentView.getContext(), R.drawable.reactions_popup_menu_new).mutate();
//        bubbleDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
        this.onClickListener = onClickListener;
//        stickersPanelArrow = new ImageView(context);
        this.themeDelegate = themeDelegate;
//        stickersPanelArrow.setImageResource(R.drawable.reaction_oval);
////        stickersPanelArrow.setColorFilter(new PorterDuffColorFilter(themeDelegate.getColor(Theme.key_chat_stickersHintPanel), PorterDuff.Mode.MULTIPLY)); // TODO 29/11/2021 Fuji team, RIDER-: check theme
//        addView(stickersPanelArrow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, 0, 0));
        setClipChildren(false);
//        setTranslationX(AndroidUtilities.dp(40));
//        setTranslationY(AndroidUtilities.dp(17));
        reactionListView = new RecyclerListView(context, themeDelegate) {
            @Override
            public boolean onInterceptTouchEvent(MotionEvent event) {
//                boolean result = ContentPreviewViewer.getInstance().onInterceptTouchEvent(event, reactionListView, 0, contentPreviewViewerDelegate, themeDelegate);
                return super.onInterceptTouchEvent(event) /*|| result*/;
            }

            @Override
            protected void onMeasure(int widthSpec, int heightSpec) {
                final int size1 = reactionsAdapter.reactions.size();
//                float size = size1 * 40 + (size1 - 1) * 2;
                float size = size1 * 40 + (size1 - 1) * 2  - (size1 == 1 ? 0 : 0);
                float maxSize = maxVisibleReactionsCount * 40 + 5.8f * 2;
                super.onMeasure(
                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(Math.min(size, maxSize)), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(40), MeasureSpec.EXACTLY)
                );
            }

        };
        reactionListView.setTag(3);
//        reactionListView.setBackgroundColor(Color.argb(50, 255, 0,0));
//        reactionListView.setOnTouchListener((v, event) -> ContentPreviewViewer.getInstance().onTouch(event, reactionListView, 0, null, contentPreviewViewerDelegate, themeDelegate));
        reactionListView.setDisallowInterceptTouchEvents(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        reactionListView.setLayoutManager(layoutManager);
        reactionListView.setClipToPadding(false);
        reactionListView.setClipChildren(false);
        reactionListView.setOverScrollMode(RecyclerListView.OVER_SCROLL_NEVER);
//        reactionListView.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14), 0);
        addView(reactionListView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                40,
                Gravity.CENTER
        ));
        createReactionListView();
//        final ArrayList<TLRPC.TL_availableReaction> availableReactions = new ArrayList<>();
//        availableReactions.add(messagesController.getAvailableReactions().get(0));
//        availableReactions.add(messagesController.getAvailableReactions().get(1));
//        availableReactions.addAll(messagesController.getAvailableReactions(messageObject)); // TODO 01/12/2021 Fuji team, RIDER-: remove redundant
        reactionsAdapter.reactions = reactions;
        reactionsAdapter.notifyDataSetChanged();
//        setWillNotDraw(false);
//        reactionListView.setBackgroundColor(Color.RED);
//        setBackground(ContextCompat.getDrawable(context, R.drawable.greydivider));
        Random random = new Random();
        animRunnable = () -> {
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                if (random.nextInt(100) < 25) {
                    final ReactionCell view = (ReactionCell) layoutManager.getChildAt(i);
                    if (view != null) {
                        view.resume();
                    }
                }
            }
            postRandomAnimation();
        };
        postRandomAnimation();
    }

    private void postRandomAnimation() {
        reactionListView.postDelayed(animRunnable, 500);
    }

//    @Override
//    protected void onDraw(Canvas canvas) {
//        RectF rect = new RectF(0, 0, getWidth(), getHeight());
//        Path path = new Path();
//        path.addRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), Path.Direction.CW);
//        canvas.clipPath(path);
//        super.onDraw(canvas);
////        Paint paint = new Paint();
////        paint.setColor(Color.WHITE);
////        paint.setAntiAlias(true);
////        canvas.drawRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), paint);
//    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        RectF rect = new RectF(AndroidUtilities.dp(4), AndroidUtilities.dp(4), getWidth() - AndroidUtilities.dp(4), getHeight() - AndroidUtilities.dp(4)  /*- AndroidUtilities.dp(20)*/);
        Path path = new Path(); // TODO 01/12/2021 Fuji team, RIDER-: check this hardcode clip
        path.addRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), Path.Direction.CW);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAlpha(100);
//        canvas.drawRoundRect(rect, AndroidUtilities.dp(72), AndroidUtilities.dp(72), paint);
//        canvas.save();

        if (reactionsAdapter.reactions.size() == 1) {
            singleMenuDrawable.setBounds(0, 0, getWidth() /*+ AndroidUtilities.dp(20)*/,getHeight() + AndroidUtilities.dp(22));
            singleMenuDrawable.draw(canvas);
        } else {
            manyMenuDrawable.setBounds(0, 0, getWidth() /*+ AndroidUtilities.dp(20)*/,getHeight() + AndroidUtilities.dp(22));
            manyMenuDrawable.draw(canvas);
        }

//        canvas.restore();
        canvas.save();
//        canvas
        final int widthBubble = AndroidUtilities.dp(25);
//        final float dpHeight = widthBubble * (bubbleDrawable.getIntrinsicHeight() / (float)bubbleDrawable.getIntrinsicWidth());
//        final int left = getWidth() * 8/10;
//        bubbleDrawable.setBounds(
//                left - widthBubble,
//                0,
//                left,
//                (int)dpHeight
//        );
//        canvas.translate(0, getHeight() - AndroidUtilities.dp(5));
////        canvas.drawRect(width - (int)dp, 0, width, (int)dpHeight,  paint);
//        bubbleDrawable.draw(canvas);
//        canvas.restore();
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
    }

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        removeCallbacks(animRunnable); // TODO 30/11/2021 Fuji team, RIDER-: check
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int size1 = reactionsAdapter.reactions.size();

        float size = size1 * 40 + (size1 - 1) * 2 + 28 - (size1 == 1 ? 0 : 0);
//
//        float size = reactionsAdapter.reactions.size() * 40 + (reactionsAdapter.reactions.size() - 1) * 2 + 28;
        float maxSize = maxVisibleReactionsCount * 40 + (maxVisibleReactionsCount - 1) * 2 + 28;
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(Math.min(size, maxSize)), MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(61), MeasureSpec.EXACTLY)
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56), MeasureSpec.EXACTLY)
        );
    }

    private void updateView() {
    }

    private void createReactionListView() {

//        reactionListView.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
        reactionListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int p = parent.getChildAdapterPosition(view);
                if (p != 0) {
                    outRect.left = AndroidUtilities.dp(2);
                }
//                if (parent.getChildCount() == 1) {
//                    outRect.left = AndroidUtilities.dp(20);
//                }
//                if (p == users.size() - 1) {
//                    outRect.bottom = AndroidUtilities.dp(4);
//                }
            }
        });
        reactionListView.setAdapter(reactionsAdapter = new ReactionsAdapter(getContext(), show -> {
            if (show) {
                int newPadding = /*reactionsAdapter.isShowingKeywords() ? AndroidUtilities.dp(24) : */0;
                if (newPadding != reactionListView.getPaddingTop() || getTag() == null) {
//                    reactionListView.setPadding(AndroidUtilities.dp(18), newPadding, AndroidUtilities.dp(18), 0);
                    reactionListView.scrollToPosition(0);

                    boolean isRtl = /*chatActivityEnterView.isRtlText();*/ false;
//                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) stickersPanelArrow.getLayoutParams();
//                    layoutParams.gravity = Gravity.BOTTOM | (isRtl ? Gravity.RIGHT : Gravity.LEFT);
//                    stickersPanelArrow.requestLayout();
                }
            }
            if (show && getTag() != null || !show && getTag() == null) {
                return;
            }
            if (show) {
                boolean allowStickersPanel = true;
                setVisibility(allowStickersPanel ? View.VISIBLE : View.INVISIBLE);
                setTag(1);
            } else {
                setTag(null);
            }
            if (runningAnimation != null) {
                runningAnimation.cancel();
                runningAnimation = null;
            }
            if (getVisibility() != View.INVISIBLE) {
                runningAnimation = new AnimatorSet();
                runningAnimation.playTogether(
                        ObjectAnimator.ofFloat(this, View.ALPHA, show ? 0.0f : 1.0f, show ? 1.0f : 0.0f)
                );
                runningAnimation.setDuration(150);
                runningAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (runningAnimation != null && runningAnimation.equals(animation)) {
                            if (!show) {
                                setVisibility(View.GONE);
                                if (ContentPreviewViewer.getInstance().isVisible()) {
                                    ContentPreviewViewer.getInstance().close();
                                }
                                ContentPreviewViewer.getInstance().reset();
                            }
                            runningAnimation = null;
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        if (runningAnimation != null && runningAnimation.equals(animation)) {
                            runningAnimation = null;
                        }
                    }
                });
                runningAnimation.start();
            } else if (!show) {
                setVisibility(View.GONE);
            }
        }));
        reactionListView.setOnItemClickListener((view, position) -> onClickListener.onClick(view));
    }
}
