package aresaml;

import java.util.List;

/**
 * A single structured finding produced by the AnalysisEngine.
 *
 * Instead of printing detection results straight to the console (the way the
 * original demo did), the engine now emits a list of Alert objects. Any UI -
 * the Swing dashboard today, a web frontend tomorrow - just renders these.
 * The UI never has to know HOW an alert was found, only what it says.
 */
public class Alert {

    /** What kind of suspicious pattern triggered this alert. */
    public enum Type {
        CYCLE("Laundering Ring"),
        SMURFING("Structuring / Smurfing"),
        LARGE_TRANSACTION("Large Transaction");

        private final String label;
        Type(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final Type type;
    private final List<Account> involvedAccounts;
    private final int riskPoints;
    private final String description;

    public Alert(Type type, List<Account> involvedAccounts, int riskPoints, String description) {
        this.type = type;
        this.involvedAccounts = involvedAccounts;
        this.riskPoints = riskPoints;
        this.description = description;
    }

    public Type getType() { return type; }
    public List<Account> getInvolvedAccounts() { return involvedAccounts; }
    public int getRiskPoints() { return riskPoints; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "[" + type.getLabel() + "] " + description;
    }
}
