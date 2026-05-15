package com.carrierfraud.infrastructure;

import com.carrierfraud.domain.RiskAlert;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RiskAlertRepository provides MongoDB persistence for RiskAlert entities.
 *
 * SPRING DATA MONGODB:
 * - Extends MongoRepository<Entity, IdType>
 * - Automatically generates CRUD methods:
 *   - save(alert) → INSERT
 *   - findAll() → SELECT *
 *   - findById(id) → SELECT WHERE id
 *   - delete(alert) → DELETE
 *   - etc.
 *
 * CUSTOM QUERIES:
 * - You can define additional methods
 * - Spring generates MongoDB queries automatically
 * - Example: findByCarrierName() generates db.alerts.find({carrierName: ...})
 *
 * WHY MONGODB?
 * - Flexible schema (alerts have variable fields)
 * - Document model (RiskAlert is a document)
 * - Horizontal scalability
 * - Fast writes (important for real-time fraud detection)
 *
 * @Repository = Spring component for data access
 */
@Repository
public interface RiskAlertRepository extends MongoRepository<RiskAlert, String> {

    /**
     * Find all alerts for a specific carrier.
     *
     * @param carrierName the carrier to search for
     * @return list of alerts for that carrier
     */
    List<RiskAlert> findByCarrierName(String carrierName);

    /**
     * Find all alerts created after a certain time.
     * Used for dashboard: "Show alerts from last 24 hours"
     *
     * @param createdDate the threshold date
     * @return alerts created after this date
     */
    List<RiskAlert> findByCreatedDateAfter(LocalDateTime createdDate);

    /**
     * Find all UNASSIGNED alerts (waiting to be claimed).
     * Used for department dashboard: "Show unassigned alerts"
     *
     * @return unassigned alerts
     */
    List<RiskAlert> findByAssignmentStatusUnassigned();

    /**
     * Count total alerts for a carrier.
     * Used for: "How many alerts for CarrierA?"
     *
     * @param carrierName the carrier
     * @return count of alerts
     */
    long countByCarrierName(String carrierName);
}
