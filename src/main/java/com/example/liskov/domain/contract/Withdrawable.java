package com.example.liskov.domain.contract;

import java.math.BigDecimal;

public interface Withdrawable {
    void withdraw(BigDecimal amount);
}