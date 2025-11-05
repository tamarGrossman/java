package com.example.chalegesproject.service;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Users;
import org.mapstruct.MapMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChallengeMapper {

    List<ChallengeDto> toChallengesDTO(List<Challenge> list);
    // המרה עם טיפול בתמונה
    default ChallengeDto challengeToDto(Challenge challenge) {
        ChallengeDto dto = new ChallengeDto();

        dto.setId(challenge.getId());
        dto.setName(challenge.getName());
        dto.setDescription(challenge.getDescription());
        dto.setDate(challenge.getDate());
        dto.setNumOfDays(challenge.getNumOfDays());

        // הוספת המשתמש ל-DTO
        if (challenge.getUser() != null) {
            dto.setUserId(challenge.getUser().getId()); // <--- השורה החשובה
        }
        if (challenge.getUser() != null) {
            dto.setUserName(challenge.getUser().getUsername());// <--- השורה החשובה
        }

        // המרת התמונה לקובץ base64 כך שאפשר להציג אותה ב-Frontend
        if (challenge.getPicture() != null) {
            try {
                dto.setPicture(ImageUtils.getImage(challenge.getPicture()));
            } catch (Exception e) {
                throw new RuntimeException("שגיאה בקריאת התמונה", e);
            }
        }

        return dto;
    }

//     ChallengeMapper.java (פונקציה challengeToDto)


////    @Mapping(target="picture", source="imagePath")
////    Challenge dtoToChallenge(ChallengeDto dto,Users user);

//     המרה הפוכה — שמירת התמונה שהועלתה
    @Mapping(target="picture", source="imagePath")
     default Challenge dtoToChallenge(ChallengeDto dto,Users user) {
        Challenge challenge = new Challenge();
        challenge.setId(dto.getId());
        challenge.setName(dto.getName());
        challenge.setDescription(dto.getDescription());
        challenge.setDate(dto.getDate());
        challenge.setNumOfDays(dto.getNumOfDays());
        challenge.setUser(user);
//        if (dto.getPicture() != null) {
            challenge.setPicture(dto.getImagePath());

        return challenge;
    }

    // ChallengeMapper.java (פונקציה dtoToChallenge)


}
