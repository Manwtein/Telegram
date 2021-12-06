package org.telegram.messenger;

import android.os.SystemClock;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ReactionInteractor {

    private static final long UPDATE_PERIOD_MILLIS = 3_600L;
    private final MessagesController messagesController;
    private final LongSparseArray<SparseArray<MessageObject>> messagesToCheck = new LongSparseArray<>();
    //    private final ArrayList<Integer> queries = new ArrayList<>();
    private final HashMap<String, Integer> queries = new HashMap<>(); // TODO 28/11/2021 Fuji team, RIDER-: late init
    private int hash = 0;
    private int availableReactionsId = 0;
    private TLRPC.TL_messages_availableReactions cachedAvailableReactions;
    private long lastViewsCheckTime;
    private int messagesToCheckSize;
    private long lastAvailableReactionsLoading;
//    private TLRPC.TL_messages_messageReactionsList lastMessageReactionsList;
//    private HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions = new HashMap<>();
//    private HashMap<String, String> filteredReactionOffsets = new HashMap<>();
//    private TLRPC.TL_messages_messageReactionsList lastMessageReactionsListByFilter;

    public ReactionInteractor(MessagesController messagesController) {
        this.messagesController = messagesController;
    }

    private ConnectionsManager getConnectionsManager() {
        return messagesController.getConnectionsManager();
    }

    private NotificationCenter getNotificationCenter() {
        return messagesController.getNotificationCenter();
    }

    private MessagesStorage getMessagesStorage() {
        return messagesController.getMessagesStorage();
    }

    private void updateAvailableReactions() {
        TLRPC.TL_messages_getAvailableReactions req = new TLRPC.TL_messages_getAvailableReactions();
        req.hash = hash; // TODO 27/11/2021 Fuji team, RIDER-: disableFree and  networkType?

        if (availableReactionsId != 0) { // TODO 28/11/2021 Fuji team, RIDER-: check this logic
            getConnectionsManager().cancelRequest(availableReactionsId, true); // TODO 28/11/2021 Fuji team, RIDER-: check param notifyServer
        }
        availableReactionsId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.messages_AvailableReactions availableReactions = (TLRPC.messages_AvailableReactions) response;
                if (availableReactions instanceof TLRPC.TL_messages_availableReactionsNotModified && cachedAvailableReactions != null) {
                    getNotificationCenter().postNotificationName(NotificationCenter.reactionsLoaded, cachedAvailableReactions); // TODO 03/12/2021 Fuji team, RIDER-: add dynamic updates
                } else if (availableReactions instanceof TLRPC.TL_messages_availableReactions) {
                    cachedAvailableReactions = (TLRPC.TL_messages_availableReactions) availableReactions;
                    hash = cachedAvailableReactions.hash;
                    getNotificationCenter().postNotificationName(NotificationCenter.reactionsLoaded, cachedAvailableReactions);
                } else {
                    // TODO 27/11/2021 Fuji team, RIDER-: error
                }
            } else {
                // TODO 27/11/2021 Fuji team, RIDER-: error
            }
        }));
    }

    public void disableChatReactions(TLRPC.InputPeer inputPeer) {
        TLRPC.TL_messages_setChatAvailableReactions req = new TLRPC.TL_messages_setChatAvailableReactions();
        req.peer = inputPeer;
        final ArrayList<String> available_reactions = new ArrayList<>();
        req.available_reactions = available_reactions;
        long did = inputPeer.chat_id; // TODO 28/11/2021 Fuji team, RIDER-: make correct id

        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                final TLRPC.ChatFull chatFull = messagesController.getChatFull(-did);
                if (chatFull != null) {
                    chatFull.available_reactions = available_reactions;
                    getMessagesStorage().updateChatInfo(chatFull, false);
                    cachedAvailableReactions = null;
                }
            }
        }));
    }

    public void setChatAvailableReactions(TLRPC.InputPeer inputPeer, ArrayList<String> availableReactions) { // TODO 28/11/2021 Fuji team, RIDER-: check network performance
        TLRPC.TL_messages_setChatAvailableReactions req = new TLRPC.TL_messages_setChatAvailableReactions();
        req.peer = inputPeer;
        req.available_reactions = availableReactions;
        long did = DialogObject.getPeerDialogId(inputPeer); // TODO 28/11/2021 Fuji team, RIDER-: make correct id

        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                final TLRPC.ChatFull chatFull = messagesController.getChatFull(-did);
                if (chatFull != null) {
                    chatFull.available_reactions = availableReactions;
                    getMessagesStorage().updateChatInfo(chatFull, false);
                    getNotificationCenter().postNotificationName(NotificationCenter.chatInfoDidLoad, chatFull, 0, true, false);
                    if (cachedAvailableReactions != null) {
//                        ArrayList<TLRPC.TL_availableReaction> reactions = new ArrayList<>();
//                        for (TLRPC.TL_availableReaction cachedReaction : cachedAvailableReactions.reactions) {
//                            if (!availableReactions.contains(cachedReaction.reaction)) {
//                                reactions.add(cachedReaction);
//                            }
//                        }
//                        cachedAvailableReactions.hash = 0;
//                        cachedAvailableReactions.reactions.removeAll(reactions);
//                        getNotificationCenter().postNotificationName(NotificationCenter.reactionsLoaded, cachedAvailableReactions);
//                        getAvailableReactions(); // TODO 28/11/2021 Fuji team, RIDER-: check whether it redundant or no
                    }
                }
            } else if (error.code == 400 && error.text.contains("REACTION_INVALID")) { // TODO 28/11/2021 Fuji team, RIDER-: check error
                updateAvailableReactions();
            }
        }));
    }

    public void sendReaction(MessageObject messageObject, String reaction) {
        TLRPC.TL_messages_sendReaction req = new TLRPC.TL_messages_sendReaction();
        req.peer = messagesController.getInputPeer(messageObject.getDialogId());
        req.msg_id = messageObject.getId();
        // TODO 28/11/2021 Fuji team, RIDER-: check message service && postponed && sponsored
//        if (isFirstReaction) { // TODO 28/11/2021 Fuji team, RIDER-: add check
//            req.reaction = reaction;
//        } else {
        final TLRPC.Message messageOwner = messageObject.messageOwner;
        boolean isAllowedToSend = false;

        for (TLRPC.TL_availableReaction availableReaction : getCachedAvailableReactions(messagesController.getChatFull(messageObject.getChatId()))) { // TODO 01/12/2021 Fuji team, RIDER-check: id
            if (availableReaction.reaction.equals(reaction)) {
                isAllowedToSend = true;
            }
        }
        if (!isAllowedToSend) {
            if (messageOwner != null) {
                for (TLRPC.TL_reactionCount reactionCount : messageOwner.reactions.results) {
                    if (reactionCount.reaction.equals(reaction)) {
                        isAllowedToSend = true;
                    }
                }
            }
        }

        if (isAllowedToSend) {
            req.reaction = reaction;
            req.flags |= 1;
        } else {
            return; // TODO 01/12/2021 Fuji team, RIDER-: add notification
        }

        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> { // TODO 28/11/2021 Fuji team, RIDER-: search different signatures
            if (error == null) {
                messagesController.processUpdates(((TLRPC.Updates) response), false); // TODO 28/11/2021 Fuji team, RIDER-: check second param
            }
        }));
    }
    public void deleteReaction(MessageObject messageObject) {
        TLRPC.TL_messages_sendReaction req = new TLRPC.TL_messages_sendReaction();
        req.peer = messagesController.getInputPeer(messageObject.getDialogId());
        req.msg_id = messageObject.getId();

        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> { // TODO 28/11/2021 Fuji team, RIDER-: search different signatures
            if (error == null) {
                messagesController.processUpdates(((TLRPC.Updates) response), false); // TODO 28/11/2021 Fuji team, RIDER-: check second param
            }
        }));
    }

    public void handleMessageReactionsUpdate(TLRPC.TL_updateMessageReactions update) {
        // TODO 28/11/2021 Fuji team, RIDER-: check this place
        handleMessageReactionsUpdate(update.reactions, update.peer, update.msg_id);
    }

    public void handleMessageReactionsUpdate(TLRPC.TL_messageReactions reactions, TLRPC.Message message) {
        // TODO 28/11/2021 Fuji team, RIDER-: check this place
        handleMessageReactionsUpdate(reactions, message.peer_id, message.id);
    }

    private void handleMessageReactionsUpdate(TLRPC.TL_messageReactions reactions, TLRPC.Peer peer, int messageId) {
        long dialogId = MessageObject.getPeerId(peer);
        getMessagesStorage().updateMessageReactions(dialogId, messageId, reactions);
        AndroidUtilities.runOnUIThread(() -> {
            getNotificationCenter().postNotificationName(NotificationCenter.didUpdateReactions, dialogId, messageId, reactions);
        });
    }

    public void addToMessagePollingQueue(long dialogId, ArrayList<MessageObject> visibleObjects) {
        SparseArray<MessageObject> array = messagesToCheck.get(dialogId);
        if (array == null) {
            array = new SparseArray<>();
            messagesToCheck.put(dialogId, array);
            messagesToCheckSize++;
        }
        for (int a = 0, N = array.size(); a < N; a++) {
            MessageObject object = array.valueAt(a);
            object.isVisibleOnScreen = false;
        }
//        boolean hasChanges = false;
        for (int a = 0, N = visibleObjects.size(); a < N; a++) {
            MessageObject messageObject = visibleObjects.get(a);
            int id = messageObject.getId();
            MessageObject object = array.get(id);
            if (object != null) {
                object.isVisibleOnScreen = true;
            } else {
                array.put(id, messageObject);
            }
        }
//        if (hasChanges) {
        lastViewsCheckTime = 0;
//        }
    }

    public void loadAvailableReactions() {
        if (Math.abs(System.currentTimeMillis() - lastAvailableReactionsLoading) < UPDATE_PERIOD_MILLIS) {
            return;
        }
        lastAvailableReactionsLoading = System.currentTimeMillis();
        updateAvailableReactions();
    }

    public void loadMessagesReactions() {
//        int currentServerTime = connectionsManager.getCurrentTime();
        if (Math.abs(System.currentTimeMillis() - lastViewsCheckTime) < 15_000) {
            return;
        }
        lastViewsCheckTime = System.currentTimeMillis();
        if (messagesToCheckSize > 0) {
            AndroidUtilities.runOnUIThread(() -> {
                long time = SystemClock.elapsedRealtime();
                int minExpireTime = Integer.MAX_VALUE;
                for (int a = 0, N = messagesToCheck.size(); a < N; a++) {
                    SparseArray<MessageObject> array = messagesToCheck.valueAt(a);
                    if (array == null) {
                        continue;
                    }
                    for (int b = 0, N2 = array.size(); b < N2; b++) {
                        MessageObject messageObject = array.valueAt(b);
//                        boolean expired;
                        int timeout = 30_000;
                        if (Math.abs(time - messageObject.reactionsLastCheckTime) < timeout) { // Если прошло времени меньше чем таймаут(т.е. возможно еще загружается)
                            if (!messageObject.pollVisibleOnScreen) { // TODO 28/11/2021 Fuji team, RIDER-: do we need remove always when it notVisible?
                                array.remove(messageObject.getId());
                                N2--;
                                b--;
                            }
                        } else { // Если прошло времени с последний загрузки чем таймаут (значит не грузится сейчас)
                            messageObject.reactionsLastCheckTime = time;
                            loadMessagesReactions(messagesController.getInputPeer(messageObject.getDialogId()), messageObject.getId());
//                            TLRPC.TL_messages_getPollResults req = new TLRPC.TL_messages_getPollResults();
//                            req.peer = getInputPeer(messageObject.getDialogId());
//                            req.msg_id = messageObject.getId();
//                            getConnectionsManager().sendRequest(req, (response, error) -> {
//                                if (error == null) {
//                                    TLRPC.Updates updates = (TLRPC.Updates) response;
//                                    if (expired) {
//                                        for (int i = 0; i < updates.updates.size(); i++) {
//                                            TLRPC.Update update = updates.updates.get(i);
//                                            if (update instanceof TLRPC.TL_updateMessagePoll) {
//                                                TLRPC.TL_updateMessagePoll messagePoll = (TLRPC.TL_updateMessagePoll) update;
//                                                if (messagePoll.poll != null && !messagePoll.poll.closed) {
//                                                    lastViewsCheckTime = System.currentTimeMillis() - 4000;
//                                                }
//                                            }
//                                        }
//                                    }
//                                    processUpdates(updates, false);
//                                }
//                            });
                        }
                    }
//                    if (minExpireTime < 5) {
//                        lastViewsCheckTime = Math.min(lastViewsCheckTime, System.currentTimeMillis() - (5 - minExpireTime) * 1000);
//                    }
                    if (array.size() == 0) {
                        messagesToCheck.remove(messagesToCheck.keyAt(a));
                        N--;
                        a--;
                    }
                }
                messagesToCheckSize = messagesToCheck.size();
            });
        }
    }

    private void loadMessagesReactions(TLRPC.InputPeer inputPeer, int messageId) {
        TLRPC.TL_messages_getMessagesReactions req = new TLRPC.TL_messages_getMessagesReactions();
        req.peer = inputPeer;
        final ArrayList<Integer> ids = new ArrayList<>();
        ids.add(messageId);
        req.id = ids;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                messagesController.processUpdates(((TLRPC.Updates) response), false);
            }
        }));
    }

    public void loadMessageReactionsDetail(TLRPC.InputPeer inputPeer, int messageId, @Nullable String reaction, String offset) {
        if (reaction == null) {
            loadFirstMessageReactionsDetail(inputPeer, messageId, offset); // TODO 28/11/2021 Fuji team, RIDER-: change size
        } else {
            loadMessageReactionsDetailByFilter(inputPeer, messageId, reaction, offset);
        }
    }

    private void loadFirstMessageReactionsDetail(TLRPC.InputPeer inputPeer, int messageId, String offset) {
       if (offset == null) {
           filteredReactions.clear();
           totalReactions.clear();
           currentMessageReactions.clear();
           nonFilteredMessageUserReaction = null;
       }

        TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();
        req.peer = inputPeer;
        req.id = messageId;
        req.limit = 100;
        long dialogId = DialogObject.getPeerDialogId(inputPeer);

        if (offset != null) {
            req.offset = offset;
            req.flags |= 2;
        }

        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
//                if (filteredReactions == null) {
                HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions = new HashMap<>(); // TODO 28/11/2021 Fuji team, RIDER-: optimize capacity
//                }
                TLRPC.TL_messages_messageReactionsList lastMessageReactionsList = (TLRPC.TL_messages_messageReactionsList) response;
//                getNotificationCenter().postNotificationName(
//                        NotificationCenter.reactionsLoaded,
//                        messageId,
//                        lastMessageReactionsList
//                );

                for (TLRPC.TL_messageUserReaction reaction : lastMessageReactionsList.reactions) {
                    final String reactionTag = reaction.reaction;
                    ArrayList<TLRPC.TL_messageUserReaction> messageUserReactions = filteredReactions.get(reactionTag);
                    if (messageUserReactions != null) {
                        messageUserReactions.add(reaction);
                    } else {
                        messageUserReactions = new ArrayList<>();
                        messageUserReactions.add(reaction);
                        filteredReactions.put(reactionTag, messageUserReactions);
                    }
                }

//                for (Map.Entry<String, ArrayList<TLRPC.TL_messageUserReaction>> entry : filteredReactions.entrySet()) {
//                    getNotificationCenter().postNotificationName(
//                            NotificationCenter.reactionsByFilterLoaded,
//                            messageId,
//                            entry.getKey(),
//                            entry.getValue()
//                    );
//                }
//                for (Map.Entry<String, ArrayList<TLRPC.TL_messageUserReaction>> entry : filteredReactions.entrySet()) {
                setNonFilteredMessageUserReactions(filteredReactions, lastMessageReactionsList);
                getNotificationCenter().postNotificationName(
                            NotificationCenter.reactionDetailForMessageLoaded,
                            dialogId,
                            messageId,
                            filteredReactions,
                            lastMessageReactionsList
                    );
//                }
                if (offset == null) {
                    for (TLRPC.TL_availableReaction reaction : cachedAvailableReactions.reactions) {
                        loadMessageReactionsDetailByFilter(inputPeer, messageId, reaction.reaction, null);
                    }
                }
            }
        }));
    }

    private void loadMessageReactionsDetailByFilter(TLRPC.InputPeer inputPeer, int messageId, String reaction, String offset) {
        TLRPC.TL_messages_getMessageReactionsList req = new TLRPC.TL_messages_getMessageReactionsList();

//        final ArrayList<TLRPC.TL_messageUserReaction> cachedFilteredReactions = filteredReactions.get(reaction);
//        final ArrayList<TLRPC.TL_messageUserReaction> messageUserReactions;
////        if (cachedFilteredReactions == null) {
//            messageUserReactions = new ArrayList<>();
//            filteredReactions.put(reaction, messageUserReactions); // TODO 28/11/2021 Fuji team, RIDER-: check main thread
//        } else {
//            messageUserReactions = cachedFilteredReactions;
//        }
        req.peer = inputPeer;
        req.id = messageId;
        req.reaction = reaction;
        req.flags |= 1;
        req.limit = 50;
//        if (filteredReactionOffsets == null) {
//            filteredReactionOffsets = new HashMap<>(); // TODO 28/11/2021 Fuji team, RIDER-: check main thread
//        }
        long dialogId = DialogObject.getPeerDialogId(inputPeer);
        if (offset != null) {
            req.offset = offset;
            req.flags |= 2;
        }
        final int requestId = getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                queries.remove(reaction);
                TLRPC.TL_messages_messageReactionsList lastMessageReactionsListByFilter = (TLRPC.TL_messages_messageReactionsList) response;
//                if ((req.flags &= 2) == 0) { // TODO 28/11/2021 Fuji team, RIDER-: check correctness of this condition
//                    messageUserReactions.clear();
//                }
//                messageUserReactions.addAll(lastMessageReactionsListByFilter.reactions);
//                filteredReactionOffsets.put(reaction, lastMessageReactionsListByFilter.next_offset); // TODO 28/11/2021 Fuji team, RIDER-: check that it overwrites next offset
                if (offset == null) {
                    currentMessageReactions.remove(reaction);
                    totalReactions.remove(reaction);
                }
                setMessageUserReactions(lastMessageReactionsListByFilter, reaction);
                getNotificationCenter().postNotificationName(
                        NotificationCenter.reactionsByFilterLoaded,
                        dialogId,
                        messageId,
                        lastMessageReactionsListByFilter,
                        reaction
                );
            }
        }));
        queries.put(reaction, requestId);
    }

