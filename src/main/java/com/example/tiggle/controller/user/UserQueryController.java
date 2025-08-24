package com.example.tiggle.controller.user;

import com.example.tiggle.dto.ResponseDto;
import com.example.tiggle.dto.user.UserSimpleResponse;
import com.example.tiggle.repository.user.StudentRepository;
import com.example.tiggle.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 조회", description = "사용자 목록/정보 조회 (JWT 필요)")
@SecurityRequirement(name = "bearerAuth")
public class UserQueryController {

    private final StudentRepository usersRepository;

    @Operation(summary = "사용자 목록 조회")
    @GetMapping("/list")
    public ResponseEntity<ResponseDto<List<UserSimpleResponse>>> getAllSimple() {
        Long requesterId = JwtUtil.getCurrentUserId();
        if (requesterId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        var list = usersRepository.findAll().stream()
                .map(u -> new UserSimpleResponse(u.getId(), u.getName()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ResponseDto<>(true, list));
    }

}
