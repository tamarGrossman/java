package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.CommentDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Comment;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.*;
import jakarta.validation.Valid;
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

// --- POST הוספת תגובה לאתגר
@PostMapping(value = "/add/{challengeId}", consumes = "multipart/form-data")
public ResponseEntity<?> addComment(
        @PathVariable Long challengeId,
       @Valid @RequestPart("commentData") CommentDto commentDto,
        @RequestPart(value = "image", required = false) MultipartFile file) { // required = false זה נכון!
    try {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();


        Users user = usersRepository.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found or session invalid.");
        }


        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NoSuchElementException("אתגר לא נמצא: ID " + challengeId));



        if (file != null && !file.isEmpty()) {
            commentDto.setImagePath(file.getOriginalFilename());
            ImageUtils.saveImage(file);
        } else {
            commentDto.setImagePath(null);
        }

        Comment comment1 = commentRepository.save(commentMapper.dtoToComment(commentDto, user, challenge));

        return ResponseEntity.status(HttpStatus.CREATED).build();

    } catch (NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (Exception e) {
        System.out.println("Error adding comment: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("שגיאה פנימית בשרת: " + e.getMessage());
    }
}


@GetMapping("/getByChallenge/{challengeId}")
public ResponseEntity<List<CommentDto>> getCommentsByChallengeId(@PathVariable long challengeId) {
    try {

        List<Comment> comments = commentRepository.findByChallengeId(challengeId);


        List<CommentDto> commentDtos = commentMapper.toCommentesDTO(comments);


        if (commentDtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(commentDtos);
        }

    } catch (Exception e) {
        System.out.println("Error fetching comments for challenge " + challengeId + ": " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

//כל התגובותשהעלה משתמש מסוים
    @GetMapping("/my-comments") // ✅ אין יותר {userId} בנתיב
    public ResponseEntity<?> getMyComments() { // ✅ הפונקציה לא מקבלת פרמטרים מבחוץ
        try {
            // 1. קבלת המשתמש אך ורק מהעוגייה (SecurityContext)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            Users authenticatedUser = usersRepository.findByUsername(username);

            if (authenticatedUser == null) {
                return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
            }
            Long realUserId = authenticatedUser.getId();

            List<Comment> comments = commentRepository.findByUser_Id(realUserId);

            List<CommentDto> commentDtos = commentMapper.toCommentesDTO(comments);

            if (commentDtos.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(commentDtos);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    }
