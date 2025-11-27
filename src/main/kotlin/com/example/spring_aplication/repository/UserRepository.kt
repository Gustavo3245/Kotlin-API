package com.example.spring_aplication.repository

import com.example.spring_aplication.database.models.User
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface UserRepository : MongoRepository<User, ObjectId> {
    fun findByEmail(email: String) : User?
}