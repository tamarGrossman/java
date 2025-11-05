package com.example.chalegesproject.controller;

import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersRepository usersRepository;

    @Autowired
    public UsersController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    // --- GET כל המשתמשים ---
    @GetMapping
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }

    // --- GET לפי ID ---
    @GetMapping("/{id}")
    public Users getUserById(@PathVariable Long id) {
        Optional<Users> user = usersRepository.findById(id);
        return user.orElse(null); // או לזרוק Exception מותאם אישית
    }

    // --- POST יצירת משתמש חדש ---
    @PostMapping
    public Users createUser(@RequestBody Users user) {
        return usersRepository.save(user);
    }
}
