package ir.bamap.blu.jpa.repository

import ir.bamap.blu.model.Orders
import ir.bamap.blu.model.PageSearchModel
import ir.bamap.blu.model.ResultSearchModel
import ir.bamap.blu.model.filter.FilterModel
import java.io.Serializable

interface BluRepository<Entity: Any, ID: Serializable>
{
    fun getEntityClass(): Class<Entity>

    //**************** Find One ****************
    /**
     * @param id
     * @return
     */
    fun findOrNull(id: ID): Entity?
    /**
     * @param cls
     * @param id
     * @return
     */
    fun <U : Entity> findOrNull(cls: Class<U>, id: ID): U?

    fun findForUpdateOrNull(id: ID): Entity?
    fun <U : Entity> findForUpdateOrNull(cls: Class<U>, id: ID): U?

    fun findByIds(ids: Iterable<ID>): List<Entity>

    /**
     * This method get Reference of Entity Object. What's Reference? When you want
     * only update object, you can first find it and then update it. Or you can update it directly
     * @param id
     * @return
     */
    fun findReference(id: ID): Entity?

    /**
     * This method get Reference of Entity Object. What's Reference? When you want
     * only update object, you can first find it and then update it. Or you can update it directly
     * @param cls
     * @param id
     * @return
     */
    fun <U : Entity> findReference(cls: Class<U>, id: ID): U?

    fun findFirst(vararg filters: FilterModel): Entity
    fun findFirstOrNull(vararg filters: FilterModel): Entity?

    fun findFirst(orders: Orders, vararg filters: FilterModel): Entity
    fun findFirstOrNull(orders: Orders, vararg filters: FilterModel): Entity?

    fun <U : Entity> findFirst(cls: Class<U>, vararg filters: FilterModel): U
    fun <U : Entity> findFirstOrNull(cls: Class<U>, vararg filters: FilterModel): U?

    fun <U : Entity> findFirst(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U
    fun <U : Entity> findFirstOrNull(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U?

    fun findLast(vararg filters: FilterModel): Entity
    fun findLastOrNull(vararg filters: FilterModel): Entity?

    fun findLast(orders: Orders, vararg filters: FilterModel): Entity
    fun findLastOrNull(orders: Orders, vararg filters: FilterModel): Entity?

    fun <U : Entity> findLast(cls: Class<U>, vararg filters: FilterModel): U
    fun <U : Entity> findLastOrNull(cls: Class<U>, vararg filters: FilterModel): U?

    fun <U : Entity> findLast(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U
    fun <U : Entity> findLastOrNull(cls: Class<U>, orders: Orders, vararg filters: FilterModel): U?

    /**
     * We are sure there is only 1 match record in db
     * @return
     */
    fun findUnique(vararg filters: FilterModel): Entity

    /**
     * We are sure there is only 1 match record in db
     * @return
    </U> */
    fun <U : Entity> findUnique(cls: Class<U>, vararg filters: FilterModel): U

    //**************** Find By ****************
    fun findBy(vararg filters: FilterModel): List<Entity>

    fun findBy(orders: Orders, vararg filters: FilterModel): List<Entity>

    fun <U : Entity> findBy(cls: Class<U>, vararg filters: FilterModel): List<U>

    fun <U : Entity> findBy(cls: Class<U>, orders: Orders, vararg filters: FilterModel): List<U>

    fun findByNull(property: String): List<Entity>

    fun <U : Entity> findByNull(cls: Class<U>, property: String): List<U>

    //**************** Find All ****************
    /**
     * Find All records
     * For big tables it hash expensive cost.Be careful about using it
     * @return
     */
    fun findAll(): List<Entity>

    /**
     * Find All records
     * For big tables it hash expensive cost.Be careful about using it
     * @param cls
     * @return
     */
    fun <U : Entity> findAll(cls: Class<U>): List<U>

    //**************** CRUD ****************
    /**
     * Create a new object.
     * The object has transitioned from "**transient**" to "**persistent**" state. The object is in
     * the persistence context and not yet saved to the database. The generation of
     * INSERT statements will occur only upon committing the transaction, flushing
     * or closing the session. So after calling the method, entityObject has not id
     * @param entityObject the object to create
     */
    fun <U : Entity>persist(entityObject: U)

    /**
     * The main intention of the merge method is to update an entity instance with new field values
     * from a "**detached**" entity instance.
     * ***For update an instance, try to use merge method.***
     * @param entityObject
     */
    fun <U : Entity> merge(entityObject: U): U

    /**
     * The method change "**persistent**" state object to "**detached**" state
     * @param entityObject
     */
    fun detach(entityObject: Entity)

    /**
     * @param entityObject
     */
    fun delete(entityObject: Entity)

    /**
     * Remove records base on its idProperty be in values
     * @param values
     * @return affected rows
     */
    fun deleteByIds(values: Collection<ID>): Int

    fun <U : Entity> deleteByIds(cls: Class<U>, ids: Collection<ID>): Int

    fun deleteBy(filter: FilterModel): Int

    fun deleteBy(filters: Collection<FilterModel>): Int
    //**************** Pagination ****************

    fun count(filter: FilterModel): Long

    fun count(filters: Collection<FilterModel>): Long

    fun <U : Entity> count(cls: Class<U>, filter: FilterModel): Long

    fun <U : Entity> count(cls: Class<U>, filters: Collection<FilterModel>): Long

    fun findBy(searchModel: PageSearchModel): ResultSearchModel<Entity>

    fun <U : Entity> findBy(cls: Class<U>, searchModel: PageSearchModel): ResultSearchModel<U>

    fun flush()

    fun getIdProperty(): String = "id"
}
