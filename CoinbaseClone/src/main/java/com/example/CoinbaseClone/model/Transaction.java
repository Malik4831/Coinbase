package com.example.CoinbaseClone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // DEPOSIT, WITHDRAWAL, TRADE

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal amount;

    @Column(precision = 36, scale = 18)
    private BigDecimal fee;

    private String blockchainTxHash; // for crypto transactions

    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRADE_BUY, TRADE_SELL
    }

    public enum TransactionStatus {
        PENDING, CONFIRMED, FAILED
    }
}