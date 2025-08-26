package com.example.tiggle.service.dutchpay;

import com.example.tiggle.dto.dutchpay.request.DutchpayDetailData;
import com.example.tiggle.dto.dutchpay.response.DutchpaySummaryResponse;
import com.example.tiggle.repository.dutchpay.DutchpayQueryRepository;
import com.example.tiggle.repository.dutchpay.projection.DutchpayDetailProjection;
import com.example.tiggle.repository.dutchpay.projection.DutchpaySummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DutchpayReadService {

    private final DutchpayQueryRepository queryRepo;
    private static final String TRANSFERRED_STATUS = "PAID";

    public DutchpayDetailData getDetail(Long dutchpayId, Long userId) {
        DutchpayDetailProjection p = queryRepo.findDetail(dutchpayId, userId);
        if (p == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "더치페이를 찾을 수 없습니다.");
        }

        boolean isCreator = p.getCreatorId() != null && p.getCreatorId().equals(userId);

        if (!isCreator && (p.getMyAmount() == null || p.getMyAmount() == 0L)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 더치페이에 접근 권한이 없습니다.");
        }

        long original = (p.getOriginalAmount() != null && p.getOriginalAmount() > 0)
                ? p.getOriginalAmount()
                : (p.getMyAmount() == null ? 0L : p.getMyAmount());

        long rounded = roundUpTo100(original); // 100원 단위 올림
        long tiggle  = Math.max(0, rounded - original);

        return new DutchpayDetailData(
                p.getDutchpayId(),
                p.getTitle(),
                p.getMessage(),
                p.getRequesterName(),
                p.getParticipantCount() == null ? 0 : p.getParticipantCount(),
                p.getTotalAmount(),
                p.getCreatedAt(),
                rounded,   // 내가 내는 금액(올림값)
                original,  // 원래 금액
                tiggle     // 티끌(올림-원래)
        );
    }

    private long roundUpTo100(long amount) {
        if (amount <= 0) return 0;
        return ((amount + 99) / 100) * 100;
    }

    public DutchpaySummaryResponse getSummary(Long userId) {
        DutchpaySummaryProjection p = queryRepo.summarizeByUserId(userId, TRANSFERRED_STATUS);

        long total = (p == null || p.getTotalTransferredAmount() == null) ? 0L : p.getTotalTransferredAmount();
        long tcnt  = (p == null || p.getTransferCount() == null)          ? 0L : p.getTransferCount();
        long pcnt  = (p == null || p.getParticipatedCount() == null)      ? 0L : p.getParticipatedCount();

        return DutchpaySummaryResponse.builder()
                .totalTransferredAmount(total)
                .transferCount(tcnt)
                .participatedCount(pcnt)
                .build();
    }
}
