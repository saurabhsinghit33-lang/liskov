# Liskov Substitution Principle — BFSI Banking Domain

A Spring Boot 4.1.1 / Java 25 reference project that demonstrates the **Liskov Substitution Principle (LSP)** — the "L" in SOLID — using a realistic Banking / Financial Services (BFSI) domain.

The project models two very different kinds of bank accounts (a regular `SavingsAccount` and a locked-in `FixedDepositAccount`) and shows how to design the type hierarchy so that **every subtype is safely substitutable for its supertype without the caller needing `instanceof` checks or defensive exception handling**.

---

## 1. What LSP says

> *Subtypes must be substitutable for their base types without altering the correctness of the program.* — Barbara Liskov, 1987

In practical Java terms, if a method accepts a base type `T`, it must work correctly for **every** subclass of `T`, including ones that don't exist yet. If a subclass has to throw `UnsupportedOperationException`, weaken a postcondition, or strengthen a precondition, the hierarchy violates LSP.

---

## 2. The problem — a classic LSP violation

A naive design would put every operation on the base `Account` class:

```java
// ❌ LSP-VIOLATING DESIGN (not used in this project)
public abstract class Account {
    public void deposit(BigDecimal amount)  { ... }
    public abstract void withdraw(BigDecimal amount);  // forced on every subtype
}

public class FixedDepositAccount extends Account {
    @Override
    public void withdraw(BigDecimal amount) {
        // A fixed deposit is locked until maturity — can't be withdrawn.
        throw new UnsupportedOperationException("Cannot withdraw from a fixed deposit before maturity");
    }
}
```

Any service that calls `account.withdraw(...)` on an `Account` reference will blow up at runtime the moment a `FixedDepositAccount` instance is passed. Callers then start writing defensive code:

```java
if (account instanceof FixedDepositAccount) { /* skip */ }
else { account.withdraw(amount); }
```

That is the smell LSP forbids.

---

## 3. How this project fixes it — capability-based interface segregation

The fix is to **split the capabilities**. "Being an account" and "being withdrawable" are two different things, so they are modelled as two different types.

### 3.1 The base type — `Account`

[`src/main/java/com/example/liskov/domain/model/Account.java`](src/main/java/com/example/liskov/domain/model/Account.java)

`Account` is `abstract` and only declares what is **truly universal to every account**: an account number, a balance, and the ability to receive a deposit.

```java
public abstract class Account {
    private final String accountNumber;
    protected BigDecimal balance;

    public void deposit(BigDecimal amount) { ... }     // universal — always safe
    public String getAccountNumber() { ... }
    public BigDecimal getBalance() { ... }
    // NOTE: no withdraw() method here. Withdrawal is not universal.
}
```

Key design decision — **`withdraw` is deliberately not declared on `Account`**, because not every account can honor it.

### 3.2 The segregated capability — `Withdrawable`

[`src/main/java/com/example/liskov/domain/contract/Withdrawable.java`](src/main/java/com/example/liskov/domain/contract/Withdrawable.java)

```java
public interface Withdrawable {
    void withdraw(BigDecimal amount);
}
```

This tiny interface represents the *capability* of being withdrawn from. Any type that implements it is **promising** to honor the contract at runtime. Any type that does not implement it is **openly declaring** that it cannot be withdrawn — and the compiler enforces that.

### 3.3 A withdrawable subtype — `SavingsAccount`

[`src/main/java/com/example/liskov/domain/model/SavingsAccount.java`](src/main/java/com/example/liskov/domain/model/SavingsAccount.java)

```java
public class SavingsAccount extends Account implements Withdrawable {
    @Override
    public void withdraw(BigDecimal amount) {
        // validates minimum-balance threshold; throws InsufficientFundsException
        // when the business rule is violated — NOT because the operation is
        // unsupported, but because the inputs are bad.
    }
}
```

Important LSP detail: `SavingsAccount.withdraw` does **not** strengthen the preconditions of `Withdrawable.withdraw` — it only enforces the account's own minimum-balance rule, which is part of its documented contract. A caller holding a `Withdrawable` reference can always call `withdraw` legitimately.

### 3.4 A non-withdrawable subtype — `FixedDepositAccount`

[`src/main/java/com/example/liskov/domain/model/FixedDepositAccount.java`](src/main/java/com/example/liskov/domain/model/FixedDepositAccount.java)

```java
public class FixedDepositAccount extends Account {           // <-- does NOT implement Withdrawable
    public void applyTermInterest() { ... }
    public LocalDate getMaturityDate() { ... }
}
```

The key move: `FixedDepositAccount` extends `Account` (so it can be deposited into — that *is* safe) but **does not implement `Withdrawable`**. There is no stub `withdraw` method that throws. The type system itself tells callers "you can't withdraw from this."

### 3.5 The exception — `InsufficientFundsException`

