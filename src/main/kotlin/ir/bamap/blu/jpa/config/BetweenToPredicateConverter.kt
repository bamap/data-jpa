package ir.bamap.blu.jpa.config

import ir.bamap.blu.exception.NotSupportedTypeException
import ir.bamap.blu.jpa.FilterPredicateContext
import ir.bamap.blu.model.filter.Between
import ir.bamap.blu.model.filter.FilterModel
import ir.bamap.blu.model.filter.NotBetween
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

class BetweenToPredicateConverter : FilterToPredicateConverter<Between> {

    override fun isSupported(filter: FilterModel): Boolean = filter is Between

    override fun convert(context: FilterPredicateContext<in Between>): Predicate {
        val filter = context.filter
        require(filter is Between)

        val builder = context.builder
        val path =  context.pathResolver.resolve(context.builder, context.root, filter.propertyName, filter.lowerBoundary)
        val upper = filter.upperBoundary
        val lower = filter.lowerBoundary

        if (filter is NotBetween)
            throw NotSupportedTypeException(filter.javaClass, "NOT_BETWEEN_PREDICATE")

        return when (upper) {
            is Int -> builder.between(path as Path<Int>, lower.toString().toInt(), upper)
            is Long -> builder.between(path as Path<Long>, lower.toString().toLong(), upper)
            is Double -> builder.between(path as Path<Double>, lower.toString().toDouble(), upper)
            is Float -> builder.between(path as Path<Float>, lower.toString().toFloat(), upper)
            is Short -> builder.between(path as Path<Short>, lower.toString().toShort(), upper)
            is Char -> builder.between(path as Path<Char>, lower.toString()[0], upper)
            is String -> builder.between(path as Path<String>, lower.toString(), upper)
            is Date -> builder.between(path as Path<Date>, lower as Date, upper)
            is LocalDate -> builder.between(path as Path<LocalDate>, lower as LocalDate, upper)
            is LocalDateTime -> builder.between(path as Path<LocalDateTime>, lower as LocalDateTime, upper)
            else -> throw NotSupportedTypeException(upper.javaClass, "BETWEEN_PREDICATE")
        }
    }
}