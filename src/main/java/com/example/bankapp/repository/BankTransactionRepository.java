package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Query("SELECT b FROM BankTransaction b ORDER BY b.createdAt DESC, b.id DESC")
    List<BankTransaction> findAllLatest();

    @Query("SELECT b FROM BankTransaction b WHERE b.fromAccount = :account OR b.toAccount = :account ORDER BY b.createdAt DESC, b.id DESC")
    List<BankTransaction> findHistory(@Param("account") String account);

    @Query("SELECT b FROM BankTransaction b WHERE b.status = :status ORDER BY b.createdAt DESC, b.id DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    @Query("SELECT COUNT(b) FROM BankTransaction b WHERE b.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);

    @Query("SELECT b FROM BankTransaction b WHERE b.status = :status AND b.amount >= :min ORDER BY b.amount DESC")
    List<BankTransaction> findLargeTransfers(@Param("min") BigDecimal min, @Param("status") TransactionStatus status);

    @Query("SELECT SUM(b.amount) FROM BankTransaction b WHERE b.fromAccount = :account AND b.status = :status AND b.toAccount <> 'CASH'")
    BigDecimal getTotalSent(@Param("account") String account, @Param("status") TransactionStatus status);
}