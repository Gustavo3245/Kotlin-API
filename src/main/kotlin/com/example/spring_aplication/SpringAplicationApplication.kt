package com.example.spring_aplication

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringAplicationApplication

fun main(args: Array<String>) {
	runApplication<SpringAplicationApplication>(*args)
}
