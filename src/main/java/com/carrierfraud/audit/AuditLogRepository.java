package com.carrierfraud.audit;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByUsername(String username);

    List<AuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    List<AuditLog> findByTimestampAfter(Instant timestamp);

    List<AuditLog> findByActionOrderByTimestampDesc(String action);
}
