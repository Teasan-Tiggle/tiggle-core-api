package com.ssafy.tiggle.service.image;

import java.util.List;

public interface ImageGenerationService {

    List<byte[]> generateImageSeriesByScript(String script);
}