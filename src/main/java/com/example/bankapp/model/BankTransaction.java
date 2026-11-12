package com.example.bankapp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    private LocalDateTime createdAt = LocalDateTime.now();

    private String description;

    private String fromAccount;

    private String toAccount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
}