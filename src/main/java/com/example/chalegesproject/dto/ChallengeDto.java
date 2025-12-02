package com.example.chalegesproject.dto;

import javax.validation.constraints.Size;
import java.time.LocalDate;
import jakarta.validation.constraints.*;


public class ChallengeDto {
    private long id;
    private Long userId; // או long אם תמיד חייב להיות ערך
//    @NotBlank(message = "שם האתגר הוא שדה חובה")
    @Size(min = 5, max = 100, message = "שם האתגר חייב להיות בין 5 ל-100 תווים")
    private String name;
    private String description;
    private LocalDate date;//תאריך העלאת האתגר
    private int numOfDays;//משך האתגר בימים
    private String picture;//התמונה בפורמט של base64
    private String imagePath;//הנתיב של התמונה
    private String userName;
    private int likeCount; // מספר הלייקים הכולל
    private boolean isLikedByCurrentUser; // האם המשתמש הנוכחי נתן לייק (true/false)

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



    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
