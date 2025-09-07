package ir.bamap.blu.jpa.service

import ir.bamap.blu.model.PageSearchModel
import ir.bamap.blu.model.ResultSearchModel
import java.io.Serializable

interface DbService<Entity : Any, ID : Serializable> {

    fun findOrNull(id: ID?): Entity?

    fun find(id: ID?): Entity

    fun findByIds(ids: Iterable<ID>): List<Entity>

    fun <T : Entity> save(entity: T): T

    fun delete(entity: Entity)

    fun deleteById(id: ID)

    fun findBy(pageSearchModel: PageSearchModel): ResultSearchModel<Entity>

    fun getEntityClass(): Class<Entity>

    fun flush()
}
