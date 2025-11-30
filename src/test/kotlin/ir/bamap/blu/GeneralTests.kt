package ir.bamap.blu

import ir.bamap.blu.entity.Student
import ir.bamap.blu.repository.StudentRepository
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
open class GeneralTests @Autowired constructor(
    private val repository: StudentRepository
){

    private val logger = LoggerFactory.getLogger(this.javaClass)
	@Test
    @Transactional
	open fun `check entity id generated after persist`() {
        logger.info("Checking entity id generated after persist called")
        val student = Student("Morteza", "Malvandi")
        repository.persist(student)
        assert(student.id > 0)
    }

}
