package org.telegram.ui.Components.QuickShare;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Random;
import org.telegram.messenger.AndroidUtilities;
import static org.telegram.messenger.AndroidUtilities.dp;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.UndoView;

public class QuickShareEffectOverlay {

    public final static int LONG_ANIMATION = 0;
    public final static int SHORT_ANIMATION = 1;
    public final static int ONLY_MOVE_ANIMATION = 2;
    @SuppressLint("StaticFieldLeak")
    public static QuickShareEffectOverlay currentOverlay;
    @SuppressLint("StaticFieldLeak")
    public static QuickShareEffectOverlay currentShortOverlay;
    private static long lastHapticTime;
    private final int animationType;
    private final QuickShareEndAnimationView effectImageView;
    private final FrameLayout container;
    private final BaseFragment fragment;
    private final int messageId;
    private final long groupId;
    public FrameLayout windowView;
    public boolean started;
    public long startTime;
    public boolean isStories;
    float animationDuration = 350;
    float timeoutDuration = 5000;
    float animateInProgress;
    float animateOutProgress;
    int[] loc = new int[2];
    ArrayList<AvatarParticle> avatars = new ArrayList<>();
    boolean isFinished;
    private WindowManager windowManager;
    private boolean dismissed;
    private float dismissProgress;
    private float lastDrawnToX;
    private float lastDrawnToY;
    private ChatMessageCell cell;
    private boolean useWindow;
    private ViewGroup decorView;

