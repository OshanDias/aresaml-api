package aresaml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single entry point for running AML analysis.
 *
 * Any UI - the Swing dashboard, the web API, or a test - builds an AMLGraph and
 * calls analyze(graph). It gets back one AnalysisResult containing every alert
 * and a scored, sorted risk profile for each account.
 *
 * SCORING MODEL (intentionally transparent - every point is explainable):
 *
 *   Laundering rings, weighted by how tight the loop is:
 *     - tight ring  (<= 3 accounts)  +60   a short A->B->C->A loop is a classic
 *                                           laundering signal
 *     - medium ring (4-5 accounts)   +40
 *     - loose ring  (6+ accounts)    +25   a long chain that happens to close
 *                                           could be coincidental, so it scores less
 *
 *   Smurfing / structuring:          +45 base, +5 per small transfer beyond the
 *                                     third, capped at +60 - more fragments = more
 *                                     deliberate evasion
 *
 *   Large single transfer:           +15 if >= 5,000, +25 if >= 10,000
 *
 *   Total is capped at 100. An account combining a tight ring with a large
 *   transfer therefore crosses into the HIGH band (>= 70).
 */
public class AnalysisEngine {

    // --- Detection thresholds (unchanged detection behaviour) ---
    public static final double SMURF_PER_TX_LIMIT    = 5000.00;
    public static final double SMURF_SUSPICIOUS_TOTAL = 9000.00;
    public static final int    SMURF_WINDOW_MINUTES   = 60;
    public static final double LARGE_TX_THRESHOLD      = 5000.00;
    public static final double VERY_LARGE_TX_THRESHOLD = 10000.00;

    public AnalysisResult analyze(AMLGraph graph) {
        List<Alert> alerts = new ArrayList<>();

        Map<Account, RiskProfile> profiles = new LinkedHashMap<>();
        for (Account account : graph.getAllAccounts()) {
            profiles.put(account, new RiskProfile(account));
        }

        // ---- Rule 1: circular laundering rings (weighted by tightness) ----
        List<List<Account>> cycles = graph.detectCycles();
        for (List<Account> ring : cycles) {
            int points = ringPoints(ring.size());
            String tightness = ringTightness(ring.size());
            String path = describeRing(ring);
            alerts.add(new Alert(Alert.Type.CYCLE, new ArrayList<>(ring), points,
                    tightness + " laundering ring (" + ring.size() + " accounts): " + path));
            for (Account acc : ring) {
                profiles.get(acc).addReason(points, tightness + " laundering ring: " + path);
            }
        }

        // ---- Rule 2: smurfing / structuring (scaled by number of fragments) ----
        Map<Account, List<Transaction>> smurfing =
                graph.detectSmurfing(SMURF_PER_TX_LIMIT, SMURF_SUSPICIOUS_TOTAL, SMURF_WINDOW_MINUTES);
        for (Map.Entry<Account, List<Transaction>> entry : smurfing.entrySet()) {
            Account acc = entry.getKey();
            List<Transaction> window = entry.getValue();
            int points = smurfPoints(window.size());
            double total = window.stream().mapToDouble(Transaction::getAmount).sum();
            String detail = String.format("Split $%,.2f across %d small transfers within %d minutes",
                    total, window.size(), SMURF_WINDOW_MINUTES);
            alerts.add(new Alert(Alert.Type.SMURFING, List.of(acc), points,
                    acc.getOwnerName() + ": " + detail));
            profiles.get(acc).addReason(points, detail);
        }

        // ---- Rule 3: large single transactions (tiered by size) ----
        for (Account account : graph.getAllAccounts()) {
            Transaction biggest = null;
            for (Transaction tx : graph.getOutgoingTransactions(account)) {
                if (biggest == null || tx.getAmount() > biggest.getAmount()) {
                    biggest = tx;
                }
            }
            if (biggest != null && biggest.getAmount() >= LARGE_TX_THRESHOLD) {
                boolean veryLarge = biggest.getAmount() >= VERY_LARGE_TX_THRESHOLD;
                int points = veryLarge ? 25 : 15;
                String size = veryLarge ? "very large" : "large";
                String detail = String.format("Sent a %s transaction of $%,.2f (%s)",
                        size, biggest.getAmount(), biggest.getTransactionId());
                alerts.add(new Alert(Alert.Type.LARGE_TRANSACTION, List.of(account), points,
                        account.getOwnerName() + ": " + detail));
                profiles.get(account).addReason(points, detail);
            }
        }

        // ---- Finalise: cap at 100 and sort most-risky-first ----
        List<RiskProfile> sorted = new ArrayList<>(profiles.values());
        for (RiskProfile p : sorted) {
            p.capScore();
        }
        sorted.sort(Comparator.comparingInt(RiskProfile::getScore).reversed());

        return new AnalysisResult(alerts, sorted);
    }

    /** Shorter rings are more suspicious, so they score higher. */
    private int ringPoints(int length) {
        if (length <= 3) return 60;
        if (length <= 5) return 40;
        return 25;
    }

    private String ringTightness(int length) {
        if (length <= 3) return "Tight";
        if (length <= 5) return "Medium";
        return "Loose";
    }

    /** More fragments = more deliberate structuring. */
    private int smurfPoints(int fragments) {
        int extra = Math.max(0, fragments - 3) * 5;
        return Math.min(45 + extra, 60);
    }

    /** Renders a ring as "Alice -> Bob -> Charlie -> Alice" for readable alerts. */
    private String describeRing(List<Account> ring) {
        StringBuilder sb = new StringBuilder();
        for (Account acc : ring) {
            sb.append(acc.getOwnerName()).append(" -> ");
        }
        sb.append(ring.get(0).getOwnerName());
        return sb.toString();
    }
}