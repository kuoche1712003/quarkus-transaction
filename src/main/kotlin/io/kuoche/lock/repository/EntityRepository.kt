package io.kuoche.lock.repository

import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.*
import io.kuoche.lock.model.po.Entity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.bson.types.ObjectId
import java.time.Clock
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class EntityRepository: ReactivePanacheMongoRepository<Entity> {
    companion object{
        const val LOCK_FIELD = "_self_lock"
    }

    suspend fun findAndLock(id: ObjectId): Entity? {
        val now = LocalDateTime.now(Clock.systemUTC())
        val fiveMinutesAgo = now.plusMinutes(-5)
        return  mongoCollection().findOneAndUpdate(
            and(
                eq("_id", id),
                or(
                    exists(LOCK_FIELD, false),
                    lte(LOCK_FIELD,fiveMinutesAgo)
                )
            ),
            combine(
                set(LOCK_FIELD, now),
                inc("count", 1)
            ),
        ).awaitSuspending()
    }

    suspend fun findAndRelease(id: ObjectId): Entity?{
        return mongoCollection().findOneAndUpdate(
            eq("_id", id),
            unset(LOCK_FIELD)
        ).awaitSuspending()
    }

}