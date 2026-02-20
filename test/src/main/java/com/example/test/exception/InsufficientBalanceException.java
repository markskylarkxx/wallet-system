package com.example.test.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient balance. Available: %s, Requested: %s", available, requested));
    }
}