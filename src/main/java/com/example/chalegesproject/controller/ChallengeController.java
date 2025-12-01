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
                // ממירים ל-DTO

                // 💡 שינוי: קריאה למאפר עם הפרמטר החדש
                List<ChallengeDto> challengeDtos = challengeMapper.challengeToDtoNoPicture(challenges); // ⬅️ שימוש במתודת הרשימה המינימלית
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
//        @GetMapping("/getById{id}")
//        public ResponseEntity<ChallengeDto> getChallengeById(@PathVariable Long id) {
//            Challenge challenge;
//            Long currentUserId = null;
//
//            try {
//                // 1. שליפת האתגר
//                challenge = challengeRepository.findById(id)
//                        .orElseThrow(() -> new NoSuchElementException("Challenge not found"));
//
//                // 2. זיהוי המשתמש הנוכחי (מי מבקש את המידע?)
//                // זו הלוגיקה העסקית, עכשיו היא פה:
//                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//                if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
//                    String username = auth.getName();
//                    // ⭐⭐ יש להניח ש-UsersRepository מוזרק לפה
//                    Users user = usersRepository.findByUsername(username);
//                    if (user != null) {
//                        currentUserId = user.getId();
//                    }
//                }
//
//                // 3. המרה ל-DTO עם בדיקת הלייק
//                // קורא למתודה שהוספנו ב-Mapper ומעביר לה את ה-ID המחושב
//                ChallengeDto dto = challengeMapper.challengeToDtoWithUserCheck(challenge, currentUserId);
//
//                // 4. החזרה
//                return ResponseEntity.ok(dto);
//
//            } catch (NoSuchElementException e) {
//                // טיפול במקרה שהאתגר לא נמצא
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
//            } catch (Exception e) {
//                // טיפול בשגיאות כלליות (כמו בעיות ב-SecurityContextHolder אם יש)
//                System.err.println("Error fetching challenge details: " + e.getMessage());
//                e.printStackTrace();
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500
//            }
//        }

