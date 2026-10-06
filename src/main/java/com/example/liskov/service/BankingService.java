package com.example.liskov.service;

import com.example.liskov.domain.contract.Withdrawable;
import com.example.liskov.domain.model.Account;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BankingService {

    /**
     * Operates purely on the base Account abstraction.
     * Works for SavingsAccount, FixedDepositAccount, or any future account type.
     */
    public void processUniversalDeposit(Account account, BigDecimal amount) {
        account.deposit(amount);
    }

    /**
     * Executes transfer from a source that explicitly supports withdrawal.
     */
    public void executeFundTransfer(Withdrawable source, Account destination, BigDecimal amount) {
        source.withdraw(amount);
        destination.deposit(amount);
    }
}