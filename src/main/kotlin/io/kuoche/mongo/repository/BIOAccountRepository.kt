package io.kuoche.mongo.repository

import io.kuoche.mongo.model.po.Account
import io.quarkus.mongodb.panache.kotlin.PanacheMongoRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class BIOAccountRepository: PanacheMongoRepository<Account>