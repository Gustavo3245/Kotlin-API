package com.example.spring_aplication.Controllers

import com.example.spring_aplication.database.models.RefreshToken
import com.example.spring_aplication.security.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.server.ResponseStatusException


@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService) {

    data class AuthRequest(
        @field:Email(message = "Invalid Email Format.")
        val email: String,
        @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*)(?=.*[@$!%*?&])[A-Za-z@$!%*?&]{8,}$")
        val password: String,
        val name: String
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    @Operation(summary = "Post the register User", description = "Save the user in the database")
    @PostMapping("/register")
    fun register(@RequestBody body: AuthRequest){
        authService.register(body.email, body.name ,body.password)
    }

    @Operation(summary = "login the register user", description = "return a refreshToken save in HttpOnlyCookies")
    @PostMapping("/login")
    fun login(@RequestBody body: AuthRequest, response: HttpServletResponse): AuthService.TokenPair{
        return authService.login(body.email, body.password, response)
    }

    @Operation(summary = "refresh and return refresh Token", description = "exchange Refresh Tokens in MongoDB")
    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse) : ResponseEntity<AuthService.TokenPair>{

        val refreshToken = request.cookies?.find { it.name == "refresh_token" }?.value
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found.")

        val tokens = authService.refresh(refreshToken, response)
        return ResponseEntity.ok(tokens)
    }
}
