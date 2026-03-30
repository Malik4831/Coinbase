package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.Transaction;
import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public Transaction createTransaction(User user, Transaction.TransactionType type, 
                                        String currency, BigDecimal amount, BigDecimal fee) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setCurrency(currency);
        transaction.setAmount(amount);
        transaction.setFee(fee);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        return transactionRepository.save(transaction);
    }

    public Transaction recordBlockchainTransaction(User user, Transaction.TransactionType type,
                                                   String currency, BigDecimal amount, String txHash) {
        Transaction transaction = createTransaction(user, type, currency, amount, BigDecimal.ZERO);
        transaction.setBlockchainTxHash(txHash);
        transaction.setStatus(Transaction.TransactionStatus.CONFIRMED);
        
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransactionStatus(Long txId, Transaction.TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        transaction.setStatus(status);
        return transactionRepository.save(transaction);
    }

    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }
}
