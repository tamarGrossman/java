package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.ChatRequest;
import com.example.chalegesproject.dto.ChatResponse;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Joiner;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
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
import java.util.*;
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

                // המרה לdto על ידי המאפר
                List<ChallengeDto> challengeDtos = challengeMapper.challengeToDtoNoPicture(challenges);
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

        @GetMapping("/getById{id}")
        public ResponseEntity<ChallengeDto> getChallengeById(@PathVariable Long id) {
            try {
                // 1. שליפת האתגר
                Challenge challenge = challengeRepository.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("Challenge not found"));

                // 2. זיהוי המשתמש
                Long currentUserId = null;
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                    String username = auth.getName();
                    Users user = usersRepository.findByUsername(username);
                    if (user != null) {
                        currentUserId = user.getId();
                    }
                }

                // .  חישוב ה-isLiked
                boolean isLiked = false;
                int realLikeCount = 0;

                String likedIdsStr = challenge.getLikedByUserIds();
                if (likedIdsStr != null && !likedIdsStr.trim().isEmpty()) {
                    // מנקה רווחים ויוצר רשימה נקייה
                    List<String> ids = Arrays.stream(likedIdsStr.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    realLikeCount = ids.size();

                    if (currentUserId != null) {
                        String myId = String.valueOf(currentUserId);
                        if (ids.contains(myId)) {
                            isLiked = true;
                        }
                    }

                }

                ChallengeDto dto = challengeMapper.challengeToDto(challenge, isLiked);

                dto.setLikedByCurrentUser(isLiked);
                dto.setLikeCount(realLikeCount);

                return ResponseEntity.ok(dto);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        // --- POST יצירת אתגר חדש ---
        @PostMapping("/create")
        public ResponseEntity<ChallengeDto> uploadChallengeWithImage(
                @RequestPart(value = "image", required = false) MultipartFile file,
                @Valid @RequestPart("challenge") ChallengeDto c) {
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

                //  טיפול בקובץ
                if (file != null && !file.isEmpty()) {
                    String filename = file.getOriginalFilename();
                    c.setImagePath(filename);
                    c.setPicture(filename);
                    ImageUtils.saveImage(file);
                } else {
                    c.setImagePath(null);
                    c.setPicture(null);
                }

                Challenge challenge = challengeRepository.save(challengeMapper.dtoToChallenges(c, user));
                return new ResponseEntity<>(challengeMapper.challengeToDto(challenge,false), HttpStatus.CREATED);

            } catch (IOException e) {
                System.out.println(e);
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        // --- POST הצטרפות לאתגר (מאובטח באמצעות Token) ---
        @PostMapping("/join/{challengeId}")
        public ResponseEntity<?> joinChallenge(@PathVariable Long challengeId) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();

                Users user = usersRepository.findByUsername(username);

                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found or session invalid.");
                }

                Challenge challenge = challengeRepository.findById(challengeId)
                        .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + challengeId));

                if (joinerRepository.findByUserAndChallenge(user, challenge).isPresent()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("המשתמש כבר הצטרף לאתגר זה.");
                }

                Joiner joiner = new Joiner();
                joiner.setUser(user);
                joiner.setChallenge(challenge);
                joiner.setStartDate(LocalDate.now());

                joinerRepository.save(joiner);
                return ResponseEntity.status(HttpStatus.CREATED).body("הצטרפות לאתגר עברה בהצלחה");

            } catch (NoSuchElementException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } catch (Exception e) {
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

                Users user = usersRepository.findByUsername(username);
                if (user == null) {
                    return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
                }

                Long currentUserId = user.getId();

                //. שליפת כל רשומות ה-Joiner של המשתמש
                List<Joiner> joiners = joinerRepository.findByUser(user);

                //  הוצאת כל ה-Challenge ששייכים לרשומות Joine
                List<Challenge> challenges = joiners.stream()
                        .map(Joiner::getChallenge)
                        .collect(Collectors.toList());

                List<ChallengeDto> challengeDtos = challenges.stream()
                        .map(challenge -> {
                            // חישוב האם המשתמש המחובר נתן לייק לאתגר הספציפי הזה
                            boolean isLiked = isLikedByUser(challenge, currentUserId);
                            return challengeMapper.challengeToDto(challenge, isLiked);
                        })
                        .collect(Collectors.toList());

                return ResponseEntity.ok(challengeDtos);

            } catch (Exception e) {
                System.out.println("Error fetching joined challenges: " + e.getMessage());
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ChatResponse> getResponseStream(@RequestBody ChatRequest chatRequest){

            return aiChatService.getResponseStream(chatRequest.message(), chatRequest.conversationId());
        }

        // --- GET אתגרים שהמשתמש המחובר יצר (העלה) ---
        @GetMapping("/uploadedBy")
        public ResponseEntity<List<ChallengeDto>> getMyCreatedChallenges() {
            try {

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String username = authentication.getName();


                Users user = usersRepository.findByUsername(username);


                if (user == null) {

                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                //  שליפת כל האתגרים שנוצרו על ידי המשתמש הזה
                //
                List<Challenge> createdChallenges = challengeRepository.findByUser(user);


                List<ChallengeDto> challengeDtos = challengeMapper.toChallengesDTO(createdChallenges);


                if (challengeDtos.isEmpty()) {
                    // מחזיר 204 No Content אם המשתמש לא העלה כלום
                    return ResponseEntity.noContent().build();
                }

                return ResponseEntity.ok(challengeDtos);

            } catch (Exception e) {
                System.out.println("Error fetching user's created challenges: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }}


            // פונקציה לבדיקה האם המשץמש עשה לייק
             boolean isLikedByUser(Challenge challenge, Long currentUserId) {
                if (challenge == null || challenge.getLikedByUserIds() == null || currentUserId == null) {
                    return false;
                }
                String userIdStr = currentUserId.toString();
                String ids = challenge.getLikedByUserIds();


                Set<String> likedUsers = new HashSet<>(Arrays.asList(ids.split(",")));
                likedUsers.remove(""); // מנקה איברים ריקים במקרה של מחרוזת ריקה
                return likedUsers.contains(userIdStr);
            }


        @PostMapping("/addLike/{challengeId}")
        @Transactional
        public ResponseEntity<Map<String, Object>> addLike(@PathVariable Long challengeId) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
                    Map<String,Object> body = Map.of("liked", false, "likeCount", 0, "message", "Not authenticated");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
                }

                String username = authentication.getName();
                Users user = usersRepository.findByUsername(username);
                if (user == null) {
                    Map<String,Object> body = Map.of("liked", false, "likeCount", 0, "message", "User not found");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
                }

                Challenge challenge = challengeRepository.findById(challengeId)
                        .orElseThrow(() -> new NoSuchElementException("Challenge not found: " + challengeId));

                Long currentUserId = user.getId();

                // מחשב את מספר הלייקים הנוכחי
                int currentLikeCount =
                        (challenge.getLikedByUserIds() == null || challenge.getLikedByUserIds().trim().isEmpty())
                                ? 0
                                : (int) Arrays.stream(challenge.getLikedByUserIds().split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .count();
//מונע מהמשתמש לעשות לייק עצמי
                if (challenge.getUser() != null && challenge.getUser().getId().equals(currentUserId)) {
                    Map<String,Object> body = Map.of(
                            "liked", false,
                            "likeCount", currentLikeCount,
                            "message", "Creator cannot like own challenge"
                    );
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
                }
//מונע ממשתמשים שלא מצורפים לאתגר
                boolean isUserJoined = joinerRepository.findByUserAndChallenge(user, challenge).isPresent();
                if (!isUserJoined) {
                    Map<String,Object> body = Map.of(
                            "liked", false,
                            "likeCount", currentLikeCount,
                            "message", "User must join challenge to like"
                    );
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
                }

                Set<String> likedUsers = Arrays.stream(
                                Optional.ofNullable(challenge.getLikedByUserIds()).orElse("").split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                String userIdStr = String.valueOf(currentUserId);
                boolean nowLiked;

                if (likedUsers.contains(userIdStr)) {
                    likedUsers.remove(userIdStr);
                    nowLiked = false;
                } else {
                    likedUsers.add(userIdStr);
                    nowLiked = true;
                }

                String newLikedUserIds = String.join(",", likedUsers);
                challenge.setLikedByUserIds(newLikedUserIds);
                challengeRepository.save(challenge);

                Map<String,Object> body = new HashMap<>();
                body.put("liked", nowLiked);
                body.put("likeCount", likedUsers.size());
                body.put("message", "OK");
                return ResponseEntity.ok(body);

            } catch (NoSuchElementException e) {
                Map<String,Object> body = Map.of("liked", false, "likeCount", 0, "message", e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
            } catch (Exception e) {
                e.printStackTrace();
                Map<String,Object> body = Map.of("liked", false, "likeCount", 0, "message", "Internal server error");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
            }
        }
        @GetMapping("/popular")
        public ResponseEntity<List<Challenge>> getPopularChallenges() {
            List<Challenge> challenges = challengeRepository.findAll();

            challenges.sort((c1, c2) -> {
                int count1 = (c1.getLikedByUserIds() == null || c1.getLikedByUserIds().isEmpty()) ? 0 :
                        c1.getLikedByUserIds().split(",").length;
                int count2 = (c2.getLikedByUserIds() == null || c2.getLikedByUserIds().isEmpty()) ? 0 :
                        c2.getLikedByUserIds().split(",").length;
                return Integer.compare(count2, count1);
            });


            return ResponseEntity.ok(challenges.stream().limit(7).collect(Collectors.toList()));
        }
    }



















