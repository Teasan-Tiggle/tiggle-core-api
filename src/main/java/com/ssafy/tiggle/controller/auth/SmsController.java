package com.ssafy.tiggle.controller.auth;

import com.ssafy.tiggle.dto.ResponseDto;
import com.ssafy.tiggle.service.sms.SmsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/auth/sms")
public class SmsController {

    private record SendReq(@NotBlank String phone, String purpose) {}
    private record VerifyReq(@NotBlank String phone, @NotBlank String code, String purpose) {}
    private record VerifyRes(boolean match) {}

    private final SmsService smsService;
    public SmsController(SmsService smsService) { this.smsService = smsService; }

    @PostMapping("/send")
    public ResponseEntity<ResponseDto<Void>> send(@RequestBody SendReq req, HttpServletRequest http) {
        String purpose = (req.purpose() == null || req.purpose().isBlank()) ? "account_opening" : req.purpose();
        smsService.sendCode(req.phone(), purpose, http.getRemoteAddr());
        return ResponseEntity.ok(new ResponseDto<>(true, null, "인증번호가 전송되었습니다."));
    }

    @PostMapping("/verify")
    public ResponseEntity<ResponseDto<VerifyRes>> verify(@RequestBody VerifyReq req) {
        String purpose = (req.purpose() == null || req.purpose().isBlank()) ? "account_opening" : req.purpose();
        boolean ok = smsService.verify(req.phone(), purpose, req.code());
        return ResponseEntity.ok(new ResponseDto<>(ok, new VerifyRes(ok), ok ? "인증 성공" : "인증번호가 일치하지 않습니다."));
    }
}
