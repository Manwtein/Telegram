/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.ActionBar;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ReactionCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.ReactionUserCell;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;

import java.util.ArrayList;

public class ReactionListView extends FrameLayout {

    public static String TAG_VIEW = "reaction_list_view_tag";
    public ActionBarPopupWindowWithReactions.ActionBarPopupContainer.ReactionMenuListener reactionMenuListener;
    FlickerLoadingView flickerLoadingView;
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    @SuppressWarnings("FieldCanBeLocal")
    private LinearLayoutManager layoutManager;
    private boolean hasChanges = false;
    private Boolean isReactionsEnabled = true;
    private boolean animateChecked;
    private ArrayList<TLRPC.TL_messageUserReaction> messageUserReactions = new ArrayList<>();
    private MessagesController messagesController;
    private String reaction;
    private boolean ignoreLayout;
    private boolean isFullItems;
    private boolean firstLoaded;
    private boolean isLoading;
    private int currentAccount;
    private TLRPC.InputPeer inputPeer;
    private int messageId;
    private String nextOffset;

    public ReactionListView(
            Context context,
            MessagesController messagesController,
            ActionBarPopupWindowWithReactions.ActionBarPopupContainer.ReactionMenuListener reactionMenuListener,
            int currentAccount,
            TLRPC.InputPeer inputPeer,
            int messageId
    ) {
        super(context); // TODO 05/12/2021 Fuji team, RIDER-:, add theme
        this.messagesController = messagesController;
        this.reactionMenuListener = reactionMenuListener;
        this.currentAccount = currentAccount;
        this.inputPeer = inputPeer;
        this.messageId = messageId;
        createView(context);
    }

    public void createView(Context context) {
        listAdapter = new ListAdapter(context);
        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
        flickerLoadingView.setViewType(FlickerLoadingView.USERS_TYPE);
        flickerLoadingView.setIsSingleCell(false);
        addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position) -> {
            reactionMenuListener.onItemChoose(((ReactionUserCell) view).getUser());
        });
        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!isFullItems && layoutManager.findLastVisibleItemPosition() > messageUserReactions.size() - 5 && !isLoading) {
                    isLoading = true;
                    MessagesController
                            .getInstance(currentAccount)
                            .loadMessageReactionsDetail(inputPeer, messageId, reaction, nextOffset);
                }
            }
        });
    }

    public String getNextOffset() {
        return nextOffset;
    }
//
//    public void setMessageUserReactions(
//            ArrayList<TLRPC.TL_messageUserReaction> messageUserReactions,
//            ArrayList<TLRPC.TL_availableReaction> availableReactions,
//            boolean isForceSetChanged,
//            String nextOffset,
//            String reaction
//    ) {
//        if (messageUserReactions == null || availableReactions == null) {
//            return;
//        }
//        int startIndex = this.messageUserReactions.size() - 1;
//        this.messageUserReactions.addAll(messageUserReactions);
//        this.availableReactions = availableReactions;
//        isFullItems = nextOffset == null;
//        if (startIndex == -1 || isForceSetChanged) {
//            listAdapter.notifyDataSetChanged();
//        } else {
//            listAdapter.notifyItemRangeInserted(startIndex, messageUserReactions.size());
//        }
//
//        this.nextOffset = nextOffset;
//
//        if (this.reaction == null) {
//            this.reaction = reaction;
//        }
//        flickerLoadingView.setVisibility(View.GONE);
//        isLoading = false;
//    }

    public void setMessageUserReactions(
            ArrayList<TLRPC.TL_messageUserReaction> messageUserReactions,
            String nextOffset,
            String reaction,
            int startIndex
    ) {
        if (messageUserReactions == null) {
            return;
        }
        this.messageUserReactions = messageUserReactions;
        isFullItems = nextOffset == null;
        if (startIndex <= 0) {
            listAdapter.notifyDataSetChanged();
        } else {
            listAdapter.notifyItemRangeInserted(startIndex, messageUserReactions.size() - (startIndex + 1));
        }

        this.nextOffset = nextOffset;

        this.reaction = reaction;
        flickerLoadingView.setVisibility(View.GONE);
        isLoading = false;
    }

    public String getReaction() {
        return reaction;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (flickerLoadingView.getVisibility() == View.VISIBLE && !messageUserReactions.isEmpty()) {
//            ignoreLayout = true;
//            flickerLoadingView.setVisibility(View.GONE);
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            flickerLoadingView.getLayoutParams().width = getMeasuredWidth();
//            flickerLoadingView.setVisibility(View.VISIBLE);
//            ignoreLayout = false;
        super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(400), MeasureSpec.AT_MOST)
        );
//        } else {
//            if (flickerLoadingView.getVisibility() == View.VISIBLE) {
//                super.onMeasure(
//                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(300), MeasureSpec.EXACTLY),
//                        MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(500), MeasureSpec.EXACTLY)
//                );
//            } else {
//                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            }
//        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (ignoreLayout) {
            return;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    /* @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.chatInfoDidLoad) {
            TLRPC.ChatFull chatFull = (TLRPC.ChatFull) args[0];
            if (chatFull.id == info.id) {
//                AndroidUtilities.runOnUIThread(() -> {
//                    info = chatFull;
//                    setInfo(chatFull);
//                    updateRows();
//                    if (listAdapter != null) {
//                        int index = 0;     // TODO 29/11/2021 Fuji team, RIDER-: add dynamic changes
//                        listAdapter.notifyItemChanged(index);
//                    }
//                });
            }
        }
    }*/
/*
    @Override
    public void onResume() {
        super.onResume();
        // TODO 29/11/2021 Fuji team, RIDER-: check
//        if (listAdapter != null) {
//            listAdapter.notifyDataSetChanged();
//        }
    }*/

   /* @Override
    public void onPause() {
        super.onPause();
//        if (wereAnyChanges) {
//            DownloadController.getInstance(currentAccount).savePresetToServer(currentType);
//            wereAnyChanges = false;
//        }
    }
*/

    /*private void updateRows() {
        rowCount = 0;
        enableReactionsRow = rowCount++;
        enableReactionsDescriptionRow = rowCount++;
        if (isReactionsEnabled) {
            availableReactionsTitleRow = rowCount++;
            rowCount += availableReactions.size();
        } else {
            availableReactionsTitleRow = -1;
//            availableReactionsRow = -1;
        }
    }*/

    public ArrayList<ThemeDescription> getThemeDescriptions() { // TODO 29/11/2021 Fuji team, RIDER-: theme settings
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, ReactionCheckCell.class, SlideChooseView.class}, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundChecked));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{TextCheckCell.class}, null, null, null, Theme.key_windowBackgroundUnchecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundCheckText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlue));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumb));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueThumbChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackBlueSelectorChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ReactionCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{SlideChooseView.class}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{SlideChooseView.class}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{SlideChooseView.class}, null, null, null, Theme.key_windowBackgroundWhiteGrayText));

        return themeDescriptions;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return messageUserReactions.size();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            final TLRPC.TL_messageUserReaction messageUserReaction = messageUserReactions.get(position);
            final TLRPC.User user = messagesController.getUser(messageUserReaction.user_id);
            TLRPC.Document image = messagesController.getAvailableReactionsForSettings().get(messageUserReaction.reaction).static_icon;
            ((ReactionUserCell) holder.itemView).setData(user, image);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            // TODO 29/11/2021 Fuji team, RIDER-: check
//            return position == photosRow || position == videosRow || position == filesRow;
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            ReactionUserCell cell = new ReactionUserCell(mContext, 10, 0, false);

//            cell.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
//            cell.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//            cell.setHeight(56);
            view = cell;
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }
    }
}
