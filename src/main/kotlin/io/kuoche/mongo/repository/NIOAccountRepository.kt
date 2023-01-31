package io.kuoche.mongo.repository

import io.kuoche.mongo.model.po.Account
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class NIOAccountRepository: ReactivePanacheMongoRepository<Account>