package ir.bamap.blu.jpa.config

import ir.bamap.blu.jpa.FilterPredicateContext
import ir.bamap.blu.model.filter.FilterModel
import jakarta.persistence.criteria.Predicate

interface FilterToPredicateConverter<T: FilterModel> {

    fun isSupported(filter: FilterModel): Boolean

    fun convert(filter: FilterPredicateContext<in T>): Predicate
}