package com.example.CoinbaseClone.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;

@Service
public class WalletDerivationService {

    @Value("${wallet.master-mnemonic:abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about}")
    private String masterMnemonic;

    @Value("${wallet.master-password:}")
    private String masterPassword;

    /**
     * Derives a unique Ethereum address for a user based on their ID
     * Uses BIP44 derivation path: m/44'/60'/0'/0/{userId}
     *
     * @param userId The user's unique identifier
     * @return Ethereum address (0x...)
     */
    public String deriveAddressForUser(Long userId) {
        try {
            // Generate master seed from mnemonic
            byte[] seed = MnemonicUtils.generateSeed(masterMnemonic, masterPassword);

            // Create master key pair from seed
            Bip32ECKeyPair masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed);

            // BIP44 derivation path for Ethereum: m/44'/60'/0'/0/{userId}
            // 44' - BIP44 purpose
            // 60' - Ethereum coin type
            // 0' - Account 0
            // 0 - Chain 0 (external)
            // userId - Address index (unique per user)

            int[] derivationPath = {
                44 | Bip32ECKeyPair.HARDENED_BIT,
                60 | Bip32ECKeyPair.HARDENED_BIT,
                0 | Bip32ECKeyPair.HARDENED_BIT,
                0,
                userId.intValue()
            };

            // Derive the child key pair
            Bip32ECKeyPair childKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, derivationPath);

            // Generate credentials and get address
            Credentials credentials = Credentials.create(childKeyPair);
            return credentials.getAddress();

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive wallet address for user: " + userId, e);
        }
    }

    /**
     * Derives credentials for a user (contains private key)
     * WARNING: Only use this for signing transactions, never expose private keys
     *
     * @param userId The user's unique identifier
     * @return Credentials object with private key and address
     */
    public Credentials deriveCredentialsForUser(Long userId) {
        try {
            byte[] seed = MnemonicUtils.generateSeed(masterMnemonic, masterPassword);
            Bip32ECKeyPair masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed);

            int[] derivationPath = {
                44 | Bip32ECKeyPair.HARDENED_BIT,
                60 | Bip32ECKeyPair.HARDENED_BIT,
                0 | Bip32ECKeyPair.HARDENED_BIT,
                0,
                userId.intValue()
            };

            Bip32ECKeyPair childKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, derivationPath);
            return Credentials.create(childKeyPair);

        } catch (Exception e) {
            throw new RuntimeException("Failed to derive credentials for user: " + userId, e);
        }
    }

    /**
     * Validates if an address belongs to a user
     * This is useful for security checks
     *
     * @param userId The user's ID
     * @param address The address to validate
     * @return true if the address belongs to the user
     */
    public boolean validateUserAddress(Long userId, String address) {
        String expectedAddress = deriveAddressForUser(userId);
        return expectedAddress.equalsIgnoreCase(address);
    }
}