//    private HashMap<String, TLRPC.TL_messages_messageReactionsList> previousMessageReactions = new HashMap<>();
    private HashMap<String, TLRPC.TL_messages_messageReactionsList> currentMessageReactions = new HashMap<>();
    private TLRPC.TL_messages_messageReactionsList nonFilteredMessageUserReaction;
    public static String nonFilteredMessageKey = "general_key";
    private HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> totalReactions = new HashMap<>();

    private HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions = new HashMap<>();

    public void setMessageUserReactions(TLRPC.TL_messages_messageReactionsList messageUserReaction, String reaction) {
        ArrayList<TLRPC.TL_messageUserReaction> userReactions = totalReactions.get(reaction);
        if (userReactions == null) {
            userReactions = new ArrayList<>();
            totalReactions.put(reaction, userReactions);
        }
        userReactions.addAll(messageUserReaction.reactions);
//        previousMessageReactions = new HashMap<>(currentMessageReactions);
        currentMessageReactions.put(reaction, messageUserReaction);
    }

    public void setNonFilteredMessageUserReactions(
            HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions,
            TLRPC.TL_messages_messageReactionsList nonFilteredMessageUserReaction
    ) {
//        previousMessageReactions = new HashMap<>(currentMessageReactions);
        currentMessageReactions.put(nonFilteredMessageKey, nonFilteredMessageUserReaction);
        this.filteredReactions = filteredReactions;
    }

    public ArrayList<TLRPC.TL_messageUserReaction> getMessageUserReactions(String reaction) {
        final ArrayList<TLRPC.TL_messageUserReaction> totalUserReactions = totalReactions.get(reaction);
        if (reaction.equals(nonFilteredMessageKey) && currentMessageReactions.get(reaction) != null) {
            return currentMessageReactions.get(reaction).reactions;
        }
        if (totalUserReactions != null) {
            return totalUserReactions;
        } else {
            return filteredReactions.get(reaction);
        }
    }

    public int getStartIndexForUpdate(String reaction) {
        final ArrayList<TLRPC.TL_messageUserReaction> totalUserReactions = totalReactions.get(reaction);
        if (totalUserReactions != null) {
            return totalUserReactions.size() - currentMessageReactions.get(reaction).reactions.size();
        } else {
            return -1;
        }
    }

    public int getGeneralReactionsCount(String reaction) {
        if (reaction.equals(nonFilteredMessageKey) && currentMessageReactions.get(reaction) != null) {
            return currentMessageReactions.get(reaction).count;
        } else {
            return 0;
        }
    }

    public String getNextOffset(String reaction) {
        final TLRPC.TL_messages_messageReactionsList reactionsList = currentMessageReactions.get(reaction);
        if (reactionsList != null) {
            return reactionsList.next_offset;
        } else {
            return null;
        }
    }

    private void cancelMessageReactionsDetailLoading() {
        for (Map.Entry<String, Integer> entry : queries.entrySet()) {
            getConnectionsManager().getConnectionsManager().cancelRequest(entry.getValue(), true);
        }
    }

