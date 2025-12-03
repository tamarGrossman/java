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
            dto.setPicture(null);
            return dto;
        }).collect(Collectors.toList());
    }
    List<ChallengeDto> challengeToDtoNoPicture(List<Challenge> challenges);


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

        boolean isLiked = false;
        if (currentUserId != null && challenge.getLikedByUserIds() != null) {
            String currentIdStr = String.valueOf(currentUserId);
            isLiked = Arrays.asList(challenge.getLikedByUserIds().split(","))
                    .contains(currentIdStr);
        }

        return challengeToDto(challenge, isLiked);
    }
    // 1. המרה הפוכה DTO -> Entity
    @Mapping(target = "id", source = "dto.id")
    @Mapping(target = "picture", source = "dto.picture")  // ← שנה לזה!
    @Mapping(target = "user", source = "user")

    @Mapping(target = "likedByUserIds", constant = "")
    Challenge dtoToChallenges(ChallengeDto dto, Users user);




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
        dto.setLikeCount(calculateLikeCount(challenge.getLikedByUserIds()));
        dto.setLikedByCurrentUser(isLiked);
        if (challenge.getUser() != null) {
            dto.setUserId(challenge.getUser().getId());
            dto.setUserName(challenge.getUser().getUsername());
        }

        if (challenge.getPicture() != null) {
            try {
                String base64Image = ImageUtils.getImage(challenge.getPicture());

                dto.setPicture(base64Image);

            } catch (Exception e) {
                System.err.println("Failed to load image for Challenge ID " + challenge.getId() + ": " + e.getMessage());
                dto.setPicture(null);
            }
        }
        return dto;
    }

    default ChallengeDto challengeToDtoNoPicture(Challenge challenge) {
        ChallengeDto dto = challengeToDto(challenge, false);
        if (dto != null) {
            dto.setPicture(null);
        }
        return dto;
    }

    default int calculateLikeCount(String likedByUserIds) {
        if (likedByUserIds == null || likedByUserIds.trim().isEmpty()) {
            return 0;
        }
        return (int) Arrays.stream(likedByUserIds.split(","))
                .filter(s -> !s.trim().isEmpty())
                .count();
    }


}
