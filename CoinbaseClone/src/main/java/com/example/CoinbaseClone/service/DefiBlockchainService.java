package com.example.CoinbaseClone.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.model.DecentralizedCoin;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.NoOpProcessor;

import java.math.BigInteger;
import java.util.Collections;


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
        String encodedFunction = FunctionEncoder.encode(
                new Function(
                        DecentralizedCoin.FUNC_TRANSFER,
                        java.util.Arrays.<Type>asList(
                                new Address(160, to),
                                new Uint256(amount)
                        ),
                        Collections.emptyList()
                )
        );

        EthSendTransaction txResponse = transactionManager(signingCredentials).sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        return extractAndWaitForReceipt(txResponse, "Transfer");
    }

    public TransactionReceipt transferTokensFrom(String contractAddress,String from,String to, BigInteger amount) throws Exception {
        String encodedFunction = FunctionEncoder.encode(
                new Function(
                        DecentralizedCoin.FUNC_TRANSFERFROM,
                        java.util.Arrays.<Type>asList(
                                new Address(160, from),
                                new Address(160, to),
                                new Uint256(amount)
                        ),
                        Collections.emptyList()
                )
        );

        EthSendTransaction txResponse = transactionManager(credentials).sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        return extractAndWaitForReceipt(txResponse, "transferFrom");
    }

    public TransactionReceipt mintTokens(String contractAddress,String to, BigInteger amount) throws Exception {
        String encodedFunction = FunctionEncoder.encode(
                new Function(
                        DecentralizedCoin.FUNC_MINT,
                        java.util.Arrays.<Type>asList(
                                new Address(160, to),
                                new Uint256(amount)
                        ),
                        Collections.emptyList()
                )
        );

        EthSendTransaction txResponse = transactionManager(credentials).sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        return extractAndWaitForReceipt(txResponse, "Mint");
    }

    public TransactionReceipt burnTokens(String contractAddress,String from,BigInteger amount) throws Exception {
        String encodedFunction = FunctionEncoder.encode(
                new Function(
                        DecentralizedCoin.FUNC_burn,
                        java.util.Arrays.<Type>asList(
                                new Address(160, from),
                                new Uint256(amount)
                        ),
                        Collections.emptyList()
                )
        );

        EthSendTransaction txResponse = transactionManager(credentials).sendTransaction(
                DefaultGasProvider.GAS_PRICE,
                DefaultGasProvider.GAS_LIMIT,
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
        );

        return extractAndWaitForReceipt(txResponse, "Burn");
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

    private TransactionReceipt waitForReceipt(String transactionHash) throws Exception {
        int attempts = 30;
        long pollDelayMs = 2_000L;

        for (int attempt = 0; attempt < attempts; attempt++) {
            EthGetTransactionReceipt receiptResponse = web3j.ethGetTransactionReceipt(transactionHash).send();
            if (receiptResponse != null && receiptResponse.getTransactionReceipt().isPresent()) {
                return receiptResponse.getTransactionReceipt().get();
            }

            Thread.sleep(pollDelayMs);
        }

        throw new IllegalStateException("Mint transaction submitted but no receipt was mined within 60 seconds. Transaction hash: " + transactionHash);
    }

    private TransactionReceipt extractAndWaitForReceipt(EthSendTransaction txResponse, String action) throws Exception {
        if (txResponse == null) {
            throw new IllegalStateException(action + " transaction did not receive a response from the RPC provider");
        }

        if (txResponse.hasError()) {
            throw new IllegalStateException(action + " transaction rejected by RPC provider: " + txResponse.getError().getMessage());
        }

        String transactionHash = txResponse.getTransactionHash();
        if (transactionHash == null || transactionHash.isBlank()) {
            throw new IllegalStateException(action + " transaction was submitted without a transaction hash");
        }

        return waitForReceipt(transactionHash);
    }


}
