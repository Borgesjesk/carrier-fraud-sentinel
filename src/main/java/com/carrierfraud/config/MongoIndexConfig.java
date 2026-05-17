package com.carrierfraud.config;

import com.carrierfraud.audit.AuditLog;
import com.carrierfraud.domain.RiskAlert;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexConfig {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initIndexes() {
        IndexOperations alertOps = mongoTemplate.indexOps(RiskAlert.class);
        alertOps.ensureIndex(new Index().on("carrierName", Sort.Direction.ASC).named("idx_alert_carrier"));
        alertOps.ensureIndex(new Index().on("createdDate", Sort.Direction.DESC).named("idx_alert_createdDate"));
        alertOps.ensureIndex(new Index().on("severity", Sort.Direction.ASC).named("idx_alert_severity"));
        alertOps.ensureIndex(new Index().on("assignmentStatus", Sort.Direction.ASC).named("idx_alert_status"));
        alertOps.ensureIndex(new Index().on("assignedDepartment", Sort.Direction.ASC).named("idx_alert_department"));

        IndexOperations auditOps = mongoTemplate.indexOps(AuditLog.class);
        auditOps.ensureIndex(new Index().on("username", Sort.Direction.ASC).named("idx_audit_username"));
        auditOps.ensureIndex(new Index().on("timestamp", Sort.Direction.DESC).named("idx_audit_timestamp"));
        auditOps.ensureIndex(new Index()
                .on("resourceType", Sort.Direction.ASC)
                .on("resourceId", Sort.Direction.ASC)
                .named("idx_audit_resource"));
    }
}
