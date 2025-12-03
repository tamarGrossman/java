package com.example.chalegesproject.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "חובה להכניס שם משתמש")
    @Size(min = 1, max = 30, message = "אורך שם המשתמש חייב להיות בין 1 ל-30 תווים")
    private String username;
    @NotBlank(message = "חובה להכניס כתובת מייל")
    @Email
    private String email;
    @NotBlank(message = "חובה להכניס סיסמא")
    @Size(min = 4, max = 10, message = "אורך הסיסמא חייב להיות בין 4 ל-10 תווים")
    private String password;
    @ManyToMany
    private Set<Role> roles=new HashSet<>();

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<Challenge> challenges;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Joiner> joiners;


    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Comment> comments;


    public Users() {
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Joiner> getJoiners() {
        return joiners;
    }

    public void setJoiners(List<Joiner> joiners) {
        this.joiners = joiners;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<Challenge> getChallenges() {
        return challenges;
    }

    public void setChallenges(List<Challenge> challenges) {
        this.challenges = challenges;
    }
}
