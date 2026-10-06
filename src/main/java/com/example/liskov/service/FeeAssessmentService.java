package com.example.liskov.service;

import com.example.liskov.domain.contract.Withdrawable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class FeeAssessmentService {

    /**
     * LSP Compliance in action:
     * This method only accepts types that can honor the withdraw contract.
     * No 'instanceof' checks needed. No runtime UnsupportedOperationExceptions.
     */
    public void deductAnnualMaintenanceFee(List<Withdrawable> accounts, BigDecimal feeAmount) {
        for (Withdrawable account : accounts) {
            account.withdraw(feeAmount);
        }
    }
}