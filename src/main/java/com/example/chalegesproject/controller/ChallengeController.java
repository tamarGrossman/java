package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.ChatRequest;
import com.example.chalegesproject.dto.ChatResponse;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Joiner;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


    @RestController
    @RequestMapping("/api/challenges")
    public class ChallengeController {

        private final ChallengeRepository challengeRepository;
        private final UsersRepository usersRepository;
        private final ChallengeMapper challengeMapper;
        private final JoinerRepository joinerRepository;
        private final AIChatService aiChatService;

        @Autowired
        public ChallengeController(ChallengeRepository challengeRepository,
                                   UsersRepository usersRepository,
                                   ChallengeMapper challengeMapper,JoinerRepository joinerRepository,AIChatService aiChatService) {
            this.challengeRepository = challengeRepository;
            this.usersRepository = usersRepository;
            this.challengeMapper = challengeMapper;
            this.joinerRepository=joinerRepository;
            this.aiChatService = aiChatService;

        }

        // --- GET כל האתגרים ---
        @GetMapping("/getAll")
        public ResponseEntity<List<ChallengeDto>> getAllChallenges() {
            try {
                // שולפים את כל האתגרים
                List<Challenge> challenges = challengeRepository.findAll();
                // ממירים ל-DTO
                List<ChallengeDto> challengeDtos=challengeMapper.toChallengesMinDTO(challenges);
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
        // --- GET אתגר ספציפי לפי ID ---
// הנתיב הוא לדוגמה: /api/challenges/5
        @GetMapping("/getById{id}")
        public ResponseEntity<ChallengeDto> getChallengeById(@PathVariable Long id) {
            try {
                // 1. שליפה מה-Repository באמצעות findById
                // findById מחזירה Optional, מה שמאפשר לטפל בחוסר מידע בצורה בטוחה
                Optional<Challenge> challengeOptional = challengeRepository.findById(id);

                if (challengeOptional.isEmpty()) {
                    // 2. אם האתגר לא נמצא (ה-Optional ריק), מחזירים 404 Not Found
                    return ResponseEntity.notFound().build();
                }

                // 3. האתגר נמצא! מוציאים אותו מה-Optional וממירים ל-DTO
                Challenge challenge = challengeOptional.get();
                ChallengeDto challengeDto = challengeMapper.challengeToDto(challenge);

                // 4. מחזירים את האתגר עם 200 OK
                return ResponseEntity.ok(challengeDto);

            } catch (Exception e) {
                // במקרה של שגיאה פנימית (לדוגמה, בעיית חיבור ל-DB)
                System.out.println("Error fetching challenge by ID " + id + ": " + e.getMessage());
                e.printStackTrace();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }


        // --- POST יצירת אתגר חדש ---
        @PostMapping("/create")
        public ResponseEntity<ChallengeDto> uploadChallengeWithImage(@RequestPart("image") MultipartFile file
                ,@RequestPart("challenge") ChallengeDto c) {
            try {
                // 2. קבלת פרטי משתמש מחובר
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                // 3. מציאת אובייקט המשתמש (לפי שם משתמש שחולץ מה-JWT)
                Users user = usersRepository.findByUsername(username);
                if (user == null) {
                    return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                }

                // 4. הגדרת ה-ID המאובטח
                c.setUserId(user.getId());

                // ... (המשך לוגיקת שמירת האתגר)
                c.setImagePath(file.getOriginalFilename());//השם של התמונה
                ImageUtils.saveImage(file);


                Challenge challenge=challengeRepository.save(challengeMapper.dtoToChallenges(c,user));
                return new ResponseEntity<>(challengeMapper.challengeToDto(challenge),HttpStatus.CREATED);

            } catch (IOException e) {
                System.out.println(e);
                return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
            }}
        // בתוך ChallengeController.java


        // --- POST הצטרפות לאתגר (מאובטח באמצעות Token) ---
        @PostMapping("/join/{challengeId}") // ⬅️ הנתיב מקבל רק את Challenge ID
        public ResponseEntity<?> joinChallenge(@PathVariable Long challengeId) {
            try {
                // 1. קבלת שם המשתמש מתוך ה-Token
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                // 2. מציאת אובייקט המשתמש המאומת
                Users user = usersRepository.findByUsername(username);

                // אם המשתמש המאומת לא נמצא ב-DB (מקרה נדיר לאחר אימות Token)
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found or session invalid.");
                }

                // 3. שליפת אובייקט האתגר
                Challenge challenge = challengeRepository.findById(challengeId)
                        // ⬅️ שימוש ב-NoSuchElementException במקום Exception כללי, נמנע משגיאה כמו
                        .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + challengeId));

                // 4. בדיקת כפילות
                if (joinerRepository.findByUserAndChallenge(user, challenge).isPresent()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("המשתמש כבר הצטרף לאתגר זה.");
                }

                // 5. יצירת ושמירת אובייקט Joiner
                Joiner joiner = new Joiner();
                joiner.setUser(user); // ⬅️ שימוש באובייקט ה-user המאומת (בטוח)
                joiner.setChallenge(challenge);
                joiner.setStartDate(LocalDate.now());

                joinerRepository.save(joiner);
                return ResponseEntity.status(HttpStatus.CREATED).body("הצטרפות לאתגר עברה בהצלחה");

            } catch (NoSuchElementException e) {
                // טיפול בשגיאת "לא נמצא" (404)
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } catch (Exception e) {
                // טיפול בשאר שגיאות פנימיות
                System.out.println("Error joining challenge: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("שגיאה פנימית בשרת: " + e.getMessage());
            }
        }
        // --- GET שליפת כל האתגרים שמשתמש הצטרף אליהם ---
        @GetMapping("/joinedChallenges")
        public ResponseEntity<List<ChallengeDto>> getJoinedChallengesForUser() {
            try {
                // 1. קבלת המשתמש המחובר מה־JWT
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                // 2. שליפת אובייקט המשתמש
                Users user = usersRepository.findByUsername(username);
                if (user == null) {
                    return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                }

                // 3. שליפת כל רשומות ה-Joiner של המשתמש
                List<Joiner> joiners = joinerRepository.findByUser(user);

                // 4. הוצאת כל ה-Challenge ששייכים לרשומות Joiner
                List<Challenge> challenges = joiners.stream()
                        .map(Joiner::getChallenge)
                        .collect(Collectors.toList());

                // 5. המרת כל האתגרים ל-DTO
                List<ChallengeDto> challengeDtos = challengeMapper.toChallengesDTO(challenges);

                // 6. החזרה
                return ResponseEntity.ok(challengeDtos);

            } catch (Exception e) {
                System.out.println("Error fetching joined challenges: " + e.getMessage());
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ChatResponse> getResponseStream(@RequestBody ChatRequest chatRequest){

            // ✅ עדכון 2: קריאה למתודה החדשה ב-Service
            return aiChatService.getResponseStream(chatRequest.message(), chatRequest.conversationId());
        }
        // --- GET אתגרים שהמשתמש העלה (יצר בעצמו) ---
        // בתוך ChallengeController.java

        // --- GET אתגרים שהמשתמש המחובר יצר (העלה) ---
        @GetMapping("/uploadedBy") // הנתיב לא כולל ID
        public ResponseEntity<List<ChallengeDto>> getMyCreatedChallenges() {
            try {
                // 1. קבלת פרטי משתמש מחובר (בדיקה ש-Token קיים ותקין)
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName(); // שם המשתמש מתוך ה-Token/JWT

                // 2. מציאת אובייקט המשתמש (לפי שם משתמש שחולץ מה-JWT)
                Users user = usersRepository.findByUsername(username);

                // 3. בדיקת אבטחה קריטית: אם המשתמש לא נמצא (למרות שה-Token קיים)
                if (user == null) {
                    // זהו אירוע חריג (Token תקין אך משתמש נמחק) - מחזירים 401
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                // 4. שליפת כל האתגרים שנוצרו על ידי המשתמש הזה
                // (שימוש ב-challengeRepository.findByUser, כפי שהוספנו)
                List<Challenge> createdChallenges = challengeRepository.findByUser(user);

                // 5. המרה ל-DTO
                List<ChallengeDto> challengeDtos = challengeMapper.toChallengesDTO(createdChallenges);

                // 6. החזרת התוצאה
                if (challengeDtos.isEmpty()) {
                    // מחזיר 204 No Content אם המשתמש לא העלה כלום
                    return ResponseEntity.noContent().build();
                }

                return ResponseEntity.ok(challengeDtos);

            } catch (Exception e) {
                System.out.println("Error fetching user's created challenges: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
















