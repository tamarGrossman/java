package com.example.chalegesproject.controller;

import com.example.chalegesproject.model.Joiner;
import com.example.chalegesproject.service.JoinerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/joiners")
public class JoinerController {

    private final JoinerRepository joinerRepository;

    @Autowired
    public JoinerController(JoinerRepository joinerRepository) {
        this.joinerRepository = joinerRepository;
    }

    // --- GET כל הקשרים ---
    @GetMapping
    public List<Joiner> getAllJoiners() {
        return joinerRepository.findAll();
    }

    // --- GET לפי ID ---
    @GetMapping("/get{id}")
    public Joiner getJoinerById(@PathVariable Long id) {
        Optional<Joiner> joiner = joinerRepository.findById(id);
        return joiner.orElse(null);
    }

    // --- POST יצירת קשר חדש ---
    @PostMapping
    public Joiner createJoiner(@RequestBody Joiner joiner) {
        return joinerRepository.save(joiner);
    }
}
