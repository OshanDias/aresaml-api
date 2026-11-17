package aresaml;
 
import java.time.LocalDateTime;
 
/**
 * Represents money moving from one account to another (an "Edge" in our Graph).
 */
public class Transaction {
    private String transactionId;
    private Account sender;
    private Account receiver;
    private double amount;
    private LocalDateTime timestamp;
 
    // Original constructor: stamps the transaction with the current time
    public Transaction(String transactionId, Account sender, Account receiver, double amount) {
        this(transactionId, sender, receiver, amount, LocalDateTime.now());
    }
 
    // New overload: lets us set a specific timestamp.
    // Needed so demo/test data can simulate transactions happening minutes/hours
    // apart, which is required to test time-window based detection (smurfing).
    public Transaction(String transactionId, Account sender, Account receiver, double amount, LocalDateTime timestamp) {
        this.transactionId = transactionId;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.timestamp = timestamp;
    }
 
    public String getTransactionId() { return transactionId; }
    public Account getSender() { return sender; }
    public Account getReceiver() { return receiver; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
 
    @Override
    public String toString() {
        return String.format("[%s] %s -> %s: $%.2f", transactionId, sender.getOwnerName(), receiver.getOwnerName(), amount);
    }
}
 
