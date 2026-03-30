package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.Token;
import com.example.CoinbaseClone.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    public Token addToken(String symbol, String name, String contractAddress, Integer decimals) {
        if (tokenRepository.existsBySymbol(symbol)) {
            throw new RuntimeException("Token with symbol " + symbol + " already exists");
        }
        if (tokenRepository.existsByContractAddress(contractAddress)) {
            throw new RuntimeException("Token with contract address " + contractAddress + " already exists");
        }

        Token token = new Token();
        token.setSymbol(symbol.toUpperCase());
        token.setName(name);
        token.setContractAddress(contractAddress);
        token.setDecimals(decimals != null ? decimals : 18);
        token.setIsActive(true);

        return tokenRepository.save(token);
    }

    public Token updateToken(Long tokenId, String name, String contractAddress, Integer decimals, Boolean isActive) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        if (name != null) token.setName(name);
        if (contractAddress != null) {
            // Check if another token already uses this address
            Optional<Token> existing = tokenRepository.findByContractAddress(contractAddress);
            if (existing.isPresent() && !existing.get().getId().equals(tokenId)) {
                throw new RuntimeException("Contract address already in use by another token");
            }
            token.setContractAddress(contractAddress);
        }
        if (decimals != null) token.setDecimals(decimals);
        if (isActive != null) token.setIsActive(isActive);

        return tokenRepository.save(token);
    }

    public void deactivateToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        token.setIsActive(false);
        tokenRepository.save(token);
    }

    public void activateToken(Long tokenId) {
        Token token = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));
        token.setIsActive(true);
        tokenRepository.save(token);
    }

    public Optional<Token> getTokenBySymbol(String symbol) {
        return tokenRepository.findBySymbol(symbol.toUpperCase());
    }

    public Optional<Token> getTokenByContractAddress(String contractAddress) {
        return tokenRepository.findByContractAddress(contractAddress);
    }

    public List<Token> getAllTokens() {
        return tokenRepository.findAll();
    }

    public List<Token> getActiveTokens() {
        return tokenRepository.findByIsActiveTrue();
    }

    public Token getTokenById(Long id) {
        return tokenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Token not found"));
    }

    public void deleteToken(Long tokenId) {
        if (!tokenRepository.existsById(tokenId)) {
            throw new RuntimeException("Token not found");
        }
        tokenRepository.deleteById(tokenId);
    }
}