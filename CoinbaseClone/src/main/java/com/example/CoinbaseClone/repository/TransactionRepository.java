package com.example.CoinbaseClone.repository;

import com.example.CoinbaseClone.model.Transaction;
import com.example.CoinbaseClone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserAndStatus(User user, Transaction.TransactionStatus status);
}