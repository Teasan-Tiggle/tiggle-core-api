package com.ssafy.tiggle.dto.dutchpay.response;

import lombok.Builder;

import java.util.List;

@Builder
public record DutchpayListResponse(
        List<DutchpayListItemResponse> items,
        String nextCursor,
        boolean hasNext
) {}
