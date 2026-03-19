package com.proj.payment_microservice.domain.repository;

import com.proj.payment_microservice.domain.model.Payment;
import com.proj.payment_microservice.domain.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

    Optional<Payment> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
