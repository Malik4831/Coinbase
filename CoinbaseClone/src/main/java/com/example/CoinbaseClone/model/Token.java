package com.example.CoinbaseClone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String symbol; // e.g., "DAN", "USDT", "DAI"

    @Column(nullable = false)
    private String name; // e.g., "Decentralized Autonomous Network", "Tether USD"

    @Column(unique = true, nullable = false)
    private String contractAddress; // Ethereum contract address

    @Column(nullable = false)
    private Integer decimals = 18; // ERC20 standard decimals

    @Column(nullable = false)
    private Boolean isActive = true; // Enable/disable trading

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}