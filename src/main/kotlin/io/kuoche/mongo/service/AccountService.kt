package io.kuoche.mongo.service

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import io.kuoche.mongo.model.po.Account
import io.kuoche.mongo.repository.NIOAccountRepository
import io.kuoche.mongo.repository.BIOAccountRepository
import io.quarkus.mongodb.reactive.ReactiveMongoClient
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import java.math.BigDecimal
import javax.enterprise.context.ApplicationScoped
import javax.transaction.Transactional

@ApplicationScoped
class AccountService(
    private val bioAccountRepository: BIOAccountRepository,
    private val nioAccountRepository: NIOAccountRepository,
    private val nioClient: ReactiveMongoClient

) {
    suspend fun transferWithoutTransaction(fromUUID: String, toUUID: String, amount: BigDecimal){
        nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount.negate())
            .where("${Account::uuid.name}", fromUUID)
            .awaitSuspending()
        throw RuntimeException("database error")
        nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount)
            .where("${Account::uuid.name}", toUUID)
            .awaitSuspending()
    }

    @Transactional
    suspend fun transferWithNIOAndAnnotation(fromUUID: String, toUUID: String, amount: BigDecimal){
        nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount.negate())
            .where("${Account::uuid.name}", fromUUID)
            .awaitSuspending()
        throw RuntimeException("database error")
        nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount)
            .where("${Account::uuid.name}", toUUID)
            .awaitSuspending()
    }

    @Transactional
    fun transferWithBIO(fromUUID: String, toUUID: String, amount: BigDecimal){
        bioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount.negate())
            .where("${Account::uuid.name}", fromUUID)
        throw RuntimeException("database error")
        bioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount)
            .where("${Account::uuid.name}", toUUID)
    }

    suspend fun transferWithAIOAndDriver(fromUUID: String, toUUID: String, amount: BigDecimal){
        val session = nioClient.startSession().awaitSuspending()
        // like java 7 auto-close resources
        session.use {
            it.startTransaction()
            try{
                nioAccountRepository.mongoCollection()
                    .updateOne(
                        session,
                        Filters.eq(Account::uuid.name, fromUUID),
                        Updates.inc(Account::amount.name, amount.negate())
                    ).awaitSuspending()
                throw RuntimeException("database error")
                nioAccountRepository.mongoCollection()
                    .updateOne(
                        session,
                        Filters.eq(Account::uuid.name, toUUID),
                        Updates.inc(Account::amount.name, amount)
                    )
                // 如果你不等待交易 commit 直接關閉 session 交易會 abort
                Uni.createFrom().publisher(it.commitTransaction()).awaitSuspending()
            }catch (e: Exception){
                Uni.createFrom().publisher(it.abortTransaction()).awaitSuspending()
                throw e
            }
        }
    }
}