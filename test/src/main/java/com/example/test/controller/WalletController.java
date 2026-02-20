package com.example.test.controller;

import com.example.test.dto.*;
import com.example.test.model.Account;
import com.example.test.model.User;
import com.example.test.repo.AccountRepo;

import com.example.test.service.ServiceCall;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

    private final ServiceCall serviceCall;
    private final AccountRepo accountRepo;

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(@RequestBody User user) {
        log.info("Received create user request for email: {}", user.getEmail());

        serviceCall.createUserAndAccount(user);
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setId(user.getId());
        responseDTO.setEmail(user.getEmail());

        if (user.getAccount() != null) {
            AccountResponseDTO accountDTO = new AccountResponseDTO();
            accountDTO.setId(user.getAccount().getId());
            accountDTO.setAccountNumber(user.getAccount().getAccountNumber());
            accountDTO.setBalance(user.getAccount().getWalletBalance() != null ?
                    user.getAccount().getWalletBalance().getAmount() : null);
            responseDTO.setAccount(accountDTO);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User and account created successfully", responseDTO));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Void>> transfer(@RequestBody DoTransDto request) {
        log.info("Received transfer request from {} to {} for amount: {}",
                request.getFromAccount(), request.getToAccount(), request.getAmount());

        serviceCall.doIntraTransfer(request);

        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", null));
    }


    @PostMapping("/fund")
    public ResponseEntity<ApiResponse<Map<String, Object>>> fundAccount(@RequestBody FundAccountDto request) {
        log.info("Received fund request for account: {} with amount: {}",
                request.getAccountNumber(), request.getAmount());

        serviceCall.fundAccount(request);
        Account account = accountRepo.findByAccountNumber(request.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("accountNumber", account.getAccountNumber());
        data.put("newBalance", account.getWalletBalance().getAmount());
        data.put("fundedAmount", request.getAmount());

        return ResponseEntity.ok(ApiResponse.success("Account funded successfully", data));
    }
}