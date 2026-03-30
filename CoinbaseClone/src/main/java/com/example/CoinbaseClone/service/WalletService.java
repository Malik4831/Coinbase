package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.model.Wallet;
import com.example.CoinbaseClone.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletDerivationService walletDerivationService;

    public Wallet createWallet(User user, String currency) {
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        Optional<Wallet> existingWallet = walletRepository.findByUserAndCurrency(user, normalizedCurrency);
        if (existingWallet.isPresent()) {
            return existingWallet.get();
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(normalizedCurrency);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setLockedBalance(BigDecimal.ZERO);

        // Derive unique blockchain address for this user and currency
        // For simplicity, we use userId + currency hash as derivation index
        // In production, you might want separate derivation per currency
        String derivationKey = user.getId() + ":" + normalizedCurrency;
        int derivationIndex = Math.abs(derivationKey.hashCode()) % Integer.MAX_VALUE;

        wallet.setBlockchainAddress(walletDerivationService.deriveAddressForUser((long) derivationIndex));

        return walletRepository.save(wallet);
    }

    public List<Wallet> getUserWallets(User user) {
        Map<String, Wallet> dedupedWallets = new LinkedHashMap<>();
        for (Wallet wallet : walletRepository.findByUser(user)) {
            String currency = wallet.getCurrency() == null ? "" : wallet.getCurrency().toUpperCase(Locale.ROOT);
            dedupedWallets.putIfAbsent(currency, wallet);
        }
        return List.copyOf(dedupedWallets.values());
    }

    public Optional<Wallet> getWalletByUserAndCurrency(User user, String currency) {
        return walletRepository.findByUserAndCurrency(user, currency.toUpperCase(Locale.ROOT));
    }

    public Wallet updateBalance(Wallet wallet, BigDecimal amount) {
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    public Wallet lockBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        wallet.setLockedBalance(wallet.getLockedBalance().add(amount));
        return walletRepository.save(wallet);
    }

    public Wallet unlockBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient locked balance");
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
        wallet.setBalance(wallet.getBalance().add(amount));
        return walletRepository.save(wallet);
    }

    public Wallet debitBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        return walletRepository.save(wallet);
    }

    public Optional<Wallet> getWalletByBlockchainAddress(String blockchainAddress) {
        return walletRepository.findByBlockchainAddress(blockchainAddress);
    }

    public Credentials getWalletCredentials(Wallet wallet) {
        String normalizedCurrency = wallet.getCurrency().toUpperCase(Locale.ROOT);
        String derivationKey = wallet.getUser().getId() + ":" + normalizedCurrency;
        int derivationIndex = Math.abs(derivationKey.hashCode()) % Integer.MAX_VALUE;
        return walletDerivationService.deriveCredentialsForUser((long) derivationIndex);
    }
}
