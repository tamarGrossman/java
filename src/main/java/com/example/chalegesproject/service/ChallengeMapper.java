package com.example.chalegesproject.service;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Users;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ChallengeMapper {
    default List<ChallengeDto> challengeToDtoNoPicture(List<Challenge> challenges, Long currentUserId) {
        if (challenges == null || challenges.isEmpty()) {
            return Collections.emptyList();
        }
        return challenges.stream().map(challenge -> {
            boolean isLiked = false;
            if (currentUserId != null && challenge.getLikedByUserIds() != null && !challenge.getLikedByUserIds().trim().isEmpty()) {
                String myId = String.valueOf(currentUserId);
                isLiked = Arrays.stream(challenge.getLikedByUserIds().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .anyMatch(s -> s.equals(myId));
            }
            ChallengeDto dto = challengeToDto(challenge, isLiked);
            // הסרת התמונה (no picture)
            dto.setPicture(null);
            return dto;
        }).collect(Collectors.toList());
    }
    List<ChallengeDto> challengeToDtoNoPicture(List<Challenge> challenges);
    // מתודה נוחה להמרת רשימה של אתגרים ל-DTO בלי לייקים
    default List<ChallengeDto> toChallengesDTO(List<Challenge> challenges) {
        if (challenges == null || challenges.isEmpty()) {
            return Collections.emptyList();
        }
        return challenges.stream()
                .map(challenge -> challengeToDto(challenge, false)) // תמיד false
                .collect(Collectors.toList());
    }

    default ChallengeDto challengeToDtoWithUserCheck(Challenge challenge, Long currentUserId) {
        if (challenge == null) {
            return null;
        }

        // בדיקה: האם המשתמש עשה לייק?
        boolean isLiked = false;
        if (currentUserId != null && challenge.getLikedByUserIds() != null) {
            // המרת המחרוזת "1,5,9" לרשימה ובדיקה האם ה-ID שלי שם
            String currentIdStr = String.valueOf(currentUserId);
            isLiked = Arrays.asList(challenge.getLikedByUserIds().split(","))
                    .contains(currentIdStr);
        }

        // קריאה למתודה הקיימת שלך עם ה-boolean הנכון
        return challengeToDto(challenge, isLiked);
    }
    // =========================================================
    // 1. המרה הפוכה DTO -> Entity
    // =========================================================
    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "picture", source = "dto.picture")  // ← שנה לזה!
    // ⭐⭐ תיקון קריטי: מיפוי אובייקט המשתמש ⭐⭐
    @Mapping(target = "user", source = "user")

    // ⭐⭐ תיקון קריטי: אתחול שדה הלייקים (אחרת ייתכן NullPointerException) ⭐⭐
    // אנו מניחים שבתהליך יצירת אתגר, ספירת הלייקים מתחילה ב-0.
    @Mapping(target = "likedByUserIds", constant = "")
    Challenge dtoToChallenges(ChallengeDto dto, Users user);


    // =========================================================
    // 2. המרות ל-DTO (רק לוגיקה, ללא abstract!)
    // =========================================================

    // 2.1. המרה מלאה עם לייקים וכולי
    // ChallengeMapper.java

    // 2.1. המרה מלאה עם לייקים וכולי
    default ChallengeDto challengeToDto(Challenge challenge, @Context boolean isLiked) {
        if (challenge == null) {
            return null;
        }

        ChallengeDto dto = new ChallengeDto();

        // ... (שדות רגילים: id, name, description, date, numOfDays, likes, user) ...
        dto.setId(challenge.getId());
        dto.setName(challenge.getName());
        dto.setDescription(challenge.getDescription());
        dto.setDate(challenge.getDate());
        dto.setNumOfDays(challenge.getNumOfDays());

        // לייקים
        dto.setLikeCount(calculateLikeCount(challenge.getLikedByUserIds()));
        dto.setLikedByCurrentUser(isLiked);

        // יוזר
        if (challenge.getUser() != null) {
            dto.setUserId(challenge.getUser().getId());
            dto.setUserName(challenge.getUser().getUsername());
        }

        // ⭐⭐⭐ התיקון הקריטי: טעינת התמונה מנתיב ל-Base64 ⭐⭐⭐
        if (challenge.getPicture() != null) {
            try {
                // ה-Entity (challenge) מכיל את שם הקובץ ב-challenge.getPicture()
                String base64Image = ImageUtils.getImage(challenge.getPicture());

                // ה-DTO שולח את ה-Base64 חזרה ל-Angular בשדה picture
                dto.setPicture(base64Image);

            } catch (Exception e) {
                System.err.println("Failed to load image for Challenge ID " + challenge.getId() + ": " + e.getMessage());
                dto.setPicture(null); // אם נכשל, אל תשלח כלום
            }
        }
        // ⭐⭐⭐ סוף תיקון טעינת תמונה ⭐⭐⭐

        return dto;
    }

    // 2.2. ללא תמונה
    default ChallengeDto challengeToDtoNoPicture(Challenge challenge) {
        ChallengeDto dto = challengeToDto(challenge, false);
        if (dto != null) {
            dto.setPicture(null);
        }
        return dto;
    }

    // 2.3. ספירת לייקים
    default int calculateLikeCount(String likedByUserIds) {
        if (likedByUserIds == null || likedByUserIds.trim().isEmpty()) {
            return 0;
        }
        return (int) Arrays.stream(likedByUserIds.split(","))
                .filter(s -> !s.trim().isEmpty())
                .count();
    }


}
