package com.example.CoinbaseClone.service;

import com.example.CoinbaseClone.model.User;
import com.example.CoinbaseClone.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private WalletDerivationService walletDerivationService;

    @Value("${app.admin.email:abumalik4831@gmail.com}")
    private String adminEmail;

    public User registerUser(String email, String password, String firstName, String lastName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(resolveRoleForEmail(email));

        // Save user first to get ID, then derive address
        User savedUser = userRepository.save(user);

        // Derive unique blockchain address using user ID
        String walletAddress = walletDerivationService.deriveAddressForUser(savedUser.getId());
        savedUser.setWalletAddress(walletAddress);

        return userRepository.save(savedUser);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) throws org.springframework.security.core.userdetails.UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(normalizeRole(user))
                .build();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String normalizeRole(User user) {
        if (user.getRole() != null && !user.getRole().isBlank()) {
            return user.getRole().toUpperCase(Locale.ROOT);
        }
        return resolveRoleForEmail(user.getEmail());
    }

    private String resolveRoleForEmail(String email) {
        if (email != null && email.equalsIgnoreCase(adminEmail)) {
            return "ADMIN";
        }
        return "USER";
    }
}