    public QuickShareEffectOverlay(Context context,
                                   BaseFragment fragment,
                                   View targetView,
                                   View fromAnimationView,
                                   float x,
                                   float y,
                                   int currentAccount,
                                   int animationType,
                                   boolean isStories,
                                   long uid) {
        this.fragment = fragment;
        this.isStories = isStories;
        this.messageId = 0;
        this.groupId = 0;
        this.animationType = animationType;
        if (isStories && animationType == ONLY_MOVE_ANIMATION) {
            QuickShareEffectOverlay.currentShortOverlay = new QuickShareEffectOverlay(context,
                    fragment,
                    cell,
                    fromAnimationView,
                    x,
                    y,
                    currentAccount,
                    SHORT_ANIMATION,
                    true,
                    uid);
        }
        float fromX, fromY;
        ChatActivity chatActivity = (fragment instanceof ChatActivity) ? (ChatActivity) fragment : null;

        if (animationType == SHORT_ANIMATION) {
            Random random = new Random();
            ArrayList<TLRPC.MessagePeerReaction> recentReactions = null;
            if (cell != null && cell.getMessageObject().messageOwner.reactions != null) {
                recentReactions = cell.getMessageObject().messageOwner.reactions.recent_reactions;
            }
            if (recentReactions != null && chatActivity != null && chatActivity.getDialogId() < 0) {
                for (int i = 0; i < recentReactions.size(); i++) {
                    if (/*reaction.equals(recentReactions.get(i).reaction) &&*/ recentReactions.get(i).unread) {
                        TLRPC.User user;
                        TLRPC.Chat chat;

                        AvatarDrawable avatarDrawable = new AvatarDrawable();
                        ImageReceiver imageReceiver = new ImageReceiver();
                        long peerId = MessageObject.getPeerId(recentReactions.get(i).peer_id);
                        if (peerId < 0) {
                            chat = MessagesController.getInstance(currentAccount).getChat(-peerId);
                            if (chat == null) {
                                continue;
                            }
                            avatarDrawable.setInfo(currentAccount, chat);
                            imageReceiver.setForUserOrChat(chat, avatarDrawable);
                        } else {
                            user = MessagesController.getInstance(currentAccount).getUser(peerId);
                            if (user == null) {
                                continue;
                            }
                            avatarDrawable.setInfo(currentAccount, user);
                            imageReceiver.setForUserOrChat(user, avatarDrawable);
                        }

                        AvatarParticle avatarParticle = new AvatarParticle();
                        avatarParticle.imageReceiver = imageReceiver;
                        avatarParticle.fromX = 0.5f;// + Math.abs(random.nextInt() % 100) / 100f * 0.2f;
                        avatarParticle.fromY = 0.5f;// + Math.abs(random.nextInt() % 100) / 100f * 0.2f;
                        avatarParticle.jumpY = 0.3f + Math.abs(random.nextInt() % 100) / 100f * 0.1f;
                        avatarParticle.randomScale = 0.8f + Math.abs(random.nextInt() % 100) / 100f * 0.4f;
                        avatarParticle.randomRotation = 60 * Math.abs(random.nextInt() % 100) / 100f;
                        avatarParticle.leftTime = (int) (400 + Math.abs(random.nextInt() % 100) / 100f * 200);

                        if (avatars.isEmpty()) {
                            avatarParticle.toX = 0.2f + 0.6f * Math.abs(random.nextInt() % 100) / 100f;
                            avatarParticle.toY = 0.4f * Math.abs(random.nextInt() % 100) / 100f;
                        } else {
                            float bestDistance = 0;
                            float bestX = 0;
                            float bestY = 0;
                            for (int k = 0; k < 10; k++) {
                                float randX = 0.2f + 0.6f * Math.abs(random.nextInt() % 100) / 100f;
                                float randY = 0.2f + 0.4f * Math.abs(random.nextInt() % 100) / 100f;
                                float minDistance = Integer.MAX_VALUE;
                                for (int j = 0; j < avatars.size(); j++) {
                                    float rx = avatars.get(j).toX - randX;
                                    float ry = avatars.get(j).toY - randY;
                                    float distance = rx * rx + ry * ry;
                                    if (distance < minDistance) {
                                        minDistance = distance;
                                    }
                                }
                                if (minDistance > bestDistance) {
                                    bestDistance = minDistance;
                                    bestX = randX;
                                    bestY = randY;
                                }
                            }
                            avatarParticle.toX = bestX;
                            avatarParticle.toY = bestY;
                        }

                        avatars.add(avatarParticle);
                    }
                }
            }
        }

        if (fromAnimationView != null) {
            fromAnimationView.getLocationOnScreen(loc);
            float viewX = loc[0];
            float viewY = loc[1];
            fromX = viewX;
            fromY = viewY;
        } else if (cell != null) {
            ((View) cell.getParent()).getLocationInWindow(loc);
            fromX = loc[0] + x;
            fromY = loc[1] + y;
        } else {
            fromX = x;
            fromY = y;
        }

        int size = dp(52);


        int contactSize = size;

        float fromScale = 1.1f;
        animateInProgress = 0f;
        animateOutProgress = 0f;

        container = new FrameLayout(context);
        windowView = new FrameLayout(context) {
            float v = 150f;

            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (dismissed) {
                    if (dismissProgress != 1f) {
                        dismissProgress += 16 / v;
                        if (dismissProgress > 1f) {
                            dismissProgress = 1f;
                            AndroidUtilities.runOnUIThread(() -> {
                                removeCurrentView();
                            });
                        }
                    }
                    if (dismissProgress != 1f) {
                        setAlpha(1f - dismissProgress);
                        super.dispatchDraw(canvas);
                    }
                    invalidate();
                    return;
                }
                if (!started) {
                    invalidate();
                    return;
                }
                float toX, toY, toH, toW;

                toH = targetView.getHeight();
                toW = targetView.getWidth();
                ViewGroup parentView = (ViewGroup) targetView.getParent();
                UndoView undoView = (UndoView) parentView;
                if (targetView != null && targetView.isShown() && (undoView == null || undoView.isShown)) {
                    targetView.getLocationInWindow(loc);
                    toX = loc[0];
                    toY = loc[1];
                    lastDrawnToX = toX;
                    lastDrawnToY = toY;
                } else {
                    toX = lastDrawnToX;
                    toY = lastDrawnToY;
                }


                if (fragment != null && fragment.getParentActivity() != null && fragment.getFragmentView() != null && fragment.getFragmentView().getParent() != null && fragment.getFragmentView().getVisibility() == View.VISIBLE && fragment.getFragmentView() != null) {
                    fragment.getFragmentView().getLocationOnScreen(loc);
                    setAlpha(((View) fragment.getFragmentView().getParent()).getAlpha());
                } else {
                    return;
                }

                float targetX = toX - (contactSize - toW) / 2f;
                float targetY = toY - (contactSize - toH) / 2f;
//                System.out.println("Manwtein, toX = " + toX + ", toY = " + toY + ", targetX = " + targetX + ", targetY = " + targetY);
                if (targetX < loc[0]) {
                    targetX = loc[0];
                }
                if (targetX + contactSize > loc[0] + getMeasuredWidth()) {
                    targetX = loc[0] + getMeasuredWidth() - contactSize;
                }

                float animateInProgressX, animateInProgressY;
                float animateOutProgress = CubicBezierInterpolator.DEFAULT.getInterpolation(QuickShareEffectOverlay.this.animateOutProgress);
                if (animationType == ONLY_MOVE_ANIMATION) {
                    animateInProgressX = CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(animateOutProgress);
                    animateInProgressY = CubicBezierInterpolator.DEFAULT.getInterpolation(animateOutProgress);
                } else {
                    animateInProgressX = animateInProgressY = animateInProgress;
                }

                float scale = 0.5f * (animateInProgressX) + (1f - animateInProgressX) * fromScale;
                float x = fromX * (1f - animateInProgressX) + targetX * animateInProgressX;
                float y = fromY * (1f - animateInProgressY) + targetY * animateInProgressY;

//                System.out.println("Manwtein, fromX = " + fromX + ", fromY = " + fromY + ", animateInProgressX = " + animateInProgressX + ", animateInProgressY = " + animateInProgressY);
                System.out.println("Manwtein, x = " + x + ", y = " + y + ", alpha = " + (1f - animateOutProgress) + ", scale = " + scale);
                if (isFinished) {
                    effectImageView.setTranslationX(x);
                    effectImageView.setTranslationY(y);
                } else {
                    effectImageView.setTranslationX(x);
                    effectImageView.setTranslationY(y);
                    effectImageView.setAlpha(0.3f + (1f - animateOutProgress));
                    effectImageView.setScaleX(scale);
                    effectImageView.setScaleY(scale);
                }

                super.dispatchDraw(canvas);

                if ((animationType == SHORT_ANIMATION) && animateInProgress != 1f) {
                    animateInProgress += 16f / animationDuration;
                    if (animateInProgress > 1f) {
                        animateInProgress = 1f;
                    }
                }

                if (QuickShareEffectOverlay.this.animateOutProgress != 1f) {
                    float duration = animationDuration;
                    QuickShareEffectOverlay.this.animateOutProgress += 16f / duration;
                    if (QuickShareEffectOverlay.this.animateOutProgress > 0.7f) {
                        if (animationType == ONLY_MOVE_ANIMATION) {
                            if (!isFinished) {
                                isFinished = true;
                                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                                effectImageView.animate().scaleX(0).scaleY(0)/*.setStartDelay(3000)*/.alpha(0).setDuration((long) (animationDuration * (1 - animateOutProgress))).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        removeCurrentView();
                                    }
                                });
                            }
                        }
                    }
                    if (QuickShareEffectOverlay.this.animateOutProgress >= 1f) {
                        QuickShareEffectOverlay.this.animateOutProgress = 1f;
                        if (animationType == SHORT_ANIMATION) {
                            currentShortOverlay = null;
                        } else {
                            currentOverlay = null;
                        }
                    }
                }
                invalidate();
            }

            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();
                for (int i = 0; i < avatars.size(); i++) {
                    avatars.get(i).imageReceiver.onAttachedToWindow();
                }
            }

            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                for (int i = 0; i < avatars.size(); i++) {
                    avatars.get(i).imageReceiver.onDetachedFromWindow();
                }
            }
        };
        effectImageView = new QuickShareEndAnimationView(context);

        int topOffset = (size - contactSize) >> 1;
        int leftOffset;
        if (animationType == SHORT_ANIMATION) {
            leftOffset = topOffset;
        } else {
            leftOffset = size - contactSize;
        }
        size = dp(52);

        windowView.addView(container);
        container.getLayoutParams().width = size;
        container.getLayoutParams().height = size;
        ((FrameLayout.LayoutParams) container.getLayoutParams()).topMargin = -topOffset;
        ((FrameLayout.LayoutParams) container.getLayoutParams()).leftMargin = -leftOffset;

        //if (availableReaction != null) {
        windowView.addView(effectImageView);
        effectImageView.getLayoutParams().width = size;
        effectImageView.getLayoutParams().height = size;

        effectImageView.getLayoutParams().width = size;
        effectImageView.getLayoutParams().height = size;
        ((FrameLayout.LayoutParams) effectImageView.getLayoutParams()).topMargin = -topOffset;
        ((FrameLayout.LayoutParams) effectImageView.getLayoutParams()).leftMargin = -leftOffset;
        // }

        container.setPivotX(leftOffset);
        container.setPivotY(topOffset);

        effectImageView.setRoundRadius(dp(28));
        AvatarDrawable avatarDrawable = effectImageView.avatarDrawable;
        if (DialogObject.isUserDialog(uid)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);
            avatarDrawable.setInfo(currentAccount, user);
            if (/*contactHolderView.currentType != TYPE_CREATE && */UserObject.isUserSelf(user)) {
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                effectImageView.setImage(null, null, avatarDrawable, user);
            } else if (user != null) {
                effectImageView.setForUserOrChat(user, avatarDrawable);
            }
        } else {
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
            avatarDrawable.setInfo(currentAccount, chat);
            if (chat != null) {
                effectImageView.setForUserOrChat(chat, avatarDrawable);
            }
        }
    }

    public static void show(BaseFragment baseFragment,
//                            ReactionsContainerLayout reactionsLayout,
//                            ChatMessageCell cell,
                            View targetView,
                            View fromAnimationView,
                            float x,
                            float y,
//                            ReactionsLayoutInBubble.VisibleReaction visibleReaction,
                            int currentAccount,
                            int animationType,
                            long uid) {
        if (targetView == null /*|| visibleReaction == null*/ || baseFragment == null || baseFragment.getParentActivity() == null) {
            return;
        }
        boolean animationEnabled = MessagesController.getGlobalMainSettings().getBoolean("view_animations", true);
        if (!animationEnabled) {
            return;
        }
        if (animationType == ONLY_MOVE_ANIMATION || animationType == LONG_ANIMATION) {
            show(baseFragment, /*null, */targetView, fromAnimationView, 0, 0, /*visibleReaction,*/ currentAccount, SHORT_ANIMATION, uid);
        }

        QuickShareEffectOverlay reactionsEffectOverlay = new QuickShareEffectOverlay(baseFragment.getParentActivity(), baseFragment, /*reactionsLayout,*/ targetView, fromAnimationView, x, y, /*visibleReaction,*/ currentAccount, animationType, false, uid);
        if (animationType == SHORT_ANIMATION) {
            currentShortOverlay = reactionsEffectOverlay;
        } else {
            currentOverlay = reactionsEffectOverlay;
        }

        boolean useWindow = false;
        if (baseFragment instanceof ChatActivity) {
            ChatActivity chatActivity = (ChatActivity) baseFragment;
            if ((animationType == LONG_ANIMATION || animationType == ONLY_MOVE_ANIMATION) && chatActivity.scrimPopupWindow != null && chatActivity.scrimPopupWindow.isShowing()) {
                useWindow = true;
            }
        }

        reactionsEffectOverlay.useWindow = useWindow;
        if (useWindow) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.width = lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
            lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
            lp.format = PixelFormat.TRANSLUCENT;

            reactionsEffectOverlay.windowManager = baseFragment.getParentActivity().getWindowManager();
            AndroidUtilities.setPreferredMaxRefreshRate(reactionsEffectOverlay.windowManager, reactionsEffectOverlay.windowView, lp);
            reactionsEffectOverlay.windowManager.addView(reactionsEffectOverlay.windowView, lp);
        } else {
            reactionsEffectOverlay.decorView = (FrameLayout) baseFragment.getParentActivity().getWindow().getDecorView();
            reactionsEffectOverlay.decorView.addView(reactionsEffectOverlay.windowView);
        }
