package aresaml;

import java.time.Duration;
import java.util.*;

/**
 * The "Brain" of AresAML.
 * Manages the Directed Graph (accounts = nodes, transactions = edges) and
 * runs the detection algorithms:
 *   1. Cycle Detection        - existing feature, finds circular money flows (A->B->C->A)
 *   2. Smurfing Detection     - NOVEL FEATURE 1, finds structured/fragmented transactions
 *   3. Risk Score Engine      - NOVEL FEATURE 2, turns raw findings into a single ranked score
 */
public class AMLGraph {

    private Map<Account, List<Transaction>> adjacencyList;

    public AMLGraph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addAccount(Account account) {
        adjacencyList.putIfAbsent(account, new ArrayList<>());
    }

    public void addTransaction(Transaction transaction) {
        Account sender = transaction.getSender();
        addAccount(sender);
        addAccount(transaction.getReceiver());
        adjacencyList.get(sender).add(transaction);
    }

    public Set<Account> getAllAccounts() {
        return adjacencyList.keySet();
    }

    public List<Transaction> getOutgoingTransactions(Account account) {
        return adjacencyList.getOrDefault(account, new ArrayList<>());
    }

    // ================= FEATURE: CYCLE DETECTION (original) =================

    /**
     * Finds circular chains of transactions using Depth-First Search (DFS).
     * Example: Alice -> Bob -> Charlie -> Alice is a laundering "ring".
     */
    public List<List<Account>> detectCycles() {
        List<List<Account>> allCycles = new ArrayList<>();
        Set<Account> visited = new HashSet<>();
        Set<Account> currentlyExploring = new HashSet<>();

        for (Account account : adjacencyList.keySet()) {
            findCyclesDFS(account, visited, currentlyExploring, new ArrayList<>(), allCycles);
        }
        return allCycles;
    }

    private void findCyclesDFS(Account current, Set<Account> visited, Set<Account> currentlyExploring,
                                List<Account> path, List<List<Account>> allCycles) {

        if (currentlyExploring.contains(current)) {
            int startIndex = path.indexOf(current);
            allCycles.add(new ArrayList<>(path.subList(startIndex, path.size())));
            return;
        }
        if (visited.contains(current)) return;

        visited.add(current);
        currentlyExploring.add(current);
        path.add(current);

        for (Transaction tx : getOutgoingTransactions(current)) {
            findCyclesDFS(tx.getReceiver(), visited, currentlyExploring, path, allCycles);
        }

        path.remove(path.size() - 1);
        currentlyExploring.remove(current);
    }

    // ================= NOVEL FEATURE 1: SMURFING / STRUCTURING DETECTION =================

    /**
     * "Smurfing" (a.k.a. structuring) is when someone breaks a large sum into many
     * small transactions to stay under a reporting threshold, but the small amounts
     * still add up to a suspicious total within a short time window.
     *
     * Example: instead of sending $12,000 once, they send $2,500 five times in one hour.
     *
     * @param perTransactionLimit amounts below this are considered "small" e.g. 5000.00
     * @param suspiciousTotal     if small transactions in the window add up to at least this, flag it e.g. 9000.00
     * @param windowMinutes       how close together (in minutes) the transactions must be e.g. 60
     * @return map of Account -> the group of transactions that triggered the flag
     */
    public Map<Account, List<Transaction>> detectSmurfing(double perTransactionLimit,
                                                            double suspiciousTotal,
                                                            int windowMinutes) {
        Map<Account, List<Transaction>> flagged = new LinkedHashMap<>();

        for (Account account : adjacencyList.keySet()) {
            List<Transaction> outgoing = new ArrayList<>(getOutgoingTransactions(account));
            // Only look at the "small" transactions - the ones designed to slip under the radar
            outgoing.removeIf(tx -> tx.getAmount() >= perTransactionLimit);
            outgoing.sort(Comparator.comparing(Transaction::getTimestamp));

            // Sliding window: for each transaction, look forward and sum everything
            // that falls within windowMinutes of it.
            for (int i = 0; i < outgoing.size(); i++) {
                List<Transaction> window = new ArrayList<>();
                double sum = 0;
                for (int j = i; j < outgoing.size(); j++) {
                    long minutesApart = Duration.between(outgoing.get(i).getTimestamp(),
                                                          outgoing.get(j).getTimestamp()).toMinutes();
                    if (minutesApart <= windowMinutes) {
                        window.add(outgoing.get(j));
                        sum += outgoing.get(j).getAmount();
                    } else {
                        break;
                    }
                }
                // Need at least 3 small transactions AND the total must look suspicious
                if (window.size() >= 3 && sum >= suspiciousTotal) {
                    flagged.put(account, window);
                    break; // one flag per account is enough for this tool
                }
            }
        }
        return flagged;
    }

    // ================= NOVEL FEATURE 2: RISK SCORE ENGINE =================

    /**
     * Combines cycle detection + smurfing detection results into one simple
     * 0-100 risk score per account, so an analyst can sort accounts by danger
     * level instead of reading raw graph output.
     *
     * This is intentionally a simple, transparent additive model (not a black box),
     * which makes it easy to explain and defend in the viva:
     *   +40 points  for each laundering ring (cycle) the account appears in
     *   +35 points  if the account shows a smurfing/structuring pattern
     *   +10 points  if any single transaction is unusually large ( >= 5000 )
     *   capped at 100
     */
    public Map<Account, Integer> calculateRiskScores(List<List<Account>> cycles,
                                                       Map<Account, List<Transaction>> smurfingFlags) {
        Map<Account, Integer> scores = new HashMap<>();
        for (Account account : adjacencyList.keySet()) {
            scores.put(account, 0);
        }

        for (List<Account> ring : cycles) {
            for (Account acc : ring) {
                scores.merge(acc, 40, Integer::sum);
            }
        }

        for (Account acc : smurfingFlags.keySet()) {
            scores.merge(acc, 35, Integer::sum);
        }

        for (Account account : adjacencyList.keySet()) {
            for (Transaction tx : getOutgoingTransactions(account)) {
                if (tx.getAmount() >= 5000) {
                    scores.merge(account, 10, Integer::sum);
                    break; // only add this once per account
                }
            }
        }

        // cap every score at 100
        scores.replaceAll((acc, score) -> Math.min(score, 100));
        return scores;
    }
}