package com.example.spring_aplication.security


import com.example.spring_aplication.database.models.RefreshToken
import com.example.spring_aplication.database.models.User
import com.example.spring_aplication.repository.RefreshRepository
import com.example.spring_aplication.repository.UserRepository
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Cookie

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshTokenRepository: RefreshRepository
) {
    data class TokenPair(
        val accessToken: String
    )

    fun register(email: String, name: String, password: String): User {
        val user = userRepository.findByEmail(email.trim())
        if(user != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists.")
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password),
                name = name
            )
        )
    }

    fun login(email: String, password: String, response: HttpServletResponse): TokenPair {

        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid credentials.")

        if(!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid credentials.")
        }

        val newAccessToken = jwtService.generateAcessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefressToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)
        setRefreshTokenCookie(response, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken
        )
    }

    @Transactional
    fun refresh(refreshToken: String, response: HttpServletResponse): TokenPair {

        if(!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val hashed = hashToken(refreshToken)
        refreshTokenRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(
                HttpStatusCode.valueOf(401),
                "Refresh token not recognized (maybe used or expired?)"
            )

        refreshTokenRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAccessToken = jwtService.generateAcessToken(userId)
        val newRefreshToken = jwtService.generateRefressToken(userId)

        storeRefreshToken(user.id, newRefreshToken)
        setRefreshTokenCookie(response, newRefreshToken)

        return TokenPair(
            accessToken = newAccessToken
        )
    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    fun setRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val expiryMs = jwtService.refreshTokenValidityMs
        val cookie = Cookie("refresh_token", refreshToken).apply {
            isHttpOnly = true
            secure = true
            path = "/auth/refresh"
            maxAge = (expiryMs / 1000).toInt()
        }
        response.addCookie(cookie)
    }
}