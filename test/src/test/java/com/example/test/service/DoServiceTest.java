package com.example.test.service;

import com.example.test.dto.DoTransDto;
import com.example.test.model.Account;
import com.example.test.model.User;
import com.example.test.model.WalletBalance;
import com.example.test.repo.AccountRepo;
import com.example.test.repo.UserRepo;
import com.example.test.repo.WalletBalanceRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private WalletBalanceRepo walletBalanceRepo;

    @InjectMocks
    private DoService doService;

    private User testUser;
    private Account testAccount;
    private WalletBalance testWalletBalance;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");

        testWalletBalance = new WalletBalance();
        testWalletBalance.setId(1L);
        testWalletBalance.setAmount(BigDecimal.valueOf(1000));

        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setAccountNumber("ACCT1234567890");
        testAccount.setUser(testUser);
        testAccount.setWalletBalance(testWalletBalance);

        testUser.setAccount(testAccount);
    }

    @Test
    void createUserAndAccount_Success() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepo.save(any(User.class))).thenReturn(testUser);
        when(walletBalanceRepo.save(any(WalletBalance.class))).thenReturn(testWalletBalance);
        when(accountRepo.save(any(Account.class))).thenReturn(testAccount);

        // Act
        assertDoesNotThrow(() -> doService.createUserAndAccount(testUser));

        // Assert
        verify(userRepo, times(2)).save(any(User.class));
        verify(walletBalanceRepo, times(1)).save(any(WalletBalance.class));
        verify(accountRepo, times(1)).save(any(Account.class));
    }

    @Test
    void createUserAndAccount_UserAlreadyExists() {
        // Arrange
        when(userRepo.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> doService.createUserAndAccount(testUser));
        assertEquals("User with email test@example.com already exists", exception.getMessage());
    }

    @Test
    void doIntraTransfer_Success() {
        // Arrange
        DoTransDto transferDto = new DoTransDto();
        transferDto.setFromAccount("ACCT1234567890");
        transferDto.setToAccount("ACCT0987654321");
        transferDto.setAmount(BigDecimal.valueOf(500));

        Account toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setAccountNumber("ACCT0987654321");
        WalletBalance toWallet = new WalletBalance();
        toWallet.setId(2L);
        toWallet.setAmount(BigDecimal.valueOf(500));
        toAccount.setWalletBalance(toWallet);

        when(accountRepo.findByAccountNumberWithLock("ACCT1234567890")).thenReturn(Optional.of(testAccount));
        when(accountRepo.findByAccountNumber("ACCT0987654321")).thenReturn(Optional.of(toAccount));
        when(walletBalanceRepo.save(any(WalletBalance.class))).thenReturn(testWalletBalance);

        // Act
        assertDoesNotThrow(() -> doService.doIntraTransfer(transferDto));

        // Assert
        assertEquals(BigDecimal.valueOf(500), testWalletBalance.getAmount());
        assertEquals(BigDecimal.valueOf(1000), toWallet.getAmount());
        verify(walletBalanceRepo, times(2)).save(any(WalletBalance.class));
    }

    @Test
    void doIntraTransfer_InsufficientBalance() {
        // Arrange
        DoTransDto transferDto = new DoTransDto();
        transferDto.setFromAccount("ACCT1234567890");
        transferDto.setToAccount("ACCT0987654321");
        transferDto.setAmount(BigDecimal.valueOf(2000)); // More than available 1000

        when(accountRepo.findByAccountNumberWithLock("ACCT1234567890")).thenReturn(Optional.of(testAccount));
        when(accountRepo.findByAccountNumber("ACCT0987654321")).thenReturn(Optional.of(new Account()));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> doService.doIntraTransfer(transferDto));
        assertTrue(exception.getMessage().contains("Insufficient balance"));
    }
}