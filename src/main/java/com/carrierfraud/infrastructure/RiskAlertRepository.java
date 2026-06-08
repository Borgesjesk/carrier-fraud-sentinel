package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.AlertAssignmentStatus;
import com.carrierfraud.domain.Department;
import com.carrierfraud.domain.RiskAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RiskAlertRepository extends MongoRepository<RiskAlert, String> {

    Optional<RiskAlert> findByAlertId(String alertId);

    List<RiskAlert> findByCarrierName(String carrierName);

    List<RiskAlert> findByCreatedDateAfter(LocalDateTime createdDate);

    List<RiskAlert> findByAssignmentStatus(AlertAssignmentStatus status);

    List<RiskAlert> findByAssignedDepartmentIn(Set<Department> departments);

    long countByCarrierName(String carrierName);
}