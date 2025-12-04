package com.example.chalegesproject.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;


public class ChallengeDto {
    private long id;
    private Long userId;
 @NotBlank(message = "שם האתגר הוא שדה חובה")
 @Size(min = 5, max = 100, message = "שם האתגר חייב להיות בין 5 ל-100 תווים")
    private String name;
    @NotBlank(message = "תיאור האתגר הוא שדה חובה")
    @Size(min = 10, max = 1000000, message = "שם האתגר חייב להיות בין 10 ל-1000 תווים")
    private String description;
    private LocalDate date;
    @Min(value = 1, message = "Challenge must be at least 1 day long")
    @Max(value = 365, message = "Challenge cannot exceed 365 days")
    private int numOfDays;
    private String picture;
    private String imagePath;
    private String userName;
    private int likeCount;
    private boolean isLikedByCurrentUser;

    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }



    public Long getUserId() {
        return userId;
    }



    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public int getNumOfDays() {
        return numOfDays;
    }

    public void setNumOfDays(int numOfDays) {
        this.numOfDays = numOfDays;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }


}
