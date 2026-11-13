# SwitchYard: OOM Documentation & UML

# System Design Diagrams — Project SwitchYard

## 1. Use Case Diagram
```mermaid
flowchart LR
    subgraph Actors
        U[Dashboard User / Admin]
    end

    subgraph SwitchYard_System [SwitchYard Orchestrator Portal]
        UC1([Configure Gateways Settings])
        UC2([Toggle Gateway Active/Inactive])
        UC3([Select Routing Strategy])
        UC4([Simulate Payment Transaction])
        UC5([Execute Fraud Check Chain])
        UC6([Process Payment via Adapter])
        UC7([View Transaction Receipt & Audit Trail])
    end

    U --> UC1
    U --> UC3
    U --> UC4
    UC1 -.->|includes| UC2
    UC4 -.->|includes| UC5
    UC5 -.->|includes| UC6
    UC4 -.->|includes| UC7
```

## 2. Class Diagram
```mermaid
classDiagram
    class PaymentOrchestrator {
        - fraudChainHead: FraudRule
        - routingStrategy: RoutingStrategy
        - gateways: List~PaymentGateway~
        - transactionHistory: List~Transaction~
        + executeTransaction(tx: Transaction) boolean
        + updateGatewayConfig(name, fee, successRate, enabled) boolean
        + printTransactionReceipt(tx: Transaction)
    }

    class Transaction {
        - txId: String
        - amount: double
        - paymentMethod: PaymentMethod
        - currentState: TransactionState
        - auditTrail: List~AuditEntry~
        + setState(state: TransactionState)
        + addAuditLog(stateName, message)
    }

    class RoutingStrategy {
        <<interface>>
        + selectGateway(gateways, tx) PaymentGateway
    }

    class PaymentGateway {
        <<interface>>
        + processPayment(tx) boolean
        + setFeePercentage(fee)
        + setEnabled(enabled)
    }

    class FraudRule {
        <<abstract>>
        # nextRule: FraudRule
        + evaluate(tx)
        # check(tx)*
    }
    
    class TransactionState {
        <<interface>>
        + next(tx)
        + fail(tx, reason)
    }

    PaymentOrchestrator --> RoutingStrategy : Strategy Pattern
    PaymentOrchestrator --> FraudRule : Chain of Responsibility
    PaymentOrchestrator o-- PaymentGateway : Aggregation
    PaymentOrchestrator o-- Transaction : Aggregation (History Tracking)
    RoutingStrategy <|.. LowestCostStrategy
    RoutingStrategy <|.. HighestSuccessRateStrategy
    PaymentGateway <|.. StripeGatewayAdapter
    PaymentGateway <|.. RazorpayUpiAdapter
    PaymentGateway <|.. PayPalGatewayAdapter
    Transaction --> TransactionState : State Pattern
```

## 3. Sequence Diagram
```mermaid
sequenceDiagram
    actor Client
    participant Orchestrator as PaymentOrchestrator
    participant Fraud as FraudChain
    participant Router as RoutingStrategy
    participant Gateway as PaymentGateway
    participant Tx as Transaction

    Client->>Orchestrator: executeTransaction(tx)
    Orchestrator->>Tx: setState(InitiatedState)
    
    Orchestrator->>Fraud: evaluate(tx)
    Note over Fraud: Amount Limit -> Location -> Velocity
    Fraud-->>Orchestrator: Validation Passed
    Orchestrator->>Tx: setState(FraudCheckedState)
    
    Orchestrator->>Router: selectGateway(gateways, tx)
    Router-->>Orchestrator: Returns Optimal Gateway
    Orchestrator->>Tx: setState(RoutedState)
    
    Orchestrator->>Gateway: processPayment(tx)
    Gateway-->>Orchestrator: Success (200 OK)
    Orchestrator->>Tx: setState(SettledState)
    
    Orchestrator-->>Client: true (Transaction Settled)

```
## 4. Activity Diagram
```mermaid
flowchart TD
    Start([Receive Transaction Request]) --> Init[Set State: INITIATED]
    Init --> FraudCheck{Fraud Chain Evaluation}
    
    FraudCheck -- Fraud Detected --> FailFraud[Set State: FAILED]
    FailFraud --> LogFraud[Log Fraud Rule Triggered] --> End
    
    FraudCheck -- Passed --> Route{Evaluate Routing Strategy}
    Route -- No Gateway Available --> FailRoute[Set State: FAILED]
    FailRoute --> LogRoute[Log Route Failure] --> End
    
    Route -- Gateway Selected --> Process[Process Payment via Gateway SDK]
    Process -- Timeout / SDK Error --> FailGateway[Set State: FAILED]
    FailGateway --> LogGateway[Log Timeout] --> End
    
    Process -- Success --> Settle[Set State: SETTLED]
    Settle --> End([Return Transaction Receipt])

```


