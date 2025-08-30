package com.ssafy.tiggle.service.videostorage;

import java.io.IOException;

public interface VideoStorageService {
    void saveVideo(byte[] videoBytes, String filename) throws IOException;
    byte[] loadVideo(String filepath) throws IOException;
    void renameVideo(String oldFilename, String newFilename) throws IOException;
}