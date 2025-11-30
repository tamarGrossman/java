package com.example.chalegesproject.service;

import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge,Long> {
    List<Challenge> findByUser(Users user);

}
