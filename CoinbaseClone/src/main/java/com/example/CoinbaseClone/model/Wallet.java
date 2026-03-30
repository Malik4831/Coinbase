package com.example.CoinbaseClone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String currency; // e.g., "ETH", "BTC", "DAN"

    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal lockedBalance = BigDecimal.ZERO; // for pending orders

    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal creditedOnChainBalance = BigDecimal.ZERO;

    private String blockchainAddress; // for external wallets
}
