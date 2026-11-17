package aresaml.web;

import aresaml.AMLGraph;
import aresaml.Account;
import aresaml.AnalysisEngine;
import aresaml.AnalysisResult;
import aresaml.CSVTransactionLoader;
import aresaml.Transaction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * All the real work behind the web API lives here as plain Java - NO Spring
 * annotations. That is deliberate: this class can be unit-tested with nothing
 * but javac, and the Spring controller stays a thin wrapper around it.
 *
 * Every method builds an AMLGraph, hands it to the AnalysisEngine, and returns
 * the AnalysisResult. The controller just turns that result into JSON.
 */
public class AmlService {

    private final AnalysisEngine engine = new AnalysisEngine();

    /** Analyses a built-in demo scenario (no file needed) - handy for a quick smoke test. */
    public AnalysisResult analyzeSampleScenario() {
        AMLGraph graph = new AMLGraph();

        Account alice   = new Account("ACC001", "Alice");
        Account bob     = new Account("ACC002", "Bob");
        Account charlie = new Account("ACC003", "Charlie");
        Account hacker  = new Account("ACC666", "The Money Launderer");
        Account smurfer = new Account("ACC777", "Suspicious Smurfer");
        Account mule    = new Account("ACC888", "Money Mule");

        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 9, 0);

        graph.addTransaction(new Transaction("TX101", alice, bob, 5000.00, now));
        graph.addTransaction(new Transaction("TX102", bob, charlie, 4800.00, now.plusMinutes(5)));
        graph.addTransaction(new Transaction("TX103", charlie, alice, 4500.00, now.plusMinutes(10)));
        graph.addTransaction(new Transaction("TX104", hacker, bob, 10000.00, now));
        graph.addTransaction(new Transaction("TX201", smurfer, mule, 2500.00, now));
        graph.addTransaction(new Transaction("TX202", smurfer, mule, 2400.00, now.plusMinutes(10)));
        graph.addTransaction(new Transaction("TX203", smurfer, mule, 2300.00, now.plusMinutes(20)));
        graph.addTransaction(new Transaction("TX204", smurfer, mule, 2200.00, now.plusMinutes(30)));
        graph.addTransaction(new Transaction("TX205", smurfer, mule, 2100.00, now.plusMinutes(40)));

        return engine.analyze(graph);
    }

    /** Analyses a CSV file already on disk (used for the bundled demo file). */
    public AnalysisResult analyzeCsvFile(String path) throws IOException {
        AMLGraph graph = new AMLGraph();
        CSVTransactionLoader.loadFromCSV(path, graph);
        return engine.analyze(graph);
    }

    /**
     * Analyses raw CSV text (e.g. an uploaded file's contents). The existing
     * CSVTransactionLoader reads from a file path, so we write the text to a
     * temporary file, load it, then delete it - the loader stays untouched.
     */
    public AnalysisResult analyzeCsvContent(String csvContent) throws IOException {
        Path temp = Files.createTempFile("ares-upload-", ".csv");
        try {
            Files.writeString(temp, csvContent);
            AMLGraph graph = new AMLGraph();
            CSVTransactionLoader.loadFromCSV(temp.toString(), graph);
            return engine.analyze(graph);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
