/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ReactionCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;

import java.util.ArrayList;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ChatEditReactionsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    @SuppressWarnings("FieldCanBeLocal")
    private LinearLayoutManager layoutManager;

    private ArrayList<DownloadController.Preset> presets = new ArrayList<>();

    private int enableReactionsRow;
    private int enableReactionsDescriptionRow;
    private int availableReactionsTitleRow;

    private int rowCount;

    private boolean hasChanges = false;

    private Boolean isReactionsEnabled = true;

    private TLRPC.ChatFull info;

    private ArrayList<TLRPC.TL_availableReaction> availableReactions;
    private ArrayList<String> availableReactionsSymbolTags;
//    private HashMap<String, TLRPC.TL_availableReaction> availableReactionsForSettings;

    private boolean animateChecked;

    public ChatEditReactionsActivity() {
        super();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.chatInfoDidLoad);
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        getNotificationCenter().removeObserver(this, NotificationCenter.chatInfoDidLoad);
        // TODO 29/11/2021 Fuji team, RIDER-: check
//        AndroidUtilities.removeAdjustResize(getParentActivity(), classGuid);
    }

    @Override
    public boolean onBackPressed() {
        updateChatAvailableReactionsIfNeed();
        return super.onBackPressed();
    }

    private void updateChatAvailableReactionsIfNeed() {
        if (hasChanges) {
            if (!isReactionsEnabled) {
                availableReactionsSymbolTags.clear();
            }
            getMessagesController().setChatAvailableReactions(
                    getMessagesController().getInputPeer(-info.id),
                    availableReactionsSymbolTags
            );
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("ChannelReactionSettingsTitle", R.string.ChannelReactionSettingsTitle));
        if (AndroidUtilities.isTablet()) { // TODO 28/11/2021 Fuji team, RIDER-: add check
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                    updateChatAvailableReactionsIfNeed();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == enableReactionsRow) {
                TextCheckCell cell = (TextCheckCell) view;
                boolean checked = cell.isChecked();
                isReactionsEnabled = !checked;
                view.setTag(isReactionsEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked);
                cell.setBackgroundColorAnimated(!checked, Theme.getColor(isReactionsEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                updateRows();
                cell.setChecked(isReactionsEnabled);
                if (isReactionsEnabled) {
                    listAdapter.notifyItemRangeInserted(enableReactionsDescriptionRow + 2, availableReactions.size());
                } else {
                    listAdapter.notifyItemRangeRemoved(enableReactionsDescriptionRow + 1, availableReactions.size() + 1);
                }
                hasChanges = true;
            } else if (position > availableReactionsTitleRow) {
                // TODO 29/11/2021 Fuji team, RIDER-: check this
//                if (!view.isEnabled()) {
//                    return;
//                }

                ReactionCheckCell cell = (ReactionCheckCell) view;
                boolean checked = !cell.isChecked();

                // TODO 29/11/2021 Fuji team, RIDER-: check this condition
//                if (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76)) {

                if (checked) {
                    if (availableReactions != null) {
                        availableReactionsSymbolTags.add(cell.getReactionSymbol());
//                        availableReactionsForSettings.put(cell.getReactionSymbol());
                    }
                    cell.resume();
                } else {
                    if (availableReactions != null) {
                        availableReactionsSymbolTags.remove(cell.getReactionSymbol());
//                        availableReactions.remove(cell.getReactionSymbol());
                    }
                    cell.pause();
                }
                cell.setChecked(checked);
//                RecyclerView.ViewHolder holder = listView.findContainingViewHolder(view);
//                    if (holder != null) {
//                        listAdapter.onBindViewHolder(holder, position);
//                    }
//                    DownloadController.getInstance(currentAccount).checkAutodownloadSettings();
                hasChanges = true;
//                    fillPresets();
//                } else {
            }
        });
        return fragmentView;
    }

    public void setInfo(TLRPC.ChatFull chatFull) {
        info = chatFull;
        if (info != null && info.available_reactions != null) {
            availableReactions = getMessagesController().getAvailableReactions((TLRPC.ChatFull) null);
            availableReactionsSymbolTags = new ArrayList<>(info.available_reactions);
//            availableReactionsForSettings = getMessagesController().getAvailableReactionsForSettings();
        }
        isReactionsEnabled = info != null && info.available_reactions != null;
    }

    @Override
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
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO 29/11/2021 Fuji team, RIDER-: check
//        if (listAdapter != null) {
//            listAdapter.notifyDataSetChanged();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (wereAnyChanges) {
//            DownloadController.getInstance(currentAccount).savePresetToServer(currentType);
//            wereAnyChanges = false;
//        }
    }


    private void updateRows() {
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
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell view = (TextCheckCell) holder.itemView;
                    if (position == enableReactionsRow) {
                        view.setDrawCheckRipple(true);
                        view.setTextAndCheck(LocaleController.getString("ChannelReactionEnable", R.string.ChannelReactionEnable), isReactionsEnabled, false);
                        view.setTag(isReactionsEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked);
                        view.setBackgroundColor(Theme.getColor(isReactionsEnabled ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
                    }
                    break;
                }
                case 1: {
                    TextInfoPrivacyCell view = (TextInfoPrivacyCell) holder.itemView;
                    view.setText(LocaleController.getString("ChannelReactionEnableDescription", R.string.ChannelReactionEnableDescription));
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    view.setFixedSize(0);
                    view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES); // TODO 28/11/2021 Fuji team, RIDER-: check
                    break;
                }
                case 2: {
                    HeaderCell view = (HeaderCell) holder.itemView;
                        view.setText(LocaleController.getString("ChannelReactionAvailableTitle", R.string.ChannelReactionAvailableTitle));
                    break;
                }
                case 3: {
                    ReactionCheckCell view = (ReactionCheckCell) holder.itemView;
                    view.pause();
                    if (availableReactions != null) {
                        final int index = position - 3;
                        if (availableReactions.size() > index) {
                            final TLRPC.TL_availableReaction availableReaction = availableReactions.get(index);
                            final String reactionTitle = availableReaction.title;
                            if (reactionTitle != null) {
                                view.setTextAndValueAndCheck(reactionTitle, true, 0, false, true);
                            }
                            final TLRPC.Document sticker = availableReaction.select_animation;
                            if (sticker != null) {
                                view.setSticker(sticker, null, availableReaction.reaction);
                            }

                            // TODO 29/11/2021 Fuji team, RIDER-: check animation
//                            if (animateChecked) {
                            view.setChecked(availableReactionsSymbolTags.contains(availableReaction.reaction));
//                            }
                        }
                    }
                    break;
                }
            }
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
            switch (viewType) {
                case 0: {
                    TextCheckCell cell = new TextCheckCell(mContext);
                    cell.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
                    cell.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                    cell.setHeight(56);
                    view = cell;
                    break;
                }
                case 1: {
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case 2: {
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case 3: {
                    view = new ReactionCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                }
                case 4:
                default: {
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                }
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == enableReactionsRow) {
                return 0;
            } else if (position == enableReactionsDescriptionRow) {
                return 1;
            } else if (position == availableReactionsTitleRow) {
                return 2;
            } else {
                return 3;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() { // TODO 29/11/2021 Fuji team, RIDER-: theme settings
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, ReactionCheckCell.class, SlideChooseView.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

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
}
