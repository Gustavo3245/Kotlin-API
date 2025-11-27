package com.example.spring_aplication.Controllers

import com.example.spring_aplication.database.models.RefreshToken
import com.example.spring_aplication.security.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService) {

    data class AuthRequest(
        @field:Email(message = "Invalid Email Format.")
        val email: String,
        @field:Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*)(?=.*[@$!%*?&])[A-Za-z@$!%*?&]{8,}$")
        val password: String
    )

    data class RefreshRequest(
        val refreshToken: String
    )

    @Operation(summary = "Post the register User", description = "Save the user in the database")
    @PostMapping("/register")
    fun register(@RequestBody body: AuthRequest){
        authService.register(body.email, body.password)
    }

    @PostMapping("/login")
    fun login(@RequestBody body: AuthRequest): AuthService.TokenPair{
        return authService.login(body.email, body.password)
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody body: RefreshRequest){
        authService.refresh(body.refreshToken)
    }
}
