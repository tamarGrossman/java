package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChatRequest;
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
    private AIChatService aiChatService;

    @Autowired
    public UsersController(UsersRepository usersRepository, AuthenticationManager authenticationManager, JwtUtils jwtUtils, AIChatService aiChatService) {
        this.usersRepository = usersRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.aiChatService = aiChatService;
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
    public ResponseEntity<?> signUp(HttpServletRequest request, @RequestBody Users user) {

        // 1. בדיקה האם המשתמש כבר מחובר (באמצעות Cookie JWT)
        String jwt = jwtUtils.getJwtFromCookies(request);

        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            // אם קיים JWT תקף, המשתמש כבר מחובר
            String existingUsername = jwtUtils.getUserNameFromJwtToken(jwt);

            // החזרת 403 Forbidden - כי המשתמש אינו מורשה לבצע רישום כשהוא מחובר
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN) // <--- תיקון: 403 Forbidden
                    .body(existingUsername +
                            " שגיאה: את/ה כבר מחובר/ת כמשתמש    ");
        }

        // 2. בדיקה האם שם המשתמש קיים במסד הנתונים
        Users u = usersRepository.findByUsername(user.getUsername());
        if (u != null) {
            // החזרת 409 Conflict - כי יש קונפליקט עם משאב קיים
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // <--- תיקון: 409 Conflict
                    .body("שגיאה: שם המשתמש " + user.getUsername() + " כבר קיים במערכת!");
        }

        // 3. הצפנה ושמירה
        String pass = user.getPassword();
        user.setPassword(new BCryptPasswordEncoder().encode(pass));
        Users savedUser = usersRepository.save(user);

        // החזרת שם המשתמש במקרה הצלחה
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser.getUsername());
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
    @GetMapping("/chat")
    public String getResponse(@RequestBody ChatRequest chatRequest){
        return aiChatService.getResponse(chatRequest.message(), chatRequest.conversationId());
    }
}