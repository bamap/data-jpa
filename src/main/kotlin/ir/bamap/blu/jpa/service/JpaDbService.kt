package ir.bamap.blu.jpa.service

import ir.bamap.blu.exception.NotFoundException
import ir.bamap.blu.jpa.config.QuerySpecification
import ir.bamap.blu.model.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.security.InvalidParameterException

@Transactional(readOnly = true)
abstract class JpaDbService<Entity : Any, ID : Serializable>(
    protected open val repository: JpaRepository<Entity, ID>
) : DbService<Entity, ID> {

    companion object {
        protected fun orderToSort(orders: Orders): Sort {
            val springOrders = orders.map {
                if (it.direction == OrderModel.Direction.ASC)
                    Sort.Order.asc(it.propertyName)
                else
                    Sort.Order.desc(it.propertyName)
            }

            return Sort.by(springOrders)
        }
    }

    protected open val specification: QuerySpecification<Entity> = QuerySpecification()

    override fun findOrNull(id: ID?): Entity? {
        if (id == null)
            return null

        val optional = repository.findById(id)
        if (optional.isEmpty)
            return null

        return optional.get()
    }

    override fun find(id: ID?): Entity {
        if (id == null)
            throw InvalidParameterException("id")

        return findOrNull(id) ?: throw NotFoundException(getEntityClass().simpleName, id)
    }

    override fun findByIds(ids: Iterable<ID>): List<Entity> = repository.findAllById(ids)

    @Transactional
    override fun <T : Entity> save(entity: T): T = repository.save(entity)

    @Transactional
    override fun delete(entity: Entity) = repository.delete(entity)

    @Transactional
    override fun deleteById(id: ID) = repository.deleteById(id)

    override fun findBy(pageSearchModel: PageSearchModel): ResultSearchModel<Entity> {
        val pageable = PageRequest.of(pageSearchModel.page, pageSearchModel.limit)
        if(pageSearchModel is CustomSearchModel)
            return findBy(pageSearchModel.toSearchModel())

        if (pageSearchModel is SearchModel && repository is JpaSpecificationExecutor<*>) {
            val sortablePageable: PageRequest = pageable.withSort(orderToSort(pageSearchModel.orders))

            val jpaSpecificationRepository = repository as JpaSpecificationExecutor<Entity>
            val specification = this.specification.getSpecification(pageSearchModel.filters)
            val result: Page<Entity> = jpaSpecificationRepository.findAll(specification, sortablePageable)

            return ResultSearchModel(result.content, result.totalElements)
        }
        val result = repository.findAll(pageable)
        return ResultSearchModel(result.content, result.totalElements)
    }

    override fun flush() {
        repository.flush()
    }
}
