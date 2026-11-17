package aresaml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads a transaction log from a CSV file and feeds it into an existing AMLGraph.
 *
 * This class is purely additive: it does NOT modify AMLGraph, Account, Transaction,
 * or any detection algorithm. It only builds Account/Transaction objects from a file
 * and calls the graph's existing addTransaction() method - exactly the same way the
 * hardcoded demo data in AresAML.java does it.
 *
 * Expected CSV format (header row required):
 * transactionId,senderId,senderName,receiverId,receiverName,amount,timestamp
 * timestamp format: yyyy-MM-dd'T'HH:mm:ss   (e.g. 2026-08-01T09:00:00)
 */
public class CSVTransactionLoader {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Reads the given CSV file and adds every transaction it describes into the graph.
     * Returns the number of transactions successfully loaded.
     */
    public static int loadFromCSV(String filePath, AMLGraph graph) throws IOException {
        Map<String, Account> accountsById = new HashMap<>();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // skip header row
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] fields = line.split(",");
                if (fields.length < 7) continue; // skip malformed rows

                String transactionId = fields[0].trim();
                String senderId = fields[1].trim();
                String senderName = fields[2].trim();
                String receiverId = fields[3].trim();
                String receiverName = fields[4].trim();
                double amount = Double.parseDouble(fields[5].trim());
                LocalDateTime timestamp = LocalDateTime.parse(fields[6].trim(), FORMAT);

                Account sender = accountsById.computeIfAbsent(senderId, id -> new Account(id, senderName));
                Account receiver = accountsById.computeIfAbsent(receiverId, id -> new Account(id, receiverName));

                graph.addTransaction(new Transaction(transactionId, sender, receiver, amount, timestamp));
                count++;
            }
        }
        return count;
    }
}
