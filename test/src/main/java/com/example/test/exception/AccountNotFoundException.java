package com.example.test.exception;

public class AccountNotFoundException extends WalletException {
    public AccountNotFoundException(String accountNumber) {
        super("Account not found: " + accountNumber);
    }
}