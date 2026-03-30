package com.example.CoinbaseClone.dto;

import com.example.CoinbaseClone.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        Order.OrderType type,
        Order.OrderStatus status,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal amount,
        BigDecimal price,
        BigDecimal filledAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getType(),
                order.getStatus(),
                order.getBaseCurrency(),
                order.getQuoteCurrency(),
                order.getAmount(),
                order.getPrice(),
                order.getFilledAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static List<OrderResponse> fromList(List<Order> orders) {
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }
}
