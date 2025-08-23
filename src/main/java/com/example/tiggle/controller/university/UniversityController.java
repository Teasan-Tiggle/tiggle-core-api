package com.example.tiggle.controller.university;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.university.DepartmentResponseDto;
import com.example.tiggle.dto.university.UniversityResponseDto;
import com.example.tiggle.service.university.UniversityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/universities")
@Tag(name = "학교 API", description = "학교 및 학과 관련 API")
public class UniversityController {

    private final UniversityService universityService;

    /**
     * 대학 목록 조회
     *
     * @return 전체 대학 목록
     */
    @GetMapping("/")
    @Operation(summary = "대학 목록 조회", description = "회원가입 시 전체 대학 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<List<UniversityResponseDto>>> getUniversities() {
        List<UniversityResponseDto> allUniversities = universityService.getAllUniversities();
        return ResponseEntity.ok(new ResponseDto<>(true, allUniversities));
    }

    /**
     * 학과 목록 조회
     *
     * @param universityId 대학 ID
     * @return 전체 학과 목록
     */
    @GetMapping("/{universityId}/departments")
    @Operation(summary = "학과 목록 조회", description = "회원가입 시 학과 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<ResponseDto<List<DepartmentResponseDto>>> getDepartments(
            @Parameter(description = "대학 ID", required = true)
            @Valid @PathVariable Long universityId
    ) {
        List<DepartmentResponseDto> allDepartments = universityService.getAllDepartments(universityId);
        return ResponseEntity.ok(new ResponseDto<>(true, allDepartments));
    }
}
