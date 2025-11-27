package com.example.spring_aplication.repository

import com.example.spring_aplication.database.models.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository : MongoRepository<Note, ObjectId> {
    fun findByOwnerId(ownerId: ObjectId) : List<Note>
}
