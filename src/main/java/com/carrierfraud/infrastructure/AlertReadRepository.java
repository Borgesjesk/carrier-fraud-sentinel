package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.AlertRead;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertReadRepository extends MongoRepository<AlertRead, String> {
    // Legacy, kept for compat but not used
    java.util.List<AlertRead> findAllByUsernameAndAlertId(String username, String alertId);

    Optional<AlertRead> findFirstByUsernameAndAlertId(String username, String alertId);

    Optional<AlertRead> findByUsernameAndAlertId(String username, String alertId);
    List<AlertRead> findByUsername(String username);
}