[`src/main/java/com/example/liskov/exception/InsufficientFundsException.java`](src/main/java/com/example/liskov/exception/InsufficientFundsException.java)

An unchecked, business-rule exception thrown by `SavingsAccount.withdraw` when the minimum-balance threshold would be violated. Notice this is **not** `UnsupportedOperationException` — it signals bad input to a supported operation, not a missing capability.

---

## 4. LSP in action — the service layer

The services show the payoff: they never need `instanceof`, never catch `UnsupportedOperationException`, and never guess what a given account can do. They let **the parameter types** do the enforcement.

### 4.1 `BankingService`

[`src/main/java/com/example/liskov/service/BankingService.java`](src/main/java/com/example/liskov/service/BankingService.java)

```java
// Works for every Account subtype — deposit is universal.
public void processUniversalDeposit(Account account, BigDecimal amount) {
    account.deposit(amount);
}

// Source MUST be Withdrawable. Compiler forbids passing a FixedDepositAccount.
public void executeFundTransfer(Withdrawable source, Account destination, BigDecimal amount) {
    source.withdraw(amount);
    destination.deposit(amount);
}
```

Try to call `executeFundTransfer(someFixedDepositAccount, ...)` and the code **will not compile**. The LSP violation is caught at compile time, not at runtime.

### 4.2 `FeeAssessmentService`

[`src/main/java/com/example/liskov/service/FeeAssessmentService.java`](src/main/java/com/example/liskov/service/FeeAssessmentService.java)

```java
public void deductAnnualMaintenanceFee(List<Withdrawable> accounts, BigDecimal feeAmount) {
    for (Withdrawable account : accounts) {
        account.withdraw(feeAmount);     // safe for every element — guaranteed by the type
    }
}
```

The signature `List<Withdrawable>` makes it impossible to accidentally include a `FixedDepositAccount` in the fee-deduction batch. No runtime check needed.

---

## 5. Why this is LSP-compliant — the checklist

| LSP rule | How this project satisfies it |
|---|---|
| Subtypes must honor the base type's contract | `SavingsAccount` and `FixedDepositAccount` both honor `Account`'s only contract: `deposit`. Both succeed. |
| Subtypes must not throw new unchecked exceptions for base-type operations | Neither subclass throws from `deposit`. `SavingsAccount.withdraw` throws `InsufficientFundsException`, but that is part of the `Withdrawable` contract, not `Account`. |
| Subtypes must not strengthen preconditions | `deposit` accepts any positive amount for both subtypes. `SavingsAccount.withdraw` only enforces the minimum-balance rule *documented on itself*, not a stricter version of some base precondition. |
| Subtypes must not weaken postconditions | `deposit` increases balance in both subtypes. `SavingsAccount.withdraw` always leaves the balance at or above the threshold on success. |
| Callers should not need to know the concrete subtype | `BankingService` and `FeeAssessmentService` operate purely on `Account` and `Withdrawable`. There is no `instanceof` anywhere in the codebase. |

---

## 6. Extending the hierarchy — the rule to follow

When you add a new account type, decide **first** which capabilities it supports:

- Can it receive deposits? → extend `Account` (today, every account can).
- Can funds be withdrawn from it on demand? → also implement `Withdrawable`.
- Does it support something new (e.g. "credit", "overdraft", "freeze")? → introduce a new segregated interface (`Creditable`, `Overdraftable`, …) rather than bolting the method onto `Account`.

**Never** add a `withdraw` method that throws `UnsupportedOperationException`. That is exactly the LSP violation this project is built to prevent.

---

## 7. Project layout

```
src/main/java/com/example/liskov
├── LiskovApplication.java                 # @SpringBootApplication bootstrap
├── domain
│   ├── contract
│   │   └── Withdrawable.java              # segregated capability interface
│   └── model
│       ├── Account.java                   # abstract base — deposit only
│       ├── SavingsAccount.java            # Account + Withdrawable
│       └── FixedDepositAccount.java       # Account only (no withdraw)
├── service
│   ├── BankingService.java                # deposit + transfer
│   └── FeeAssessmentService.java          # bulk withdraw via List<Withdrawable>
├── dto
│   ├── AccountResponse.java               # response record with factory
│   └── TransferRequest.java               # request body for a future transfer endpoint
└── exception
    └── InsufficientFundsException.java    # business-rule exception
```

---

## 8. Build & run

Requires **Java 25**. Use the Maven wrapper:

```bash
./mvnw spring-boot:run                            # run the app
./mvnw clean package                              # compile, test, package
./mvnw test                                       # run tests
./mvnw -Dtest=LiskovApplicationTests test         # run a single test class
java -jar target/liskov-0.0.1-SNAPSHOT.jar        # run the packaged jar
```

The only runtime config lives in [`src/main/resources/application.yaml`](src/main/resources/application.yaml) and currently just sets the application name — there is no database or external dependency to configure.
