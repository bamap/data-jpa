package ir.bamap.blu.jpa.config

import ir.bamap.blu.exception.NotSupportedTypeException
import ir.bamap.blu.model.OrderModel
import ir.bamap.blu.model.filter.*
import jakarta.persistence.criteria.*
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

open class QuerySpecification<Entity: Any> {
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
        val path = getPath(root, sortModel.propertyName)
        if (sortModel.cases.isEmpty())
            return if (sortModel.direction == OrderModel.Direction.ASC)
                criteriaBuilder.asc(path)
            else
                criteriaBuilder.desc(path)

        val orderCase = criteriaBuilder.selectCase<Int>()
        sortModel.cases.forEachIndexed { index, case ->
            orderCase.`when`(criteriaBuilder.equal(path, case), index)
        }
        orderCase.otherwise(sortModel.cases.size)

        return if (sortModel.direction == OrderModel.Direction.ASC)
            criteriaBuilder.asc(orderCase)
        else
            criteriaBuilder.desc(orderCase)
    }

    protected open fun <U : Entity> getPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        query: CriteriaQuery<*>?,
        filter: FilterModel
    ): Predicate {
        if (filter is ClassFilter)
            return builder.equal(root.type(), filter.cls)

        if (filter is GroupFilter) {

            val filters = getPredicates(builder, root, query, filter.filters).toTypedArray()

            return when (filter) {
                is NotOr -> builder.not(builder.or(*filters))
                is NotAnd -> builder.not(builder.and(*filters))
                is And -> builder.and(*filters)
                is Or -> builder.or(*filters)
                else -> throw NotSupportedTypeException(filter::class.java, "DATA_JPA_GET_PREDICATE")
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

        return when (filter) {
            is IsNull -> builder.isNull(getPath(root, filter.propertyName))
            is IsNotNull -> builder.isNotNull(getPath(root, filter.propertyName))
            else -> throw NotSupportedTypeException(filter.javaClass, "GET_PREDICATE")
        }
    }

    protected open fun <U : Entity> getComparativeOperatorPredicate(
        criteriaBuilder: CriteriaBuilder,
        root: Root<U>,
        filter: ComparativeOperatorFilter
    ): Predicate {
        val literal = getLiteral(filter)
        return when (filter) {
            is NotEqual -> criteriaBuilder.notEqual(getPath(root, filter.propertyName, filter.literal), literal)
            is Equal -> criteriaBuilder.equal(getPath(root, filter.propertyName, filter.literal), literal)
            is Like -> criteriaBuilder.like(
                getPath(root, filter.propertyName, filter.literal) as Path<String>,
                literal.toString()
            )

            is NotLike -> criteriaBuilder.notLike(
                getPath(root, filter.propertyName, filter.literal) as Path<String>,
                literal.toString()
            )

            is LessThan -> getLessThanPredicate(criteriaBuilder, root, filter)
            is LessThanOrEqualTo -> getLessThanOrEqualToPredicate(criteriaBuilder, root, filter)
            is GreaterThanOrEqualTo -> getGreaterThanOrEqualToPredicate(criteriaBuilder, root, filter)
            is GreaterThan -> getGreaterThanPredicate(criteriaBuilder, root, filter)
            else -> throw NotSupportedTypeException(filter.javaClass, "COMPARATIVE_PREDICATE")
        }
    }

    protected open fun <U : Entity> getBetweenPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: Between
    ): Predicate {
        val path = getPath(root, filter.propertyName, filter.lowerBoundary)
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
        val path = getPath(root, filter.propertyName, filter.literal)
        return when (val literal = getLiteral(filter)) {
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

    protected open fun <U : Entity> getLessThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: LessThanOrEqualTo
    ): Predicate {
        val path = getPath(root, filter.propertyName, filter.literal)
        return when (val literal = getLiteral(filter)) {
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

    protected open fun <U : Entity> getGreaterThanOrEqualToPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: GreaterThanOrEqualTo
    ): Predicate {
        val path = getPath(root, filter.propertyName, filter.literal)
        return when (val literal = getLiteral(filter)) {
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

    protected open fun <U : Entity> getGreaterThanPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        filter: GreaterThan
    ): Predicate {
        val path = getPath(root, filter.propertyName, filter.literal)
        return when (val literal = getLiteral(filter)) {
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

    protected open fun getPath(root: Path<*>, propertyName: String, propertyValue: Any? = null): Path<*> {
        val propertyNames = propertyName.split(".")

        if (propertyNames.size > 1) {
            val nestedRoot = propertyNames.take(propertyNames.size - 1)
                .fold(root) { acc, element -> acc.get<Any>(element) }

            return getPath(nestedRoot, propertyNames.last(), propertyValue)
        }

        return when (propertyValue) {
            null -> return root.get<Any>(propertyName)
            is Int -> return root.get<Int>(propertyName)
            is Long -> return root.get<Long>(propertyName)
            is Double -> return root.get<Double>(propertyName)
            is Float -> return root.get<Float>(propertyName)
            is String -> return root.get<String>(propertyName)
            is Date -> return root.get<Date>(propertyName)
            is LocalDate -> return root.get<LocalDate>(propertyName)
            is LocalDateTime -> return root.get<LocalDateTime>(propertyName)
            is Char -> return root.get<Char>(propertyName)
            is Number -> return root.get<Number>(propertyName)
            else -> root.get<Any>(propertyName)
        }
    }

    @Deprecated("Use getPath(root, propertyName, propertyValue) instead")
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

            else -> throw NotSupportedTypeException(filter.javaClass, "GET_PATH")
        }
    }

    @Deprecated("Use getPath(root, propertyName, propertyValue) instead")
    protected open fun getPathByValue(root: Root<*>, property: String, value: Any): Path<*> {
        when (value) {
            is Int -> return root.get<Int>(property)
            is Long -> return root.get<Long>(property)
            is Double -> return root.get<Double>(property)
            is Float -> return root.get<Float>(property)
            is String -> return root.get<String>(property)
            is Date -> return root.get<Date>(property)
            is LocalDate -> return root.get<LocalDate>(property)
            is LocalDateTime -> return root.get<LocalDateTime>(property)
            is Char -> return root.get<Char>(property)
            is Number -> return root.get<Number>(property)
        }

        return root.get<Any>(property)
    }

    protected open fun getLiteral(filter: ComparativeOperatorFilter): Any {
        return filter.literal
    }
}
