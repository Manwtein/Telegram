package org.telegram.ui.ActionBar;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.ReactionInteractor;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.ViewPagerReactions;

import java.util.ArrayList;
import java.util.HashMap;

class ReactionSeenContainerView extends ViewPagerReactions {

    //    public TLRPC.TL_messageReactions messageReactions;
    public ArrayList<TLRPC.TL_reactionCount> results;
    private int maxHeight = 0;
    private int maxWidth = 400;
    private MessagesController messagesController;
    private ActionBarPopupWindowWithReactions.ActionBarPopupContainer.ReactionMenuListener reactionMenuListener;
    private Adapter listAdapter;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private ArrayList<TLRPC.TL_availableReaction> availableReactions = new ArrayList<>();
    private TabsView tabsView;

    private TLRPC.InputPeer inputPeer;
    private int messageId;
    private int accountId;

    public ReactionSeenContainerView(
            @NonNull Context context,
            int maxHeight,
            MessagesController messagesController,
            ActionBarPopupWindowWithReactions.ActionBarPopupContainer.ReactionMenuListener reactionMenuListener,
            TLRPC.TL_messageReactions messageReactions,
            Theme.ResourcesProvider resourcesProvider,
            TLRPC.InputPeer inputPeer,
            int messageId,
            int accountId
    ) {
        super(context);
        this.maxHeight = maxHeight;
        this.messagesController = messagesController;
        this.reactionMenuListener = reactionMenuListener;
//        this.messageReactions = messageReactions;
        this.results = new ArrayList<>(messageReactions.results);
        final TLRPC.TL_reactionCount generalReactionCount = new TLRPC.TL_reactionCount();
        generalReactionCount.reaction = ReactionInteractor.nonFilteredMessageKey;
        generalReactionCount.count = messagesController.getGeneralReactionsCount(ReactionInteractor.nonFilteredMessageKey);
        this.results.add(0, generalReactionCount);
        this.inputPeer = inputPeer;
        this.messageId = messageId;
        this.accountId = accountId;
        createViews(context, resourcesProvider);
    }

    private void createViews(Context context, Theme.ResourcesProvider resourcesProvider) {
        ActionBarMenuSubItem cell = new ActionBarMenuSubItem(context, true, true, resourcesProvider);
        cell.setItemHeight(44);
        cell.setTextAndIcon(LocaleController.getString("Back", R.string.Back), R.drawable.msg_arrow_back);
        cell.getTextView().setPadding(LocaleController.isRTL ? 0 : AndroidUtilities.dp(40), 0, LocaleController.isRTL ? AndroidUtilities.dp(40) : 0, 0);
        final FrameLayout backContainer = new FrameLayout(getContext());
        backContainer.addView(cell);
//        cell.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                reactionMenuListener.onBackClick();
//            }
//        });
        backContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reactionMenuListener.onBackClick();
            }
        });
        addView(backContainer);
        setBackButton(backContainer);
        listAdapter = new ListAdapter(context);
        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(true);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        addView(
                listView, LayoutHelper.createFrame(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.MATCH_PARENT,
                        Gravity.TOP | Gravity.LEFT,
                        0,
                        0,
                        0,
                        20
                ));
        setAdapter(listAdapter);
//        flickerLoadingView = new FlickerLoadingView(context);
//        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
//        flickerLoadingView.setViewType(FlickerLoadingView.USERS_TYPE);
//        flickerLoadingView.setIsSingleCell(false);
//        addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        Drawable shadowDrawable2 = ContextCompat.getDrawable(context, R.drawable.popup_fixed_alert).mutate();
        shadowDrawable2.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
        setBackground(shadowDrawable2);
        tabsView = createTabsView(results, resourcesProvider);
        addView(tabsView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                34,
                Gravity.TOP | Gravity.LEFT,
                0,
                44,
                0,
                0
        ));
//        addView(new ReactionListView(context, messagesController, itemChooseListener),
//                LayoutHelper.createFrame(MeasureSpec.AT_MOST, MeasureSpec.AT_MOST, Gravity.TOP, 0, 10, 0, 10)
//        );
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(
//                widthMeasureSpec,
//                MeasureSpec.makeMeasureSpec(Math.max(maxHeight, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.AT_MOST)
//        );
//    }

//    public void setMessageUserReactions(
//            HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions,
//            ArrayList<TLRPC.TL_availableReaction> availableReactions,
//            HashMap<String, String> reactionOffsets,
//            TLRPC.TL_messages_messageReactionsList generalReactions
//    ) {
//        this.previousFilteredReactions = this.filteredReactions;
//        this.filteredReactions = new HashMap<>();
//        this.availableReactions = availableReactions;
//        this.generalReactions = generalReactions;
//        this.previousReactionOffsets = this.reactionOffsets;
//        this.reactionOffsets = new HashMap<>();
//        reactionOffsets.putAll(previousReactionOffsets);
//        reactionOffsets.putAll(reactionOffsets);
//        invalidateItem(getCurrentPosition());
//    }

    public void updateReactionListView() {
        invalidateItem(getCurrentPosition());
    }

    private class ListAdapter extends Adapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        @Override
        public View createView(int viewType) {
            ReactionListView listView = new ReactionListView(
                    mContext,
                    messagesController,
                    reactionMenuListener,
                    accountId,
                    inputPeer,
                    messageId
            );
//            listView.setLayoutParams(LayoutHelper.createFrame(MeasureSpec.AT_MOST, MeasureSpec.AT_MOST, Gravity.CENTER, 0, 10, 0, 10));
            listView.setLayoutParams(LayoutHelper.createFrame(
                    LayoutHelper.MATCH_PARENT,
                    LayoutHelper.MATCH_PARENT,
                    Gravity.TOP,
                    0,
                    88,
                    0,
                    0
            ));
            return listView;
        }

        @Override
        public void bindView(View view, int position, int viewType) {
            final ReactionListView containerView = (ReactionListView) view;
//            if (position == 0 && generalReactions != null && generalReactions.reactions.size() > 0) {
////                generalReactions
//                        containerView.setMessageUserReactions(
//                                generalReactions.reactions,
//                                availableReactions,
//                                containerView.getNextOffset() == null && generalReactions.next_offset != null,
//                                generalReactions.next_offset,
//                                null
//                        );
//            } else {
            final String reaction = results.get(position).reaction;
//            TLRPC.TL_messages_messageReactionsList reactions = currentMessageReactions.get(reaction);
            containerView.setMessageUserReactions(
                    messagesController.getMessageUserReactions(reaction),
                    messagesController.getNextOffset(reaction),
                    reaction,
                    (messagesController.getStartIndexForUpdate(reaction) - 1)
            );
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }
    }
}
