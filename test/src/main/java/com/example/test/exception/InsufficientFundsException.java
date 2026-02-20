package com.example.test.exception;

public class InsufficientFundsException extends WalletException {
    public InsufficientFundsException(String accountNumber) {
        super("Insufficient funds in account: " + accountNumber);
    }
}