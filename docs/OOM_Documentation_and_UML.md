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
    UC1 -.->|includes| UC2
    U --> UC3
    U --> UC4
    UC4 -.->|includes| UC5
    UC5 -.->|extends| UC6
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

    class LowestCostStrategy
    class HighestSuccessRateStrategy

    class PaymentGateway {
        <<interface>>
        + processPayment(tx) boolean
        + setFeePercentage(fee)
        + setEnabled(enabled)
    }

    class StripeGatewayAdapter
    class RazorpayUpiAdapter
    class PayPalGatewayAdapter
    
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
    PaymentOrchestrator *-- Transaction : Manages Lifecycle
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
|---------------------|------------------------------|
| **Encapsulation** | `Transaction` fields (`amount`, `currentState`) are private. State changes strictly via `setState()`, and audit logs are appended via `addAuditLog()`. |
| **Inheritance** | `AmountLimitFraudRule`, `BlacklistedLocationFraudRule`, and `VelocityCheckFraudRule` inherit the core `evaluate` execution template from the abstract `FraudRule` class. |
| **Polymorphism** | Calling `routingStrategy.selectGateway()` dynamically invokes the logic of either `LowestCostStrategy` or `HighestSuccessRateStrategy` based on the UI dropdown selection at runtime. |
| **Abstraction** | The `PaymentOrchestrator` interacts only with the `PaymentGateway` interface, remaining completely insulated from the underlying complexities of the mock external SDKs. |
| **Composition** | `Transaction` strictly owns its `List<AuditEntry>`. The lifecycle of the audit logs is tied directly to the transaction; destroying the transaction destroys the logs. |
| **Aggregation** | `Transaction` holds references to `Customer` and `Merchant` objects, which exist independently of any single transaction (a customer can have multiple transactions). |
| **SRP (SOLID)** | Single Responsibility Principle: `FraudRule` classes only validate fraud, `RoutingStrategy` classes only evaluate gateway selection, and `TransactionState` classes only handle lifecycle transitions. |
| **OCP (SOLID)** | Open/Closed Principle: To add a new Crypto gateway, developers create a new class implementing `PaymentGateway` and add it to the factory list without altering `PaymentOrchestrator`. |
| **DIP (SOLID)** | Dependency Inversion Principle: `PaymentOrchestrator` depends on abstractions (`RoutingStrategy`, `FraudRule`, `PaymentGateway`) rather than concrete classes, passed in via its constructor. |
| **Strategy Pattern** | Allows hot-swapping between `LowestCostStrategy` and `HighestSuccessRateStrategy` in real-time via the Dashboard UI without stopping the server. |
| **Chain of Responsibility** | `FraudChainFactory` links rules (Amount -> Location -> Velocity). If one rule throws a `FraudDetectedException`, the chain breaks and the transaction is immediately blocked. |
| **Adapter Pattern** | Wraps incompatible third-party SDKs (`RazorpayUpiSDK`) inside standard classes (`RazorpayUpiAdapter`) that conform to the system's `PaymentGateway` contract. |
| **State Pattern** | Manages `Transaction` transitions (`InitiatedState` -> `FraudCheckedState` -> `RoutedState` -> `SettledState`). Calling `.next()` dictates the valid directional flow. |
