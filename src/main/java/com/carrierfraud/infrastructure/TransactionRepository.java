package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.Transaction;

import java.util.List;

public interface TransactionRepository {

    void save(Transaction transaction);

    List<Transaction> findByCarrierName(String carrierName);

    List<Transaction> findAll();
}

