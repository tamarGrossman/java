package com.example.chalegesproject.service;

import com.example.chalegesproject.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Long> {
    List<Comment> findByChallengeId(long challengeId);
}
