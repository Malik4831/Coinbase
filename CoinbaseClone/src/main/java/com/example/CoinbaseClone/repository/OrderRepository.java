package com.example.CoinbaseClone.repository;

import com.example.CoinbaseClone.model.Order;
import com.example.CoinbaseClone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    List<Order> findByStatus(Order.OrderStatus status);
    List<Order> findByUserAndStatus(User user, Order.OrderStatus status);
}