//        cell.invalidate();
//        if (cell.getCurrentMessagesGroup() != null && cell.getParent() != null) {
//            ((View) cell.getParent()).invalidate();
//        }
        targetView.invalidate();
        if (/*targetView.getCurrentMessagesGroup() != null && */targetView.getParent() != null) {
            ((View) targetView.getParent()).invalidate();
        }
    }

    public static void startAnimation() {
        if (currentOverlay != null) {
            currentOverlay.started = true;
            currentOverlay.startTime = System.currentTimeMillis();
            if (currentOverlay.animationType == LONG_ANIMATION && System.currentTimeMillis() - lastHapticTime > 200) {
                lastHapticTime = System.currentTimeMillis();
                currentOverlay.cell.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        } else {
            startShortAnimation();
            if (currentShortOverlay != null) {
//                currentShortOverlay.cell.reactionsLayoutInBubble.animateReaction(currentShortOverlay.reaction);
            }
        }
    }

    public static void startShortAnimation() {
        if (currentShortOverlay != null && !currentShortOverlay.started) {
            currentShortOverlay.started = true;
            currentShortOverlay.startTime = System.currentTimeMillis();
            if (currentShortOverlay.animationType == SHORT_ANIMATION && System.currentTimeMillis() - lastHapticTime > 200) {
                lastHapticTime = System.currentTimeMillis();
                currentShortOverlay.container.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
//                currentShortOverlay.cell.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); todo
            }
        }
    }

    public static void removeCurrent(boolean instant) {
        for (int i = 0; i < 2; i++) {
            QuickShareEffectOverlay overlay = i == 0 ? currentOverlay : currentShortOverlay;
            if (overlay != null) {
                if (instant) {
                    overlay.removeCurrentView();
                } else {
                    overlay.dismissed = true;
                }
            }
        }
        currentShortOverlay = null;
        currentOverlay = null;
    }

    public static boolean isPlaying(int messageId, long groupId, ReactionsLayoutInBubble.VisibleReaction reaction) {
        if (currentOverlay != null && (currentOverlay.animationType == ONLY_MOVE_ANIMATION || currentOverlay.animationType == LONG_ANIMATION)) {
            return ((currentOverlay.groupId != 0 && groupId == currentOverlay.groupId) || messageId == currentOverlay.messageId) /*&& currentOverlay.reaction.equals(reaction)*/;
        }
        return false;
    }

//    public static void onScrolled(int dy) {
//        if (currentOverlay != null) {
//            currentOverlay.lastDrawnToY -= dy;
//            if (dy != 0) {
//                currentOverlay.wasScrolled = true;
//            }
//        }
//    }

    public static int sizeForBigReaction() {
        return (int) (Math.round(Math.min(AndroidUtilities.dp(350), Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y)) * 0.7f) / AndroidUtilities.density);
    }

    public static int sizeForAroundReaction() {
        int size = AndroidUtilities.dp(40);
        return (int) (2f * size / AndroidUtilities.density);
    }

    public static void dismissAll() {
        if (currentOverlay != null) {
            currentOverlay.dismissed = true;
        }
        if (currentShortOverlay != null) {
            currentShortOverlay.dismissed = true;
        }
    }

    private void removeCurrentView() {
        try {
            if (useWindow) {
                windowManager.removeView(windowView);
            } else {
                AndroidUtilities.removeFromParent(windowView);
            }
        } catch (Exception e) {

        }
    }

    private class QuickShareEndAnimationView extends BackupImageView {

        public final AvatarDrawable avatarDrawable = new AvatarDrawable() {
            @Override
            public void invalidateSelf() {
                super.invalidateSelf();
                invalidate();
            }
        };
        boolean wasPlaying;
        boolean attached;

        public QuickShareEndAnimationView(Context context) {
            super(context);
            getImageReceiver().setFileLoadingPriority(FileLoader.PRIORITY_HIGH);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attached = false;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (started)
                super.onDraw(canvas);
        }
    }

    private class AvatarParticle {
        public int leftTime;
        ImageReceiver imageReceiver;
        float progress;
        float outProgress;
        float jumpY;
        float fromX;
        float fromY;
        float toX;
        float toY;
        float randomScale;
        float randomRotation;
        float currentRotation;
        boolean incrementRotation;
        float globalTranslationY;
    }
}
