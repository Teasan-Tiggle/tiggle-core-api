package com.example.tiggle.service.piggy;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.common.ApiResponse;
import com.example.tiggle.dto.piggy.request.UpdatePiggySettingsRequest;
import com.example.tiggle.dto.piggy.response.EsgCategoryDto;
import com.example.tiggle.dto.piggy.response.PiggyBankResponse;
import com.example.tiggle.entity.EsgCategory;
import com.example.tiggle.entity.PiggyBank;
import com.example.tiggle.entity.Student;
import com.example.tiggle.repository.esg.EsgCategoryRepository;
import com.example.tiggle.repository.piggy.PiggyBankRepository;
import com.example.tiggle.repository.user.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PiggyServiceImpl implements PiggyService {

    private final PiggyBankRepository piggyBankRepository;
    private final EsgCategoryRepository esgCategoryRepository;
    private final StudentRepository studentRepository;

    @Override
    public Mono<ApiResponse<PiggyBankResponse>> getMyPiggy(Integer userId) {
        return Mono.fromCallable(() -> toResponse(getOrCreatePiggy(userId)))
                .map(ApiResponse::success)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<ApiResponse<PiggyBankResponse>> updateSettings(Integer userId, UpdatePiggySettingsRequest req) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = getOrCreatePiggy(userId);

            if (req.getName() != null) piggy.setName(req.getName());
            if (req.getTargetAmount() != null) piggy.setTargetAmount(req.getTargetAmount());
            if (req.getAutoDonation() != null) piggy.setAutoDonation(req.getAutoDonation());
            if (req.getAutoSaving() != null) piggy.setAutoSaving(req.getAutoSaving());

            if (req.getEsgCategoryId() != null) {
                if (req.getEsgCategoryId() <= 0) {
                    piggy.setEsgCategory(null);
                } else {
                    EsgCategory cat = esgCategoryRepository.findById(req.getEsgCategoryId())
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ESG 카테고리입니다."));
                    piggy.setEsgCategory(cat);
                }
            }

            PiggyBank saved = piggyBankRepository.save(piggy);
            return ApiResponse.success(toResponse(saved));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ApiResponse<List<EsgCategoryDto>>> listCategories() {
        return Mono.fromCallable(() ->
                        esgCategoryRepository.findAll().stream().map(this::toDto).toList()
                ).map(ApiResponse::success)
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<ApiResponse<PiggyBankResponse>> setCategory(Integer userId, Long categoryId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = getOrCreatePiggy(userId);
            EsgCategory cat = esgCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ESG 카테고리입니다."));
            piggy.setEsgCategory(cat);
            piggy = piggyBankRepository.save(piggy);
            return ApiResponse.success(toResponse(piggy));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    @Transactional
    public Mono<ApiResponse<PiggyBankResponse>> unsetCategory(Integer userId) {
        return Mono.fromCallable(() -> {
            PiggyBank piggy = getOrCreatePiggy(userId);
            piggy.setEsgCategory(null);
            piggy = piggyBankRepository.save(piggy);
            return ApiResponse.success(toResponse(piggy));
        }).subscribeOn(Schedulers.boundedElastic());
    }


    private PiggyBank getOrCreatePiggy(Integer userId) {
        return piggyBankRepository.findByOwner_Id(userId).orElseGet(() -> {
            Student owner = studentRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
            PiggyBank created = PiggyBank.builder()
                    .owner(owner)
                    .name("저금통")
                    .build();
            return piggyBankRepository.save(created);
        });
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
                p.getEsgCategory() == null ? null : toDto(p.getEsgCategory())
        );
    }

    private EsgCategoryDto toDto(EsgCategory c) {
        return new EsgCategoryDto(c.getId(), c.getName(), c.getDescription(), c.getCharacterName());
    }
}
