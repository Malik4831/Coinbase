package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class TokenRegistry {

    @Autowired
    private TokenService tokenService;

    // Cache for performance - in production, consider Redis or similar
    private Map<String, String> tokenAddressCache = new HashMap<>();

    public String getContractAddress(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Token symbol cannot be null or blank");
        }

        String upperSymbol = symbol.toUpperCase();

        // Check cache first
        if (tokenAddressCache.containsKey(upperSymbol)) {
            String cachedAddress = tokenAddressCache.get(upperSymbol);
            return cachedAddress != null ? cachedAddress : null;
        }

        // Query database
        Optional<Token> token = tokenService.getTokenBySymbol(upperSymbol);
        if (token.isPresent() && token.get().getIsActive()) {
            String address = token.get().getContractAddress();
            tokenAddressCache.put(upperSymbol, address);
            return address;
        }

        // Not found or inactive
        tokenAddressCache.put(upperSymbol, null);
        return null;
    }

    public Token getTokenInfo(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Token symbol cannot be null or blank");
        }

        Optional<Token> token = tokenService.getTokenBySymbol(symbol.toUpperCase());
        return token.orElse(null);
    }

    public Map<String, String> getAllActiveTokenAddresses() {
        List<Token> activeTokens = tokenService.getActiveTokens();
        Map<String, String> addressMap = new HashMap<>();

        for (Token token : activeTokens) {
            addressMap.put(token.getSymbol(), token.getContractAddress());
        }

        return addressMap;
    }

    public void refreshCache() {
        tokenAddressCache.clear();
        Map<String, String> allAddresses = getAllActiveTokenAddresses();
        tokenAddressCache.putAll(allAddresses);
    }

    public boolean isTokenSupported(String symbol) {
        return getContractAddress(symbol) != null;
    }
}
