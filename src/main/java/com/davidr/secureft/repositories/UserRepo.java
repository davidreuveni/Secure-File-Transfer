package com.davidr.secureft.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.davidr.secureft.datamodels.User;

@Repository
public interface UserRepo extends MongoRepository<User, String> {
    User findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);

    boolean existsByEmail(String email);
}
