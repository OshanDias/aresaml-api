# AresAML API

A Spring Boot REST API that exposes the AresAML anti-money-laundering engine over HTTP.
It reuses the exact detection engine from the original AresAML project (directed-graph
cycle detection, smurfing/structuring detection, and transparent 0–100 risk scoring) and
serves the results as JSON so any frontend can consume them.

## Architecture

```
HTTP request
   -> AmlController   (thin Spring REST layer, no logic)
   -> AmlService      (plain Java: builds the graph, calls the engine)
   -> AnalysisEngine  (the reusable AML engine)
   -> AnalysisResult  (alerts + scored, explained risk profiles)  --> JSON
```

The engine classes (`AMLGraph`, `AnalysisEngine`, `Alert`, `RiskProfile`, …) are the
same ones from the desktop AresAML project, copied in unchanged. All web-specific code
lives in the `aresaml.web` package.

## Requirements

- JDK 17+
- Maven (bundled with NetBeans/IntelliJ, or standalone)

## Run

```bash
mvn spring-boot:run
```

Then the API is available at `http://localhost:8080`.

## Endpoints

| Method | Path                    | Description                                   |
|--------|-------------------------|-----------------------------------------------|
| GET    | `/api/health`           | Liveness check                                |
| GET    | `/api/analysis/sample`  | Analyse a built-in demo scenario              |
| GET    | `/api/analysis/demo`    | Analyse the bundled `sample-transactions.csv` |
| POST   | `/api/analysis/upload`  | Analyse an uploaded CSV (form field `file`)   |

### Example

```bash
curl http://localhost:8080/api/analysis/demo
curl -F "file=@transactions.csv" http://localhost:8080/api/analysis/upload
```

## CSV format

```
transactionId,senderId,senderName,receiverId,receiverName,amount,timestamp
TX001,ACC010,Nimal Perera,ACC011,Kasun Silva,1200.00,2026-08-01T09:00:00
```
`timestamp` uses `yyyy-MM-dd'T'HH:mm:ss`.
