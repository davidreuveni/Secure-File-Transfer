package com.davidr.secureft.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.davidr.secureft.datamodels.LoginSession;

import java.util.Optional;

public interface LoginSessionRepository extends MongoRepository<LoginSession, String> {
    Optional<LoginSession> findByToken(String token);
}