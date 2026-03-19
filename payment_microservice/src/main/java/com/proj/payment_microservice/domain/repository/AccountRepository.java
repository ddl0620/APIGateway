package com.proj.payment_microservice.domain.repository;

import com.proj.payment_microservice.domain.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AccountRepository extends MongoRepository<Account, String> {

    Optional<Account> findByUserId(String userId);
}
