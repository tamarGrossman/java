package com.example.chalegesproject.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageUtils {
    private static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "\\uploads\\";

//     המרה מתמונה מקובץ ל-Base64 (נראה תקין)
//    public static String getImage(String imagePath) throws IOException {
//        Path path = Paths.get(imagePath);
//        byte[] bytes = Files.readAllBytes(path);
//        return Base64.getEncoder().encodeToString(bytes);
//    }
public static String getImage(String imagePath) {
    try {
        Path path = Paths.get(UPLOAD_DIRECTORY+imagePath);
        byte[] bytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
        System.out.println("⚠️ File not found or cannot read: " + imagePath);
        return null; // או תמונה ברירת מחדל
    }
}



    // 🌟 התיקון לשמירת התמונה שהועלתה
    public static String saveImage(MultipartFile file) throws IOException {

        String fileName = UPLOAD_DIRECTORY + file.getOriginalFilename();
        Path path = Paths.get(fileName);

        // 1. ודא שהתיקייה 'uploads' קיימת
        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        // בתוך saveImage:
        System.out.println("✅ Saving file to: " + path.toAbsolutePath());

        // 2. 🌟 התיקון: העברת הקובץ שהועלה ישירות לנתיב החדש
        // (מחליף את הקריאה והכתיבה המוטעות שהיו קודם)
        file.transferTo(path.toFile());
        System.out.println("🎉 File successfully saved!");
        return fileName;
    }
}

