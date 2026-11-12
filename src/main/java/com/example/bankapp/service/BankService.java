package com.example.bankapp.service;

import org.springframework.transaction.annotation.Propagation;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.exception.TransferValidationException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository bankTransactionRepository;

   
@Transactional(rollbackFor = Exception.class)
public BankTransaction transfer(String fromAccount, String toAccount, BigDecimal amount, boolean simulateFailure) {

        // Step 1: validate
        if (fromAccount.equals(toAccount)) {
            throw new TransferValidationException("Sender and receiver must be different accounts.");
        }

        customerRepository.getByAccountNumber(fromAccount)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found: " + fromAccount));
        customerRepository.getByAccountNumber(toAccount)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found: " + toAccount));

        // Step 2: withdraw
        int updatedRows = customerRepository.debit(fromAccount, amount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + fromAccount);
        }

        // Step 3: experiment
        if (simulateFailure) {
            throw new RuntimeException("Simulated system failure after the withdrawal.");
        }

        // Step 4: deposit
        customerRepository.credit(toAccount, amount);

        // Step 5: save SUCCESS record
        return saveRecord(fromAccount, toAccount, amount, "Transfer completed");
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        customerRepository.credit(accountNumber, amount);
        return saveRecord("CASH", accountNumber, amount, "Cash deposit");
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        checkAmount(amount);
        customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        int updatedRows = customerRepository.debit(accountNumber, amount);
        if (updatedRows == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNumber);
        }
        return saveRecord(accountNumber, "CASH", amount, "Cash withdrawal");
    }

    @Transactional
    public Customer createCustomer(String accountNumber, String fullName, String email, BigDecimal balance) {
        if (customerRepository.countAccounts(accountNumber) > 0) {
            throw new TransferValidationException("Account " + accountNumber + " already exists.");
        }
        if (customerRepository.countEmails(email) > 0) {
            throw new TransferValidationException("Email " + email + " already exists.");
        }
        Customer c = new Customer();
        c.setAccountNumber(accountNumber);
        c.setFullName(fullName);
        c.setEmail(email);
        c.setBalance(balance);
        return customerRepository.save(c);
    }

    public Optional<Customer> findCustomer(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber);
    }

    public List<Customer> searchCustomers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return customerRepository.findAllOrdered();
        }
        return customerRepository.searchByName(keyword.trim());
    }

    public BigDecimal getTotalBalance() {
        BigDecimal total = customerRepository.getTotalBalance();
        return total == null ? BigDecimal.ZERO : total;
    }

    public long countCustomers() {
        return customerRepository.countCustomersNative();
    }

    public long countByStatus(TransactionStatus status) {
        return bankTransactionRepository.countByStatus(status);
    }

    public List<BankTransaction> getLatestTransactions() {
        return bankTransactionRepository.findAllLatest().stream().limit(10).toList();
    }

    public List<BankTransaction> getHistory(String accountNumber) {
        return bankTransactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(BigDecimal min) {
        return bankTransactionRepository.findLargeTransfers(min, TransactionStatus.SUCCESS);
    }

    public BigDecimal getTotalSent(String accountNumber) {
        BigDecimal total = bankTransactionRepository.getTotalSent(accountNumber, TransactionStatus.SUCCESS);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferValidationException("Amount must be greater than zero.");
        }
    }

    private BankTransaction saveRecord(String from, String to, BigDecimal amount, String description) {
        BankTransaction tx = new BankTransaction();
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setStatus(TransactionStatus.SUCCESS);
        return bankTransactionRepository.save(tx);
    }
}