package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.UpdatePiggySettingsRequest;
import com.example.tiggle.dto.piggy.response.EsgCategoryDto;
import com.example.tiggle.dto.piggy.response.PiggyBankResponse;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.piggy.PiggyBankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggyServiceImpl implements PiggyService {

    private final PiggyBankRepository piggyBankRepository;
    private final EsgCategoryRepository esgCategoryRepository;

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(Integer userId) {
        return Mono.fromCallable(() ->
                        piggyBankRepository.findByOwner_Id(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."))
                ).map(this::toResponse)
                .map(ApiResponse::success)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> updateSettings(Integer userId, UpdatePiggySettingsRequest req) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            if (req.getName() != null)          piggy.setName(req.getName());
            if (req.getTargetAmount() != null)  piggy.setTargetAmount(req.getTargetAmount());
            if (req.getAutoDonation() != null)  piggy.setAutoDonation(req.getAutoDonation());
            if (req.getAutoSaving() != null)    piggy.setAutoSaving(req.getAutoSaving());

            if (req.getEsgCategoryId() != null) {
                if (req.getEsgCategoryId() <= 0) {
                    piggy.setEsgCategory(null);
                } else {
                    EsgCategory cat = esgCategoryRepository.findById(req.getEsgCategoryId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 ESG 카테고리입니다."));
                    piggy.setEsgCategory(cat);
                }
            }

            piggyBankRepository.save(piggy);

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> setCategory(Integer userId, Long categoryId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            EsgCategory cat = esgCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "존재하지 않는 ESG 카테고리입니다."));
            piggy.setEsgCategory(cat);
            piggyBankRepository.save(piggy);

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Integer userId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다. 먼저 생성해주세요."));

            piggy.setEsgCategory(null);
            piggyBankRepository.save(piggy);

            PiggyBank reloaded = piggyBankRepository.findByOwner_Id(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저금통이 없습니다."));
            return ApiResponse.success(toResponse(reloaded));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private PiggyBankResponse toResponse(PiggyBank p) {
        return new PiggyBankResponse(
                p.getId(),
                p.getName(),
                p.getCurrentAmount(),
                p.getTargetAmount(),
                p.getSavingCount(),
                p.getDonationCount(),
                p.getDonationTotalAmount(),
                p.getAutoDonation(),
                p.getAutoSaving(),
                p.getEsgCategory() == null ? null :
                        new EsgCategoryDto(
                                p.getEsgCategory().getId(),
                                p.getEsgCategory().getName(),
                                p.getEsgCategory().getDescription(),
                                p.getEsgCategory().getCharacterName()
                        )
        );
    }
}
