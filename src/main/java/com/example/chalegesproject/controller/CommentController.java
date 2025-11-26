package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.CommentDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Comment;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@RestController
@RequestMapping("/api/comment")
public class CommentController   {
    private final CommentRepository commentRepository;
    private final UsersRepository usersRepository;
    private final ChallengeRepository challengeRepository;
    private final CommentMapper commentMapper;


    @Autowired
    public CommentController(CommentRepository commentRepository,
                             UsersRepository usersRepository,
                             ChallengeRepository challengeRepository,
                             CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.usersRepository = usersRepository;
        this.challengeRepository = challengeRepository;
        this.commentMapper = commentMapper;
    }


    @GetMapping("/getAll")//כל התגובות-רשימה
    public ResponseEntity<List<CommentDto>> getAllComments() {
        try {
        List<Comment> comments = commentRepository.findAll();

            List<CommentDto> commentDtos=commentMapper.toCommentesDTO(comments);
        if (commentDtos.isEmpty()) {
            // אם אין תגובות, נחזיר 204 No Content
            return ResponseEntity.noContent().build();
        } else {
            // אם יש תגובות, נחזיר 200 OK עם הרשימה
            return ResponseEntity.ok(commentDtos);
        } } catch (Exception e) {
            // במקרה של שגיאה פנימית
            System.out.println("Error fetching comment: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

//    @PostMapping("/add")//הוספת תגובה
//    public ResponseEntity<CommentDto> uploadCommentWithImage(@RequestPart("image") MultipartFile file
//            ,@RequestPart("comment") CommentDto c) {
//        try {
//            c.setImagePath(file.getOriginalFilename());//השם של התמונה
//            ImageUtils.saveImage(file);
//
//            Users user=usersRepository.findById(c.getUserId()).get();
//            Challenge challenge=challengeRepository.findById(c.getChallengeId()).get();
//
//            Comment comment=commentRepository.save(commentMapper.dtoToComment(c,user,challenge));
//            return new ResponseEntity<>(commentMapper.commentToDto(comment),HttpStatus.CREATED);
//
//        } catch (IOException e) {
//            System.out.println(e);
//            return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
//        }

//
//        @PostMapping("/comments/add") // ⬅️ הנתיב הזה חייב להיות מוגן ב-SecurityConfig
//        public ResponseEntity<?> addComment(@RequestBody CommentRequest commentRequest) {
//            try {
//                // 1. **שליפת שם המשתמש המאומת**
//                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//                String username = authentication.getName();
//
//                // 2. **שליפת אובייקט המשתמש המלא מה-DB**
//                Users user = usersRepository.findByUsername(username)
//                        .orElseThrow(() -> new InternalServerErrorException("Authentication mismatch: User not found."));
//
//                // 3. **שליפת האתגר שאליו מתייחסת התגובה**
//                Challenge challenge = challengeRepository.findById(commentRequest.getChallengeId())
//                        .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + commentRequest.getChallengeId()));
//
//                // 4. **יצירת ושמירת אובייקט התגובה**
//                Comment comment = new Comment();
//                comment.setUser(user);
//                comment.setChallenge(challenge);
//                comment.setContent(commentRequest.getContent());
//                comment.setDate(LocalDate.now());
//
//                commentRepository.save(comment);
//
//                return ResponseEntity.status(HttpStatus.CREATED).body("תגובה נוספה בהצלחה");
//
//            } catch (NoSuchElementException e) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
//            } catch (Exception e) {
//                System.err.println("Error adding comment: " + e.getMessage());
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("שגיאה פנימית בשרת.");
//            }
// --- POST הוספת תגובה לאתגר (מאובטח באמצעות Token) ---

@PreAuthorize("isAuthenticated()") // ⬅️ דורש טוקן מאומת
@PostMapping(value = "/add/{challengeId}", consumes = "multipart/form-data") // ⬅️ הנתיב מקבל את ה-Challenge ID
public ResponseEntity<?> addComment(
        @PathVariable Long challengeId,
        @RequestPart("commentData") CommentDto commentDto,@RequestPart("image") MultipartFile file) {
    try {
        // 1. קבלת שם המשתמש מתוך ה-Token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        // 2. מציאת אובייקט המשתמש המאומת
        Users user = usersRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found or session invalid.");
        }

        // 3. שליפת אובייקט האתגר
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + challengeId));

        commentDto.setImagePath(file.getOriginalFilename());//השם של התמונה
        ImageUtils.saveImage(file);

        Comment comment1=commentRepository.save(commentMapper.dtoToComment(commentDto,user,challenge));

        return ResponseEntity.status(HttpStatus.CREATED).build();

    } catch (NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (Exception e) {
        System.out.println("Error adding comment: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("שגיאה פנימית בשרת: " + e.getMessage());
    }
}

}









//        // 4. יצירת אובייקט Comment והעתקת הערכים מה־DTO
//        Comment comment = new Comment();
//        comment.setUser(user);
//        comment.setChallenge(challenge);
//        comment.setContent(commentDto.getContent());
//        comment.setPicture(commentDto.getPicture());
//        comment.setDate(LocalDate.now()); // תאריך פרסום אוטומטי

// אם יש imagePath ב־DTO אפשר גם לשים אותו אם זה רלוונטי
//        if (commentDto.getImagePath() != null) {
//            comment.setPicture(commentDto.getImagePath());


// 5. שמירה ב-DB
//        commentRepository.save(comment);