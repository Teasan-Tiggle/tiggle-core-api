package com.ssafy.tiggle.dto.donation.request;

import com.ssafy.tiggle.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonationRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    @Schema(description = "esg 카테고리", example = "PLANET")
    private Category category;

    @NotNull(message = "기부금액은 필수입니다.")
    @Schema(description = "기부금액", example = "1000")
    private Long amount;
}
