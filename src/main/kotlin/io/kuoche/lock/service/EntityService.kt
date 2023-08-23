package io.kuoche.lock.service

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.*
import io.kuoche.lock.model.po.Entity
import io.kuoche.lock.model.po.Version
import io.kuoche.lock.repository.EntityRepository
import io.kuoche.lock.repository.VersionRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.bson.types.ObjectId
import org.jboss.logging.Logger
import javax.enterprise.context.ApplicationScoped
import kotlin.random.Random

@ApplicationScoped
class EntityService(
    private val entityRepository: EntityRepository,
    private val versionRepository: VersionRepository,
    private val client: ReactiveMongoClient,
    private val logger: Logger,
) {

    suspend fun create(): String{
        val entity = Entity(name = "test name")
        val savedEntity = entityRepository.persist(entity).awaitSuspending()
        val version = Version(entityId = savedEntity.id!!, detail = "test detail")
        versionRepository.persist(version).awaitSuspending()
        return savedEntity.id!!.toString()
    }


    suspend fun sadLock(id: String){
        val entityId = ObjectId(id)
        entityRepository.findAndLock(entityId)
            ?: throw RuntimeException("entity not exist or lock")
        val version = Version(entityId = entityId, detail = "${Random.nextInt()}")
        versionRepository.persist(version).awaitSuspending()
        logger.info(version.detail)
        entityRepository.findAndRelease(entityId)
    }


    suspend fun transactionLock(id: String){
        val entityId = ObjectId(id)
        val session = client.startSession().awaitSuspending()
        session.use {
            it.startTransaction()
            try {
                entityRepository.mongoCollection().findOneAndUpdate(it,
                    eq("_id", entityId),
                    inc("count", 1)
                ).awaitSuspending() ?: throw RuntimeException("entity not exist")
                val version = Version(entityId = entityId, detail = "${Random.nextInt()}")
                versionRepository.persist(version).awaitSuspending()
                Uni.createFrom().publisher(it.commitTransaction()).awaitSuspending()
            }catch (e: Exception){
                logger.error(e.message, e)
                Uni.createFrom().publisher(it.abortTransaction()).awaitSuspending()
                throw RuntimeException("entity not exist or lock")
            }
        }
    }



}