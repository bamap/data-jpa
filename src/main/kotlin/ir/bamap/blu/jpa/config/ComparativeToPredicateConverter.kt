package ir.bamap.blu.jpa.config

import ir.bamap.blu.exception.NotSupportedTypeException
import ir.bamap.blu.jpa.FilterPredicateContext
import ir.bamap.blu.model.filter.ComparativeOperatorFilter
import ir.bamap.blu.model.filter.Equal
import ir.bamap.blu.model.filter.FilterModel
import ir.bamap.blu.model.filter.GreaterThan
import ir.bamap.blu.model.filter.GreaterThanOrEqualTo
import ir.bamap.blu.model.filter.In
import ir.bamap.blu.model.filter.LessThan
import ir.bamap.blu.model.filter.LessThanOrEqualTo
import ir.bamap.blu.model.filter.Like
import ir.bamap.blu.model.filter.NotEqual
import ir.bamap.blu.model.filter.NotIn
import ir.bamap.blu.model.filter.NotLike
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Date

open class ComparativeToPredicateConverter : FilterToPredicateConverter<ComparativeOperatorFilter> {

    override fun isSupported(filter: FilterModel): Boolean = filter is ComparativeOperatorFilter

    override fun convert(context: FilterPredicateContext<in ComparativeOperatorFilter>): Predicate {

        val filter = context.filter
        require(filter is ComparativeOperatorFilter)

        val builder = context.builder
        val pathResolver = context.pathResolver
        val root = context.root
        val literal = filter.literal

        return when (filter) {
            is NotEqual -> builder.notEqual(pathResolver.resolve(builder, root, filter.propertyName, filter.literal), filter.literal)
            is Equal -> builder.equal(pathResolver.resolve(builder, root, filter.propertyName, filter.literal), filter.literal)
            is Like -> builder.like(
                pathResolver.resolve(builder, root, filter.propertyName, filter.literal) as Path<String>,
                filter.literal.toString()
            )

            is NotLike -> builder.notLike(
                pathResolver.resolve(builder, root, filter.propertyName, filter.literal) as Path<String>,
                literal.toString()
            )

            is LessThan -> getLessThanPredicate(builder, root, filter, pathResolver)
            is LessThanOrEqualTo -> getLessThanOrEqualToPredicate(builder, root, filter, pathResolver)
            is GreaterThanOrEqualTo -> getGreaterThanOrEqualToPredicate(builder, root, filter, pathResolver)
            is GreaterThan -> getGreaterThanPredicate(builder, root, filter, pathResolver)
            else -> throw NotSupportedTypeException(filter.javaClass, "COMPARATIVE_PREDICATE")
        }
    }

    protected open fun getLessThanPredicate(
        builder: CriteriaBuilder,
        root: Root<*>,
        filter: LessThan,
        pathResolver: PathResolver,
    ): Predicate {
        val path = pathResolver.resolve(builder, root, filter.propertyName, filter.literal)
        return when (val literal = filter.literal) {
            is Date -> builder.lessThan(path as Path<Date>, literal)
            is LocalDate -> builder.lessThan(path as Path<LocalDate>, literal)
            is LocalDateTime -> builder.lessThan(path as Path<LocalDateTime>, literal)
            is Int -> builder.lessThan(path as Path<Int>, literal)
            is Long -> builder.lessThan(path as Path<Long>, literal)
            is Double -> builder.lessThan(path as Path<Double>, literal)
            is Float -> builder.lessThan(path as Path<Float>, literal)
            is Short -> builder.lessThan(path as Path<Short>, literal)
            is Char -> builder.lessThan(path as Path<Char>, literal)
            is String -> builder.lessThan(path as Path<String>, literal)
            else -> throw NotSupportedTypeException(literal.javaClass, "LESS_PREDICATE")
        }
    }

    protected open fun getLessThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<*>,
        filter: LessThanOrEqualTo,
        pathResolver: PathResolver,
    ): Predicate {
        val path = pathResolver.resolve(builder, root, filter.propertyName, filter.literal)
        return when (val literal = filter.literal) {
            is Date -> builder.lessThanOrEqualTo(path as Path<Date>, literal)
            is LocalDate -> builder.lessThanOrEqualTo(path as Path<LocalDate>, literal)
            is LocalDateTime -> builder.lessThanOrEqualTo(path as Path<LocalDateTime>, literal)
            is Int -> builder.lessThanOrEqualTo(path as Path<Int>, literal)
            is Long -> builder.lessThanOrEqualTo(path as Path<Long>, literal)
            is Double -> builder.lessThanOrEqualTo(path as Path<Double>, literal)
            is Float -> builder.lessThanOrEqualTo(path as Path<Float>, literal)
            is Short -> builder.lessThanOrEqualTo(path as Path<Short>, literal)
            is Char -> builder.lessThanOrEqualTo(path as Path<Char>, literal)
            is String -> builder.lessThanOrEqualTo(path as Path<String>, literal)
            else -> throw NotSupportedTypeException(literal.javaClass, "LESS_EQUAL_PREDICATE")
        }
    }

    protected open fun getGreaterThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<*>,
        filter: GreaterThanOrEqualTo,
        pathResolver: PathResolver,
    ): Predicate {
        val path = pathResolver.resolve(builder, root, filter.propertyName, filter.literal)
        return when (val literal = filter.literal) {
            is Date -> builder.greaterThanOrEqualTo(path as Path<Date>, literal)
            is LocalDate -> builder.greaterThanOrEqualTo(path as Path<LocalDate>, literal)
            is LocalDateTime -> builder.greaterThanOrEqualTo(path as Path<LocalDateTime>, literal)
            is Int -> builder.greaterThanOrEqualTo(path as Path<Int>, literal)
            is Long -> builder.greaterThanOrEqualTo(path as Path<Long>, literal)
            is Double -> builder.greaterThanOrEqualTo(path as Path<Double>, literal)
            is Float -> builder.greaterThanOrEqualTo(path as Path<Float>, literal)
            is Short -> builder.greaterThanOrEqualTo(path as Path<Short>, literal)
            is Char -> builder.greaterThanOrEqualTo(path as Path<Char>, literal)
            is String -> builder.greaterThanOrEqualTo(path as Path<String>, literal)
            else -> throw NotSupportedTypeException(literal.javaClass, "GREATER_EQUAL_PREDICATE")
        }
    }

    protected open fun getGreaterThanPredicate(
        builder: CriteriaBuilder,
        root: Root<*>,
        filter: GreaterThan,
        pathResolver: PathResolver,
    ): Predicate {
        val path = pathResolver.resolve(builder, root, filter.propertyName, filter.literal)
        return when (val literal = filter.literal) {
            is Date -> builder.greaterThan(path as Path<Date>, literal)
            is LocalDate -> builder.greaterThan(path as Path<LocalDate>, literal)
            is LocalDateTime -> builder.greaterThan(path as Path<LocalDateTime>, literal)
            is Int -> builder.greaterThan(path as Path<Int>, literal)
            is Long -> builder.greaterThan(path as Path<Long>, literal)
            is Double -> builder.greaterThan(path as Path<Double>, literal)
            is Float -> builder.greaterThan(path as Path<Float>, literal)
            is Short -> builder.greaterThan(path as Path<Short>, literal)
            is Char -> builder.greaterThan(path as Path<Char>, literal)
            is String -> builder.greaterThan(path as Path<String>, literal)
            else -> throw NotSupportedTypeException(literal.javaClass, "GREATER_PREDICATE")
        }
    }
}