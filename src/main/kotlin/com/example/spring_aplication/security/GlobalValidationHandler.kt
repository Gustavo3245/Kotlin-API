package com.example.spring_aplication.security

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalValidationHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationerror(exception: MethodArgumentNotValidException) : ResponseEntity<Map<String, Any>> {
        val errors = exception.bindingResult.allErrors.map {
            it.defaultMessage ?: "Invalid Value"
        }
        return ResponseEntity
            .status(400)
            .body(mapOf("errors" to errors))
    }
}