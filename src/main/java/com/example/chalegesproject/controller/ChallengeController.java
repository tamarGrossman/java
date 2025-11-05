package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.ChallengeMapper;
import com.example.chalegesproject.service.ChallengeRepository;
import com.example.chalegesproject.service.ImageUtils;
import com.example.chalegesproject.service.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin
    @RestController
    @RequestMapping("/api/challenges")
    public class ChallengeController {

        private final ChallengeRepository challengeRepository;
        private final UsersRepository usersRepository;
        private final ChallengeMapper challengeMapper;

        @Autowired
        public ChallengeController(ChallengeRepository challengeRepository,
                                   UsersRepository usersRepository,
                                   ChallengeMapper challengeMapper) {
            this.challengeRepository = challengeRepository;
            this.usersRepository = usersRepository;
            this.challengeMapper = challengeMapper;
        }

        // --- GET כל האתגרים ---
        @GetMapping("/getChallenges")
        public ResponseEntity<List<ChallengeDto>> getAllChallenges() {
            try {
                // שולפים את כל האתגרים
                List<Challenge> challenges = challengeRepository.findAll();
                // ממירים ל-DTO
                List<ChallengeDto> challengeDtos=challengeMapper.toChallengesDTO(challenges);
                if (challengeDtos.isEmpty()) {
                    // אם אין אתגרים, מחזירים 204 No Content
                    return ResponseEntity.noContent().build();
                }
                // מחזירים את הרשימה עם 200 OK
                return ResponseEntity.ok(challengeDtos);
            } catch (Exception e) {
                // במקרה של שגיאה פנימית
                System.out.println("Error fetching challenges: " + e.getMessage());
                e.printStackTrace();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }

        // --- POST יצירת אתגר חדש ---
        @PostMapping("/createChallenge")
        public ResponseEntity<ChallengeDto> uploadChallengeWithImage(@RequestPart("image") MultipartFile file
                ,@RequestPart("challenge") ChallengeDto c) {
            try {

                c.setImagePath(file.getOriginalFilename());//השם של התמונה
                ImageUtils.saveImage(file);

                Users user=usersRepository.findById(c.getUserId()).get();
                Challenge challenge=challengeRepository.save(challengeMapper.dtoToChallenge(c,user));
                return new ResponseEntity<>(challengeMapper.challengeToDto(challenge),HttpStatus.CREATED);

            } catch (IOException e) {
                System.out.println(e);
                return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }




        }









