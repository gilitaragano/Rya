package nxt;

import nxt.crypto.EncryptedData;
import nxt.util.Listener;

import java.util.*;

//TODO: convert to database
public final class CoinShuffler {

    private static enum State {
        INITIATED,
        CONTINUED,
        FINALIZED,
        CANCELLED
    }

    static {
        Nxt.getBlockchainProcessor().addListener(new Listener<Block>() {
            @Override
            public void notify(Block block) {
                for (Shuffling shuffling : Shuffling.getAllShufflings(0, -1)) {
                    // Cancel the shuffling in case the blockchain reached its cancellation height
                    if (block.getHeight() > shuffling.getCancellationHeight()) {
                        shuffling.cancel();
                    }
                }
            }
        }, BlockchainProcessor.Event.AFTER_BLOCK_APPLY);
    }

    private static final Map<Long, Shuffling> shufflings = new HashMap<>();

    public static boolean isInitiated(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.INITIATED;
    }

    public static boolean isContinued(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CONTINUED;
    }

    public static boolean isFinalized(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.FINALIZED;
    }

    public static boolean isCancelled(Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.state == State.CANCELLED;
    }

    public static byte getNumberOfParticipants(Long shufflingId) {
        return shufflings.get(shufflingId).numberOfParticipants;
    }

    public static boolean isParticipant(Long accountId, Long shufflingId) {
        Shuffling shuffling = shufflings.get(shufflingId);
        return shuffling != null && shuffling.participants.contains(accountId);
    }

    public static boolean sentEncryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).encryptedRecipients.get(accountId) != null;
    }

    public static boolean sentDecryptedRecipients(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).decryptedRecipients.get(accountId) != null;
    }

    public static boolean sentKeys(Long accountId, Long shufflingId) {
        return shufflings.get(shufflingId).keys.get(accountId) != null;
    }

    public static void createShuffling(Long transactionId, Account account, Long currencyId, long amount, byte numberOfParticipants, short maxInitiationDelay, short maxContinuationDelay, short maxFinalizationDelay, short maxCancellationDelay) {
        account.addToCurrencyUnits(currencyId, -amount);

        Shuffling newShuffling = new Shuffling(currencyId, amount, numberOfParticipants, maxInitiationDelay, maxContinuationDelay, maxFinalizationDelay, maxCancellationDelay);

        for (Shuffling existingShuffling : shufflings.values()) {
            if (existingShuffling.state == State.INITIATED && newShuffling.equals(existingShuffling)) {
                existingShuffling.addParticipant(account.getId());
                return;
            }
        }

        shufflings.put(transactionId, newShuffling);
    }

    public static void continueShuffling(Account account, Long shufflingId, EncryptedData recipients) {
        Shuffling shuffling = shufflings.get(shufflingId);
        shuffling.encryptedRecipients.put(account.getId(), recipients);
        if (shuffling.encryptedRecipients.size() == shuffling.numberOfParticipants) {
            shuffling.state = State.FINALIZED;
        }
        shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
    }

    public static void finalizeShuffling(Account account, Long shufflingId, long[] recipients) {
        Shuffling shuffling = shufflings.get(shufflingId);
        if (shuffling.decryptedRecipients.size() > 0 && !Arrays.equals(recipients, shuffling.decryptedRecipients.values().toArray(new long[0][0])[0])) {
            shuffling.state = State.CANCELLED;
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
            return;
        }
        shuffling.decryptedRecipients.put(account.getId(), recipients);
        if (shuffling.decryptedRecipients.size() == shuffling.numberOfParticipants) {
            for (Long recipientAccountId : shuffling.decryptedRecipients.values().toArray(new Long[0][0])[0]) {
                Account.getAccount(recipientAccountId).addToCurrencyAndUnconfirmedCurrencyUnits(shuffling.currencyId, shuffling.amount);
            }
            shufflings.remove(shufflingId);
        } else {
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }
    }

    public static void cancelShuffling(Account account, Long shufflingId, byte[] keys) {
        Shuffling shuffling = shufflings.get(shufflingId);
        shuffling.keys.put(account.getId(), keys);
        if (shuffling.keys.size() == shuffling.numberOfParticipants) {
            // TODO: Decrypt and analyze data to find rogues
        } else {
            shuffling.state = State.CANCELLED;
            shuffling.lastActionTimestamp = BlockchainImpl.getInstance().getLastBlock().getTimestamp();
        }
    }

}
