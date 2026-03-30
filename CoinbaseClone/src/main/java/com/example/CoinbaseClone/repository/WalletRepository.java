package com.example.CoinbaseClone.repository;

import com.example.CoinbaseClone.model.Wallet;
import com.example.CoinbaseClone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser(User user);
    Optional<Wallet> findByUserAndCurrency(User user, String currency);
    Optional<Wallet> findByBlockchainAddress(String blockchainAddress);
}