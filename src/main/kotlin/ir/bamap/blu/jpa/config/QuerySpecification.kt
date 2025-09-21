package ir.bamap.blu.jpa.config

import ir.bamap.blu.exception.NotSupportedException
import ir.bamap.blu.model.OrderModel
import ir.bamap.blu.model.filter.*
import jakarta.persistence.criteria.*
import org.springframework.cglib.core.Local
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.util.*

open class QuerySpecification<Entity> {
    open fun getSpecification(filters: Collection<FilterModel>): Specification<Entity> {
        return Specification { root: Root<Entity>, query: CriteriaQuery<*>?, builder: CriteriaBuilder ->
            val predicates: List<Predicate> = getPredicates(builder, root, query, filters)
            builder.and(*predicates.toTypedArray())
        }
    }

    open fun <U : Entity> getPredicates(
        builder: CriteriaBuilder,
        root: Root<U>,
        query: CriteriaQuery<*>?,
        filters: Collection<FilterModel>
    ): List<Predicate> {
        return filters
            .map { filterModel -> getPredicate(builder, root, query, filterModel) }
    }

    open fun getOrders(
        builder: CriteriaBuilder, root: Root<*>,
        orderModels: Collection<OrderModel>?
    ): List<Order> {
        if (orderModels == null) return emptyList()

        return orderModels
            .map { sortModel -> getOrder(builder, root, sortModel) }

    }

    protected open fun getOrder(criteriaBuilder: CriteriaBuilder, root: Root<*>, sortModel: OrderModel): Order {
        return if (sortModel.direction == OrderModel.Direction.ASC)
            criteriaBuilder.asc(root.get<Any>(sortModel.propertyName))
        else
            criteriaBuilder.desc(root.get<Any>(sortModel.propertyName))

    }

