package ir.bamap.blu.jpa.repository

import ir.bamap.blu.exception.EmptyException
import ir.bamap.blu.jpa.config.QuerySpecification
import ir.bamap.blu.model.*
import ir.bamap.blu.model.filter.FilterModel
import ir.bamap.blu.model.filter.Filters
import ir.bamap.blu.model.filter.In
import ir.bamap.blu.model.filter.IsNull
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.Collections.emptyList

open class BluRepositoryImpl<Entity : Any, ID : Serializable> constructor(
    protected open val cls: Class<Entity>,
    protected open val specification: QuerySpecification<Entity> = QuerySpecification()
) : BluRepository<Entity, ID> {

    protected val logger = LoggerFactory.getLogger(this.javaClass)

    @PersistenceContext
    protected lateinit var entityManager: EntityManager

    override fun getEntityClass(): Class<Entity> = this.cls

    //**************** Find One ****************
    override fun findOrNull(id: ID): Entity? = findOrNull(cls, id)

    override fun findForUpdateOrNull(id: ID): Entity? {
        return findForUpdateOrNull(cls, id)
    }

    override fun <U : Entity> findForUpdateOrNull(cls: Class<U>, id: ID): U? {
        return entityManager.find(cls, id, LockModeType.PESSIMISTIC_WRITE)
    }

    override fun findByIds(ids: Iterable<ID>): List<Entity> {
        val inFilter = In(getIdProperty(), ids.toSet().toList())
        return findBy(inFilter)
    }

    override fun <U : Entity> findOrNull(cls: Class<U>, id: ID): U? = entityManager.find(cls, id)

    override fun findReference(id: ID): Entity? = findReference(cls, id)

    override fun <U : Entity> findReference(cls: Class<U>, id: ID): U? = entityManager.getReference(cls, id)

    override fun findFirst(vararg filters: FilterModel): Entity = findFirst(cls, *filters)

    override fun findFirstOrNull(vararg filters: FilterModel): Entity? = findFirstOrNull(cls, *filters)

    override fun findFirst(orders: Orders, vararg filters: FilterModel): Entity = findFirst(cls, orders, *filters)

    override fun findFirstOrNull(orders: Orders, vararg filters: FilterModel): Entity? {
        return findFirstOrNull(cls, orders, *filters)
    }

    override fun <U : Entity> findFirst(cls: Class<U>, vararg filters: FilterModel): U {
        return findFirst(cls, Orders(), *filters)
    }

    override fun <U : Entity> findFirstOrNull(cls: Class<U>, vararg filters: FilterModel): U? {
        return findFirstOrNull(cls, Orders(), *filters)
    }

    override fun <U : Entity> findFirst(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U {

        val result = findFirstOrNull(cls, orders, *filters)
        if (result != null)
            return result

        logger.error("Filter[${Filters(*filters)}] in entity[$cls] has no result")
        throw EmptyException("find_first")
    }

    override fun <U : Entity> findFirstOrNull(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U? {
        val finalFilters = Filters(*filters)
        val searchModel = SearchModel(0, 1, finalFilters, orders)
        val result = findByWithoutCount(cls, searchModel)
        if (result.isEmpty())
            return null

        return result[0]
    }

    override fun findLast(vararg filters: FilterModel): Entity = findLast(cls, *filters)

    override fun findLastOrNull(vararg filters: FilterModel): Entity? = findLastOrNull(cls, *filters)

    override fun findLast(orders: Orders, vararg filters: FilterModel): Entity = findLast(cls, orders, *filters)

    override fun findLastOrNull(orders: Orders, vararg filters: FilterModel): Entity? =
        findLastOrNull(cls, orders, *filters)

    override fun <U : Entity> findLast(cls: Class<U>, vararg filters: FilterModel): U {
        return findLast(cls, Orders(), *filters)
    }

    override fun <U : Entity> findLastOrNull(cls: Class<U>, vararg filters: FilterModel): U? =
        findLastOrNull(cls, Orders(), *filters)

    override fun <U : Entity> findLast(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U {
        return findLastOrNull(cls, orders, *filters)
            ?: throw ArrayIndexOutOfBoundsException("Filter[${Filters(*filters)}] in entity[$cls] has no result")
    }

    override fun <U : Entity> findLastOrNull(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U? {
        val reverseOrders = Orders()
        orders.forEach {
            val reverseDirection =
                if (it.direction == OrderModel.Direction.ASC) OrderModel.Direction.DESC else OrderModel.Direction.ASC
            reverseOrders.add(OrderModel(it.propertyName, reverseDirection))
        }

        return findFirstOrNull(cls, reverseOrders, *filters)
    }

    override fun findUnique(vararg filters: FilterModel): Entity = findUnique(cls, *filters)

    override fun <U : Entity> findUnique(cls: Class<U>, vararg filters: FilterModel): U {
        val criteriaQuery = createQuery(cls, filters.toList(), emptyList())
        return entityManager.createQuery(criteriaQuery).singleResult
    }

    protected open fun <U : Entity> createQuery(
        cls: Class<U>,
        filters: Collection<FilterModel>,
        orders: Collection<OrderModel>
    ): CriteriaQuery<U> {
        val criteriaBuilder = entityManager.criteriaBuilder

        val criteriaQuery = criteriaBuilder.createQuery(cls)

        val root = criteriaQuery.from(cls)

        if (filters.isNotEmpty()) {
            val predicate = this.specification.getSpecification(filters.toList())
                .toPredicate(root as Root<Entity?>, criteriaQuery, criteriaBuilder)

            if (predicate != null)
                criteriaQuery.where(predicate)
        }

        if (orders.isNotEmpty()) {
            val ordersList = this.specification.getOrders(criteriaBuilder, root, orders)
            criteriaQuery.orderBy(*ordersList.toTypedArray())
        }

        return criteriaQuery
    }

    //**************** Find By ****************
    override fun findBy(vararg filters: FilterModel): List<Entity> = findBy(cls, *filters)

    override fun findBy(orders: Orders, vararg filters: FilterModel): List<Entity> = findBy(cls, orders, *filters)

    override fun findIdsBy(vararg filters: FilterModel): Set<ID> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery()
        val root = criteriaQuery.from(cls)

        val idPath = root.get<ID>(getIdProperty())
        criteriaQuery.select(idPath)

        if (filters.isNotEmpty()) {
            val predicate = specification.getSpecification(filters.toList())
                .toPredicate(root, criteriaQuery, criteriaBuilder)
            if (predicate != null)
                criteriaQuery.where(predicate)
        }

        return entityManager.createQuery(criteriaQuery).resultList
            .map { it as ID }
            .toSet()
    }

    override fun findIdsBy(filters: Collection<FilterModel>): Set<ID> {
        return findIdsBy(*filters.toTypedArray())
    }

    override fun <U : Entity> findBy(cls: Class<U>, vararg filters: FilterModel): List<U> =
        findBy(cls, Orders(), *filters)

    override fun <U : Entity> findBy(cls: Class<U>, orders: Orders, vararg filters: FilterModel): List<U> {
        val query = createQuery(cls, filters.toList(), orders)
        return entityManager.createQuery(query).resultList
    }

    override fun findByNull(property: String): List<Entity> = findByNull(cls, property)

    override fun <U : Entity> findByNull(cls: Class<U>, property: String): List<U> = findBy(cls, IsNull(property))

    //**************** Find All ****************
    override fun findAll(): List<Entity> = findAll(cls)

    override fun <U : Entity> findAll(cls: Class<U>): List<U> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val criteriaQuery = criteriaBuilder.createQuery(cls)

        val rootEntry = criteriaQuery.from(cls)
        criteriaQuery.select(rootEntry)
        val allQuery = entityManager.createQuery(criteriaQuery)

        return allQuery.resultList
    }

    //**************** CRUD ****************
    override fun <U : Entity> persist(entityObject: U): Unit = entityManager.persist(entityObject)

    override fun <U : Entity> merge(entityObject: U): U = entityManager.merge(entityObject)

    override fun detach(entityObject: Entity) = entityManager.detach(entityObject)

    override fun delete(entityObject: Entity) {
        val contains = entityManager.contains(entityObject)
        val removeObject =
            if (contains)
                entityObject
            else
                entityManager.merge(entityObject)

        entityManager.remove(removeObject)
    }

    override fun deleteByIds(values: Collection<ID>): Int = deleteByIds(cls, values)

    override fun <U : Entity> deleteByIds(cls: Class<U>, ids: Collection<ID>): Int {

        val criteriaBuilder = entityManager.criteriaBuilder
        val deleteQuery = criteriaBuilder.createCriteriaDelete(cls)
        val root = deleteQuery.from(cls)
        val pathProperty = root.get<Any>(getIdProperty())
        deleteQuery.where(pathProperty.`in`(ids.toSet()))

        return entityManager.createQuery(deleteQuery).executeUpdate()
    }

    override fun deleteBy(filter: FilterModel): Int = deleteBy(listOf(filter))

    override fun deleteBy(filters: Collection<FilterModel>): Int {
        val criteriaBuilder = entityManager.criteriaBuilder
        val deleteQuery = criteriaBuilder.createCriteriaDelete(cls)
        val root = deleteQuery.from(cls)

        val restrictions = this.specification.getPredicates(criteriaBuilder, root, null, filters)
        deleteQuery.where(*restrictions.toTypedArray())
        return entityManager.createQuery(deleteQuery).executeUpdate()
    }

    //**************** Pagination ****************

    override fun count(filter: FilterModel): Long {
        return count(cls, Filters(filter))
    }

    override fun count(filters: Collection<FilterModel>): Long = count(cls, filters)

    override fun <U : Entity> count(cls: Class<U>, filter: FilterModel): Long {
        return count(cls, Filters(filter))
    }

    override fun <U : Entity> count(cls: Class<U>, filters: Collection<FilterModel>): Long {
        val criteriaBuilder = entityManager.criteriaBuilder

        val criteriaQuery = criteriaBuilder.createQuery(Long::class.java)
        val root = criteriaQuery.from(cls)
        criteriaQuery.select(criteriaBuilder.countDistinct(root))

        if (filters.isNotEmpty()) {
            val predicates = specification.getPredicates(criteriaBuilder, root as Root<Entity>, criteriaQuery, filters)
            criteriaQuery.where(*predicates.toTypedArray())
        }


        return entityManager.createQuery(criteriaQuery).singleResult
    }

    override fun findBy(searchModel: PageSearchModel): ResultSearchModel<Entity> {
        return findBy(cls, searchModel)
    }

    override fun <U : Entity> findBy(cls: Class<U>, searchModel: PageSearchModel): ResultSearchModel<U> {

        val finalSearchModel = when (searchModel) {
            is SearchModel -> searchModel
            is CustomSearchModel -> searchModel.toSearchModel()
            else -> SearchModel(searchModel.page, searchModel.limit)
        }

        val records = findByWithoutCount(cls, finalSearchModel)
        val total = count(cls, finalSearchModel.filters)
        val result = ResultSearchModel(records, total)

        return result
    }

    override fun flush() {
        entityManager.flush()
    }

    protected open fun getFirstResult(searchModel: SearchModel): Int {
        return searchModel.getFirstResult()
    }

    protected open fun <U : Entity> findByWithoutCount(cls: Class<U>, searchModel: SearchModel): List<U> {
        if (searchModel.limit == 0)
            return emptyList()
        val criteriaBuilder = entityManager.criteriaBuilder

        val criteriaQuery = criteriaBuilder.createQuery(cls)

        val root = criteriaQuery.from(cls)

        if (searchModel.filters.isNotEmpty()) {
            val specification = specification.getSpecification(searchModel.filters)
            val predicate = specification.toPredicate(root as Root<Entity?>, criteriaQuery, criteriaBuilder)
            if (predicate != null)
                criteriaQuery.where(predicate)
        }

        if (searchModel.orders.isNotEmpty()) {
            val orders = this.specification.getOrders(criteriaBuilder, root, searchModel.orders)
            criteriaQuery.orderBy(orders)
        }

        val typedQuery = entityManager.createQuery(criteriaQuery)
        typedQuery.firstResult = getFirstResult(searchModel)

        if (searchModel.limit > 0)
            typedQuery.maxResults = searchModel.limit

        return typedQuery.resultList
    }
}
