package org.telegram.ui;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.EmojiData;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ReactionCell;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class ReactionAnimationsOverlay implements NotificationCenter.NotificationCenterDelegate {

    private final int ANIMATION_JSON_VERSION = 1;
    private final int startAnimationFrames = 20;
    @SuppressWarnings("FieldCanBeLocal")
    private final int endAnimationFrames = 20;
    ChatActivity chatActivity;
    int currentAccount;
    ArrayList<TLRPC.TL_availableReaction> set;
    boolean inited = false;
    HashMap<Long, Integer> lastEffectAnimationIndex = new HashMap<>();
    HashMap<Long, Integer> lastActivationAnimationIndex = new HashMap<>();
    int lastTappedMsgId = -1;
    long lastTappedTime = 0;
    String lastTappedEmoji;
    ArrayList<Long> timeIntervals = new ArrayList<>();
    ArrayList<Integer> animationIndexes = new ArrayList<>();
    Runnable sentInteractionsRunnable;
    ArrayList<DrawingShowAnimationObject> drawingShowAnimationObjects = new ArrayList<>();
    ArrayList<DrawingHideAnimationObject> drawingHideAnimationObjects = new ArrayList<>();
    FrameLayout contentLayout;
    RecyclerListView listView;
    long dialogId;
    int threadMsgId;
    boolean isPersonalChat;
    private boolean attached;
    private AnimationStartListener animationStartListener;

    public ReactionAnimationsOverlay(
            ChatActivity chatActivity,
            FrameLayout frameLayout,
            RecyclerListView chatListView,
            int currentAccount,
            long dialogId,
            int threadMsgId
    ) {
        this.chatActivity = chatActivity;
        this.contentLayout = frameLayout;
        this.listView = chatListView;
        this.currentAccount = currentAccount;
        this.dialogId = dialogId;
        this.threadMsgId = threadMsgId;
    }

    protected void onAttachedToWindow() {
        attached = true;
        checkStickerPack();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
    }

    protected void onDetachedFromWindow() {
        attached = false;
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
    }

    public void checkStickerPack() {
        if (inited) {
            return;
        }
        final MessagesController messagesController = MessagesController.getInstance(currentAccount);
        set = messagesController.getAvailableReactions(messagesController.getChatFull(-dialogId));
        if (set != null && !set.isEmpty()) {
            inited = true;
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.diceStickersDidLoad) {
//            String name = (String) args[0];
//            if (INTERACTIONS_STICKER_PACK.equals(name)) {
//                checkStickerPack();
//            }
//        } else if (id == NotificationCenter.onEmojiInteractionsReceived) {
//            long dialogId = (long) args[0];
//            TLRPC.TL_sendMessageEmojiInteraction action = (TLRPC.TL_sendMessageEmojiInteraction) args[1];
//            if (dialogId == this.dialogId && supportedEmoji.contains(action.emoticon)) {
//                int messageId = action.msg_id;
//                if (action.interaction.data != null) {
//                    try {
//                        JSONObject jsonObject = new JSONObject(action.interaction.data);
//                        JSONArray array = jsonObject.getJSONArray("a");
//                        for (int i = 0; i < array.length(); i++) {
//                            JSONObject actionObject = array.getJSONObject(i);
//                            int animation = actionObject.optInt("i", 1) - 1;
//                            double time = actionObject.optDouble("t", 0.0);
//                            AndroidUtilities.runOnUIThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    findViewAndShowAnimation(messageId, animation);
//                                }
//                            }, (long) (time * 1000));
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        } else if (id == NotificationCenter.updateInterfaces) {
//            Integer printingType = MessagesController.getInstance(currentAccount).getPrintingStringType(dialogId, threadMsgId);
//            if (printingType != null && printingType == 5) {
//                cancelHintRunnable();
//            }
        }
    }

    private void findViewAndShowAnimation(int messageId, int animation) {
        if (!attached) {
            return;
        }
//        ChatMessageCell bestView = null;
//        for (int i = 0; i < listView.getChildCount(); i++) {
//            View child = listView.getChildAt(i);
//            if (child instanceof ChatMessageCell) {
//                ChatMessageCell cell = (ChatMessageCell) child;
//                if (cell.getPhotoImage().hasNotThumb() && cell.getMessageObject().getStickerEmoji() != null) {
//                    if (cell.getMessageObject().getId() == messageId) {
//                        bestView = cell;
//                        break;
//                    }
//                }
//            }
//        }
//
//        if (bestView != null) {
//            chatActivity.restartSticker(bestView);
//            if (!EmojiData.hasEmojiSupportVibration(bestView.getMessageObject().getStickerEmoji())) {
//                bestView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
//            }
//            showAnimationForCell(bestView, animation, false, true);
//        }
    }

    public void draw(Canvas canvas) {
        if (drawingShowAnimationObjects.isEmpty() && drawingHideAnimationObjects.isEmpty()) {
            return;
        }
        drawShowReactions(canvas);
//        drawHideReactions(canvas);
        contentLayout.invalidate();
    }

    private void drawShowReactions(Canvas canvas) {
        for (int i = 0; i < drawingShowAnimationObjects.size(); i++) {
            DrawingShowAnimationObject drawingAnimationObject = drawingShowAnimationObjects.get(i);
            drawingAnimationObject.viewFound = false;
            ChatMessageCell cell = null;
            for (int k = 0; k < listView.getChildCount(); k++) {
                View child = listView.getChildAt(k);
                if (child instanceof ChatMessageCell) {
                    cell = (ChatMessageCell) child; // TODO 02/12/2021 Fuji team, RIDER-: check this
                    if (cell.getMessageObject().getId() == drawingAnimationObject.messageId) {
                        drawingAnimationObject.viewFound = true;

                        float viewX;
                        float viewY;
                        if (cell.getCurrentPosition() != null && !cell.getCurrentPosition().last) {
                            ChatMessageCell view = chatActivity.findLastGroupView(cell);
                            if (view == null) {
                                view = cell;
                            }
                            viewX = listView.getX() + view.getReactionXPosition() + view.getX();
                            viewY = listView.getY() + view.getReactionYPosition() + view.getY();
                        } else {
                            viewX = listView.getX() + cell.getReactionXPosition() + cell.getX();
                            viewY = listView.getY() + cell.getReactionYPosition() + cell.getY();
                        }
                        /*if (drawingObject.isOut) {
                            viewX += -cell.getPhotoImage().getImageWidth() * 2 + AndroidUtilities.dp(24);
                        } else {
                            viewX += -AndroidUtilities.dp(24);
                        }*/
//                            viewY -= cell.getPhotoImage().getImageWidth();
//                        drawingAnimationObject.finalAnimPositionX = cell.getCurrentMessagesGroup() == null ? viewX : chatActivity.getReactionFinalAnimPositionX(cell) + listView.getY();
//                        drawingAnimationObject.finalAnimPositionY = cell.getCurrentMessagesGroup() == null ? viewY : chatActivity.getReactionFinalAnimPositionY(cell) + listView.getY();

                        if (drawingAnimationObject.isTheSamePosition) {
                            drawingAnimationObject.finalAnimPositionX = drawingAnimationObject.lastTouchPositionX  + listView.getX() + cell.getX();
                            drawingAnimationObject.finalAnimPositionY = drawingAnimationObject.lastTouchPositionY  + listView.getY() + cell.getY();
                        } else {
                            drawingAnimationObject.finalAnimPositionX = viewX;
                            drawingAnimationObject.finalAnimPositionY = viewY;

                            if (cell.getCurrentMessagesGroup() != null) {
                                if (drawingAnimationObject.finalAnimPositionX == 0 || drawingAnimationObject.finalAnimPositionY == 0) {
                                    drawingAnimationObject.finalAnimPositionX = drawingAnimationObject.lastAnimPositionX;
                                    drawingAnimationObject.finalAnimPositionY = drawingAnimationObject.lastAnimPositionY;
                                } else {
                                    drawingAnimationObject.lastAnimPositionX = drawingAnimationObject.finalAnimPositionX;
                                    drawingAnimationObject.lastAnimPositionY = drawingAnimationObject.finalAnimPositionY;

                                }
                            }
                        }
//                            drawingObject.lastW = cell.getPhotoImage().getImageWidth();
                        drawingAnimationObject.lastW = 500;
                        break;
                    } /*else {
                        cell = null;
                    }*/
                }
            }
            final DrawingObject activationDrawingObject = drawingAnimationObject.activationDrawingObject;
            final RLottieDrawable activationLottieAnimation = activationDrawingObject.imageReceiver.getLottieAnimation();
            final DrawingObject effectDrawingObject = drawingAnimationObject.effectDrawingObject;
            final RLottieDrawable effectLottieAnimation = effectDrawingObject.imageReceiver.getLottieAnimation();
            RLottieDrawable lottieAnimation = null;
            if (activationLottieAnimation != null && effectLottieAnimation != null) {
                if (activationLottieAnimation.getFramesCount() > effectLottieAnimation.getFramesCount()) {
                    lottieAnimation = activationLottieAnimation;
                } else {
                    lottieAnimation = effectLottieAnimation;
                }
            }
//            if (cell != null) {
            canvas.save();
            drawActivationAnimation(cell, drawingAnimationObject, canvas);
            drawEffectAnimation(cell, drawingAnimationObject, canvas);
//            }
            if (drawingAnimationObject.wasPlayed && lottieAnimation != null && lottieAnimation.getCurrentFrame() == lottieAnimation.getFramesCount() - 2) {
                drawingShowAnimationObjects.remove(drawingAnimationObject);
                i--;
                if (cell != null) {
                    cell.setIsProcessingReactAnimation(false);
                    cell.setIsProcessingReactAnimationLastFrames(false);
                }
                System.out.println("remove");
            } else if (lottieAnimation != null && lottieAnimation.isRunning()) {
                drawingAnimationObject.wasPlayed = true;
                System.out.println("wasPlayed");
            } else if (lottieAnimation != null && !lottieAnimation.isRunning()) {
                lottieAnimation.setCurrentFrame(0, true);
                lottieAnimation.start();
                System.out.println("setCurrentFrame");
            }
//            if (cell != null)
            canvas.restore();

        }
    }

    private void drawHideReactions(Canvas canvas) {
        for (int i = 0; i < drawingHideAnimationObjects.size(); i++) {
            DrawingHideAnimationObject drawingAnimationObject = drawingHideAnimationObjects.get(i);
            drawingAnimationObject.viewFound = false;
            ChatMessageCell cell = null;
            for (int k = 0; k < listView.getChildCount(); k++) {
                View child = listView.getChildAt(k);
                if (child instanceof ChatMessageCell) {
                    cell = (ChatMessageCell) child;
                    if (cell.getMessageObject().getId() == drawingAnimationObject.messageId) {
                        drawingAnimationObject.viewFound = true;
                        float viewX = listView.getX() + cell.getReactionXPosition();
                        float viewY = listView.getY() + cell.getReactionYPosition();
                        /*if (drawingObject.isOut) {
                            viewX += -cell.getPhotoImage().getImageWidth() * 2 + AndroidUtilities.dp(24);
                        } else {
                            viewX += -AndroidUtilities.dp(24);
                        }*/
//                            viewY -= cell.getPhotoImage().getImageWidth();
                        drawingAnimationObject.startAnimPositionX = cell.getCurrentMessagesGroup() == null ? viewX : chatActivity.getReactionFinalAnimPositionX(cell);
                        drawingAnimationObject.startAnimPositionY = cell.getCurrentMessagesGroup() == null ? viewY : chatActivity.getReactionFinalAnimPositionY(cell);

                        if (cell.getCurrentMessagesGroup() != null) {
                            if (drawingAnimationObject.finalAnimPositionX == 0 || drawingAnimationObject.finalAnimPositionY == 0) {
//                                drawingAnimationObject.finalAnimPositionX = drawingAnimationObject.lastAnimPositionX;
//                                drawingAnimationObject.finalAnimPositionY = drawingAnimationObject.lastAnimPositionY;
                            } else {
//                                drawingAnimationObject.lastAnimPositionX = drawingAnimationObject.finalAnimPositionX;
//                                drawingAnimationObject.lastAnimPositionY = drawingAnimationObject.finalAnimPositionY;
                            }
                        }
//                            drawingObject.lastW = cell.getPhotoImage().getImageWidth();
                        drawingAnimationObject.lastW = 500;
                        break;
                    } /*else {
                        cell = null;
                    }*/
                }
            }
            final DrawingObject hideDrawingObject = drawingAnimationObject.hideDrawingObject;
            final Drawable hideLottieAnimation = hideDrawingObject.imageReceiver.getDrawable();
//            if (cell != null) {
            canvas.save();
//            }
            if (drawingAnimationObject.wasPlayed && drawingAnimationObject.isFinished) {
                drawingShowAnimationObjects.remove(drawingAnimationObject);
                i--;
//                if (cell != null) {
//                    cell.setIsProcessingReactAnimation(false);
//                }
            } else {
//                if (animationStartListener != null) {
//                    animationStartListener.onAnimationStart();
//                    animationStartListener = null;
//                }
                hideDrawingObject.imageReceiver.setImageCoords(
                        drawingAnimationObject.startAnimPositionX - drawingAnimationObject.lastW / 2 + new Random().nextInt(),
                        drawingAnimationObject.startAnimPositionY - drawingAnimationObject.lastW / 2 + new Random().nextInt(),
                        drawingAnimationObject.lastW,
                        drawingAnimationObject.lastW
                );
                if (hideLottieAnimation != null) {
                    hideLottieAnimation.draw(canvas);
                }
                drawingAnimationObject.wasPlayed = true;
            } /*else if (lottieAnimation != null && !lottieAnimation.isRunning()) {
                lottieAnimation.setCurrentFrame(0, true);
                lottieAnimation.start();
            }*/

//            if (cell != null)
            canvas.restore();

        }
    }

    private void drawActivationAnimation(ChatMessageCell cell, DrawingShowAnimationObject drawingAnimationObject, Canvas canvas) {
//        Paint debugPaint = new Paint();
//        debugPaint.setColor(Color.RED);
//        debugPaint.setAlpha(120);
        DrawingObject drawingObject = drawingAnimationObject.activationDrawingObject;
        final RLottieDrawable lottieAnimation = drawingObject.imageReceiver.getLottieAnimation();

        if (lottieAnimation != null && lottieAnimation.getCurrentFrame() == lottieAnimation.getFramesCount() - 2) {
            return;
        }
        canvas.save();
        final float drawingObjectSize = drawingAnimationObject.lastW;

        final float startAnimPositionX = drawingAnimationObject.startAnimPositionX;
        final float startAnimPositionY = drawingAnimationObject.startAnimPositionY;
        float finalAnimPositionX = drawingAnimationObject.finalAnimPositionX;
        float finalAnimPositionY = drawingAnimationObject.finalAnimPositionY;


/*        if (finalAnimPositionX == 0) {
//            finalAnimPositionX = drawingAnimationObject.lastAnimPositionX;
            return;
        } else {
            drawingAnimationObject.lastAnimPositionX = finalAnimPositionX;
        }
        if (finalAnimPositionY == 0) {
            return;
//            finalAnimPositionY = drawingAnimationObject.lastAnimPositionY;
        } else {
            drawingAnimationObject.lastAnimPositionY = finalAnimPositionY;
        }*/

//        canvas.drawRect(0, 0, finalAnimPositionX, finalAnimPositionY, debugPaint);

        final float finalPositionY;
        final float finalPositionX;

        if (animationStartListener != null && (lottieAnimation != null || drawingObject.imageReceiver.getDrawable() != null)) {
            animationStartListener.onAnimationStart();
            animationStartListener = null;
        }

        if (lottieAnimation == null && drawingObject.imageReceiver.getDrawable() == null) {
            finalPositionX = startAnimPositionX;
            finalPositionY = startAnimPositionY;
            System.out.println("lottieAnimation = null");
        } else if (lottieAnimation.getCurrentFrame() < startAnimationFrames) {
            final float progress = lottieAnimation.getCurrentFrame() / (float) startAnimationFrames;
            finalPositionX = startAnimPositionX + ((finalAnimPositionX - startAnimPositionX) * progress);
            finalPositionY = startAnimPositionY + ((finalAnimPositionY - startAnimPositionY) * progress);
            canvas.scale(Math.max(0.25f, progress), Math.max(0.25f, progress), drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
            System.out.println("startAnimationFrames " + lottieAnimation.getCurrentFrame());
//            if (lottieAnimation.getCurrentFrame() > 2) {
//            if (animationStartListener != null) {
//                animationStartListener.onAnimationStart();
//                animationStartListener = null;
//            }
//            }
        } else {
            finalPositionX = finalAnimPositionX;
            finalPositionY = finalAnimPositionY;
            canvas.scale(1, 1, drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
            System.out.println("else");
        }
        drawingObject.imageReceiver.setImageCoords(
                finalPositionX - drawingObjectSize / 2,
                finalPositionY - drawingObjectSize / 2,
                drawingObjectSize,
                drawingObjectSize
        );
        if (lottieAnimation != null && lottieAnimation.getCurrentFrame() > lottieAnimation.getFramesCount() - endAnimationFrames) {
            final int framesCount = lottieAnimation.getFramesCount();
            final int leftFrames = framesCount - lottieAnimation.getCurrentFrame();
            canvas.scale(
                    leftFrames / (endAnimationFrames - 2f),
                    leftFrames / (endAnimationFrames - 2f),
                    drawingObject.imageReceiver.getCenterX(),
                    drawingObject.imageReceiver.getCenterY()
            );
            if (cell != null && cell.getMessageObject() != null && !drawingObject.isUpdatedLocalReaction) {
                drawingObject.isUpdatedLocalReaction = true;
                cell.setLocalReaction(drawingAnimationObject.availableReaction, set);
                cell.setIsProcessingReactAnimationLastFrames(true);
                chatActivity.updateChatRowWithMessageObject(cell, false); // TODO 01/12/2021 Fuji team, RIDER-: check this
            }
        }

//        if (!drawingObject.isOut) {
//            canvas.save();
//            canvas.scale(-1f, 1, drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
//            drawingObject.imageReceiver.draw(canvas);
//            canvas.restore();
//        } else {
        drawingObject.imageReceiver.draw(canvas); // TODO 01/12/2021 Fuji team, RIDER-: check
//        }
        canvas.restore();
    }

    private void drawEffectAnimation(ChatMessageCell cell, DrawingShowAnimationObject drawingAnimationObject, Canvas canvas) {
        canvas.save();
        DrawingObject drawingObject = drawingAnimationObject.effectDrawingObject;
        final float drawingObjectSize = drawingAnimationObject.lastW * 2;

        final float startAnimPositionX = drawingAnimationObject.startAnimPositionX;
        final float startAnimPositionY = drawingAnimationObject.startAnimPositionY;
        float finalAnimPositionX = drawingAnimationObject.finalAnimPositionX;
        float finalAnimPositionY = drawingAnimationObject.finalAnimPositionY;

//        if (finalAnimPositionX == 0 ) {
//            finalAnimPositionX = drawingAnimationObject.lastAnimPositionX;
//        } else {
//            drawingAnimationObject.lastAnimPositionX = finalAnimPositionX;
//        }
//        if (finalAnimPositionY == 0 ) {
//            finalAnimPositionY = drawingAnimationObject.lastAnimPositionY;
//        } else {
//            drawingAnimationObject.lastAnimPositionY = finalAnimPositionY;
//        }

        float finalPositionY;
        float finalPositionX;
        final RLottieDrawable lottieAnimation = drawingObject.imageReceiver.getLottieAnimation();

        if (lottieAnimation == null) {
            finalPositionX = startAnimPositionX;
            finalPositionY = startAnimPositionY;
        } else if (lottieAnimation.getCurrentFrame() < startAnimationFrames) {
            final float progress = lottieAnimation.getCurrentFrame() / (float) startAnimationFrames;
            finalPositionX = startAnimPositionX + ((finalAnimPositionX - startAnimPositionX) * progress);
            finalPositionY = startAnimPositionY + ((finalAnimPositionY - startAnimPositionY) * progress);
            canvas.scale(Math.max(0.25f, progress), Math.max(0.25f, progress), drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
        } else {
            finalPositionX = finalAnimPositionX;
            finalPositionY = finalAnimPositionY;
            canvas.scale(1, 1, drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
        }

        final float imageWidth = drawingAnimationObject.activationDrawingObject.imageReceiver.getImageWidth();
//        final float imageWidth = 0;
//        if (drawingObject.isOut) {
        finalPositionX += -imageWidth / 2;
//        } else {
//            finalPositionX += -imageWidth / 2;
//            finalPositionX += -AndroidUtilities.dp(40);
//        }
//        finalPositionY -= imageWidth / 2;

        drawingObject.imageReceiver.setImageCoords(
                finalPositionX - drawingObjectSize / 2,
                finalPositionY - drawingObjectSize / 2,
                drawingObjectSize,
                drawingObjectSize
        );

        // TODO 01/12/2021 Fuji team, RIDER-: check
//        if (!drawingObject.isOut) {
//            canvas.save();
//            canvas.scale(-1f, 1, drawingObject.imageReceiver.getCenterX(), drawingObject.imageReceiver.getCenterY());
//            drawingObject.imageReceiver.draw(canvas);
//            canvas.restore();
//        } else {
        drawingObject.imageReceiver.draw(canvas);
//        }
        canvas.restore();
    }

    public void tryHideReactionAnimation(
            String reaction,
            ChatMessageCell view,
            ChatActivity chatActivity/*,
            float tapPositionX,
            float tapPositionY*/
    ) {
        isPersonalChat = chatActivity.currentUser == null; // TODO 01/12/2021 Fuji team, RIDER-: check this
        final MessageObject messageObject = view.getMessageObject();
        if (reaction == null || chatActivity.isSecretChat() || messageObject == null || messageObject.getId() < 0) {
            return;
        }

        TLRPC.TL_availableReaction currentAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : set) {
            if (availableReaction.reaction.equals(reaction)) {
                currentAvailableReaction = availableReaction;
            }
        }

//        boolean show = tryHideReactionAnimation(currentAvailableReaction, view);
        MessagesController.getInstance(currentAccount).deleteReaction(messageObject);
        if (currentAvailableReaction == null) {
            return;
        }
        if (view.getCurrentPosition() != null && !view.getCurrentPosition().last) {
            view = chatActivity.findLastGroupView(view);
            if (view != null) {
                startHideAnimation(view, currentAvailableReaction);
            }
        } else {
            startHideAnimation(view, currentAvailableReaction);
        }
    }

    private void startHideAnimation(ChatMessageCell view, TLRPC.TL_availableReaction currentAvailableReaction) {
        view.setImageForReactionHideAnimation(currentAvailableReaction);
        view.clearReaction();
    }

    private boolean tryHideReactionAnimation(
            TLRPC.TL_availableReaction currentAvailableReaction,
            ChatMessageCell view
    ) {
        if (drawingShowAnimationObjects.size() > 12) {
            return false;
        }
//        if (!view.getPhotoImage().hasNotThumb()) {
//            return false;
//        }

//        float imageH = view.getPhotoImage().getImageHeight();
        float imageH = 500;
//        float imageW = view.getPhotoImage().getImageWidth();
        float imageW = 500; // TODO 01/12/2021 Fuji team, RIDER-: make correct size
        if (imageH <= 0 || imageW <= 0) {
            return false;
        }

//        if (supportedEmoji.contains(reaction)) {
//        ArrayList<TLRPC.Document> arrayList = emojiInteractionsStickersMap.get(view.getMessageObject().getStickerEmoji());
//            if (arrayList != null && !arrayList.isEmpty()) {
//                int sameAnimationsCount = 0;
//                for (int i = 0; i < drawingObjects.size(); i++) {
//                    if (drawingObjects.get(i).messageId == view.getMessageObject().getId()) {
//                        sameAnimationsCount++;
//                        if (drawingObjects.get(i).imageReceiver.getLottieAnimation() == null || drawingObjects.get(i).imageReceiver.getLottieAnimation().isGeneratingCache()) {
//                            return false;
//                        }
//                    }
//                }
//                if (sameAnimationsCount >= 4) {
//                    return false;
//                }
//                if (animation < 0 || animation > arrayList.size() - 1) {
//                    animation = Math.abs(random.nextInt()) % arrayList.size();
//                }
//                TLRPC.Document document = arrayList.get(animation);
        if (currentAvailableReaction == null) {
            return false;
        }
        TLRPC.Document imageDocument = currentAvailableReaction.static_icon;

        DrawingObject drawingHideObject = new DrawingObject();
        drawingHideObject.document = imageDocument;
        drawingHideObject.isOut = view.getMessageObject().isOutOwner();

        Integer lastIndex = lastEffectAnimationIndex.get(imageDocument.id);
        int currentIndex = lastIndex == null ? 0 : lastIndex;
        lastEffectAnimationIndex.put(imageDocument.id, (currentIndex + 1) % 4);

        ImageLocation imageLocation = ImageLocation.getForDocument(imageDocument);
        drawingHideObject.imageReceiver.setUniqKeyPrefix(currentIndex + "_" + view.getMessageObject().getId() + "_"); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        int w = (int) (2f * imageW / AndroidUtilities.density);
        drawingHideObject.imageReceiver.setImage(imageLocation, w + "_" + w + "_pcache", null, "tgs", set, 1); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        drawingHideObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
        drawingHideObject.imageReceiver.setAllowStartAnimation(true);
        drawingHideObject.imageReceiver.setAutoRepeat(0);
        if (drawingHideObject.imageReceiver.getLottieAnimation() != null) {
            drawingHideObject.imageReceiver.getLottieAnimation().start();
        }
        final ViewParent reactionCellParent = view.getParent();
//        final View menuView = (View) reactionCellParent.getParent();
//        int[] location = new int[2];
//        view.getLocationOnScreen(location);
//        int x = location[0];
//        int y = location[1];

        drawingHideObject.imageReceiver.onAttachedToWindow();
        drawingHideObject.imageReceiver.setParentView(contentLayout);

        // Activation animation
        DrawingHideAnimationObject drawingAnimationObject = new DrawingHideAnimationObject(drawingHideObject);
//        drawingAnimationObject.startAnimPositionX = tapPositionX;
//        drawingAnimationObject.startAnimPositionY = tapPositionY;
        drawingAnimationObject.messageId = view.getMessageObject().getId();
        drawingAnimationObject.reaction = currentAvailableReaction.reaction;
        drawingHideAnimationObjects.add(drawingAnimationObject);
        contentLayout.invalidate();
//        view.setIsProcessingReactAnimation(true);

       /* if (sendTap) {
            if (lastTappedMsgId != 0 && lastTappedMsgId != view.getMessageObject().getId()) {
                if (sentInteractionsRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(sentInteractionsRunnable);
                    sentInteractionsRunnable.run();
                }
            }
            lastTappedMsgId = view.getMessageObject().getId();
            lastTappedEmoji = currentAvailableReaction.reaction;
            if (lastTappedTime == 0) {
                lastTappedTime = System.currentTimeMillis();
                timeIntervals.clear();
                animationIndexes.clear();
                timeIntervals.add(0L);
                animationIndexes.add(animation);
            } else {
                timeIntervals.add(System.currentTimeMillis() - lastTappedTime);
                animationIndexes.add(animation);
            }
            if (sentInteractionsRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(sentInteractionsRunnable);
                sentInteractionsRunnable = null;
            }
            AndroidUtilities.runOnUIThread(sentInteractionsRunnable = () -> {
                sendCurrentTaps();
                sentInteractionsRunnable = null;
            }, 500);
        }*/

//        if (sendSeen) {
//            MessagesController.getInstance(currentAccount).sendTyping(dialogId, threadMsgId, 11, reaction, 0);
//        }
        return true;
//            }
//        }
//        return false;
    }

    public void tryStartReactionAnimation(
            String reaction,
            ChatMessageCell view,
            ChatActivity chatActivity,
            float tapPositionX,
            float tapPositionY,
            boolean isTheSamePosition
    ) {
        isPersonalChat = chatActivity.currentUser == null; // TODO 01/12/2021 Fuji team, RIDER-: check this
        final MessageObject messageObject = view.getMessageObject();
        if (reaction == null || chatActivity.isSecretChat() || messageObject == null || messageObject.getId() < 0) {
            return;
        }

        TLRPC.TL_availableReaction currentAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : set) {
            if (availableReaction.reaction.equals(reaction)) {
                currentAvailableReaction = availableReaction;
            }
        }

        boolean show = tryStartReactionAnimation(currentAvailableReaction, view, tapPositionX, tapPositionY, isTheSamePosition);
        if (show && !EmojiData.hasEmojiSupportVibration(messageObject.getStickerEmoji())) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        MessagesController.getInstance(currentAccount).sendReaction(messageObject, reaction);
    }

    public void tryStartReactionAnimation(
            String reaction,
            ChatMessageCell view,
            ChatActivity chatActivity,
            float tapPositionX,
            float tapPositionY
    ) {
        isPersonalChat = chatActivity.currentUser == null; // TODO 01/12/2021 Fuji team, RIDER-: check this
        final MessageObject messageObject = view.getMessageObject();
        if (reaction == null || chatActivity.isSecretChat() || messageObject == null || messageObject.getId() < 0) {
            return;
        }

        TLRPC.TL_availableReaction currentAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : set) {
            if (availableReaction.reaction.equals(reaction)) {
                currentAvailableReaction = availableReaction;
            }
        }

        boolean show = tryStartReactionAnimation(currentAvailableReaction, view, tapPositionX, tapPositionY, false);
        if (show && !EmojiData.hasEmojiSupportVibration(messageObject.getStickerEmoji())) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        MessagesController.getInstance(currentAccount).sendReaction(messageObject, reaction);
    }

    public void tryStartReactionAnimation(ReactionCell reactionCell, ChatMessageCell view, ChatActivity chatActivity, AnimationStartListener animationStartListener) {
        this.animationStartListener = animationStartListener;
        final String reaction = reactionCell.getReactionSymbol();
        isPersonalChat = chatActivity.currentUser == null; // TODO 01/12/2021 Fuji team, RIDER-: check this
        final MessageObject messageObject = view.getMessageObject();
        if (reaction == null || chatActivity.isSecretChat() || messageObject == null || messageObject.getId() < 0) {
            return;
        }

        TLRPC.TL_availableReaction currentAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : set) {
            if (availableReaction.reaction.equals(reaction)) {
                currentAvailableReaction = availableReaction;
            }
        }

        boolean show = tryStartReactionAnimation(currentAvailableReaction, reactionCell, view, -1, true, false);
        if (show && !EmojiData.hasEmojiSupportVibration(messageObject.getStickerEmoji())) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        MessagesController.getInstance(currentAccount).sendReaction(messageObject, reaction);

//        Integer printingType = MessagesController.getInstance(currentAccount).getPrintingStringType(dialogId, threadMsgId);
//        boolean canShowHint = true;
//        if (printingType != null && printingType == 5) {
//            canShowHint = false;
//        }
//        if (canShowHint && hintRunnable == null && show && (Bulletin.getVisibleBulletin() == null || !Bulletin.getVisibleBulletin().isShowing()) && SharedConfig.emojiInteractionsHintCount > 0 && UserConfig.getInstance(currentAccount).getClientUserId() != chatActivity.currentUser.id) {
//            SharedConfig.updateEmojiInteractionsHintCount(SharedConfig.emojiInteractionsHintCount - 1);
//            TLRPC.Document document = MediaDataController.getInstance(currentAccount).getEmojiAnimatedSticker(view.getMessageObject().getStickerEmoji());
//            StickerSetBulletinLayout layout = new StickerSetBulletinLayout(chatActivity.getParentActivity(), null, StickerSetBulletinLayout.TYPE_EMPTY, document, chatActivity.getResourceProvider());
//            layout.subtitleTextView.setVisibility(View.GONE);
//            layout.titleTextView.setText(AndroidUtilities.replaceTags(LocaleController.formatString("EmojiInteractionTapHint", R.string.EmojiInteractionTapHint, chatActivity.currentUser.first_name)));
//            layout.titleTextView.setTypeface(null);
//            layout.titleTextView.setMaxLines(3);
//            layout.titleTextView.setSingleLine(false);
//            Bulletin bulletin = Bulletin.make(chatActivity, layout, Bulletin.DURATION_LONG);
//            AndroidUtilities.runOnUIThread(hintRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    bulletin.show();
//                    hintRunnable = null;
//                }
//            }, 1500);
//        }
    }

    private boolean tryStartReactionAnimation(
            TLRPC.TL_availableReaction currentAvailableReaction,
            ReactionCell reactionCell,
            ChatMessageCell view,
            int animation,
            boolean sendTap,
            boolean sendSeen
    ) {
        if (drawingShowAnimationObjects.size() > 12) {
            return false;
        }
//        if (!view.getPhotoImage().hasNotThumb()) {
//            return false;
//        }

//        float imageH = view.getPhotoImage().getImageHeight();
        float imageH = 500;
//        float imageW = view.getPhotoImage().getImageWidth();
        float imageW = 500; // TODO 01/12/2021 Fuji team, RIDER-: make correct size
        if (imageH <= 0 || imageW <= 0) {
            return false;
        }

//        if (supportedEmoji.contains(reaction)) {
//        ArrayList<TLRPC.Document> arrayList = emojiInteractionsStickersMap.get(view.getMessageObject().getStickerEmoji());
//            if (arrayList != null && !arrayList.isEmpty()) {
//                int sameAnimationsCount = 0;
//                for (int i = 0; i < drawingObjects.size(); i++) {
//                    if (drawingObjects.get(i).messageId == view.getMessageObject().getId()) {
//                        sameAnimationsCount++;
//                        if (drawingObjects.get(i).imageReceiver.getLottieAnimation() == null || drawingObjects.get(i).imageReceiver.getLottieAnimation().isGeneratingCache()) {
//                            return false;
//                        }
//                    }
//                }
//                if (sameAnimationsCount >= 4) {
//                    return false;
//                }
//                if (animation < 0 || animation > arrayList.size() - 1) {
//                    animation = Math.abs(random.nextInt()) % arrayList.size();
//                }
//                TLRPC.Document document = arrayList.get(animation);
        if (currentAvailableReaction == null) {
            return false;
        }
        TLRPC.Document effectDocument = currentAvailableReaction.effect_animation;
        TLRPC.Document activationDocument = currentAvailableReaction.activate_animation;

        DrawingObject drawingEffectObject = new DrawingObject();
        drawingEffectObject.document = effectDocument;
        drawingEffectObject.isOut = view.getMessageObject().isOutOwner();

        Integer lastIndex = lastEffectAnimationIndex.get(effectDocument.id);
        int currentIndex = lastIndex == null ? 0 : lastIndex;
        lastEffectAnimationIndex.put(effectDocument.id, (currentIndex + 1) % 4);

        ImageLocation imageLocation = ImageLocation.getForDocument(effectDocument);
        drawingEffectObject.imageReceiver.setUniqKeyPrefix(currentIndex + "_" + view.getMessageObject().getId() + "_"); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        int w = (int) (2f * imageW / AndroidUtilities.density);
        drawingEffectObject.imageReceiver.setImage(imageLocation, w + "_" + w + "_pcache", null, "tgs", set, 1); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        drawingEffectObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
        drawingEffectObject.imageReceiver.setAllowStartAnimation(true);
        drawingEffectObject.imageReceiver.setAutoRepeat(0);
        if (drawingEffectObject.imageReceiver.getLottieAnimation() != null) {
            drawingEffectObject.imageReceiver.getLottieAnimation().start();
        }
        final ViewParent reactionCellParent = reactionCell.getParent();
        final View menuView = (View) reactionCellParent.getParent();
        int[] location = new int[2];
        reactionCell.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        drawingEffectObject.imageReceiver.onAttachedToWindow();
        drawingEffectObject.imageReceiver.setParentView(contentLayout);

        // Activation animation

        float activationImageW = 300;

        DrawingObject drawingActivationObject = new DrawingObject();
        drawingActivationObject.document = activationDocument;
        drawingActivationObject.isOut = view.getMessageObject().isOutOwner();
        drawingActivationObject.isOut = view.getMessageObject().isOutOwner();

        Integer lastActivationIndex = lastActivationAnimationIndex.get(activationDocument.id);
        int currentActivationIndex = lastActivationIndex == null ? 0 : lastActivationIndex;
        lastActivationAnimationIndex.put(activationDocument.id, (currentActivationIndex + 1) % 4);

        ImageLocation imageLocation2 = ImageLocation.getForDocument(activationDocument);
        drawingActivationObject.imageReceiver.setUniqKeyPrefix(currentActivationIndex + "_" + view.getMessageObject().getId() + "_act"); // TODO 01/12/2021 Fuji team, RIDER-: check
        int wActivation = (int) (2f * activationImageW / AndroidUtilities.density);
        drawingActivationObject.imageReceiver.setImage(imageLocation2, wActivation + "_" + activationImageW + "_pcache", null, "tgs", set, 1);
        drawingActivationObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
        drawingActivationObject.imageReceiver.setAllowStartAnimation(true);
        drawingActivationObject.imageReceiver.setAutoRepeat(0);
        if (drawingActivationObject.imageReceiver.getLottieAnimation() != null) {
            drawingActivationObject.imageReceiver.getLottieAnimation().start();
        }
        drawingActivationObject.imageReceiver.onAttachedToWindow();
        drawingActivationObject.imageReceiver.setParentView(contentLayout);
        DrawingShowAnimationObject drawingAnimationObject = new DrawingShowAnimationObject(drawingActivationObject, drawingEffectObject);
        drawingAnimationObject.startAnimPositionX = x + reactionCell.getWidth() / 2f;
        drawingAnimationObject.startAnimPositionY = y + menuView.getTranslationY() + reactionCell.getY() / 2;
        drawingAnimationObject.messageId = view.getMessageObject().getId();
        drawingAnimationObject.availableReaction = currentAvailableReaction;
        drawingShowAnimationObjects.add(drawingAnimationObject);
        contentLayout.invalidate();
        return true;
    }

    private boolean tryStartReactionAnimation(
            TLRPC.TL_availableReaction currentAvailableReaction,
            ChatMessageCell view,
            float tapPositionX,
            float tapPositionY,
            boolean isTheSamePosition
    ) {
        if (drawingShowAnimationObjects.size() > 12) {
            return false;
        }
//        if (!view.getPhotoImage().hasNotThumb()) {
//            return false;
//        }

//        float imageH = view.getPhotoImage().getImageHeight();
        float imageH = 500;
//        float imageW = view.getPhotoImage().getImageWidth();
        float imageW = 500; // TODO 01/12/2021 Fuji team, RIDER-: make correct size
        if (imageH <= 0 || imageW <= 0) {
            return false;
        }

//        if (supportedEmoji.contains(reaction)) {
//        ArrayList<TLRPC.Document> arrayList = emojiInteractionsStickersMap.get(view.getMessageObject().getStickerEmoji());
//            if (arrayList != null && !arrayList.isEmpty()) {
//                int sameAnimationsCount = 0;
//                for (int i = 0; i < drawingObjects.size(); i++) {
//                    if (drawingObjects.get(i).messageId == view.getMessageObject().getId()) {
//                        sameAnimationsCount++;
//                        if (drawingObjects.get(i).imageReceiver.getLottieAnimation() == null || drawingObjects.get(i).imageReceiver.getLottieAnimation().isGeneratingCache()) {
//                            return false;
//                        }
//                    }
//                }
//                if (sameAnimationsCount >= 4) {
//                    return false;
//                }
//                if (animation < 0 || animation > arrayList.size() - 1) {
//                    animation = Math.abs(random.nextInt()) % arrayList.size();
//                }
//                TLRPC.Document document = arrayList.get(animation);
        if (currentAvailableReaction == null) {
            return false;
        }
        TLRPC.Document effectDocument = currentAvailableReaction.effect_animation;
        TLRPC.Document activationDocument = currentAvailableReaction.activate_animation;

        DrawingObject drawingEffectObject = new DrawingObject();
        drawingEffectObject.document = effectDocument;
        drawingEffectObject.isOut = view.getMessageObject().isOutOwner();

        Integer lastIndex = lastEffectAnimationIndex.get(effectDocument.id);
        int currentIndex = lastIndex == null ? 0 : lastIndex;
        lastEffectAnimationIndex.put(effectDocument.id, (currentIndex + 1) % 4);

        ImageLocation imageLocation = ImageLocation.getForDocument(effectDocument);
        drawingEffectObject.imageReceiver.setUniqKeyPrefix(currentIndex + "_" + view.getMessageObject().getId() + "_"); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        int w = (int) (2f * imageW / AndroidUtilities.density);
        drawingEffectObject.imageReceiver.setImage(imageLocation, w + "_" + w + "_pcache", null, "tgs", set, 1); // TODO 30/11/2021 Fuji team, RIDER-: check logic
        drawingEffectObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
        drawingEffectObject.imageReceiver.setAllowStartAnimation(true);
        drawingEffectObject.imageReceiver.setAutoRepeat(0);
        if (drawingEffectObject.imageReceiver.getLottieAnimation() != null) {
            drawingEffectObject.imageReceiver.getLottieAnimation().start();
        }
        final ViewParent reactionCellParent = view.getParent();
        final View menuView = (View) reactionCellParent.getParent();
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        drawingEffectObject.imageReceiver.onAttachedToWindow();
        drawingEffectObject.imageReceiver.setParentView(contentLayout);

        // Activation animation

        float activationImageW = 300;

        DrawingObject drawingActivationObject = new DrawingObject();
        drawingActivationObject.document = activationDocument;
        drawingActivationObject.isOut = view.getMessageObject().isOutOwner();

        Integer lastActivationIndex = lastActivationAnimationIndex.get(activationDocument.id);
        int currentActivationIndex = lastActivationIndex == null ? 0 : lastActivationIndex;
        lastActivationAnimationIndex.put(activationDocument.id, (currentActivationIndex + 1) % 4);

        ImageLocation imageLocation2 = ImageLocation.getForDocument(activationDocument);
        drawingActivationObject.imageReceiver.setUniqKeyPrefix(currentActivationIndex + "_" + view.getMessageObject().getId() + "_act"); // TODO 01/12/2021 Fuji team, RIDER-: check
        int wActivation = (int) (2f * activationImageW / AndroidUtilities.density);
        drawingActivationObject.imageReceiver.setImage(imageLocation2, wActivation + "_" + activationImageW + "_pcache", null, "tgs", set, 1);
        drawingActivationObject.imageReceiver.setLayerNum(Integer.MAX_VALUE);
        drawingActivationObject.imageReceiver.setAllowStartAnimation(true);
        drawingActivationObject.imageReceiver.setAutoRepeat(0);
        if (drawingActivationObject.imageReceiver.getLottieAnimation() != null) {
            drawingActivationObject.imageReceiver.getLottieAnimation().start();
        }
        drawingActivationObject.imageReceiver.onAttachedToWindow();
        drawingActivationObject.imageReceiver.setParentView(contentLayout);
        DrawingShowAnimationObject drawingAnimationObject = new DrawingShowAnimationObject(drawingActivationObject, drawingEffectObject);
        drawingAnimationObject.startAnimPositionX = tapPositionX + listView.getX() + view.getX();
        drawingAnimationObject.startAnimPositionY = tapPositionY + listView.getY() + view.getY();
        drawingAnimationObject.messageId = view.getMessageObject().getId();
        drawingAnimationObject.availableReaction = currentAvailableReaction;
        drawingShowAnimationObjects.add(drawingAnimationObject);
        if (isTheSamePosition) {
            drawingAnimationObject.lastTouchPositionX = tapPositionX;
            drawingAnimationObject.lastTouchPositionY = tapPositionY;
        }
        drawingAnimationObject.isTheSamePosition = isTheSamePosition;
        contentLayout.invalidate();
        view.setIsProcessingReactAnimation(true);



       /* if (sendTap) {
            if (lastTappedMsgId != 0 && lastTappedMsgId != view.getMessageObject().getId()) {
                if (sentInteractionsRunnable != null) {
                    AndroidUtilities.cancelRunOnUIThread(sentInteractionsRunnable);
                    sentInteractionsRunnable.run();
                }
            }
            lastTappedMsgId = view.getMessageObject().getId();
            lastTappedEmoji = currentAvailableReaction.reaction;
            if (lastTappedTime == 0) {
                lastTappedTime = System.currentTimeMillis();
                timeIntervals.clear();
                animationIndexes.clear();
                timeIntervals.add(0L);
                animationIndexes.add(animation);
            } else {
                timeIntervals.add(System.currentTimeMillis() - lastTappedTime);
                animationIndexes.add(animation);
            }
            if (sentInteractionsRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(sentInteractionsRunnable);
                sentInteractionsRunnable = null;
            }
            AndroidUtilities.runOnUIThread(sentInteractionsRunnable = () -> {
                sendCurrentTaps();
                sentInteractionsRunnable = null;
            }, 500);
        }*/

//        if (sendSeen) {
//            MessagesController.getInstance(currentAccount).sendTyping(dialogId, threadMsgId, 11, reaction, 0);
//        }
        return true;
//            }
//        }
//        return false;
    }

    private void sendCurrentTaps() {
        if (lastTappedMsgId == 0) {
            return;
        }
        TLRPC.TL_sendMessageEmojiInteraction interaction = new TLRPC.TL_sendMessageEmojiInteraction();
        interaction.msg_id = lastTappedMsgId;
        interaction.emoticon = lastTappedEmoji;
        interaction.interaction = new TLRPC.TL_dataJSON();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("v", ANIMATION_JSON_VERSION);
            JSONArray array = new JSONArray();

            for (int i = 0; i < timeIntervals.size(); i++) {
                JSONObject action = new JSONObject();
                action.put("i", animationIndexes.get(i) + 1);
                action.put("t", timeIntervals.get(i) / 1000f);
                array.put(i, action);
            }

            jsonObject.put("a", array);
        } catch (JSONException e) {
            clearSendingInfo();
            FileLog.e(e);
            return;
        }
        interaction.interaction.data = jsonObject.toString();

        TLRPC.TL_messages_setTyping req = new TLRPC.TL_messages_setTyping();
        if (threadMsgId != 0) {
            req.top_msg_id = threadMsgId;
            req.flags |= 1;
        }
        req.action = interaction;
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, null);
        clearSendingInfo();
    }

    private void clearSendingInfo() {
        lastTappedMsgId = 0;
        lastTappedEmoji = null;
        lastTappedTime = 0;
        timeIntervals.clear();
        animationIndexes.clear();
    }

    public void onScrolled(int dy) {
        for (int i = 0; i < drawingShowAnimationObjects.size(); i++) {
            if (!drawingShowAnimationObjects.get(i).viewFound) {
                drawingShowAnimationObjects.get(i).finalAnimPositionY -= dy;
            }
            /*if (!drawingHideAnimationObjects.get(i).viewFound) {
                drawingHideAnimationObjects.get(i).finalAnimPositionY -= dy;
            }*/
        }
    }

    interface AnimationStartListener {
        void onAnimationStart();
    }

    private static class DrawingObject {
        boolean isOut;
        boolean isUpdatedLocalReaction;
        TLRPC.Document document;
        ImageReceiver imageReceiver = new ImageReceiver();
    }

    private static class DrawingShowAnimationObject {
        public DrawingObject effectDrawingObject;
        public DrawingObject activationDrawingObject;
        public float startAnimPositionX;
        public float startAnimPositionY;
        public float finalAnimPositionX;
        public float finalAnimPositionY;
        public float lastAnimPositionX;
        public float lastAnimPositionY;
        public float lastTouchPositionX;
        public float lastTouchPositionY;
        public float lastW;
        public boolean viewFound;
        public boolean isTheSamePosition;
        boolean wasPlayed;
        TLRPC.TL_availableReaction availableReaction;
        int messageId;

        DrawingShowAnimationObject(DrawingObject activationDrawingObject, DrawingObject effectDrawingObject) {
            this.activationDrawingObject = activationDrawingObject;
            this.effectDrawingObject = effectDrawingObject;
        }
    }

    private static class DrawingHideAnimationObject {
        public DrawingObject hideDrawingObject;
        public float startAnimPositionX;
        public float startAnimPositionY;
        public float finalAnimPositionX;
        public float finalAnimPositionY;
        public float lastW;
        public boolean viewFound;
        boolean wasPlayed;
        boolean isFinished;
        String reaction;
        int messageId;

        DrawingHideAnimationObject(DrawingObject hideDrawingObject) {
            this.hideDrawingObject = hideDrawingObject;
        }
    }
}
