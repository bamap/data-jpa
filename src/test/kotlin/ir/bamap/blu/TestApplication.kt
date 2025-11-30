package ir.bamap.blu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.fromApplication
import org.springframework.boot.with

@SpringBootApplication
open class TestApplication

fun main(args: Array<String>) {
    fromApplication<TestApplication>()
        .with(TestcontainersConfiguration::class)
        .run(*args)
}
