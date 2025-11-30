package ir.bamap.blu.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
open class Student(
    open val firstname: String = "Ali",
    open val lastname: String = "Malvandi",

    @Id
    @GeneratedValue
    open val id: Long = 0
) {
}