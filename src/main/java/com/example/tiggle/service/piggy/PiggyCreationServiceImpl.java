package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.CreatePiggyBankRequest;
import com.example.tiggle.dto.piggy.response.PiggySummaryResponse;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.entity.Student;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.piggy.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.service.finopenapi.FinancialApiService;
import com.example.tiggle.service.security.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggyCreationServiceImpl implements PiggyCreationService {

    private final PiggyBankRepository piggyBankRepository;
    private final StudentRepository studentRepository;
    private final EsgCategoryRepository esgCategoryRepository;
    private final FinancialApiService financialApiService;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public Mono<ApiResponse<PiggySummaryResponse>> create(String encryptedUserKey, Integer userId, CreatePiggyBankRequest req) {
        String userKey = encryptionService.decrypt(encryptedUserKey);
        return Mono.fromCallable(() -> {
            // 1) 중복 방지
            piggyBankRepository.findByOwner_Id(userId).ifPresent(pb -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 개설된 저금통이 있습니다.");
            });

            // 2) 사용자 확인
            Student owner = studentRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자를 찾을 수 없습니다."));

            // 3) 금융 API: 예금계좌 생성
            var finRes = financialApiService.createDemandDepositAccount(userKey)
                    .onErrorMap(e -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융API 호출 실패", e))
                    .block();

            if (finRes == null || finRes.getHeader() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융API 응답이 올바르지 않습니다.");
            }
            if (!"H0000".equals(finRes.getHeader().getResponseCode())) {
                String msg = finRes.getHeader().getResponseMessage();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "금융 계좌 개설 실패: " + (msg != null ? msg : "알 수 없는 오류"));
            }

            String createdAccountNo = finRes.getRec() != null ? finRes.getRec().getAccountNo() : null;
            if (createdAccountNo == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "생성된 계좌번호가 누락되었습니다.");
            }
            log.info("[Piggy][Create] userId={}, createdAccountNo={}", userId, createdAccountNo);

            PiggyBank pb = PiggyBank.builder()
                    .owner(owner)
                    .name(req.getName())
                    .targetAmount(req.getTargetAmount())
                    .accountNo(createdAccountNo)
                    .build();

            if (req.getEsgCategoryId() != null) {
                EsgCategory cat = esgCategoryRepository.findById(req.getEsgCategoryId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "ESG 카테고리를 찾을 수 없습니다."));
                pb.setEsgCategory(cat);
            }

            PiggyBank saved = piggyBankRepository.save(pb);

            PiggySummaryResponse body =
                    new PiggySummaryResponse(saved.getName(), saved.getCurrentAmount(), BigDecimal.ZERO);

            return ApiResponse.success(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
