package com.proj.payment_microservice.domain.repository;

import com.proj.payment_microservice.domain.model.LedgerEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LedgerEntryRepository extends MongoRepository<LedgerEntry, String> {
}
