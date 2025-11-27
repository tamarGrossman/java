package com.example.chalegesproject.service;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.CommentDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Comment;
import com.example.chalegesproject.model.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Mapper(componentModel = "spring")

public interface CommentMapper {
    List<CommentDto> toCommentesDTO(List<Comment> list);
    // המרה מ-Entity ל-DTO עם טיפול בתמונה
    default CommentDto commentToDto(Comment comment) throws IOException {
        CommentDto dto = new CommentDto();

        dto.setId((long) comment.getId());

        dto.setContent(comment.getContent());
        dto.setDate(comment.getDate());
        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId()); // <--- השורה החשובה
        }
        if (comment.getUser() != null) {
            dto.setUsername(comment.getUser().getUsername());// <--- השורה החשובה
        }
        // ⬅️ התיקון החשוב: מיפוי ה-Challenge ID
        if (comment.getChallenge() != null) {
            dto.setChallengeId(comment.getChallenge().getId()); // שולף את ה-ID מהאובייקט המקושר
        }
        try {
            // המרת התמונה לקובץ base64 כדי שנוכל לשלוח ל-Frontend
            if (comment.getPicture() != null) {
                dto.setPicture(ImageUtils.getImage(comment.getPicture()));
            }
        } catch (Exception e) {
            throw new RuntimeException("שגיאה בקריאת התמונה", e);
        }
            return dto;
        }


    // המרה הפוכה מ-DTO ל-Entity כולל שמירת תמונה
    @Mapping(target="picture", source="imagePath")
    default Comment dtoToComment(CommentDto dto, Users user, Challenge challenge) throws IOException {
        Comment comment = new Comment();

        comment.setId(dto.getId());
        comment.setUser(user);
        comment.setChallenge(challenge);
        comment.setContent(dto.getContent());
        comment.setDate(java.time.LocalDate.now());

        if (dto.getImagePath() != null && !dto.getImagePath().isEmpty()) {
            comment.setPicture(dto.getImagePath());
        }

        return comment;
    }
}

