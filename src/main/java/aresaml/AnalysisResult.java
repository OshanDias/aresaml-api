package aresaml;

import java.util.List;

/**
 * The complete output of one analysis run - everything a UI needs in a single object.
 *
 * Before: AresAML.main() called detectCycles(), detectSmurfing() and
 * calculateRiskScores() itself, then formatted each one inline. Any other UI had
 * to repeat all of that. Now the engine returns ONE AnalysisResult and the UI
 * just reads these lists. This is what makes the engine reusable across a desktop
 * app, a web API, or a test.
 */
public class AnalysisResult {

    private final List<Alert> alerts;
    private final List<RiskProfile> riskProfiles; // sorted highest-risk first

    public AnalysisResult(List<Alert> alerts, List<RiskProfile> riskProfiles) {
        this.alerts = alerts;
        this.riskProfiles = riskProfiles;
    }

    /** Every finding, across all detection types. */
    public List<Alert> getAlerts() { return alerts; }

    /** Risk profile for every account, already sorted from most to least risky. */
    public List<RiskProfile> getRiskProfiles() { return riskProfiles; }

    /** Only the accounts that scored above the given threshold - the ones worth reviewing. */
    public List<RiskProfile> getProfilesAbove(int threshold) {
        return riskProfiles.stream()
                .filter(p -> p.getScore() >= threshold)
                .toList();
    }

    public long countAlertsOfType(Alert.Type type) {
        return alerts.stream().filter(a -> a.getType() == type).count();
    }

    public boolean hasFindings() {
        return !alerts.isEmpty();
    }
}
