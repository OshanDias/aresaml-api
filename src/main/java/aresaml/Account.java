package aresaml;
 
/**
 * Represents a Bank Account (a "Node" in our Graph).
 * We use private fields and public getters for encapsulation.
 */
public class Account {
    private String accountNumber;
    private String ownerName;
 
    public Account(String accountNumber, String ownerName) {
        this.accountNumber = accountNumber;
        this.ownerName = ownerName;
    }
 
    public String getAccountNumber() { return accountNumber; }
    public String getOwnerName() { return ownerName; }
 
    @Override
    public String toString() {
        return ownerName + " (" + accountNumber + ")";
    }
 
    // Needed for Graphs: two accounts are equal if they have the same account number
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return accountNumber.equals(account.accountNumber);
    }
 
    @Override
    public int hashCode() {
        return accountNumber.hashCode();
    }
}
