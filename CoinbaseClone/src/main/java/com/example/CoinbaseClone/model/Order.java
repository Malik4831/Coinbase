package com.example.CoinbaseClone.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderType type; // BUY, SELL

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // PENDING, FILLED, CANCELLED

    @Column(nullable = false)
    private String baseCurrency; // e.g., "ETH"

    @Column(nullable = false)
    private String quoteCurrency; // e.g., "USD"

    @Column(nullable = false, precision = 36, scale = 18)
    private BigDecimal amount; // amount to buy/sell

    @Column(precision = 36, scale = 18)
    private BigDecimal price; // limit price, null for market orders

    @Column(precision = 36, scale = 18)
    private BigDecimal filledAmount = BigDecimal.ZERO;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum OrderType {
        BUY, SELL
    }

    public enum OrderStatus {
        PENDING, PARTIALLY_FILLED, FILLED, CANCELLED
    }
}