package com.gomdol.concert.auth.presentation.contorller;

import com.gomdol.concert.auth.presentation.dto.LoginRequest;
import com.gomdol.concert.auth.presentation.dto.LogoutRequest;
import com.gomdol.concert.auth.presentation.dto.RefreshRequest;
import com.gomdol.concert.auth.presentation.dto.TokenResponse;
import com.gomdol.concert.common.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "로그인/토큰 갱신/로그아웃")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Operation(summary = "로그인(토큰 발급)", description = "자격 증명을 검증하고 액세스 토큰을 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiException.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        TokenResponse mock = new TokenResponse("Bearer", "eyJhbGciOi...", 3600, "3f3b1b5c...", 1209600);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(mock);
    }

    @Operation(summary = "Access 재발급", description = "자격 증명을 검증하고 액세스 토큰을 재발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "401", description = "재발급 불가",
                    content = @Content(schema = @Schema(implementation = ApiException.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(new TokenResponse("new.access.jwt", "Bearer", 1800, null, null));
    }

    @Operation(summary = "로그아웃", description = "로그아웃을 진행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiException.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest req) {
    }
}
