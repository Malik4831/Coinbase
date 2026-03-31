package com.example.CoinbaseClone.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.model.DecentralizedCoin;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.NoOpProcessor;

import java.math.BigInteger;


@Service
public class DefiBlockchainService {
    private final Web3j web3j;
    private final Credentials credentials;


    @Autowired
    public DefiBlockchainService(Web3j web3j, Credentials credentials) throws Exception {
        this.web3j = web3j;
        this.credentials = credentials;

    }

    public String getSignerAddress() {
        return credentials.getAddress();
    }


    public DecentralizedCoin loadContract(String contractAddress)throws Exception {
        return DecentralizedCoin.load(
                contractAddress,
                web3j,
                transactionManager(credentials),
                new DefaultGasProvider()
        );
    }

    public DecentralizedCoin loadContract(String contractAddress, Credentials signingCredentials) throws Exception {
        return DecentralizedCoin.load(
                contractAddress,
                web3j,
                transactionManager(signingCredentials),
                new DefaultGasProvider()
        );
    }

    public DecentralizedCoin loadReadonlyContract(String contractAddress) throws Exception {
        return DecentralizedCoin.load(
                contractAddress,
                web3j,
                new ReadonlyTransactionManager(web3j, credentials.getAddress()),
                new DefaultGasProvider()
        );
    }


    public BigInteger getTokenBalance(String walletAddress,String contractAddress) throws Exception {
        DecentralizedCoin loadedContract = loadReadonlyContract(contractAddress);
        return loadedContract.balanceOf( walletAddress).send();
    }



    public TransactionReceipt transferTokens(String contractAddress, Credentials signingCredentials, String to, BigInteger amount) throws Exception {
        DecentralizedCoin loadedContract = loadContract(contractAddress, signingCredentials);
        return loadedContract.transfer(to, amount).send();
    }

    public TransactionReceipt transferTokensFrom(String contractAddress,String from,String to, BigInteger amount) throws Exception {
        DecentralizedCoin loadedContract = loadContract(contractAddress);

        return loadedContract.transferFrom(from,to,amount).send();
    }

    public TransactionReceipt mintTokens(String contractAddress,String to, BigInteger amount) throws Exception {
        DecentralizedCoin loadedContract = loadContract(contractAddress);

        return loadedContract.mint( to, amount).send();
    }

    public TransactionReceipt burnTokens(String contractAddress,String from,BigInteger amount) throws Exception {
        DecentralizedCoin loadedContract = loadContract(contractAddress);

        return loadedContract.burn(from, amount).send();
    }

    public RemoteFunctionCall<String> getTokenName(String contractAddress) throws Exception {
        DecentralizedCoin loadedContract = loadReadonlyContract(contractAddress);
        return loadedContract.name();
    }

    public String getTokenSymbol(String contractAddress) throws Exception {
        DecentralizedCoin loadedContract = loadReadonlyContract(contractAddress);

        return loadedContract.symbol().send();
    }

    private TransactionManager transactionManager(Credentials signingCredentials) {
        return new RawTransactionManager(
                web3j,
                signingCredentials,
                resolveChainId(),
                new NoOpProcessor(web3j)
        );
    }

    private long resolveChainId() {
        try {
            var chainIdResponse = web3j.ethChainId().send();
            if (chainIdResponse != null && chainIdResponse.getChainId() != null) {
                return chainIdResponse.getChainId().longValueExact();
            }

            var netVersionResponse = web3j.netVersion().send();
            if (netVersionResponse != null && netVersionResponse.getNetVersion() != null) {
                return Long.parseLong(netVersionResponse.getNetVersion());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve chain id from the configured RPC provider", e);
        }

        throw new IllegalStateException("Configured RPC provider did not return a chain id or network version");
    }


}
