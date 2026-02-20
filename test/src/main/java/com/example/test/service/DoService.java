package com.example.test.service;

import com.example.test.dto.DoTransDto;
import com.example.test.dto.FundAccountDto;
import com.example.test.exception.*;
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

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new DuplicateEmailException(user.getEmail());
        }

        User savedUser = userRepo.save(user);

        WalletBalance walletBalance = new WalletBalance();
        walletBalance.setAmount(BigDecimal.ZERO);
        WalletBalance savedWalletBalance = walletBalanceRepo.save(walletBalance);

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setUser(savedUser);
        account.setWalletBalance(savedWalletBalance);

        Account savedAccount = accountRepo.save(account);
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
            throw new InvalidAmountException("Transfer amount must be greater than zero");
        }

        Account fromAccount = accountRepo.findByAccountNumberWithLock(request.getFromAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getFromAccount()));

        Account toAccount = accountRepo.findByAccountNumber(request.getToAccount())
                .orElseThrow(() -> new AccountNotFoundException(request.getToAccount()));

        if (fromAccount.getAccountNumber().equals(toAccount.getAccountNumber())) {
            throw new SameAccountTransferException();
        }

        WalletBalance fromWallet = fromAccount.getWalletBalance();
        WalletBalance toWallet = toAccount.getWalletBalance();

        if (fromWallet.getAmount().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(fromWallet.getAmount(), request.getAmount());
        }

        fromWallet.setAmount(fromWallet.getAmount().subtract(request.getAmount()));
        toWallet.setAmount(toWallet.getAmount().add(request.getAmount()));

        walletBalanceRepo.save(fromWallet);
        walletBalanceRepo.save(toWallet);

        log.info("Transfer completed successfully. Transaction reference: {}", generateTransactionReference());
    }

    @Override
    @Transactional
    public void fundAccount(FundAccountDto request) {
        log.info("Funding account: {} with amount: {}", request.getAccountNumber(), request.getAmount());

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Funding amount must be greater than zero");
        }

        Account account = accountRepo.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(request.getAccountNumber()));

        WalletBalance walletBalance = account.getWalletBalance();
        walletBalance.setAmount(walletBalance.getAmount().add(request.getAmount()));
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