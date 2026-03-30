package com.example.CoinbaseClone.controller;

import com.example.CoinbaseClone.dto.WalletResponse;
import com.example.CoinbaseClone.model.Wallet;
import com.example.CoinbaseClone.service.PlatformInventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/admin/platform-inventory")
public class PlatformInventoryController {

    @Autowired
    private PlatformInventoryService platformInventoryService;

    @GetMapping
    public ResponseEntity<?> getPlatformInventory() {
        try {
            List<Wallet> wallets = platformInventoryService.getPlatformWallets();
            return ResponseEntity.ok(Map.of(
                    "platformUserEmail", platformInventoryService.getPlatformUserEmail(),
                    "walletCount", wallets.size(),
                    "wallets", WalletResponse.fromList(wallets)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to fetch platform inventory: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/wallets")
    public ResponseEntity<?> createPlatformWallet(@RequestParam String currency) {
        try {
            Wallet wallet = platformInventoryService.createPlatformWallet(currency);
            return ResponseEntity.ok(WalletResponse.from(wallet));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to create platform wallet: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seedInventory(@RequestParam String currency, @RequestParam BigDecimal amount) {
        try {
            Wallet wallet = platformInventoryService.seedInventory(currency, amount);
            return ResponseEntity.ok(Map.of(
                    "message", "Platform inventory funded successfully",
                    "wallet", WalletResponse.from(wallet)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to seed platform inventory: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrapWallets(@RequestParam List<String> currencies) {
        try {
            List<Wallet> wallets = platformInventoryService.bootstrapWallets(currencies);
            return ResponseEntity.ok(Map.of(
                    "message", "Platform wallets bootstrapped successfully",
                    "walletCount", wallets.size(),
                    "wallets", WalletResponse.fromList(wallets)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to bootstrap platform wallets: " + e.getMessage()
            ));
        }
    }
}
