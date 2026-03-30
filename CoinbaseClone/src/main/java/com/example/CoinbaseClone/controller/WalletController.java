package com.example.CoinbaseClone.controller;

import com.example.CoinbaseClone.dto.WalletResponse;
import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.model.Wallet;
import com.example.CoinbaseClone.model.Transaction;
import com.example.CoinbaseClone.service.BlockchainService;
import com.example.CoinbaseClone.service.TransactionService;
import com.example.CoinbaseClone.service.UserService;
import com.example.CoinbaseClone.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private TransactionService transactionService;

    @GetMapping("/me")
    public ResponseEntity<List<WalletResponse>> getCurrentUserWallets(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<Wallet> wallets = walletService.getUserWallets(user);
        return ResponseEntity.ok(WalletResponse.fromList(wallets));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<WalletResponse>> getUserWallets(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        List<Wallet> wallets = walletService.getUserWallets(user);
        return ResponseEntity.ok(WalletResponse.fromList(wallets));
    }

    @PostMapping("/me")
    public ResponseEntity<WalletResponse> createWalletForCurrentUser(@RequestParam String currency,
                                                                     Authentication authentication) {
        User user = getCurrentUser(authentication);
        Wallet wallet = walletService.createWallet(user, currency);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @PostMapping("/{userId}")
    public ResponseEntity<WalletResponse> createWallet(@PathVariable Long userId, @RequestParam String currency) {
        User user = userService.getUserById(userId);
        Wallet wallet = walletService.createWallet(user, currency);
        return ResponseEntity.ok(WalletResponse.from(wallet));
    }

    @PostMapping({"/deposit", "/deploy"})
    public ResponseEntity<?> deposit(
            @RequestParam String currency,
            @RequestParam(required = false) String contractAddress,
            @RequestParam(required = true) String fromAddress,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "18") Integer decimals,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        Wallet wallet = walletService.getWalletByUserAndCurrency(user, currency)
                .orElseGet(() -> walletService.createWallet(user, currency));
        BigInteger onChainAmount = null;
        String resolvedContractAddress = null;
        String signerAddress = null;
        try {
            resolvedContractAddress = blockchainService.resolveForDiagnostics(contractAddress);
            signerAddress = blockchainService.getProtocolSpenderAddress();
            onChainAmount = toTokenUnits(amount, decimals);
            TransactionReceipt receipt = blockchainService.transferTokensFrom(contractAddress,
                    fromAddress,
                    wallet.getBlockchainAddress(),
                    onChainAmount
            );

            walletService.updateBalance(wallet, amount);
            transactionService.recordBlockchainTransaction(
                    user,
                    Transaction.TransactionType.DEPOSIT,
                    currency.toUpperCase(),
                    amount,
                    receipt.getTransactionHash()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Deposit successful",
                    "currency", currency.toUpperCase(),
                    "amount", amount,
                    "decimals", decimals,
                    "onChainAmount", onChainAmount.toString(),
                    "contractAddress", resolvedContractAddress,
                    "spenderAddress", signerAddress,
                    "fromAddress", fromAddress,
                    "toAddress", wallet.getBlockchainAddress(),
                    "txHash", receipt.getTransactionHash()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to deposit: " + e.getMessage(),
                    "details", Map.ofEntries(
                            Map.entry("currency", currency.toUpperCase()),
                            Map.entry("requestedContractAddress", contractAddress == null ? "" : contractAddress),
                            Map.entry("resolvedContractAddress", resolvedContractAddress == null ? "" : resolvedContractAddress),
                            Map.entry("spenderAddress", signerAddress == null ? "" : signerAddress),
                            Map.entry("fromAddress", fromAddress == null ? "" : fromAddress),
                            Map.entry("toAddress", wallet.getBlockchainAddress() == null ? "" : wallet.getBlockchainAddress()),
                            Map.entry("amount", amount == null ? "" : amount.toPlainString()),
                            Map.entry("decimals", decimals == null ? "" : decimals),
                            Map.entry("onChainAmount", onChainAmount == null ? "" : onChainAmount.toString()),
                            Map.entry("userId", user.getId() == null ? "" : user.getId()),
                            Map.entry("walletId", wallet.getId() == null ? "" : wallet.getId())
                    )
            ));
        }
    }

    @GetMapping("/deposit-context")
    public ResponseEntity<?> getDepositContext(
            @RequestParam String currency,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Wallet wallet = walletService.getWalletByUserAndCurrency(user, currency)
                    .orElseGet(() -> walletService.createWallet(user, currency));

            return ResponseEntity.ok(Map.of(
                    "currency", currency.toUpperCase(),
                    "depositAddress", wallet.getBlockchainAddress(),
                    "spenderAddress", blockchainService.getProtocolSpenderAddress()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to load deposit context: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/mint-token")
    public ResponseEntity<?> mintToken(
            @RequestParam String currency,
            @RequestParam String contractAddress,
            @RequestParam String toAddress,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "18") Integer decimals) {
        try {
            BigInteger onChainAmount = toTokenUnits(amount, decimals);
            TransactionReceipt receipt = blockchainService.mintTokens(contractAddress, toAddress, onChainAmount);

            return ResponseEntity.ok(Map.of(
                    "message", "Token minted successfully",
                    "currency", currency.toUpperCase(),
                    "contractAddress", blockchainService.resolveForDiagnostics(contractAddress),
                    "toAddress", toAddress,
                    "amount", amount.toPlainString(),
                    "decimals", decimals,
                    "onChainAmount", onChainAmount.toString(),
                    "txHash", receipt.getTransactionHash()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to mint token: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(
            @RequestParam String currency,
            @RequestParam(required = false) String contractAddress,
            @RequestParam String toAddress,
            @RequestParam BigDecimal amount,
            @RequestParam(defaultValue = "18") Integer decimals,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Wallet wallet = walletService.getWalletByUserAndCurrency(user, currency)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            BigInteger onChainAmount = toTokenUnits(amount, decimals);
            TransactionReceipt receipt;
            if ((contractAddress == null || contractAddress.isBlank()) && "ETH".equalsIgnoreCase(currency)) {
                throw new RuntimeException("Native ETH withdrawal is not implemented yet");
            }

            receipt = blockchainService.transferTokens(
                    contractAddress,
                    walletService.getWalletCredentials(wallet),
                    toAddress,
                    onChainAmount
            );

            walletService.debitBalance(wallet, amount);
            transactionService.recordBlockchainTransaction(
                    user,
                    Transaction.TransactionType.WITHDRAWAL,
                    currency.toUpperCase(),
                    amount,
                    receipt.getTransactionHash()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Withdrawal successful",
                    "currency", currency.toUpperCase(),
                    "amount", amount,
                    "toAddress", toAddress,
                    "txHash", receipt.getTransactionHash()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to withdraw: " + e.getMessage()
            ));
        }
    }

    private User getCurrentUser(Authentication authentication) {
        return userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private BigInteger toTokenUnits(BigDecimal amount, Integer decimals) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (decimals == null || decimals < 0) {
            throw new IllegalArgumentException("Decimals must be zero or greater");
        }

        return amount
                .movePointRight(decimals)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toBigIntegerExact();
    }
}
