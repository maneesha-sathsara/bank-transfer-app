package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository bankTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String fromAcc, String toAcc, BigDecimal amount, String errorMsg) {
        BankTransaction failedTx = new BankTransaction();
        failedTx.setFromAccount(fromAcc);
        failedTx.setToAccount(toAcc);
        failedTx.setAmount(amount == null ? BigDecimal.ZERO : amount);
        failedTx.setDescription(errorMsg);
        failedTx.setStatus(TransactionStatus.FAILED);
        bankTransactionRepository.save(failedTx);
    }
}