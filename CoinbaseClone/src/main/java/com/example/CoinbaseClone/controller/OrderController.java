package com.example.CoinbaseClone.controller;

import com.example.CoinbaseClone.dto.OrderResponse;
import com.example.CoinbaseClone.model.Order;
import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.repository.UserRepository;
import com.example.CoinbaseClone.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.web3j.protocol.core.RemoteFunctionCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/create-and-execute")
    public ResponseEntity<?> createAndExecuteOrder(
            @RequestParam String baseCurrency,
            @RequestParam String quoteCurrency,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam String type,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Order.OrderType orderType = Order.OrderType.valueOf(type.toUpperCase());
            
            Order order = orderService.createAndExecuteOrder(
                user,
                orderType,
                baseCurrency,
                quoteCurrency,
                amount,
                price
            );

            return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "status", order.getStatus(),
                "filledAmount", order.getFilledAmount(),
                "message", "Order successfully executed"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to execute order: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(
            @RequestParam String baseCurrency,
            @RequestParam String quoteCurrency,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam String type,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Order.OrderType orderType = Order.OrderType.valueOf(type.toUpperCase());
            
            Order order = orderService.createOrder(
                user,
                orderType,
                baseCurrency,
                quoteCurrency,
                amount,
                price
            );

            return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "status", order.getStatus(),
                "message", "Order created successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to create order: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/execute")
    public ResponseEntity<?> executeOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Order order = orderService.executeOrder(orderId, user);

            return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "status", order.getStatus(),
                "filledAmount", order.getFilledAmount(),
                "message", "Order executed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to execute order: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            Order order = orderService.cancelOrder(orderId, user);

            return ResponseEntity.ok(Map.of(
                "orderId", order.getId(),
                "status", order.getStatus(),
                "message", "Order cancelled successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to cancel order: " + e.getMessage()
            ));
        }
    }

    @GetMapping("getUserOrders")
    public ResponseEntity<?> getUserOrders(Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<Order> orders = orderService.getUserOrders(user);
            
            return ResponseEntity.ok(Map.of(
                "orders", OrderResponse.fromList(orders),
                "count", orders.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch orders: " + e.getMessage()
            ));
        }


    }

    @GetMapping("getTokenName")

    public RemoteFunctionCall<String> getTokenName(String contractAddress) throws Exception {
        return orderService.getTokenName(contractAddress);
    }
@GetMapping("getTokenSymbol")
    public String getTokenSymbol(String contractAddress) throws Exception {
        return orderService.getTokenSymbol(contractAddress);
    }
    @GetMapping("getTokenBalance")
    public BigInteger getTokenBalance(String contractAddress, String walletAddress) throws Exception {
        return orderService.getTokenBalance(contractAddress,walletAddress);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(
            @PathVariable Long orderId,
            Authentication authentication) {
        try {
            User user = getCurrentUser(authentication);
            List<Order> orders = orderService.getUserOrders(user);
            Order order = orders.stream()
                    .filter(o -> o.getId().equals(orderId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            
            return ResponseEntity.ok(OrderResponse.from(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch order: " + e.getMessage()
            ));
        }
    }
}
