package com.ssafy.tiggle.service.shortform.tts;

import java.util.List;

public interface TextToSpeechService {

    List<byte[]> synthesizeSpeechKoreanBySentences(String script);
}