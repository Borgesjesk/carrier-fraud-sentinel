package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.AlertAssignmentStatus;
import com.carrierfraud.domain.RiskAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RiskAlertRepository extends MongoRepository<RiskAlert, String> {

    List<RiskAlert> findByCarrierName(String carrierName);

    List<RiskAlert> findByCreatedDateAfter(LocalDateTime createdDate);

    List<RiskAlert> findByAssignmentStatus(AlertAssignmentStatus status);

    long countByCarrierName(String carrierName);
}