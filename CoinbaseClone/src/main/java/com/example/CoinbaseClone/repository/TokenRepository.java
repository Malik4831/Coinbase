package com.example.CoinbaseClone.repository;

import com.example.CoinbaseClone.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findBySymbol(String symbol);
    Optional<Token> findByContractAddress(String contractAddress);
    List<Token> findByIsActiveTrue();
    boolean existsBySymbol(String symbol);
    boolean existsByContractAddress(String contractAddress);
}