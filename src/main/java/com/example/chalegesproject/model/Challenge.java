package com.example.chalegesproject.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Challenge {
    @Id
    @GeneratedValue
    private long id;
    @ManyToOne
    private Users user;
    private String name;
    private String description;
    private LocalDate date;//תאריך העלאת האתגר
    private int numOfDays;//משך האתגר בימים
    private String picture;//הנתיב של התמונה

    @OneToMany(mappedBy = "challenge")
    @JsonIgnore
    private List<Joiner> joiners;


    @OneToMany(mappedBy = "challenge")
    @JsonIgnore
    private List<Comment> comments;




    public List<Joiner> getJoiners() {
        return joiners;
    }

    public void setJoiners(List<Joiner> joiners) {
        this.joiners = joiners;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
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

    public int getNumOfDays() {
        return numOfDays;
    }

    public void setNumOfDays(int numOfDays) {
        this.numOfDays = numOfDays;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
