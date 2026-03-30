package com.example.CoinbaseClone.dto;

import com.example.CoinbaseClone.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletResponse(
        Long id,
        Long userId,
        String currency,
        BigDecimal balance,
        BigDecimal lockedBalance,
        String blockchainAddress
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getUser() != null ? wallet.getUser().getId() : null,
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getLockedBalance(),
                wallet.getBlockchainAddress()
        );
    }

    public static List<WalletResponse> fromList(List<Wallet> wallets) {
        return wallets.stream()
                .map(WalletResponse::from)
                .toList();
    }
}
