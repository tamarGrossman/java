package com.example.chalegesproject.service;

import com.example.chalegesproject.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge,Long> {


}
