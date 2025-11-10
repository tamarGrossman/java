package com.example.chalegesproject.controller;

import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/users")
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
    @GetMapping("/get{id}")
    public Users getUserById(@PathVariable Long id) {
        Optional<Users> user = usersRepository.findById(id);
        return user.orElse(null); // או לזרוק Exception מותאם אישית
    }

    // --- POST יצירת משתמש חדש ---
    @PostMapping("/signup")
    public ResponseEntity<Users> signUp(@RequestBody Users user){
        //נבדוק ששם המשתמש לא קיים
        Users u=usersRepository.findByUserName(user.getUsername());
        if(u!=null)
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        String pass=user.getPassword();//הסיסמא שהמשתמש הכניס - לא מוצפנת
        user.setPassword(new BCryptPasswordEncoder().encode(pass));
        return new ResponseEntity<>(user,HttpStatus.CREATED);
    }
}
