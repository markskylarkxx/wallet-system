package com.example.test.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity@Data@AllArgsConstructor@NoArgsConstructor
public class WalletBalance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal amount =BigDecimal.ZERO;
    @OneToOne(mappedBy = "walletBalance")
    private Account account;
}
