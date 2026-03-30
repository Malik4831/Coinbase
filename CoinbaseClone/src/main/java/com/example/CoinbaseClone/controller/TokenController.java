package com.example.CoinbaseClone.controller;

import com.example.CoinbaseClone.model.Token;
import com.example.CoinbaseClone.service.TokenService;
import com.example.CoinbaseClone.service.TokenRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/admin/tokens")
public class TokenController {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenRegistry tokenRegistry;

    @PostMapping
    public ResponseEntity<Token> addToken(@RequestParam String symbol,
                                        @RequestParam String name,
                                        @RequestParam String contractAddress,
                                        @RequestParam(required = false, defaultValue = "18") Integer decimals) {
        try {
            Token token = tokenService.addToken(symbol, name, contractAddress, decimals);
            tokenRegistry.refreshCache(); // Refresh cache after adding new token
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{tokenId}")
    public ResponseEntity<Token> updateToken(@PathVariable Long tokenId,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) String contractAddress,
                                           @RequestParam(required = false) Integer decimals,
                                           @RequestParam(required = false) Boolean isActive) {
        try {
            Token token = tokenService.updateToken(tokenId, name, contractAddress, decimals, isActive);
            tokenRegistry.refreshCache(); // Refresh cache after updating token
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{tokenId}/activate")
    public ResponseEntity<String> activateToken(@PathVariable Long tokenId) {
        try {
            tokenService.activateToken(tokenId);
            tokenRegistry.refreshCache();
            return ResponseEntity.ok("Token activated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{tokenId}/deactivate")
    public ResponseEntity<String> deactivateToken(@PathVariable Long tokenId) {
        try {
            tokenService.deactivateToken(tokenId);
            tokenRegistry.refreshCache();
            return ResponseEntity.ok("Token deactivated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<Token>> getAllTokens() {
        List<Token> tokens = tokenService.getAllTokens();
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Token>> getActiveTokens() {
        List<Token> tokens = tokenService.getActiveTokens();
        return ResponseEntity.ok(tokens);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Token> getTokenBySymbol(@PathVariable String symbol) {
        Token token = tokenRegistry.getTokenInfo(symbol);
        if (token != null) {
            return ResponseEntity.ok(token);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/addresses")
    public ResponseEntity<Map<String, String>> getTokenAddresses() {
        Map<String, String> addresses = tokenRegistry.getAllActiveTokenAddresses();
        return ResponseEntity.ok(addresses);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<String> deleteToken(@PathVariable Long tokenId) {
        try {
            tokenService.deleteToken(tokenId);
            tokenRegistry.refreshCache();
            return ResponseEntity.ok("Token deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-cache")
    public ResponseEntity<String> refreshCache() {
        tokenRegistry.refreshCache();
        return ResponseEntity.ok("Token cache refreshed successfully");
    }
}
