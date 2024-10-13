package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import static org.telegram.messenger.AndroidUtilities.dp;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ListView.AdapterWithDiffUtils;
import org.telegram.ui.QuickShareMenu;

@SuppressLint("ViewConstructor")
public class QuickShareContainerLayout extends FrameLayout {

    private static final int VIEW_TYPE_CONTACT = 0;
    public final RecyclerView recyclerListView;
    public final LinearLayoutManager linearLayoutManager;
    public final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final int shadowSize = dp(7);
    public final Drawable shadow;
    private final int currentAccount = UserConfig.selectedAccount;
    private final ArrayList<MessageObject> sendingMessageObjects;
    private final Adapter listAdapter;
    private final ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
    private final int contactSizeDp = 52;
    public final int contactSize = dp(contactSizeDp);
    private final int maxItems = 5;
    //    private final int maxWidth = contactSize * maxItems;
    private final int textViewBottomMargin = dp(16);
    public final int textViewHeight = dp(24) + textViewBottomMargin;
    private final int marginInBackgroundDp = 8;
    public final int marginInBackground = dp(marginInBackgroundDp);
    private final int totalHeight = contactSize + textViewHeight + marginInBackground * 2 /*+ shadowSize * 2*/;
    private final int startItemPadding = dp(10);
    private final int defaultItemPadding = dp(10);
    private final int endItemPadding = dp(10);
    private final int maxWidth = startItemPadding + endItemPadding + (defaultItemPadding + contactSize) * 2 + 3 * (defaultItemPadding * 2 + contactSize) /*+ shadowSize * 2*/;
    private final Rect shadowPad = new Rect();
    private final QuickShareContainerListener quickShareContainerListener;
    public boolean terminalEventHappened = false;
    public ArrayList<InnerItem> items = new ArrayList<>();
    public ArrayList<InnerItem> oldItems = new ArrayList<>();
    public RectF rect = new RectF();
    public RectF relativeBackgroundRect = new RectF();
    public float radius = dp(72);
    public boolean disableDefaultDrawing = false;
    public boolean firstDrawn = false;
    public float childrenAnimationProgress = 1f;
    public float backgroundXProgress = 1f;
    public RectF menuBackgroundAnimRectF;
    private ChatActivity parentFragment;
    private Theme.ResourcesProvider resourcesProvider;

    public QuickShareContainerLayout(@NonNull Context context,
                                     Theme.ResourcesProvider resourcesProvider,
                                     QuickShareContainerListener quickShareContainerListener,
                                     ArrayList<MessageObject> sendingMessageObjects,
                                     ChatActivity parentFragment) {
        super(context);
        setTag(QuickShareMenu.QUICK_SHARE_TAG);
        recyclerListView = new RecyclerView(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
                    int сhosenId = -1;
                    for (int i = 0; i < getChildCount(); i++) {
                        View currentChild = getChildAt(i);
                        boolean chosen = isChosen(event, currentChild);
                        if (chosen) {
                            сhosenId = i;
                        }
                    }
                    if (!disableDefaultDrawing) {
                        for (int i = 0; i < getChildCount(); i++) {
                            QuickShareViewHolder currentChild = (QuickShareViewHolder) getChildAt(i);
                            if (сhosenId == -1) {
                                currentChild.setAlphaImageStateChosen();
                            } else {
                                currentChild.setChosen(i == сhosenId);
                            }
                        }
                    }
                    if (сhosenId == -1 && !disableDefaultDrawing && event.getAction() == MotionEvent.ACTION_DOWN) {
                        quickShareContainerListener.onClickOutside();
                        return true;
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    terminalEventHappened = true;
                    for (int i = 0; i < getChildCount(); i++) {
                        QuickShareViewHolder currentChild = (QuickShareViewHolder) getChildAt(i);
                        boolean chosen = isChosen(event, currentChild);
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            if (chosen) {
                                if (!disableDefaultDrawing) {
                                    currentChild.setVisibility(View.INVISIBLE);
                                    sendInternal(true, currentChild.did, currentChild.imageView);
                                }
                                return true;
                            }
                        }
                    }
                }
                return true;
            }

