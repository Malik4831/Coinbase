package com.example.CoinbaseClone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import java.math.BigInteger;

@Service
public class BlockchainService {

    @Autowired
    private DefiBlockchainService defiBlockchainService;

    @Value("${web3j.contract-address}")
    private String defaultContractAddress;

    public BigInteger getTokenBalance(String contractAddress, String walletAddress) throws Exception {
        return defiBlockchainService.getTokenBalance(resolveContractAddress(contractAddress), walletAddress);
    }



    public TransactionReceipt transferTokens(String contractAddress, Credentials signingCredentials, String to, BigInteger amount) throws Exception {
        return defiBlockchainService.transferTokens(resolveContractAddress(contractAddress), signingCredentials, to, amount);
    }

    public TransactionReceipt transferTokensFrom(String contractAddress,String from, String to, BigInteger amount) throws Exception {
        return defiBlockchainService.transferTokensFrom(resolveContractAddress(contractAddress), from,to, amount);
    }

    public TransactionReceipt mintTokens(String contractAddress, String to, BigInteger amount) throws Exception {
        return defiBlockchainService.mintTokens(resolveContractAddress(contractAddress), to, amount);
    }

    public TransactionReceipt burnTokens(String contractAddress,String from, BigInteger amount) throws Exception {
        return defiBlockchainService.burnTokens(resolveContractAddress(contractAddress),from, amount);
    }

    public RemoteFunctionCall<String> getTokenName(String contractAddress) throws Exception {
        return defiBlockchainService.getTokenName(resolveContractAddress(contractAddress));
    }

    public String getTokenSymbol(String contractAddress) throws Exception {
        return defiBlockchainService.getTokenSymbol(resolveContractAddress(contractAddress));
    }

    public String getProtocolSpenderAddress() {

        return defiBlockchainService.getSignerAddress();
    }

    public String resolveForDiagnostics(String contractAddress) {
        return resolveContractAddress(contractAddress);
    }

    private String resolveContractAddress(String contractAddress) {
        if (contractAddress == null || contractAddress.isBlank()) {
            if (defaultContractAddress == null || defaultContractAddress.isBlank()) {
                throw new IllegalStateException("No contract address configured for blockchain operation");
            }
            return defaultContractAddress;
        }
        return contractAddress;
    }
}
