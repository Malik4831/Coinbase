package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.model.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PlatformInventoryService {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Value("${exchange.platform-user-email:platform@exchange.local}")
    private String platformUserEmail;

    @Transactional
    public User getOrCreatePlatformUser() {
        return userService.findByEmail(platformUserEmail)
                .orElseGet(() -> userService.registerUser(
                        platformUserEmail,
                        UUID.randomUUID().toString(),
                        "Platform",
                        "Inventory"
                ));
    }

    @Transactional
    public Wallet getOrCreatePlatformWallet(String currency) {
        User platformUser = getOrCreatePlatformUser();
        return walletService.getWalletByUserAndCurrency(platformUser, currency)
                .orElseGet(() -> walletService.createWallet(platformUser, currency));
    }

    @Transactional(readOnly = true)
    public List<Wallet> getPlatformWallets() {
        User platformUser = getOrCreatePlatformUser();
        return walletService.getUserWallets(platformUser);
    }

    @Transactional
    public Wallet createPlatformWallet(String currency) {
        return getOrCreatePlatformWallet(currency);
    }

    @Transactional
    public Wallet seedInventory(String currency, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Seed amount must be greater than zero");
        }

        Wallet wallet = getOrCreatePlatformWallet(currency);
        return walletService.updateBalance(wallet, amount);
    }

    @Transactional
    public List<Wallet> bootstrapWallets(List<String> currencies) {
        if (currencies == null || currencies.isEmpty()) {
            throw new IllegalArgumentException("At least one currency is required");
        }

        currencies.forEach(this::getOrCreatePlatformWallet);
        return getPlatformWallets();
    }

    public String getPlatformUserEmail() {
        return platformUserEmail;
    }
}
