package ir.bamap.blu.jpa.config

import ir.bamap.blu.exception.NotSupportedTypeException
import ir.bamap.blu.jpa.FilterPredicateContext
import ir.bamap.blu.model.OrderModel
import ir.bamap.blu.model.filter.*
import jakarta.persistence.criteria.*
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Filter

open class QuerySpecification<Entity : Any>(
    open val pathResolver: PathResolver = PathResolver(),
) {

    protected open val converters = mutableListOf<FilterToPredicateConverter<*>>()

    init {
        initConverters()
    }

    protected open fun initConverters() {
        converters.add(BetweenToPredicateConverter())
        converters.add(InToPredicateConverter())
        converters.add(ComparativeToPredicateConverter())
    }

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

    protected open fun getOrder(builder: CriteriaBuilder, root: Root<*>, sortModel: OrderModel): Order {
        val path = pathResolver.resolve(builder, root, sortModel.propertyName)
        if (sortModel.cases.isEmpty())
            return if (sortModel.direction == OrderModel.Direction.ASC)
                builder.asc(path)
            else
                builder.desc(path)

        val orderCase = builder.selectCase<Int>()
        sortModel.cases.forEachIndexed { index, case ->
            orderCase.`when`(builder.equal(path, case), index)
        }
        orderCase.otherwise(sortModel.cases.size)

        return if (sortModel.direction == OrderModel.Direction.ASC)
            builder.asc(orderCase)
        else
            builder.desc(orderCase)
    }

    protected open fun <U : Entity> getPredicate(
        builder: CriteriaBuilder,
        root: Root<U>,
        query: CriteriaQuery<*>?,
        filter: FilterModel
    ): Predicate {
        for (converter in converters) {
            if (converter.isSupported(filter)) {
                val context = FilterPredicateContext(pathResolver, builder, root, filter)
                return converter.convert(context)
            }
        }

        if (filter is ClassFilter)
            return builder.equal(root.type(), filter.cls)

        if (filter is GroupFilter) {

            val filters = getPredicates(builder, root, query, filter.filters).toTypedArray()

            return when (filter) {
                is NotOr -> builder.not(builder.or(*filters))
                is NotAnd -> builder.not(builder.and(*filters))
                is And -> builder.and(*filters)
                is Or -> builder.or(*filters)
                else -> throw NotSupportedTypeException(filter::class.java, "GROUP_GET_PREDICATE")
            }
        }

        if (filter is NotFilter)
            return builder.not(getPredicate(builder, root, query, filter.filter))

        return when (filter) {
            is IsNull -> builder.isNull(pathResolver.resolve(builder, root, filter.propertyName))
            is IsNotNull -> builder.isNotNull(pathResolver.resolve(builder, root, filter.propertyName))
            else -> throw NotSupportedTypeException(filter.javaClass, "GET_PREDICATE")
        }
    }
}
