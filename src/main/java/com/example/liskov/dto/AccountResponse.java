package com.example.liskov.dto;

import com.example.liskov.domain.model.Account;

import java.math.BigDecimal;

public record AccountResponse(
        String accountNumber,
        BigDecimal balance,
        String accountType,
        String status
) {
    public static AccountResponse from(Account account, String status) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getBalance(),
                account.getClass().getSimpleName(),
                status
        );
    }
}