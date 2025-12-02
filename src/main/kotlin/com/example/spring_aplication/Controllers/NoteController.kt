package com.example.spring_aplication.Controllers

import com.example.spring_aplication.Controllers.NoteController.NoteResponse
import com.example.spring_aplication.database.models.Note
import com.example.spring_aplication.repository.NoteRepository
import com.example.spring_aplication.repository.UserRepository
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.http.HttpStatusCode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

// https://notes.com/notes

@RestController
@RequestMapping("/notes")
class NoteController(private val repository: NoteRepository,
                            private val noteRepository: NoteRepository,
                            private val userRepository: UserRepository) {

    //DTOs
    data class  NoteRequest(
        val id: String?,
        @NotBlank(message = "Title cant be blank")
        val title: String,
        val content: String,
        val color: Long,
    )

    //DTOs
    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
    )

    @PostMapping
    fun save(@RequestBody body: NoteRequest) : NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note = repository.save(Note(
            id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
            title = body.title,
            content = body.content,
            color = body.color,
            createdAt = Instant.now(),
            ownerId = ObjectId(ownerId))
        )
        return note.toResponse()

    }

    @GetMapping("/email/search")
    fun findNotesByEmail(@RequestParam name: String) : List<NoteResponse> {

        val existingUserEmail = userRepository.findByName(name) ?:
            throw ResponseStatusException(HttpStatusCode.valueOf(404), "User with that name not found")

        return userRepository.findAllNotesByName(name).map {
            it.toResponse()
        }
    }

    @GetMapping
    fun findNotesByOwnedId() : List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String

        return repository.findByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        println(ownerId)
        if(note.ownerId.toHexString() == ownerId){
            repository.deleteById(ObjectId(id))
        }
    }
}

private fun Note.toResponse(): NoteController.NoteResponse {
    return NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt
    )
}
