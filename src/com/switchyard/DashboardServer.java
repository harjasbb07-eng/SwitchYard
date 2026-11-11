package com.switchyard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.switchyard.fraud.FraudChainFactory;
import com.switchyard.gateway.GatewayFactory;
import com.switchyard.gateway.PaymentGateway;
import com.switchyard.model.AuditEntry;
import com.switchyard.model.Customer;
import com.switchyard.model.Merchant;
import com.switchyard.model.PaymentMethod;
import com.switchyard.model.Transaction;
import com.switchyard.routing.HighestSuccessRateStrategy;
import com.switchyard.routing.LowestCostStrategy;
import com.switchyard.service.PaymentOrchestrator;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.UUID;

public class DashboardServer {

    private static PaymentOrchestrator orchestrator;

    public static void main(String[] args) throws IOException {
        orchestrator = new PaymentOrchestrator(
                FraudChainFactory.createDefaultFraudChain(),
                new LowestCostStrategy(),
                GatewayFactory.createDefaultGateways()
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Static File Handlers
        server.createContext("/", exchange -> serveStaticFile(exchange, "web/index.html", "text/html"));
        server.createContext("/style.css", exchange -> serveStaticFile(exchange, "web/style.css", "text/css"));
        server.createContext("/app.js", exchange -> serveStaticFile(exchange, "web/app.js", "application/javascript"));

        // REST API Handlers
        server.createContext("/api/state", new StateHandler());
        server.createContext("/api/gateways/update", new GatewayUpdateHandler());
        server.createContext("/api/transactions/process", new ProcessTransactionHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Dashboard running at http://localhost:8080");
    }

    private static void serveStaticFile(HttpExchange exchange, String filePath, String contentType) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        } else {
            String response = "404 File Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    static class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("{");
            
            // Build Gateways JSON
            json.append("\"gateways\": [");
            var gateways = orchestrator.getGateways();
            for (int i = 0; i < gateways.size(); i++) {
                PaymentGateway g = gateways.get(i);
                json.append(String.format("{\"name\":\"%s\", \"fee\":%s, \"successRate\":%s, \"enabled\":%s}",
                        g.getGatewayName(), g.getFeePercentage(), g.getSuccessRate(), g.isEnabled()));
                if (i < gateways.size() - 1) json.append(",");
            }
            json.append("],");

            // Build Analytics JSON
            long total = orchestrator.getTransactionHistory().size();
            long settled = orchestrator.getTransactionHistory().stream().filter(t -> "SETTLED".equals(t.getCurrentState().getStateName())).count();
            long failed = orchestrator.getTransactionHistory().stream().filter(t -> "FAILED".equals(t.getCurrentState().getStateName())).count();
            
            json.append(String.format("\"analytics\": {\"total\":%d, \"settled\":%d, \"failed\":%d}", total, settled, failed));
            json.append("}");

            sendResponse(exchange, json.toString(), 200);
        }
    }

    static class GatewayUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                String name = extractJsonString(body, "name");
                double fee = extractJsonDouble(body, "fee");
                double successRate = extractJsonDouble(body, "successRate");
                boolean enabled = extractJsonBoolean(body, "enabled");

                orchestrator.updateGatewayConfig(name, fee, successRate, enabled);
                sendResponse(exchange, "{\"status\":\"success\"}", 200);
            }
        }
    }

    static class ProcessTransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                
                String customerId = extractJsonString(body, "customerId");
                String merchantName = extractJsonString(body, "merchantName");
                double amount = extractJsonDouble(body, "amount");
                PaymentMethod method = PaymentMethod.valueOf(extractJsonString(body, "paymentMethod"));
                String location = extractJsonString(body, "location");
                String strategy = extractJsonString(body, "strategy");

                if ("HIGHEST_SUCCESS_RATE".equals(strategy)) {
                    orchestrator.setRoutingStrategy(new HighestSuccessRateStrategy());
                } else {
                    orchestrator.setRoutingStrategy(new LowestCostStrategy());
                }

                Customer customer = new Customer(customerId, customerId + "_User", "user@test.com");
                Merchant merchant = new Merchant("M-100", merchantName);
                Transaction tx = new Transaction(UUID.randomUUID().toString().substring(0,8), amount, method, location, customer, merchant);

                orchestrator.executeTransaction(tx);

                // Build Transaction Result JSON
                StringBuilder json = new StringBuilder("{");
                json.append(String.format("\"txId\":\"%s\",", tx.getTxId()));
                json.append(String.format("\"status\":\"%s\",", tx.getCurrentState().getStateName()));
                json.append(String.format("\"selectedGateway\":\"%s\",", tx.getSelectedGateway()));
                json.append(String.format("\"appliedFee\":%s,", tx.getAppliedFee()));
                json.append(String.format("\"fraudResult\":\"%s\",", tx.getFraudResult()));
                json.append(String.format("\"executionTimeMs\":%d,", tx.getExecutionTimeMs()));
                
                json.append("\"auditTrail\": [");
                for (int i = 0; i < tx.getAuditTrail().size(); i++) {
                    AuditEntry entry = tx.getAuditTrail().get(i);
                    json.append(String.format("{\"state\":\"%s\", \"message\":\"%s\"}", entry.getStateName(), entry.getMessage()));
                    if (i < tx.getAuditTrail().size() - 1) json.append(",");
                }
                json.append("]}");

                sendResponse(exchange, json.toString(), 200);
            }
        }
    }

    // --- Simple Manual JSON Helpers (Since no external libs are allowed) ---
    private static void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    private static double extractJsonDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Double.parseDouble(json.substring(start, end).trim());
    }
    private static boolean extractJsonBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search) + search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Boolean.parseBoolean(json.substring(start, end).trim());
    }
}
