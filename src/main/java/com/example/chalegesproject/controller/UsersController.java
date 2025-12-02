package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChatRequest;
import com.example.chalegesproject.dto.ChatResponse;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.security.CustomUserDetails;
import com.example.chalegesproject.security.jwt.JwtUtils;
import com.example.chalegesproject.service.AIChatService;
import com.example.chalegesproject.service.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/api/users")
public class UsersController {

    private UsersRepository usersRepository;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;

    @Autowired
    public UsersController(UsersRepository usersRepository, AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.usersRepository = usersRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
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

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(HttpServletRequest request, @RequestBody Users user) {

        // 1. בדיקה האם המשתמש כבר מחובר (באמצעות Cookie JWT)
        String jwt = jwtUtils.getJwtFromCookies(request);

        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String existingUsername = jwtUtils.getUserNameFromJwtToken(jwt);
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN) // 403 Forbidden
                    .body(existingUsername +
                            " שגיאה: את/ה כבר מחובר/ת כמשתמש");
        }

        // 2. בדיקה האם שם המשתמש קיים במסד הנתונים
        Users u = usersRepository.findByUsername(user.getUsername());
        if (u != null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict
                    .body("שגיאה: שם המשתמש " + user.getUsername() + " כבר קיים במערכת!");
        }


        // 3. הצפנה ושמירה
        String pass = user.getPassword(); // שמירת הסיסמה הלא מוצפנת
        user.setPassword(new BCryptPasswordEncoder().encode(pass));
        Users savedUser = usersRepository.save(user); // שמירת המשתמש החדש

        try {
            // 4. יצירת אובייקט Authentication עבור המשתמש החדש, באמצעות הסיסמה הלא מוצפנת שנשמרה
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(savedUser.getUsername(), pass));

            // 5. שמירת האובייקט ב-SecurityContext (כניסה למערכת)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 6. יצירת עוגיית JWT
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

            // 7. החזרת התגובה עם העוגייה ב-Header (JWT Cookie)
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(userDetails.getUsername() + " נרשם וחובר בהצלחה!");

        } catch (Exception e) {
            // 🛑 **התיקון הקריטי:** הדפסת השגיאה ליומן השרת
            System.err.println("❌ שגיאה בניסיון לחבר משתמש לאחר רישום: " + e.getMessage());
            e.printStackTrace();

            // 🛑 החזרת הודעה שמכילה את השגיאה, ללא עוגייה
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedUser.getUsername() +
                            " נרשם בהצלחה, אך לא ניתן היה לחבר אוטומטית. (שגיאת אימות: " + e.getMessage() + ")");
        }
    }
    @PostMapping("/signin")
    public ResponseEntity<?> signin(@RequestBody Users u) {

        // 1. בדיקה אם המשתמש כבר מחובר
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        // בודק אם קיים אימות והוא לא אנונימי (כלומר, מישהו כבר מחובר)
        if (existingAuth != null && existingAuth.isAuthenticated()
                && !(existingAuth instanceof AnonymousAuthenticationToken)) {

            // אם המשתמש המחובר כרגע הוא אותו משתמש שמנסה להתחבר שוב:
            if (existingAuth.getName().equals(u.getUsername())) {
                // מחזירים סטטוס 200 OK עם הודעת "כבר מחובר"
                return ResponseEntity.ok()
                        .body("אתה כבר מחובר כ-" + u.getUsername());
            }
        }

        // 2. אם לא מחובר, ממשיכים בתהליך האימות הרגיל
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(u.getUsername(), u.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(userDetails.getUsername());
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOut() {
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body("you've been signed out! ");
    }
    // בתוך המחלקה UsersController
// ...
// ...

    // --- GET סטטוס משתמש מחובר (נדרש לאנגולר) ---
    @GetMapping("/is-logged-in")
    public ResponseEntity<Boolean> getCurrentUserStatus() {
        // 1. קבלת אובייקט האימות מה-SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. בדיקה האם המשתמש מאומת ואינו משתמש "אנונימי" (כלומר, מחובר)
        boolean isAuthenticated = authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);

        // 3. החזרת true או false
        return ResponseEntity.ok(isAuthenticated);

        // אם היית רוצה להחזיר את שם המשתמש:
    /*
    if (isAuthenticated) {
        return ResponseEntity.ok(authentication.getName());
    } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }
    */
    }
    // דוגמה ב-Java (User Controller)

    }