package com.example.chalegesproject.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class ImageUtils {
    private static String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "\\uploads\\";

public static String getImage(String imagePath) {
    try {
        Path path = Paths.get(UPLOAD_DIRECTORY+imagePath);
        byte[] bytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(bytes);
    } catch (IOException e) {
        return null; // או תמונה ברירת מחדל
    }
}


 public static String saveImage(MultipartFile file) throws IOException {

        String fileName = UPLOAD_DIRECTORY + file.getOriginalFilename();
        Path path = Paths.get(fileName);

        // מודא שהתיקייה 'uploads' קיימת
        Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        file.transferTo(path.toFile());
        System.out.println("🎉 File successfully saved!");
        return fileName;
    }
}