            private boolean isChosen(MotionEvent event, View currentChild) {
                float x = event.getX();
                float y = event.getY();
                int childIndex = indexOfChild(currentChild);
                int leftPadding = getItemLeftOffset(childIndex);
                int rightPadding = getItemRightOffset(childIndex);
                boolean chosenByX = x >= currentChild.getX() - leftPadding && x <= currentChild.getX() + currentChild.getMeasuredWidth() + rightPadding;
                boolean chosenByY = y >= currentChild.getY() + textViewHeight && y <= currentChild.getY() + getMeasuredHeight();
                return chosenByX && chosenByY;
            }

            private float getInterpolatorForIndex(View child) {
                int childrenSize = listAdapter.getItemCount();
                int currentChildIndex = indexOfChild(child);
                if (childrenSize % 2 == 0) {
                    int pseudoMiddleIndex = childrenSize / 2;
                    int index = indexOfChild(child);
                    index = index < pseudoMiddleIndex ? index - pseudoMiddleIndex / 2 : index - pseudoMiddleIndex;
                    return getScaleInterpolator(
                            childrenAnimationProgress,
                            Math.min(0.9f, Math.abs(index) * 0.2f)
                    );
                } else {
                    int middle = childrenSize / 2;
                    if (middle == currentChildIndex) {
                        return getScaleInterpolator(
                                childrenAnimationProgress,
                                0
                        );
                    } else {
                        return getScaleInterpolator(
                                childrenAnimationProgress,
                                Math.min(0.9f, Math.abs(indexOfChild(child) - middle) * 0.2f)
                        );
                    }
                }
            }

            private float getScaleInterpolator(float progress, float firstStage) {
                float secondStage = 0.4f;
                float result;

                if (progress <= firstStage) {
                    result = 0f;
                } else if (progress <= firstStage + secondStage) {
                    progress -= firstStage;
                    result = 1f * (progress / secondStage);
                } else {
                    result = 1f;
                }
                return result;
            }

            @Override
            public boolean drawChild(Canvas canvas, View child, long drawingTime) {
                if (disableDefaultDrawing) {
                    int count = canvas.save();
                    float scaleProgress = getInterpolatorForIndex(child);
                    float dx = (menuBackgroundAnimRectF.centerX()) - getMeasuredWidth() / 2f;
                    float dy = ((menuBackgroundAnimRectF.centerY()) - getPaddingTop()) - textViewHeight - contactSize / 2f - marginInBackground;
                    float currentOffset = (menuBackgroundAnimRectF.width() / listAdapter.getItemCount());
                    float offsetX;
                    int indexOfChild = indexOfChild(child);
                    int middleItem = listAdapter.getItemCount() / 2;
                    if (listAdapter.getItemCount() % 2 == 0) {
                        currentOffset /= 2;
                        if (indexOfChild < middleItem) {
                            offsetX = (indexOfChild - middleItem) * currentOffset;
                        } else {
                            offsetX = (indexOfChild - middleItem + 1) * currentOffset;
                        }
                    } else {
                        offsetX = (indexOfChild - middleItem) * currentOffset;
                    }
                    canvas.scale(
                            scaleProgress,
                            scaleProgress,
                            menuBackgroundAnimRectF.centerX() - dx + offsetX,
                            menuBackgroundAnimRectF.centerY() - dy
                    );
                    invalidate();
                    boolean result = super.drawChild(canvas, child, drawingTime);
                    canvas.restoreToCount(count);
                    return result;
                } else {
                    return super.drawChild(canvas, child, drawingTime);
                }
            }
        };
        recyclerListView.setClipChildren(false);
        recyclerListView.setClipToPadding(false);
        setClipToPadding(false);
        setClipChildren(false);
        recyclerListView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) {

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
//                System.out.println("Scroll horizontally");
                return super.scrollHorizontallyBy(dx, recycler, state);

            }

            @Override
            public boolean canScrollHorizontally() {
                return false;
            }
        };
        recyclerListView.setLayoutManager(linearLayoutManager);
        recyclerListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int position = parent.getChildAdapterPosition(view);
                if (position == 0) {
                    outRect.left = startItemPadding;
                } else {
                    outRect.left = defaultItemPadding / 2;
                }
                if (position == listAdapter.getItemCount() - 1) {
                    outRect.right = endItemPadding;
                } else {
                    outRect.right = defaultItemPadding / 2;
                }
            }
        });
        recyclerListView.setAdapter(listAdapter = new Adapter());
