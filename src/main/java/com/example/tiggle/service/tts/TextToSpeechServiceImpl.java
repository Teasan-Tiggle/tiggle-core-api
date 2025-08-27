package com.example.tiggle.service.tts;

import com.google.cloud.texttospeech.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextToSpeechServiceImpl implements TextToSpeechService {

    private final TextToSpeechClient textToSpeechClient;

    @Override
    public byte[] synthesizeSpeechKorean(String text) {
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
}