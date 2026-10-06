package com.example.liskov.domain.model;

import com.example.liskov.domain.contract.Withdrawable;
import com.example.liskov.exception.InsufficientFundsException;

import java.math.BigDecimal;

public class SavingsAccount extends Account implements Withdrawable {

    private final BigDecimal minimumBalanceThreshold;

    public SavingsAccount(String accountNumber, BigDecimal initialDeposit, BigDecimal minimumBalanceThreshold) {
        super(accountNumber, initialDeposit);
        this.minimumBalanceThreshold = minimumBalanceThreshold;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero.");
        }

        BigDecimal remainingBalance = this.balance.subtract(amount);
        if (remainingBalance.compareTo(minimumBalanceThreshold) < 0) {
            throw new InsufficientFundsException(
                    "Withdrawal denied: Minimum balance threshold of $" + minimumBalanceThreshold + " violated."
            );
        }

        this.balance = remainingBalance;
    }
}