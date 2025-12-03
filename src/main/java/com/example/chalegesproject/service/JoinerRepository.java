package com.example.chalegesproject.service;

import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Joiner;
import com.example.chalegesproject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JoinerRepository extends JpaRepository<Joiner,Long> {
    Optional<Joiner> findByUserAndChallenge(Users user, Challenge challenge);

    List<Joiner> findByUser(Users user);
}
