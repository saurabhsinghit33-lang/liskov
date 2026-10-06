package com.example.liskov.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FixedDepositAccount extends Account {

    private final LocalDate maturityDate;
    private final BigDecimal interestRate;

    public FixedDepositAccount(String accountNumber, BigDecimal initialDeposit, LocalDate maturityDate, BigDecimal interestRate) {
        super(accountNumber, initialDeposit);
        this.maturityDate = maturityDate;
        this.interestRate = interestRate;
    }

    public void applyTermInterest() {
        BigDecimal interestEarned = this.balance.multiply(this.interestRate);
        this.deposit(interestEarned);
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }
}