## 5. OOM & Design Pattern Mapping Table

The following table explicitly maps core Object-Oriented methodologies, SOLID principles, and design patterns to the SwitchYard source code.

| Concept / Principle | Implementation in SwitchYard |
|---|---|
| **Encapsulation** | `Transaction` fields are strictly private. State transitions occur securely via `setState()`, and logs are appended exclusively via `addAuditLog()`. |
| **Inheritance** | Specific fraud rules (`AmountLimitFraudRule`, `VelocityCheckFraudRule`) extend the base `FraudRule` abstract class, inheriting its template execution flow. |
| **Polymorphism** | Calling `routingStrategy.selectGateway()` dynamically executes logic from either `LowestCostStrategy` or `HighestSuccessRateStrategy` based on user selection. |
| **Abstraction** | The `PaymentOrchestrator` relies entirely on the `PaymentGateway` interface, hiding the complex reality of dealing with distinct external payment SDKs. |
| **Composition** | `Transaction` strictly owns its internal `List<AuditEntry>`. If the transaction object is garbage-collected, the audit logs are destroyed with it. |
| **Aggregation** | `PaymentOrchestrator` aggregates `Transaction` history, and `Transaction` references `Customer`/`Merchant`. These objects are instantiated independently. |
| **SRP (SOLID)** | Single Responsibility Principle: `FraudRule` classes only validate fraud, `RoutingStrategy` classes only evaluate gateway selection, and `TransactionState` classes only handle lifecycle transitions. |
| **OCP (SOLID)** | Open/Closed Principle: To add a new Crypto gateway, developers create a new class implementing `PaymentGateway` and add it to the factory list without altering `PaymentOrchestrator`. |
| **LSP / ISP (SOLID)** | Liskov Substitution Principle: Any `PaymentGateway`, `RoutingStrategy`, or `FraudRule` subclass can replace another without breaking the orchestrator. Interface Segregation Principle: Small, focused interfaces like `TransactionState` and `RoutingStrategy`. |
| **DIP (SOLID)** | Dependency Inversion Principle: `PaymentOrchestrator` depends on abstractions (`RoutingStrategy`, `FraudRule`, `PaymentGateway`) rather than concrete classes, passed in via its constructor. |
| **Exception Handling** | `PaymentException` hierarchy with 4 subclasses. `PaymentOrchestrator.executeTransaction()` catches each, calls `.fail()`, and ensures `executionTimeMs` and history logging execute in the `finally` block. |
| **Strategy Pattern** | Enables hot-swapping routing logic dynamically at runtime without restarting the server or altering the orchestrator's core code. |
| **Chain of Responsibility** | `FraudChainFactory` sequentially links validation nodes. If any node throws an exception, the request is immediately halted and blocked. |
| **Template Method** | `FraudRule.evaluate()` is final: it executes `check()` and inherently handles forwarding to `nextRule`. Subclasses only need to implement the specific `check()` logic. |
| **Factory Pattern** | `GatewayFactory` and `FraudChainFactory` encapsulate the object creation logic, ensuring `ConsoleRunner` and `DashboardServer` never directly invoke concrete constructors. |
| **Adapter Pattern** | Standardizes incompatible mock SDKs (`RazorpayUpiSDK`, `StripeExternalSDK`) behind uniform `PaymentGateway` adapter shells. |
| **State Pattern** | Models the exact transaction lifecycle (`Initiated` -> `FraudChecked` -> `Routed` -> `Settled` / `Failed`), strictly governing directional flow. |