//        addView(recyclerListView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(recyclerListView, ViewGroup.LayoutParams.WRAP_CONTENT, totalHeight);
        bgPaint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
        shadow = ContextCompat.getDrawable(context, R.drawable.reactions_bubble_shadow).mutate();
        shadow.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_messagePanelShadow), PorterDuff.Mode.MULTIPLY));
        shadowPad.left = shadowPad.top = shadowPad.right = shadowPad.bottom = shadowSize;
        setPadding(shadowSize, shadowSize, shadowSize, shadowSize);
        this.quickShareContainerListener = quickShareContainerListener;
        this.sendingMessageObjects = sendingMessageObjects;
        this.parentFragment = parentFragment;
        this.resourcesProvider = resourcesProvider;
    }

    private int getItemLeftOffset(int index) {
        if (index == 0) {
            return startItemPadding;
        } else {
            return defaultItemPadding / 2;
        }
    }

    private int getItemRightOffset(int index) {
        if (index == listAdapter.getItemCount() - 1) {
            return endItemPadding;
        } else {
            return defaultItemPadding / 2;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), heightMeasureSpec);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (disableDefaultDrawing) {
            return;
        }
        firstDrawn = true;
        int canvasCount = canvas.save();
        int backgroundHeight = getMeasuredHeight() - textViewHeight;
        canvas.translate(0, textViewHeight);
        shadow.setBounds(0, 0, getMeasuredWidth(), backgroundHeight);
        shadow.draw(canvas); // SHADOW FOR THE CLOUD
        rect.set(shadowPad.left, shadowPad.top, getMeasuredWidth() - shadowPad.right, backgroundHeight - shadowPad.bottom);
        float topOffset = 0f;
        float expandSize = 0f; // TODO: 20.10.2024
        radius = ((rect.height() - topOffset) - expandSize * 2f) / 2f;
        canvas.drawRoundRect(rect, radius, radius, bgPaint); // DRAW THE BACKGROUND OF THE CLOUD
        canvas.restoreToCount(canvasCount);
        super.dispatchDraw(canvas);
    }

    public void updateItems(ArrayList<TLRPC.Dialog> newDialogs) {
        this.dialogs.clear();
        dialogs.addAll(newDialogs);
        listAdapter.updateItems(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return recyclerListView.onTouchEvent(event);
    }

    protected void sendInternal(boolean withSound, TLRPC.Dialog did, View fromView) {
        if (AlertsCreator.checkSlowMode(getContext(), currentAccount, did.id, false)) { // TODO: 21.10.2024 check few logic
            return;
        }

        if (sendingMessageObjects != null) {
            long key = did.id;
//                TLRPC.TL_forumTopic topic = selectedDialogTopics.get(selectedDialogs.get(key));
            TLRPC.TL_forumTopic topic = null;
            MessageObject replyTopMsg = topic != null ? new MessageObject(currentAccount, topic.topicStartMessage, false, false) : null;
            if (replyTopMsg != null) {
                replyTopMsg.isTopicMainMessage = true;
            }
//                if (frameLayout2.getTag() != null && commentTextView.length() > 0) {
//                    SendMessagesHelper.getInstance(currentAccount).sendMessage(SendMessagesHelper.SendMessageParams.of(text[0] == null ? null : text[0].toString(), key, replyTopMsg, replyTopMsg, null, true, entities, null, null, withSound, 0, null, false));
//                }
            boolean showSendersName = true; // TODO: 21.10.2024 check
            int result = SendMessagesHelper.getInstance(currentAccount).sendMessage(sendingMessageObjects, key, !showSendersName, false, withSound, 0, replyTopMsg);
            if (result != 0) {
                AlertsCreator.showSendMediaAlert(result, parentFragment, null);
            } else {
                quickShareContainerListener.onSend(did, null, fromView); // TODO: 21.10.2024 fix topic
            }
        } // TODO: 21.10.2024 handle empty messages?
    }

    public int getBackgroundHeight() {
        return getMeasuredHeight() - textViewHeight;
    }

    public void drawChildren(Canvas canvas, float progress, RectF menuBackgroundAnimRectF, float xProgress) {
        this.menuBackgroundAnimRectF = menuBackgroundAnimRectF;
        childrenAnimationProgress = progress;
        backgroundXProgress = xProgress;
        super.dispatchDraw(canvas);
    }

    public interface QuickShareContainerListener {
        void onSend(TLRPC.Dialog did, TLRPC.TL_forumTopic topic, View fromView);

        void onClickOutside();
    }

    static class InnerItem extends AdapterWithDiffUtils.Item {
        TLRPC.Dialog dialog;

        public InnerItem(int viewType, TLRPC.Dialog dialog) {
            super(viewType, false);
            this.dialog = dialog;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InnerItem innerItem = (InnerItem) o;
            if (viewType == innerItem.viewType && (viewType == VIEW_TYPE_CONTACT)) {
                return dialog != null && dialog.id == innerItem.dialog.id; // TODO: 20.10.2024 check
            }
            return viewType == innerItem.viewType;
        }
    }

    public class Adapter extends AdapterWithDiffUtils {

        public Adapter() {
            fetchDialogs();
        }

        public void fetchDialogs() {
            dialogs.clear();
//        dialogsMap.clear();
            long selfUserId = UserConfig.getInstance(currentAccount).clientUserId;
            ArrayList<TLRPC.Dialog> dialogsForward = MessagesController.getInstance(currentAccount).dialogsForward;
            if (!dialogsForward.isEmpty()) {
                TLRPC.Dialog dialog = dialogsForward.get(0);
                dialogs.add(dialog);
//            dialogsMap.put(dialog.id, dialog);
            }

//            ArrayList<TLRPC.Dialog> archivedDialogs = new ArrayList<>();
            ArrayList<TLRPC.Dialog> allDialogs = MessagesController.getInstance(currentAccount).getAllDialogs();
            for (int a = 0; a < allDialogs.size(); a++) {
                TLRPC.Dialog dialog = allDialogs.get(a);
                if (!(dialog instanceof TLRPC.TL_dialog)) {
                    continue;
                }
                if (dialog.id == selfUserId) {
                    continue;
                }
                if (!DialogObject.isEncryptedDialog(dialog.id)) {
                    if (DialogObject.isUserDialog(dialog.id)) {
                        if (dialog.folder_id == 1) {
//                            archivedDialogs.add(dialog);
                        } else {
                            dialogs.add(dialog);
                            if (dialogs.size() >= maxItems) {
                                break;
                            }
                        }
//                        dialogsMap.put(dialog.id, dialog);
                    } else {
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                        if (!(chat == null || ChatObject.isNotInChat(chat) || chat.gigagroup && !ChatObject.hasAdminRights(chat) || ChatObject.isChannel(chat) && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) && !chat.megagroup)) {
                            if (dialog.folder_id == 1) {
//                                archivedDialogs.add(dialog);
                            } else {
                                dialogs.add(dialog);
                                if (dialogs.size() >= maxItems) {
                                    break;
                                }
                            }
//                            dialogsMap.put(dialog.id, dialog);
                        }
                    }
                }
            }
            updateItems(true);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                default:
                case VIEW_TYPE_CONTACT:
                    view = new QuickShareViewHolder(
                            QuickShareContainerLayout.this,
                            contactSizeDp,
                            marginInBackgroundDp,
                            textViewHeight,
                            textViewBottomMargin,
                            maxWidth,
                            resourcesProvider);
                    break;
            }

            view.setLayoutParams(new RecyclerView.LayoutParams(contactSize, totalHeight));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_CONTACT) {
                QuickShareViewHolder contactHolderView = (QuickShareViewHolder) holder.itemView;
                TLRPC.Dialog dialog = items.get(position).dialog;
                contactHolderView.did = dialog;
                long uid = dialog.id;
//                imageView.setImageResource(contact.getImage());
//                imageView.setName(contact.getName());
                BackupImageView imageView = contactHolderView.imageView;
                AvatarDrawable avatarDrawable = contactHolderView.avatarDrawable;
                if (DialogObject.isUserDialog(uid)) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);
                    invalidate();
                    avatarDrawable.setInfo(currentAccount, user);
                    if (/*contactHolderView.currentType != TYPE_CREATE && */UserObject.isUserSelf(user)) {
                        avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                        contactHolderView.setName(LocaleController.getString(R.string.SavedMessages));
                        imageView.setImage(null, null, avatarDrawable, user);
                    } else if (user != null) {
                        contactHolderView.setName(ContactsController.formatName(user.first_name, user.last_name));
                        imageView.setForUserOrChat(user, avatarDrawable);
                    }
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-uid);
                    avatarDrawable.setInfo(currentAccount, chat);
                    if (chat != null) {
                        imageView.setForUserOrChat(chat, avatarDrawable);
                        contactHolderView.setName(chat.title);
                    }
                }
                imageView.setRoundRadius(dp(28));
                if (position == 0) {
                    int contactHalfSize = contactSize / 2 + startItemPadding;
                    int diffX = Math.max(0, contactHolderView.getTextViewRealWidth() / 2 - contactHalfSize);
                    if (diffX != 0) {
                        contactHolderView.textViewGroup.setTranslationX((float) diffX);
                    }
                } else if (position == getItemCount() - 1) {
                    int contactFullSize = contactSize / 2 + endItemPadding;
                    int diffX = Math.max(0, contactHolderView.getTextViewRealWidth() / 2 - contactFullSize);
                    if (diffX != 0) {
                        contactHolderView.textViewGroup.setTranslationX((float) -diffX);
                    }
                }
//                contactHolderView.imageView.setImage(null, null, avatarDrawable, user);

//                ReactionHolderView h = (ReactionHolderView) holder.itemView;
//                h.setScaleX(1);
//                h.setScaleY(1);
//                h.setReaction(items.get(position).reaction, position);
            }
        }

        @Override
        public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
//            if (holder.getItemViewType() == VIEW_TYPE_CONTACT) {
//                int position = holder.getAdapterPosition();
//                if (position >= 0 && position < items.size()) {
//                    ((ReactionHolderView) holder.itemView).updateSelected(items.get(position).reaction, false);
//                }
//            }
            super.onViewAttachedToWindow(holder);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        public void updateItems(boolean animated) {
            oldItems.clear();
            oldItems.addAll(items);
            items.clear();
            for (int i = 0; i < dialogs.size(); i++) {
                TLRPC.Dialog contact = dialogs.get(i);
                items.add(new InnerItem(VIEW_TYPE_CONTACT, contact));
            }
            if (animated) {
                setItems(oldItems, items);
            } else {
                super.notifyDataSetChanged();
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false; // TODO: 20.10.2024
        }
    }
}
