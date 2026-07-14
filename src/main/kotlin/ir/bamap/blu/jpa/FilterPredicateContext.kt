package ir.bamap.blu.jpa

import ir.bamap.blu.jpa.config.PathResolver
import ir.bamap.blu.model.filter.FilterModel
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Root

open class FilterPredicateContext<T: FilterModel>(
    open val pathResolver: PathResolver,
    open val builder: CriteriaBuilder,
    open val root: Root<*>,
    open val filter: T
) {
}