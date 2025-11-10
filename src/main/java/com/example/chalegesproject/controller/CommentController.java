package com.example.chalegesproject.controller;

import com.example.chalegesproject.dto.ChallengeDto;
import com.example.chalegesproject.dto.CommentDto;
import com.example.chalegesproject.model.Challenge;
import com.example.chalegesproject.model.Comment;
import com.example.chalegesproject.model.Users;
import com.example.chalegesproject.service.*;
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

    @PostMapping("/add")//הוספת תגובה
    public ResponseEntity<CommentDto> uploadCommentWithImage(@RequestPart("image") MultipartFile file
            ,@RequestPart("comment") CommentDto c) {
        try {
            c.setImagePath(file.getOriginalFilename());//השם של התמונה
            ImageUtils.saveImage(file);

            Users user=usersRepository.findById(c.getUserId()).get();
            Challenge challenge=challengeRepository.findById(c.getChallengeId()).get();

            Comment comment=commentRepository.save(commentMapper.dtoToComment(c,user,challenge));
            return new ResponseEntity<>(commentMapper.commentToDto(comment),HttpStatus.CREATED);

        } catch (IOException e) {
            System.out.println(e);
            return new ResponseEntity<>(null,HttpStatus.INTERNAL_SERVER_ERROR);
        }








}
    }