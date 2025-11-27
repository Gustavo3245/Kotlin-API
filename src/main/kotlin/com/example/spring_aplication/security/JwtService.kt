package com.example.spring_aplication.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.Date

@Service
class JwtService(@Value("\${jwt.secret}") private val jwtSecret: String) {
    private val secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    private val acessTokenValidityMs = 15L * 60L * 1000L;
    val refreshTokenValidityMs = 30L * 24 * 60 * 1000L;

    private fun generateToken(userId: String, type: String, espiry: Long) : String {
        val now = Date()
        val espiryDate = Date(now.time + espiry)
        
        return Jwts.builder()
            .subject(userId)
            .claim("type", type)
            .issuedAt(now)
            .expiration(espiryDate)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact()
    
    }

    fun generateAcessToken(userId: String) : String {
        return generateToken(userId, "acesss", acessTokenValidityMs)
    }

    fun generateRefressToken(userId: String) : String {
        return generateToken(userId, "refresh", refreshTokenValidityMs)
    }

    fun validateAccessToken(token: String): Boolean {
        val claims = parseAllClaims(token) ?: return false
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "access"
    }

    fun validateRefreshToken(token: String) : Boolean {
        val claims = parseAllClaims(token) ?: return false;
        val tokenType = claims["type"] as? String ?: return false
        return tokenType == "refresh";
    }

    //Authorization: Bearar <token>
    fun getUserIdFromToken(token: String): String {
        val claims = parseAllClaims(token) ?: throw ResponseStatusException(HttpStatus.valueOf(401), "Invalid token")
        return claims.subject
    }

    private fun parseAllClaims(token: String) : Claims? {
        val rawToken = if(token.startsWith("Bearar ")) {
            token.removePrefix("Bearar ")
        } else token

        return try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(rawToken)
                .payload
        } catch (exception: Exception) {
            null
        }
    }
}