    protected open fun <U : Entity> getPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        query: CriteriaQuery<*>?,
        filter: FilterModel
    ): Predicate {
        if (filter is GroupFilter) {

            val filters = getPredicates(builder, root, query, filter.filters).toTypedArray()

            return when (filter) {
                is NotOr -> builder.not(builder.or(*filters))
                else -> builder.or(*filters)
            }
        }

        if (filter is ComparativeOperatorFilter)
            return getComparativeOperatorPredicate(builder, root, filter)

        if (filter is Between)
            return getBetweenPredicate(builder, root, filter)

        if (filter is In)
            return getInPredicate(builder, root, filter)

        if (filter is NotFilter)
            return builder.not(getPredicate(builder, root, query, filter.filter))

        val path = getPath(root, filter)
        return when (filter) {
            is IsNull -> builder.isNull(path)
            is IsNotNull -> builder.isNotNull(path)
            else -> throw NotSupportedException(filter.javaClass, "GET_PREDICATE")
        }
    }

    protected open fun <U : Entity> getComparativeOperatorPredicate(
        criteriaBuilder: CriteriaBuilder,
        root: Root<U>,
        filter: ComparativeOperatorFilter
    ): Predicate {
        val literal = getLiteral(filter)
        return when (filter) {
            is NotEqual -> criteriaBuilder.notEqual(getPath(root, filter), literal)
            is Equal -> criteriaBuilder.equal(getPath(root, filter), literal)
            is Like -> criteriaBuilder.like(getPath(root, filter) as Path<String>, literal.toString())
            is NotLike -> criteriaBuilder.notLike(getPath(root, filter) as Path<String>, literal.toString())
            is LessThan -> getLessThanPredicate(criteriaBuilder, root, filter)
            is LessThanOrEqualTo -> getLessThanOrEqualToPredicate(criteriaBuilder, root, filter)
            is GreaterThanOrEqualTo -> getGreaterThanOrEqualToPredicate(criteriaBuilder, root, filter)
            is GreaterThan -> getGreaterThanPredicate(criteriaBuilder, root, filter)
            else -> throw NotSupportedException(filter.javaClass, "COMPARATIVE_PREDICATE")
        }
    }

    protected open fun <U : Entity> getBetweenPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: Between
    ): Predicate {
        val path = getPath(root, filter)
        val upper = filter.upperBoundary
        val lower = filter.lowerBoundary

        if (filter is NotBetween)
            throw NotSupportedException(filter.javaClass, "NOT_BETWEEN_PREDICATE")

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
            else -> throw NotSupportedException(upper.javaClass, "BETWEEN_PREDICATE")
        }
    }

    protected open fun <U : Entity> getInPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: In
    ): Predicate {
        val propertyPath = root.get<Any>(filter.propertyName)
        val predicate = propertyPath.`in`(filter.literal)
        if (filter is NotIn)
            return predicate.not()

        return predicate
    }

    protected open fun <U : Entity> getLessThanPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: LessThan
    ): Predicate {
        val path = getPath(root, filter)
        return when (val literal = getLiteral(filter)) {
            is Date -> builder.lessThan(path as Path<Date>, literal)
            is LocalDate -> builder.lessThan(path as Path<LocalDate>, literal)
            is Int -> builder.lessThan(path as Path<Int>, literal)
            is Long -> builder.lessThan(path as Path<Long>, literal)
            is Double -> builder.lessThan(path as Path<Double>, literal)
            is Float -> builder.lessThan(path as Path<Float>, literal)
            is Short -> builder.lessThan(path as Path<Short>, literal)
            is Char -> builder.lessThan(path as Path<Char>, literal)
            is String -> builder.lessThan(path as Path<String>, literal)
            else -> throw NotSupportedException(literal.javaClass, "LESS_PREDICATE")
        }
    }

    protected open fun <U : Entity> getLessThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: LessThanOrEqualTo
    ): Predicate {
        val path = getPath(root, filter)
        return when (val literal = getLiteral(filter)) {
            is Date -> builder.lessThanOrEqualTo(path as Path<Date>, literal)
            is LocalDate -> builder.lessThanOrEqualTo(path as Path<LocalDate>, literal)
            is Int -> builder.lessThanOrEqualTo(path as Path<Int>, literal)
            is Long -> builder.lessThanOrEqualTo(path as Path<Long>, literal)
            is Double -> builder.lessThanOrEqualTo(path as Path<Double>, literal)
            is Float -> builder.lessThanOrEqualTo(path as Path<Float>, literal)
            is Short -> builder.lessThanOrEqualTo(path as Path<Short>, literal)
            is Char -> builder.lessThanOrEqualTo(path as Path<Char>, literal)
            is String -> builder.lessThanOrEqualTo(path as Path<String>, literal)
            else -> throw NotSupportedException(literal.javaClass, "LESS_EQUAL_PREDICATE")
        }
    }

    protected open fun <U : Entity> getGreaterThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: GreaterThanOrEqualTo
    ): Predicate {
        val path = getPath(root, filter)
        return when (val literal = getLiteral(filter)) {
            is Date -> builder.greaterThanOrEqualTo(path as Path<Date>, literal)
            is LocalDate -> builder.greaterThanOrEqualTo(path as Path<LocalDate>, literal)
            is Int -> builder.greaterThanOrEqualTo(path as Path<Int>, literal)
            is Long -> builder.greaterThanOrEqualTo(path as Path<Long>, literal)
            is Double -> builder.greaterThanOrEqualTo(path as Path<Double>, literal)
            is Float -> builder.greaterThanOrEqualTo(path as Path<Float>, literal)
            is Short -> builder.greaterThanOrEqualTo(path as Path<Short>, literal)
            is Char -> builder.greaterThanOrEqualTo(path as Path<Char>, literal)
            is String -> builder.greaterThanOrEqualTo(path as Path<String>, literal)
            else -> throw NotSupportedException(literal.javaClass, "GREATER_EQUAL_PREDICATE")
        }
    }

    protected open fun <U : Entity> getGreaterThanPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: GreaterThan
    ): Predicate {
        val path = getPath(root, filter)
        return when (val literal = getLiteral(filter)) {
            is Date -> builder.greaterThan(path as Path<Date>, literal)
            is LocalDate -> builder.greaterThan(path as Path<LocalDate>, literal)
            is Int -> builder.greaterThan(path as Path<Int>, literal)
            is Long -> builder.greaterThan(path as Path<Long>, literal)
            is Double -> builder.greaterThan(path as Path<Double>, literal)
            is Float -> builder.greaterThan(path as Path<Float>, literal)
            is Short -> builder.greaterThan(path as Path<Short>, literal)
            is Char -> builder.greaterThan(path as Path<Char>, literal)
            is String -> builder.greaterThan(path as Path<String>, literal)
            else -> throw NotSupportedException(literal.javaClass, "GREATER_PREDICATE")
        }
    }

    protected open fun getPath(root: Root<*>, filter: FilterModel): Path<*> {
        return when (filter) {
            is IsNull -> root.get<Any>(filter.propertyName)
            is IsNotNull -> root.get<Any>(filter.propertyName)
            is NotEqual -> getPathByValue(root, filter.propertyName, filter.literal)
            is Equal -> getPathByValue(root, filter.propertyName, filter.literal)
            is GreaterThan -> getPathByValue(root, filter.propertyName, filter.literal)
            is GreaterThanOrEqualTo -> getPathByValue(root, filter.propertyName, filter.literal)
            is LessThan -> getPathByValue(root, filter.propertyName, filter.literal)
            is LessThanOrEqualTo -> getPathByValue(root, filter.propertyName, filter.literal)
            is NotBetween -> getPathByValue(root, filter.propertyName, filter.lowerBoundary)
            is Between -> getPathByValue(root, filter.propertyName, filter.lowerBoundary)
            is Like -> root.get<String>(filter.propertyName)
            is NotLike -> root.get<String>(filter.propertyName)

            else -> throw NotSupportedException(filter.javaClass, "GET_PATH")
        }
    }

    protected open fun getLiteral(filter: ComparativeOperatorFilter): Any {
        return filter.literal
    }

    protected open fun getPathByValue(root: Root<*>, property: String, value: Any): Path<*> {
        when (value) {
            is Int -> return root.get<Int>(property)
            is Long -> return root.get<Long>(property)
            is Double -> return root.get<Double>(property)
            is Float -> return root.get<Float>(property)
            is String -> return root.get<String>(property)
            is Date -> return root.get<Date>(property)
            is LocalDate -> return root.get<LocalDate>(property)
            is Char -> return root.get<Char>(property)
            is Number -> return root.get<Number>(property)
        }

        return root.get<Any>(property)
    }
}