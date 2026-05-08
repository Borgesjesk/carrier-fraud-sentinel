package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.RiskAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAlertRepository extends MongoRepository<RiskAlert, String> {
}