package com.ssafy.tiggle.service.tts;

import java.util.List;

public interface TextToSpeechService {

    List<byte[]> synthesizeSpeechKoreanBySentences(String script);
}