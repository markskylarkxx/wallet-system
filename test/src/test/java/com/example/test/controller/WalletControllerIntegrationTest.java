package com.example.test.controller;

import com.example.test.dto.DoTransDto;
import com.example.test.model.Account;
import com.example.test.model.User;
import com.example.test.model.WalletBalance;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.UserRepo;
import com.example.test.repo.WalletBalanceRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WalletControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private WalletBalanceRepo walletBalanceRepo;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepo.deleteAll();
        accountRepo.deleteAll();
        walletBalanceRepo.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setEmail("integration@test.com");

        // Save user
        testUser = userRepo.save(testUser);

        // Create wallet balance
        WalletBalance walletBalance = new WalletBalance();
        walletBalance.setAmount(BigDecimal.valueOf(1000));
        walletBalance = walletBalanceRepo.save(walletBalance);

        // Create account
        testAccount = new Account();
        testAccount.setAccountNumber("INTEGRATION123");
        testAccount.setUser(testUser);
        testAccount.setWalletBalance(walletBalance);
        testAccount = accountRepo.save(testAccount);

        testUser.setAccount(testAccount);
        userRepo.save(testUser);
    }

    @Test
    void createUser_Success() throws Exception {
        User newUser = new User();
        newUser.setEmail("newuser@test.com");

        mockMvc.perform(post("/api/v1/wallet/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User and account created successfully"))
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));
    }

    @Test
    void createUser_DuplicateEmail() throws Exception {
        User duplicateUser = new User();
        duplicateUser.setEmail("integration@test.com"); // Already exists

        mockMvc.perform(post("/api/v1/wallet/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User with email integration@test.com already exists"));
    }

    @Test
    void transfer_Success() throws Exception {
        // Create destination account
        User destUser = new User();
        destUser.setEmail("dest@test.com");
        destUser = userRepo.save(destUser);

        WalletBalance destWallet = new WalletBalance();
        destWallet.setAmount(BigDecimal.valueOf(500));
        destWallet = walletBalanceRepo.save(destWallet);

        Account destAccount = new Account();
        destAccount.setAccountNumber("DEST123");
        destAccount.setUser(destUser);
        destAccount.setWalletBalance(destWallet);
        destAccount = accountRepo.save(destAccount);

        destUser.setAccount(destAccount);
        userRepo.save(destUser);

        DoTransDto transferDto = new DoTransDto();
        transferDto.setFromAccount(testAccount.getAccountNumber());
        transferDto.setToAccount(destAccount.getAccountNumber());
        transferDto.setAmount(BigDecimal.valueOf(300));

        mockMvc.perform(post("/api/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Transfer completed successfully"));
    }

    @Test
    void transfer_InsufficientBalance() throws Exception {
        // Create destination account
        Account destAccount = new Account();
        destAccount.setAccountNumber("DEST456");
        destAccount.setUser(new User());
        destAccount.setWalletBalance(new WalletBalance());
        destAccount = accountRepo.save(destAccount);

        DoTransDto transferDto = new DoTransDto();
        transferDto.setFromAccount(testAccount.getAccountNumber());
        transferDto.setToAccount(destAccount.getAccountNumber());
        transferDto.setAmount(BigDecimal.valueOf(5000)); // More than available

        mockMvc.perform(post("/api/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient balance")));
    }

    @Test
    void transfer_SameAccount() throws Exception {
        DoTransDto transferDto = new DoTransDto();
        transferDto.setFromAccount(testAccount.getAccountNumber());
        transferDto.setToAccount(testAccount.getAccountNumber());
        transferDto.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/v1/wallet/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot transfer to the same account"));
    }
}