// בתוך ChallengeController.java

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

                // 3. לוגיקה קריטית - חישוב ה-isLiked
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
                        // בדיקה מדויקת
                        if (ids.contains(myId)) {
                            isLiked = true;
                        }
                    }

                    // הדפסת דיבאג לשרת (תסתכלי למטה בלוגים כשאת מריצה!)
                    System.out.println("DEBUG CHECK: ChallengeID=" + id +
                            " | UsersString=[" + likedIdsStr + "]" +
                            " | MyID=" + currentUserId +
                            " | Found? " + isLiked);
                }

                // 4. שימוש ב-Mapper (או יצירה ידנית אם המאפר עושה בעיות)
                // אנחנו נכפה את הערכים שחישבנו עכשיו!
                ChallengeDto dto = challengeMapper.challengeToDto(challenge, isLiked);

                // דריסה ידנית ליתר ביטחון - כדי לוודא שהמאפר לא טועה
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
                @RequestPart(value = "image", required = false) MultipartFile file, // נכון: required=false
                @RequestPart("challenge") ChallengeDto c) {
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

                // ⭐⭐ הלוגיקה הנכונה של טיפול בקובץ:
                if (file != null && !file.isEmpty()) {
                    // אם יש קובץ: שמור אותו ועדכן את הנתיב ב-DTO
                    c.setImagePath(file.getOriginalFilename()); // השם של התמונה
                    ImageUtils.saveImage(file);
                } else {
                    // אם אין קובץ: נתיב התמונה מוגדר ל-null
                    c.setImagePath(null);
                }
                // סוף בלוק הטיפול בקובץ. ממשיכים לשמירת האתגר.

                // השורות המכשלות והמיותרות הוסרו מכאן

                Challenge challenge = challengeRepository.save(challengeMapper.dtoToChallenges(c, user));
                return new ResponseEntity<>(challengeMapper.challengeToDto(challenge,false), HttpStatus.CREATED);

            } catch (IOException e) {
                System.out.println(e);
                return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


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
        // בתוך com.example.chalegesproject.controller.ChallengeController.java

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

                // ⭐⭐ שלב 2.5: קבלת ID המשתמש המחובר לחישוב לייקים ⭐⭐
                // אנחנו צריכים את ה-ID הזה כדי לבדוק את המחרוזת LikedByUserIds
                Long currentUserId = user.getId();

                // 3. שליפת כל רשומות ה-Joiner של המשתמש
                List<Joiner> joiners = joinerRepository.findByUser(user);

                // 4. הוצאת כל ה-Challenge ששייכים לרשומות Joiner
                List<Challenge> challenges = joiners.stream()
                        .map(Joiner::getChallenge)
                        .collect(Collectors.toList());

                // ⭐⭐ 5. המרת כל האתגרים ל-DTO עם לוגיקת isLiked ⭐⭐
                // 💡 שינוי: במקום קריאה ישירה למאפר שלא עובדת, משתמשים ב-stream() כדי לעבור על כל פריט
                // ולקרוא למאפר עם ה-boolean הנדרש.
                List<ChallengeDto> challengeDtos = challenges.stream()
                        .map(challenge -> {
                            // חישוב האם המשתמש המחובר נתן לייק לאתגר הספציפי הזה
                            boolean isLiked = isLikedByUser(challenge, currentUserId);
                            // קריאה למאפר המלאה עם הפרמטר הנדרש
                            return challengeMapper.challengeToDto(challenge, isLiked);
                        })
                        .collect(Collectors.toList());


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
            }}


            // ⭐ פונקציית עזר ב-Controller לבדיקה האם המשתמש לחץ ⭐
             boolean isLikedByUser(Challenge challenge, Long currentUserId) {
                if (challenge == null || challenge.getLikedByUserIds() == null || currentUserId == null) {
                    return false;
                }
                String userIdStr = currentUserId.toString();
                String ids = challenge.getLikedByUserIds();

                // יצירת Set מופרד בפסיקים ובדיקה מהירה
                Set<String> likedUsers = new HashSet<>(Arrays.asList(ids.split(",")));
                likedUsers.remove(""); // מנקה איברים ריקים במקרה של מחרוזת ריקה
                return likedUsers.contains(userIdStr);
            }
            // -------------------------------------------------------------------------
            // ⭐⭐ POST: Toggle Like לאתגר (חדש!) ⭐⭐
            // -------------------------------------------------------------------------

        @PostMapping("/addLike/{challengeId}")
        public ResponseEntity<Void> addLike(@PathVariable Long challengeId) {
            Challenge challenge;

            try {
                // ⭐⭐ תיקון 1: בדיקה אמינה יותר למשתמש מחובר ⭐⭐
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                // אם אין אימות, או המשתמש הוא "anonymousUser" - תחזיר מייד 401
                if (authentication == null || !authentication.isAuthenticated() ||
                        "anonymousUser".equals(authentication.getPrincipal())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
                }

                String username = authentication.getName();
                Users user = usersRepository.findByUsername(username);

                // אם המשתמש לא נמצא ב-DB למרות שיש לו טוקן - זה מוזר, נחזיר 401
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401
                }

                // 3. שליפת האתגר
                challenge = challengeRepository.findById(challengeId)
                        .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + challengeId));

                Long currentUserId = user.getId();
                String userIdStr = currentUserId.toString();

                // ⭐⭐ בדיקה 1: מניעת לייק עצמי ⭐⭐
                if (challenge.getUser().getId().equals(currentUserId)) {
                    // 400 Forbidden - אסור ליוצר האתגר לעשות לייק.
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }

                // ⭐⭐ בדיקה 2: רק מי שנרשם לאתגר יכול לעשות לייק ⭐⭐
                // חובה: JoinerRepository חייב להכיל את המתודה findByUserAndChallenge
                boolean isUserJoined = joinerRepository.findByUserAndChallenge(user, challenge).isPresent();

                if (!isUserJoined) {
                    // 403 Forbidden - המשתמש לא רשום לאתגר.
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }


                // 4. פיצול המחרוזת ל-IDs והכנה ל-TOGGLE
                String currentIdsString = challenge.getLikedByUserIds() != null ? challenge.getLikedByUserIds() : "";

// מפרק את המחרוזת, מנקה רווחים מכל ID, ומסנן ID's ריקים.
                Set<String> likedUsers = Arrays.stream(currentIdsString.split(","))
                        .map(String::trim) // ⭐⭐⭐ התיקון: חותך רווחים ⭐⭐⭐
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toSet());
                // 5. לוגיקת TOGGLE
                if (likedUsers.contains(userIdStr)) {
                    // אם המשתמש כבר נתן לייק - מוחקים (Unlike)
                    likedUsers.remove(userIdStr);
                } else {
                    // אם המשתמש לא נתן לייק - מוסיפים (Like)
                    likedUsers.add(userIdStr);
                }

                // 6. איחוד המערך חזרה למחרוזת ושמירה
                String newLikedUserIds = String.join(",", likedUsers);
                challenge.setLikedByUserIds(newLikedUserIds);
                challengeRepository.save(challenge);

                return ResponseEntity.ok().build();
            } catch (NoSuchElementException e) {
                // מטפל במקרה שהאתגר לא נמצא
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } catch (Exception e) {
                System.out.println("Error toggling like: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
        }



















