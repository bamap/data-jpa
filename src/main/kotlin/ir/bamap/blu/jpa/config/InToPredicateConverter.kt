package ir.bamap.blu.jpa.config

import ir.bamap.blu.jpa.FilterPredicateContext
import ir.bamap.blu.model.filter.FilterModel
import ir.bamap.blu.model.filter.In
import ir.bamap.blu.model.filter.NotIn
import jakarta.persistence.criteria.Predicate

class InToPredicateConverter : FilterToPredicateConverter<In> {

    override fun isSupported(filter: FilterModel): Boolean = filter is In

    override fun convert(context: FilterPredicateContext<in In>): Predicate {

        val filter = context.filter
        require(filter is In)
        val propertyPath = context.root.get<Any>(filter.propertyName)
        val predicate = propertyPath.`in`(filter.literal)
        if (filter is NotIn)
            return predicate.not()

        return predicate
    }
}