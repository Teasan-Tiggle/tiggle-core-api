package com.ssafy.tiggle.service.tts;

import com.google.cloud.texttospeech.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextToSpeechServiceImpl implements TextToSpeechService {

    private final TextToSpeechClient textToSpeechClient;

    private byte[] synthesizeSpeechKorean(String text) {
        try {
            SynthesisInput input = SynthesisInput.newBuilder()
                    .setText(text)
                    .build();

            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("ko-KR")
                    .setName("ko-KR-Standard-A")
                    .setSsmlGender(SsmlVoiceGender.NEUTRAL)
                    .build();

            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)
                    .build();

            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(
                    input, voice, audioConfig);

            log.info("한국어 TTS 음성 합성이 완료되었습니다. 텍스트 길이: {}", text.length());

            return response.getAudioContent().toByteArray();

        } catch (Exception e) {
            log.error("한국어 TTS 음성 합성에 실패했습니다. 텍스트: {}", text, e);
            throw new RuntimeException("음성 합성에 실패했습니다.", e);
        }
    }

    @Override
    public List<byte[]> synthesizeSpeechKoreanBySentences(String script) {
        try {
            log.info("문장별 TTS 생성 시작 - 전체 스크립트 길이: {}", script.length());

            // 줄바꿈으로 문장 분리 (실제 줄바꿈과 문자열 "\n" 둘 다 처리)
            String normalizedScript = script.replace("\\n", "\n");
            List<String> sentences = Arrays.stream(normalizedScript.split("\n"))
                    .map(String::trim)
                    .filter(sentence -> !sentence.isEmpty())
                    .collect(Collectors.toList());

            log.info("총 {} 개의 문장으로 분리됨", sentences.size());

            // 각 문장별로 TTS 생성
            List<byte[]> ttsFiles = sentences.stream()
                    .map(sentence -> {
                        log.debug("문장 TTS 생성 중: {}", sentence);
                        return synthesizeSpeechKorean(sentence);
                    })
                    .collect(Collectors.toList());

            log.info("문장별 TTS 생성 완료 - {} 개의 음성 파일 생성됨", ttsFiles.size());
            return ttsFiles;

        } catch (Exception e) {
            log.error("문장별 TTS 생성에 실패했습니다. 스크립트: {}", script, e);
            throw new RuntimeException("문장별 음성 합성에 실패했습니다.", e);
        }
    }
}