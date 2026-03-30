package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.Order;
import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.model.Wallet;
import com.example.CoinbaseClone.model.Transaction;
import com.example.CoinbaseClone.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.core.RemoteFunctionCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PlatformInventoryService platformInventoryService;

    @Transactional
    public Order createOrder(User user, Order.OrderType type, String baseCurrency,
                           String quoteCurrency, BigDecimal amount, BigDecimal price) {
        Order order = new Order();
        order.setUser(user);
        order.setType(type);
        order.setBaseCurrency(baseCurrency.toUpperCase());
        order.setQuoteCurrency(quoteCurrency.toUpperCase());
        order.setAmount(amount);
        order.setPrice(price);

        // For simplicity, assume market order if price is null
        if (price == null) {
            // Get current market price (mock)
            order.setPrice(getMarketPrice(baseCurrency, quoteCurrency));
        }

        // Lock balance for the order
        Wallet wallet = walletService.getWalletByUserAndCurrency(
                        user,
                        type == Order.OrderType.BUY ? order.getQuoteCurrency() : order.getBaseCurrency()
                )
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal lockAmount = type == Order.OrderType.BUY ? amount.multiply(order.getPrice()) : amount;
        walletService.lockBalance(wallet, lockAmount);

        return orderRepository.save(order);
    }

    @Transactional
    public Order executeOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be executed");
        }

        Wallet baseWallet = walletService.getWalletByUserAndCurrency(user, order.getBaseCurrency())
                .orElseThrow(() -> new RuntimeException("Base currency wallet not found"));
        Wallet quoteWallet = walletService.getWalletByUserAndCurrency(user, order.getQuoteCurrency())
                .orElseThrow(() -> new RuntimeException("Quote currency wallet not found"));
        Wallet platformBaseWallet = platformInventoryService.getOrCreatePlatformWallet(order.getBaseCurrency());
        Wallet platformQuoteWallet = platformInventoryService.getOrCreatePlatformWallet(order.getQuoteCurrency());

        BigDecimal quoteAmount = order.getAmount().multiply(order.getPrice());

        if (order.getType() == Order.OrderType.BUY) {
            finalizeLockedDebit(quoteWallet, quoteAmount);
            walletService.debitBalance(platformBaseWallet, order.getAmount());
            walletService.updateBalance(platformQuoteWallet, quoteAmount);
            walletService.updateBalance(baseWallet, order.getAmount());

            transactionService.createTransaction(
                    user,
                    Transaction.TransactionType.TRADE_BUY,
                    order.getBaseCurrency(),
                    order.getAmount(),
                    BigDecimal.ZERO
            );
        } else if (order.getType() == Order.OrderType.SELL) {
            finalizeLockedDebit(baseWallet, order.getAmount());
            walletService.debitBalance(platformQuoteWallet, quoteAmount);
            walletService.updateBalance(platformBaseWallet, order.getAmount());
            walletService.updateBalance(quoteWallet, quoteAmount);

            transactionService.createTransaction(
                    user,
                    Transaction.TransactionType.TRADE_SELL,
                    order.getBaseCurrency(),
                    order.getAmount(),
                    BigDecimal.ZERO
            );
        }

        order.setStatus(Order.OrderStatus.FILLED);
        order.setFilledAmount(order.getAmount());
        return orderRepository.save(order);
    }

    private void finalizeLockedDebit(Wallet wallet, BigDecimal amount) {
        if (wallet.getLockedBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient locked balance");
        }
        wallet.setLockedBalance(wallet.getLockedBalance().subtract(amount));
    }

    public List<Order> getUserOrders(User user) {
        return orderRepository.findByUser(user);
    }

    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);

        // Unlock balance
        Wallet wallet = walletService.getWalletByUserAndCurrency(user,
                order.getType() == Order.OrderType.BUY ? order.getQuoteCurrency() : order.getBaseCurrency())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        BigDecimal unlockAmount = order.getType() == Order.OrderType.BUY ?
                order.getAmount().multiply(order.getPrice()) : order.getAmount();
        walletService.unlockBalance(wallet, unlockAmount);

        return orderRepository.save(order);
    }

    private BigDecimal getMarketPrice(String base, String quote) {
        try {
            // CoinGecko API free endpoint - no authentication required
            String coinId = getCoinIdFromSymbol(base);
            String quoteCurrency = quote.toLowerCase();
            
            String url = String.format(
                "https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=%s",
                coinId, quoteCurrency
            );

            // Make HTTP request to CoinGecko API
            String response = restTemplate.getForObject(url, String.class);
            
            // Parse JSON response manually
            return parsePrice(response, coinId, quoteCurrency);
            
        } catch (Exception e) {
            // Fallback to default price if API fails
            System.err.println("Failed to fetch market price: " + e.getMessage());
            return getDefaultPrice(base, quote);
        }
    }

    /**
     * Maps cryptocurrency symbol to CoinGecko ID
     */
    private String getCoinIdFromSymbol(String symbol) {
        return switch (symbol.toUpperCase()) {
            case "BTC" -> "bitcoin";
            case "ETH" -> "ethereum";
            case "USDT" -> "tether";
            case "USDC" -> "usd-coin";
            case "BNB" -> "binancecoin";
            case "SOL" -> "solana";
            case "XRP" -> "ripple";
            case "ADA" -> "cardano";
            case "DOGE" -> "dogecoin";
            case "DAN" -> "ethereum"; // Default to ETH for our custom token
            default -> symbol.toLowerCase();
        };
    }

    /**
     * Parses price from JSON response string
     */
    private BigDecimal parsePrice(String jsonResponse, String coinId, String currency) {
        try {
            // Simple JSON parsing for CoinGecko response
            // Expected format: {"bitcoin":{"usd":45000}}
            String searchKey = "\"" + currency + "\":";
            int keyIndex = jsonResponse.indexOf(searchKey);
            
            if (keyIndex == -1) {
                return getDefaultPrice(coinId, currency);
            }
            
            // Find the price value after the key
            int startIndex = jsonResponse.indexOf(":", keyIndex) + 1;
            int endIndex = jsonResponse.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = jsonResponse.indexOf("}", startIndex);
            }
            
            String priceStr = jsonResponse.substring(startIndex, endIndex).trim();
            return new BigDecimal(priceStr);
            
        } catch (Exception e) {
            return getDefaultPrice(coinId, currency);
        }
    }

    /**
     * Returns fallback prices if API fails
     */
    private BigDecimal getDefaultPrice(String base, String quote) {
        // Fallback prices in case API is unavailable
        if ("ethereum".equalsIgnoreCase(base) || "ETH".equalsIgnoreCase(base)) {
            return "usd".equalsIgnoreCase(quote) ? new BigDecimal("3000") : new BigDecimal("1");
        }
        if ("bitcoin".equalsIgnoreCase(base) || "BTC".equalsIgnoreCase(base)) {
            return "usd".equalsIgnoreCase(quote) ? new BigDecimal("65000") : new BigDecimal("1");
        }
        return new BigDecimal("1");
    }

    public Order createAndExecuteOrder(User user, Order.OrderType type, String baseCurrency,
                                      String quoteCurrency, BigDecimal amount, BigDecimal price) {
        // Create the order first
        Order order = createOrder(user, type, baseCurrency, quoteCurrency, amount, price);
        
        // Then execute it immediately
        return executeOrder(order.getId(), user);
    }

    public RemoteFunctionCall<String> getTokenName(String contractAddress) throws Exception {
        return blockchainService.getTokenName(contractAddress);
    }

    public String getTokenSymbol(String contractAddress) throws Exception {
        return blockchainService.getTokenSymbol(contractAddress);
    }

    public BigInteger getTokenBalance(String contractAddress, String walletAddress) throws Exception {
        return blockchainService.getTokenBalance(contractAddress,walletAddress);
    }
}
