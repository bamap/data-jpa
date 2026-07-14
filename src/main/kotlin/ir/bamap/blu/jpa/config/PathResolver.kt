package ir.bamap.blu.jpa.config

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

open class PathResolver {

    open fun resolve(
        builder: CriteriaBuilder,
        root: Path<*>,
        propertyName: String,
        propertyValue: Any? = null
    ): Path<*> {
        val propertyNames = propertyName.split(".")

        if (propertyNames.size > 1) {
            val nestedRoot = propertyNames.take(propertyNames.size - 1)
                .fold(root) { acc, element -> acc.get<Any>(element) }

            return resolve(builder, nestedRoot, propertyNames.last(), propertyValue)
        }

        return getByPropertyValue(root, propertyName, propertyValue)
    }

    protected open fun getByPropertyValue(root: Path<*>, propertyName: String, propertyValue: Any? = null): Path<*> {
        return when (propertyValue) {
            null -> root.get<Any>(propertyName)
            is Int -> root.get<Int>(propertyName)
            is Long -> root.get<Long>(propertyName)
            is Double -> root.get<Double>(propertyName)
            is Float -> root.get<Float>(propertyName)
            is String -> root.get<String>(propertyName)
            is Date -> root.get<Date>(propertyName)
            is LocalDate -> root.get<LocalDate>(propertyName)
            is LocalDateTime -> root.get<LocalDateTime>(propertyName)
            is Char -> root.get<Char>(propertyName)
            is Number -> root.get<Number>(propertyName)
            else -> root.get<Any>(propertyName)
        }
    }
}