package aresaml;

import java.util.ArrayList;
import java.util.List;

/**
 * The risk picture for a single account: its total 0-100 score PLUS the list of
 * human-readable reasons that produced that score.
 *
 * This is the key upgrade over the old design, where risk was just an Integer in
 * a Map. Now an analyst (or an interviewer) can ask "why is this account a 75?"
 * and the tool answers in plain English - the scoring is transparent, not a black box.
 */
public class RiskProfile {

    private final Account account;
    private int score;
    private final List<String> reasons = new ArrayList<>();

    public RiskProfile(Account account) {
        this.account = account;
    }

    /** Adds points for a triggered rule and records the plain-English reason. */
    void addReason(int points, String reason) {
        this.score += points;
        this.reasons.add(reason);
    }

    /** Clamps the score to the 0-100 range. Called by the engine once scoring is done. */
    void capScore() {
        this.score = Math.min(this.score, 100);
    }

    public Account getAccount() { return account; }
    public int getScore() { return score; }
    public List<String> getReasons() { return reasons; }
    public boolean isFlagged() { return score > 0; }

    /** Simple band an analyst can sort/colour by. */
    public String getRiskBand() {
        if (score >= 70) return "HIGH";
        if (score >= 35) return "MEDIUM";
        if (score > 0)   return "LOW";
        return "CLEAR";
    }

    @Override
    public String toString() {
        return String.format("%-28s %3d/100  [%s]", account.getOwnerName(), score, getRiskBand());
    }
}
