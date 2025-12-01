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
    @Mapping(target = "picture", source = "dto.imagePath")
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
    default ChallengeDto challengeToDto(Challenge challenge, @Context boolean isLiked) {
        if (challenge == null) {
            return null;
        }

        ChallengeDto dto = new ChallengeDto();

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

        // תמונה (מושבתת)
        if (challenge.getPicture() != null) {
            try {
                // dto.setPicture(ImageUtils.getImage(challenge.getPicture()));
            } catch (Exception e) {
                // handle
            }
        }

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
