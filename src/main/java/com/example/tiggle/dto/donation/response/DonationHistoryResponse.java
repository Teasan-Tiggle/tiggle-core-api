package com.example.tiggle.dto.donation.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationHistoryResponse {

    @Schema(description = "esg 카테고리", example = "PLANET")
    private String category;

    @Schema(description = "기부금액", example = "1000")
    private Long amount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "기부일시", example = "2025-08-24T19:38:00")
    private LocalDateTime donatedAt;

    @Schema(description = "기부기록", example = "PLANET")
    private String title;
}
