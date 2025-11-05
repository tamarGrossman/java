package com.example.chalegesproject.dto;

import com.example.chalegesproject.model.Users;
import jakarta.persistence.GeneratedValue;
import org.apache.catalina.User;

import java.time.LocalDate;



    public class CommentDto {
        private long id;
        private String username;
        private long userId;
        private String content;
        private String picture;
        private LocalDate date;
        private long challengeId;
        private String imagePath;


        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
        public long getChallengeId() { return challengeId; }
        public void setChallengeId(long challengeId) { this.challengeId = challengeId;}
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }



        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getPicture() {
            return picture;
        }

        public void setPicture(String picture) {
            this.picture = picture;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }


    }


