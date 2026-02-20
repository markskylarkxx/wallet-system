package com.example.test.service;

import com.example.test.dto.DoTransDto;
import com.example.test.dto.FundAccountDto;
import com.example.test.model.Account;
import com.example.test.model.User;
import com.example.test.model.WalletBalance;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.UserRepo;
import com.example.test.repo.WalletBalanceRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class DoService implements ServiceCall {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private WalletBalanceRepo walletBalanceRepo;

    @Override
    @Transactional
    public void createUserAndAccount(User user) {
        log.info("Creating user and account for email: {}", user.getEmail());

        // Check if user already exists
        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("User with email " + user.getEmail() + " already exists");
        }

        // Save user first
        User savedUser = userRepo.save(user);

        // Create wallet balance
        WalletBalance walletBalance = new WalletBalance();
        walletBalance.setAmount(BigDecimal.ZERO);
        WalletBalance savedWalletBalance = walletBalanceRepo.save(walletBalance);

        // Create account with unique account number
        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setUser(savedUser);
        account.setWalletBalance(savedWalletBalance);

        Account savedAccount = accountRepo.save(account);

        // Set the account reference in user
        savedUser.setAccount(savedAccount);
        userRepo.save(savedUser);

        log.info("User and account created successfully. Account number: {}", savedAccount.getAccountNumber());
    }

    @Override
    @Transactional
    public void doIntraTransfer(DoTransDto request) {
        log.info("Processing intra transfer from account: {} to account: {}, amount: {}",
                request.getFromAccount(), request.getToAccount(), request.getAmount());

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Transfer amount must be greater than zero");
        }

        // Get source account with pessimistic lock to prevent race conditions
        Account fromAccount = accountRepo.findByAccountNumberWithLock(request.getFromAccount())
                .orElseThrow(() -> new RuntimeException("Source account not found: " + request.getFromAccount()));

        // Get destination account
        Account toAccount = accountRepo.findByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new RuntimeException("Destination account not found: " + request.getToAccount()));

        // Check if source and destination are different
        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        // Get wallet balances
        WalletBalance fromWallet = fromAccount.getWalletBalance();
        WalletBalance toWallet = toAccount.getWalletBalance();

        // Check sufficient balance
        if (fromWallet.getAmount().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance in source account. Available: " +
                    fromWallet.getAmount() + ", Requested: " + request.getAmount());
        }

        // Perform transfer
        fromWallet.setAmount(fromWallet.getAmount().subtract(request.getAmount()));
        toWallet.setAmount(toWallet.getAmount().add(request.getAmount()));

        // Save updated balances
        walletBalanceRepo.save(fromWallet);
        walletBalanceRepo.save(toWallet);

        log.info("Transfer completed successfully. Transaction reference: {}", generateTransactionReference());
    }


    @Override
    @Transactional
    public void fundAccount(FundAccountDto request) {
        log.info("Funding account: {} with amount: {}", request.getAccountNumber(), request.getAmount());

        // Validate amount
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Funding amount must be greater than zero");
        }

        // Get account with pessimistic lock
        Account account = accountRepo.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountNumber()));

        // Get wallet balance
        WalletBalance walletBalance = account.getWalletBalance();

        // Add funds
        walletBalance.setAmount(walletBalance.getAmount().add(request.getAmount()));

        // Save updated balance
        walletBalanceRepo.save(walletBalance);

        log.info("Account {} funded successfully. New balance: {}",
                request.getAccountNumber(), walletBalance.getAmount());
    }

    private String generateAccountNumber() {
        return "ACCT" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String generateTransactionReference() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}