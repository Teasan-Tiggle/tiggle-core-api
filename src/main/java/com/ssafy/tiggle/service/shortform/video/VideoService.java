package com.ssafy.tiggle.service.shortform.video;

import com.ssafy.tiggle.entity.EsgCategory;
import com.ssafy.tiggle.entity.Video;
import com.ssafy.tiggle.repository.VideoRepository;
import com.ssafy.tiggle.service.videostorage.VideoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {
    
    private final VideoRepository videoRepository;
    private final VideoStorageService videoStorageService;
    
    public Video createVideo(String title, byte[] videoBytes, EsgCategory esgCategory) {
        try {
            // 1. 비디오 저장 (파일명은 임시로 생성, 나중에 ID로 교체)
            String tempFileName = "temp_" + System.currentTimeMillis() + ".mp4";
            videoStorageService.saveVideo(videoBytes, tempFileName);
            
            // 2. Video 엔티티 생성 및 저장
            Video video = new Video();
            video.setTitle(title);
            video.setEsgCategory(esgCategory);
            video.setFileName(tempFileName); // 임시 파일명
            
            Video savedVideo = videoRepository.save(video);
            
            // 3. 실제 파일명을 ID 기반으로 변경
            String finalFileName = savedVideo.getId() + ".mp4";
            videoStorageService.renameVideo(tempFileName, finalFileName);
            
            // 4. DB의 파일명 업데이트
            savedVideo.setFileName(finalFileName);
            videoRepository.save(savedVideo);
            
            log.info("비디오 생성 완료 - ID: {}, 파일명: {}", savedVideo.getId(), finalFileName);
            return savedVideo;
            
        } catch (Exception e) {
            log.error("비디오 생성 실패", e);
            throw new RuntimeException("비디오 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    public byte[] downloadVideo(Long videoId) {
        try {
            Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("비디오를 찾을 수 없습니다: " + videoId));
            
            byte[] videoBytes = videoStorageService.loadVideo(video.getFileName());
            log.info("비디오 다운로드 완료 - ID: {}, 파일명: {}", videoId, video.getFileName());
            
            return videoBytes;
            
        } catch (IOException e) {
            log.error("비디오 다운로드 실패 - ID: {}", videoId, e);
            throw new RuntimeException("비디오 다운로드에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    public Video findById(Long videoId) {
        return videoRepository.findById(videoId)
            .orElseThrow(() -> new RuntimeException("비디오를 찾을 수 없습니다: " + videoId));
    }
}