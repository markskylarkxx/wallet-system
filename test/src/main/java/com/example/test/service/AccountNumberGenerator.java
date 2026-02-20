package com.example.test.service;

import com.example.test.repo.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private final AccountRepo accountRepo;
    private static final Random RANDOM = new Random();

    /**
     * Generates a unique 10-digit account number.
     * Format: YYMMDD + 4 random digits
     */
    public String generate() {
        String accountNumber;
        int attempts = 0;
        do {
            accountNumber = buildAccountNumber();
            attempts++;
            if (attempts > 10) {
                throw new RuntimeException("Unable to generate unique account number after 10 attempts");
            }
        } while (accountRepo.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private String buildAccountNumber() {
        LocalDate today = LocalDate.now();
        String datePart = String.format("%02d%02d%02d",
                today.getYear() % 100,
                today.getMonthValue(),
                today.getDayOfMonth());
        int randomPart = RANDOM.nextInt(9000) + 1000;
        return datePart + randomPart;
    }
}