package com.example.spring_aplication.security

import com.example.spring_aplication.database.models.RefreshToken
import com.example.spring_aplication.database.models.User
import com.example.spring_aplication.repository.RefreshRepository
import com.example.spring_aplication.repository.UserRepository
import org.apache.coyote.Response
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

@Service
class AuthService(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
    private val hashEncoder: HashEncoder,
    private val refreshRepository: RefreshRepository) {

    data class TokenPair(
        val acessToken: String,
        val refreshToken: String
    )

    fun register(email: String, password: String) : User {
        val user = userRepository.findByEmail(email.trim())
        if(user != null){
            throw ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists")
        }
        return userRepository.save(
            User(
                email = email,
                hashedPassword = hashEncoder.encode(password)
            )
        )
    }

    fun login(email: String, password: String): TokenPair {
        val user = userRepository.findByEmail(email)
            ?: throw BadCredentialsException("Invalid Credentials.")

        if(!hashEncoder.matches(password, user.hashedPassword)) {
            throw BadCredentialsException("Invalid Credentials.")
        }

        val newAcessToken = jwtService.generateAcessToken(user.id.toHexString())
        val newRefreshToken = jwtService.generateRefressToken(user.id.toHexString())

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            acessToken = newAcessToken,
            refreshToken = newRefreshToken
        )

    }

    @Transactional
    fun refresh(refreshToken: String): TokenPair {
        if(!jwtService.validateRefreshToken(refreshToken)) {
            throw ResponseStatusException(HttpStatusCode.valueOf(401), "Invalid refresh token.")
        }

        val userId = jwtService.getUserIdFromToken(refreshToken)
        val user = userRepository.findById(ObjectId(userId)).orElseThrow {
            throw ResponseStatusException(HttpStatus.valueOf(404), "Not found refresh token")
        }

        val hashed = hashToken(refreshToken)
        refreshRepository.findByUserIdAndHashedToken(user.id, hashed)
            ?: throw ResponseStatusException(HttpStatus.valueOf(401), "refresh token not recognized")

        refreshRepository.deleteByUserIdAndHashedToken(user.id, hashed)

        val newAcessToken = jwtService.generateAcessToken(userId)
        val newRefreshToken = jwtService.generateRefressToken(userId)

        storeRefreshToken(user.id, newRefreshToken)

        return TokenPair(
            acessToken = newAcessToken,
            refreshToken = newRefreshToken
        )

    }

    private fun storeRefreshToken(userId: ObjectId, rawRefreshToken: String) {
        val hashed = hashToken(rawRefreshToken)
        val expiryMs = jwtService.refreshTokenValidityMs
        val expiresAt = Instant.now().plusMillis(expiryMs)

        refreshRepository.save(
            RefreshToken(
                userId = userId,
                expiresAt = expiresAt,
                hashedToken = hashed
            )
        )
    }

    private fun hashToken(token: String) : String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.encodeToByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}