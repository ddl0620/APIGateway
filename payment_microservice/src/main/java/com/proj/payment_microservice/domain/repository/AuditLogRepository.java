package com.proj.payment_microservice.domain.repository;

import com.proj.payment_microservice.domain.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {
}
