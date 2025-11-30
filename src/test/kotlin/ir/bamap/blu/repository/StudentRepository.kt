package ir.bamap.blu.repository

import ir.bamap.blu.entity.Student
import ir.bamap.blu.jpa.repository.BluRepositoryImpl
import org.springframework.stereotype.Repository

@Repository
class StudentRepository: BluRepositoryImpl<Student, Long>(Student::class.java) {
}