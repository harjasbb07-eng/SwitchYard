# SwitchYard: Payment Orchestrator, Fraud Engine & Live Dashboard

An Object-Oriented Methodology (OOM) group project in **pure Core Java (JDK 17+)**. SwitchYard simulates how a payment orchestrator works: every payment passes through a fraud-rule chain, is routed to the best payment gateway by a swappable strategy, is processed through a gateway adapter, and moves through a strict transaction state machine. Everything can be watched and controlled live from a web dashboard.

**Team:** Harjas, Pratham, Aryan

## Features

- **Transaction state machine:** `INITIATED -> FRAUD_CHECKED -> ROUTED -> SETTLED`, or `FAILED` from any stage. Illegal transitions throw `InvalidTransactionStateException`.
- **Fraud engine (chain of rules):** amount limit, blacklisted location, and velocity check (max 3 transactions per customer ID; the counter resets only when the server restarts).
- **Dynamic routing:** `LowestCostStrategy` and `HighestSuccessRateStrategy`, switchable at runtime. Disabled gateways and gateways that do not support the payment method are skipped automatically.
- **Gateway adapters:** Stripe, Razorpay and PayPal adapters wrap three different mock SDKs behind one `PaymentGateway` interface.
- **Live configuration:** change a gateway's fee, success rate or active state at runtime and see the routing decision change immediately.
- **Full audit trail** for every transaction, with live analytics on the dashboard (total / settled / failed counts and a Settled-vs-Failed doughnut chart).
- **Two front ends:** a console runner for automated scenarios and a web dashboard.

## Tech Stack

| Layer | Technology |
|---|---|
| Core engine | Pure Core Java (JDK 17+), no frameworks, no database, no build tool |
| Server | `com.sun.net.httpserver.HttpServer` (built into the JDK) |
| UI | Vanilla HTML, CSS, JavaScript, Chart.js (CDN) |

## Project Structure

```
SwitchYard/
├── README.md
├── docs/                      UML diagrams, OOM concept mapping, test report
├── web/                       Dashboard UI (index.html, style.css, app.js)
└── src/com/switchyard/
    ├── ConsoleRunner.java     Automated CLI scenarios
    ├── DashboardServer.java   HTTP server + JSON endpoints for the UI
    ├── model/                 Transaction, Customer, Merchant, AuditEntry, PaymentMethod
    ├── state/                 State pattern (Initiated, FraudChecked, Routed, Settled, Failed)
    ├── routing/               Strategy pattern (LowestCost, HighestSuccessRate)
    ├── service/               PaymentOrchestrator
    ├── fraud/                 Chain of Responsibility (FraudRule and its subclasses, FraudChainFactory)
    ├── gateway/               Adapter + Factory (mock SDKs, adapters, GatewayFactory)
    └── exception/             Custom exception hierarchy
```

## How to Run

Requires **JDK 17 or newer**. Run all commands from the repository root (the dashboard serves files from `web/` relative to this folder).

**Compile**

```bash
mkdir -p out
javac -d out $(find src -name "*.java")
```

**Run the automated console demo**

```bash
java -cp out com.switchyard.ConsoleRunner
```

**Launch the live dashboard**

```bash
java -cp out com.switchyard.DashboardServer
```

Then open <http://localhost:8080> in a browser.

## Live Demo Walkthrough

1. Select UPI + Lowest Cost and click Process. Razorpay (0.5%) is selected.
2. Drag the Razorpay fee slider to 3.0%.
3. Process the same input again. Stripe (2.0%) is now selected automatically.
4. Reset Razorpay to 0.5% and switch to Highest Success Rate. Stripe (99.2%) is selected.
5. Disable Stripe and process again. The orchestrator falls back to Razorpay.
6. Use amount INR 2,50,000 or location `SHADOW_PROXY`. The fraud chain blocks the payment and the state becomes `FAILED`.
7. Open the audit trail panel to see every state transition.

> The velocity rule allows at most 3 transactions per customer ID, and the count is never reset while the server is running. The dashboard changes the Customer ID after every payment and has a "Randomize ID" button, so normal demo runs do not trigger it by accident. To demonstrate the rule on the dashboard, type the same Customer ID for 4 payments in a row. `ConsoleRunner` scenario 6 demonstrates it automatically.

## Design Patterns and Principles

| Concept | Where it is used |
|---|---|
| State | `TransactionState` and its five implementations in `state/` |
| Strategy | `RoutingStrategy`, `LowestCostStrategy`, `HighestSuccessRateStrategy` |
| Chain of Responsibility | `FraudRule` and its subclasses, linked by `FraudChainFactory` |
| Adapter | `StripeGatewayAdapter`, `RazorpayUpiAdapter`, `PayPalGatewayAdapter` wrap incompatible mock SDKs |
| Factory | `GatewayFactory`, `FraudChainFactory` |
| Template Method | `FraudRule.evaluate()` calls the abstract `check()` |
| Dependency Inversion | `PaymentOrchestrator` depends on interfaces and receives them via its constructor |

The complete mapping (encapsulation, inheritance, polymorphism, abstraction, composition, aggregation, SOLID) and all UML diagrams are in [`docs/`](docs/).

## Exception Handling

All business errors extend a checked `PaymentException`: `FraudDetectedException` (carries the triggered rule), `GatewayTimeoutException`, `InvalidTransactionStateException`, and `NoGatewayAvailableException`. `PaymentOrchestrator.executeTransaction()` catches each type separately, records the reason in the audit trail, moves the transaction to `FAILED`, and always records execution time and history in a `finally` block.

## Testing

See [`docs/TEST_REPORT.md`](docs/TEST_REPORT.md) for the scenarios, expected results and observed results.

## Limitations

- **All payment gateways are simulated.** Stripe, Razorpay and PayPal here are adapters that we wrote ourselves around mock SDK classes. No real gateway, bank, or network is contacted and no real money moves. The SDK behaviour (including the PayPal timeout test case) is simulated.
- Gateway fee and success rate are configurable values used for routing decisions; they do not reflect real gateway pricing or reliability.
- Data is stored **in memory only**. Transaction history, analytics and velocity counters are lost when the server stops.
- The velocity rule counts transactions per customer ID for the whole server run (no time window, no reset), and keeps its counters inside a single JVM process, so it would not work across multiple servers.
- No authentication or HTTPS on the dashboard. It is meant for local demonstration only.
- JSON is built and parsed by hand (no JSON library), so it supports only the fields this project uses.
- Execution times reflect in-memory simulation, not real network latency.

## Possible Future Work

- Real gateway SDK integration behind the existing adapter interface
- Persistent storage for transactions and fraud state
- A time window for the velocity rule
- Per-gateway transaction counts and a fraud-blocked counter in the analytics panel
- More routing strategies (for example, load-balanced or region-based)
- Configurable fraud rules from the UI
