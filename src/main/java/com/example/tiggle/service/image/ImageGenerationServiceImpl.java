package com.example.tiggle.service.image;

import com.google.cloud.aiplatform.v1.*;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final PredictionServiceClient predictionServiceClient;

    @org.springframework.beans.factory.annotation.Value("${google.cloud.vertex-ai.project-id}")
    private String projectId;

    @org.springframework.beans.factory.annotation.Value("${google.cloud.vertex-ai.location}")
    private String location;

    @Override
    public byte[] generateImage(String prompt) {
        try {
            String endpoint = String.format("projects/%s/locations/%s/publishers/google/models/imagen-3.0-generate-001",
                    projectId, location);

            // 요청 인스턴스 생성
            String instanceJson = String.format("""
                {
                    "prompt": "%s"
                }
                """, prompt.replace("\"", "\\\""));

            String parametersJson = """
                {
                    "sampleCount": 1,
                    "aspectRatio": "9:16",
                    "safetyFilterLevel": "block_some",
                    "personGeneration": "dont_allow"
                }
                """;

            Value.Builder instanceBuilder = Value.newBuilder();
            JsonFormat.parser().merge(instanceJson, instanceBuilder);
            List<Value> instances = new ArrayList<>();
            instances.add(instanceBuilder.build());

            Value.Builder parametersBuilder = Value.newBuilder();
            JsonFormat.parser().merge(parametersJson, parametersBuilder);

            // 예측 요청
            PredictRequest predictRequest = PredictRequest.newBuilder()
                    .setEndpoint(endpoint)
                    .addAllInstances(instances)
                    .setParameters(parametersBuilder.build())
                    .build();

            PredictResponse response = predictionServiceClient.predict(predictRequest);

            if (response.getPredictionsCount() > 0) {
                Value prediction = response.getPredictions(0);

                // Base64로 인코딩된 이미지 데이터 추출
                String bytesBase64Encoded = prediction.getStructValue()
                        .getFieldsMap().get("bytesBase64Encoded").getStringValue();

                byte[] imageBytes = Base64.getDecoder().decode(bytesBase64Encoded);

                log.info("Imagen을 통한 이미지 생성이 완료되었습니다. 프롬프트: {}, 이미지 크기: {} bytes",
                        prompt, imageBytes.length);

                return imageBytes;
            } else {
                throw new RuntimeException("이미지 생성 응답이 비어있습니다.");
            }

        } catch (InvalidProtocolBufferException e) {
            log.error("Imagen 이미지 생성 중 JSON 파싱 에러가 발생했습니다. 프롬프트: {}", prompt, e);
            throw new RuntimeException("이미지 생성 중 요청 형식 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("Imagen 이미지 생성에 실패했습니다. 프롬프트: {}", prompt, e);
            throw new RuntimeException("이미지 생성에 실패했습니다.", e);
        }
    }
}