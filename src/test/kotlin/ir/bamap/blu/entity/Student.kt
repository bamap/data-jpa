package ir.bamap.blu.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Student(
    val firstname: String,
    val lastname: String,

    @Id
    @GeneratedValue
    val id: Long = 0
) {
}