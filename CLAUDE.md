# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`com.example:liskov` — a Spring Boot 4.1.1 / Java 25 learning project that demonstrates the **Liskov Substitution Principle (LSP)** inside a BFSI (banking) domain. Build system is Maven (wrapper included). There is no README; this file is the primary orientation.

## Commands

Use the Maven wrapper so the pinned Maven version is used:

```bash
./mvnw spring-boot:run           # run the app (default port 8080)
./mvnw clean package             # compile + run tests + build jar
./mvnw test                      # run the whole test suite
./mvnw -Dtest=LiskovApplicationTests test              # run a single test class
./mvnw -Dtest=LiskovApplicationTests#contextLoads test # run a single test method
java -jar target/liskov-0.0.1-SNAPSHOT.jar             # run the packaged jar
```

Toolchain requirement: **Java 25** (set in `pom.xml` `<java.version>25</java.version>`). The project also uses the Spring Boot 4.x `spring-boot-starter-webmvc` / `spring-boot-starter-webmvc-test` artifacts (not the Spring Boot 3 `-web` names) — keep that in mind when adding dependencies.

## Architecture — the LSP design to preserve

The whole point of this codebase is that *substitutability* is enforced by **capability-based interface segregation**, not by `instanceof` checks or `UnsupportedOperationException` stubs. Any change that lets a non-withdrawable account appear where `Withdrawable` is expected would defeat the lesson.

Package layout under `com.example.liskov`:

- `domain/model` — the account hierarchy
  - `Account` (abstract) — holds `accountNumber`, `balance`, and `deposit(...)`. **Does not declare `withdraw`.** Deposit is universal; withdrawal is a *capability*, not a universal operation.
  - `SavingsAccount extends Account implements Withdrawable` — withdraw enforces a `minimumBalanceThreshold`; violations throw `InsufficientFundsException`.
  - `FixedDepositAccount extends Account` — intentionally does **not** implement `Withdrawable` (locked until maturity). Exposes `applyTermInterest()` instead.
- `domain/contract/Withdrawable` — the segregated capability interface. Keep withdrawal out of `Account` and in this interface.
- `service`
  - `BankingService.processUniversalDeposit(Account, …)` operates on the base type (works for every account).
  - `BankingService.executeFundTransfer(Withdrawable source, Account destination, …)` — the source parameter is `Withdrawable`, not `Account`, so the type system forbids a FixedDepositAccount from being used as a source.
  - `FeeAssessmentService.deductAnnualMaintenanceFee(List<Withdrawable>, …)` — same pattern for bulk operations.
- `dto` — `AccountResponse` (record, with `from(Account, status)` factory) and `TransferRequest` (plain POJO; present but not yet wired to a controller).
- `exception/InsufficientFundsException` — unchecked, thrown from withdrawal paths.

Guidance for extensions:

- When adding a new account type, decide first whether it is withdrawable. If yes, implement `Withdrawable`; if not, do **not** add a stub that throws — just omit the interface.
- Prefer `Withdrawable` as a parameter type whenever a method needs to debit funds. Prefer `Account` only for operations that are safe for every account (currently just `deposit`).
- There is no persistence layer, no web controller, and no repository yet — only the domain + two services + DTOs + a `@SpringBootApplication` bootstrap. If adding a controller, note that `TransferRequest` already exists and is the intended request body for a transfer endpoint fed into `BankingService.executeFundTransfer`.

## Configuration

`src/main/resources/application.yaml` only sets `spring.application.name=liskov`. No database, no profiles, no secrets. If you add external config, follow the repo's broader convention (env vars for anything sensitive — never hard-code).
