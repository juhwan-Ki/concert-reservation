package com.gomdol.concert.user.presentation.controller;

import com.gomdol.concert.user.presentation.dto.SingUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;


@Tag(name = "User", description = "회원가입")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Operation(summary = "회원가입", description = "회원가입을 진행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공", content = @Content),
            @ApiResponse(responseCode = "400", description = "유효성 검증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "이미 가입된 사용자(이메일/닉네임 중복)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@Valid @RequestBody SingUpRequest request) {
    }
}
