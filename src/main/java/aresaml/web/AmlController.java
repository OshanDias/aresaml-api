package aresaml.web;

import aresaml.AnalysisResult;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * The REST layer. Deliberately thin: it does no analysis itself, it just calls
 * AmlService and lets Spring turn the returned objects into JSON.
 *
 * Endpoints:
 *   GET  /api/health              -> quick liveness check
 *   GET  /api/analysis/sample     -> analyse the built-in demo scenario
 *   GET  /api/analysis/demo       -> analyse the bundled sample-transactions.csv
 *   POST /api/analysis/upload     -> analyse an uploaded CSV file (form field "file")
 */
@RestController
@RequestMapping("/api")
public class AmlController {

    private final AmlService service = new AmlService();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "AresAML API");
    }

    @GetMapping("/analysis/sample")
    public AnalysisResult sample() {
        return service.analyzeSampleScenario();
    }

    @GetMapping("/analysis/demo")
    public ResponseEntity<?> demo() {
        try {
            // Read the bundled CSV from the classpath, then hand its text to the service.
            var in = getClass().getClassLoader().getResourceAsStream("sample-transactions.csv");
            if (in == null) {
                return ResponseEntity.status(500).body(Map.of("error", "sample-transactions.csv not found on classpath"));
            }
            String csv = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(service.analyzeCsvContent(csv));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/analysis/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file uploaded"));
        }
        try {
            String csv = new String(file.getBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.ok(service.analyzeCsvContent(csv));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