//    public void getReactionDetail() {
//        private HashMap<String, ArrayList<TLRPC.TL_messageUserReaction>> filteredReactions;
//        private HashMap<String, String> filteredReactionOffsets;
//    }

    public TLRPC.Document getSticker(String reaction) {
        TLRPC.TL_availableReaction foundAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : cachedAvailableReactions.reactions) {
            if (availableReaction.reaction.equals(reaction)) {
                foundAvailableReaction = availableReaction;
            }
        }
        if (foundAvailableReaction == null) {
            return null;
        }
        return foundAvailableReaction.select_animation;
    }

    public String getReactionTitle(String reaction) { // TODO 01/12/2021 Fuji team, RIDER-: remove
        TLRPC.TL_availableReaction foundAvailableReaction = null;
        for (TLRPC.TL_availableReaction availableReaction : cachedAvailableReactions.reactions) {
            if (availableReaction.reaction.equals(reaction)) {
                foundAvailableReaction = availableReaction;
            }
        }
        if (foundAvailableReaction == null) {
            return null;
        }
        return foundAvailableReaction.title;
    }

//    public HashMap<TLRPC.TL_availableReaction, Boolean> getAvailableReactionsForSettings(ArrayList<String> settingsReactions) {
//        HashMap<TLRPC.TL_availableReaction, Boolean> availableReactionsForSettings = new HashMap<>();
//        for (TLRPC.TL_availableReaction availableReaction : cachedAvailableReactions.reactions) {
//            availableReactionsForSettings.put(availableReaction, settingsReactions.contains(availableReaction.reaction));
//        }
//        return availableReactionsForSettings;
//    }

    public ArrayList<TLRPC.TL_availableReaction> getCachedAvailableReactions(TLRPC.ChatFull chatFull) {
        final ArrayList<TLRPC.TL_availableReaction> availableReactions = new ArrayList<>();
        if (chatFull != null) {
            for(TLRPC.TL_availableReaction availableReaction: cachedAvailableReactions.reactions) {
                if (chatFull.available_reactions.contains(availableReaction.reaction)) {
                    availableReactions.add(availableReaction);
                }
            }
        } else {
            availableReactions.addAll(cachedAvailableReactions.reactions);
        }
        return availableReactions;
    }

    public ArrayList<TLRPC.TL_availableReaction> getCachedAvailableReactions(MessageObject messageObject) {
        TLRPC.ChatFull chatFull = messagesController.getChatFull(messageObject.getChatId());
        return getCachedAvailableReactions(chatFull);
    }

    public HashMap<String, TLRPC.TL_availableReaction> getAvailableReactionsForSettings() {
        HashMap<String, TLRPC.TL_availableReaction> availableReactionsForSettings = new HashMap<>();
        for (TLRPC.TL_availableReaction availableReaction : cachedAvailableReactions.reactions) {
            availableReactionsForSettings.put(availableReaction.reaction, availableReaction);
        }
        return availableReactionsForSettings;
    }

    private static ArrayList<String> availableDoubleTapReaction = new ArrayList<>();
    static {
        availableDoubleTapReaction.add("Red Heart");
        availableDoubleTapReaction.add("Thumbs Up");
    }

    public String getAvailableFastReaction(MessageObject messageObject) {
        for (String availableDoubleTapReaction : availableDoubleTapReaction) {
            for (TLRPC.TL_availableReaction availableReaction : getCachedAvailableReactions(messageObject)) {
                if (availableReaction.title.equals(availableDoubleTapReaction)) {
                    return availableReaction.reaction;
                }
            }
        }
        return null;
    }

    public void clear() {
        messagesToCheck.clear();
        messagesToCheckSize = 0;
        lastViewsCheckTime = 0; // TODO 28/11/2021 Fuji team, RIDER-: check whether it necessary
        lastAvailableReactionsLoading = 0; // TODO 28/11/2021 Fuji team, RIDER-: check whether it necessary
        filteredReactions.clear();
        totalReactions.clear();
        currentMessageReactions.clear();
        nonFilteredMessageUserReaction = null;
    }
}
