package com.ssafy.tiggle.service.videostorage;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoStorageServiceImpl implements VideoStorageService {

    private String storagePath = "/Users/ijonghyeon/Desktop/SSAFY/video";

    @Override
    public void saveVideo(byte[] videoBytes, String filename) throws IOException {
        Path dirPath = Paths.get(storagePath);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        Path filePath = dirPath.resolve(filename);
        Files.write(filePath, videoBytes);
    }

    @Override
    public byte[] loadVideo(String filename) throws IOException {
        Path filePath = Paths.get(storagePath).resolve(filename);
        return Files.readAllBytes(filePath);
    }
    
    @Override
    public void renameVideo(String oldFilename, String newFilename) throws IOException {
        Path oldPath = Paths.get(storagePath).resolve(oldFilename);
        Path newPath = Paths.get(storagePath).resolve(newFilename);
        Files.move(oldPath, newPath);
    }
}
