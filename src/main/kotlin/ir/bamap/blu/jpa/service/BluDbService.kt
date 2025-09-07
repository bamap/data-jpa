package ir.bamap.blu.jpa.service

import ir.bamap.blu.exception.NotFoundException
import ir.bamap.blu.jpa.repository.BluRepository
import ir.bamap.blu.model.PageSearchModel
import ir.bamap.blu.model.ResultSearchModel
import ir.bamap.blu.model.filter.In
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.security.InvalidParameterException

@Transactional(readOnly = true)
abstract class BluDbService<Entity : Any, ID : Serializable>(
    protected open val repository: BluRepository<Entity, ID>
) : DbService<Entity, ID> {

    override fun findOrNull(id: ID?): Entity? {
        if (id == null)
            return null

        return repository.findOrNull(id)
    }

    override fun find(id: ID?): Entity {
        if (id == null)
            throw InvalidParameterException("id")

        return findOrNull(id) ?: throw NotFoundException(getEntityClass().simpleName, id)
    }

    override fun findByIds(ids: Iterable<ID>): List<Entity> {
        val listIds = ids.toSet().toList()
        if(listIds.isEmpty())
            return emptyList()
        val inFilter = In(repository.getIdProperty(), listIds)
        return repository.findBy(inFilter)
    }

    @Transactional
    override fun <T : Entity> save(entity: T): T = repository.merge(entity)

    @Transactional
    override fun delete(entity: Entity) = repository.delete(entity)

    @Transactional
    override fun deleteById(id: ID) {
        repository.deleteByIds(listOf(id))
    }

    override fun findBy(pageSearchModel: PageSearchModel): ResultSearchModel<Entity> =
        repository.findBy(pageSearchModel)

    override fun flush() {
        repository.flush()
    }
}
