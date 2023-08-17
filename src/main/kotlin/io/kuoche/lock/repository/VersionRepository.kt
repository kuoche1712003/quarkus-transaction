package io.kuoche.lock.repository

import io.kuoche.lock.model.po.Version
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoRepository
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class VersionRepository: ReactivePanacheMongoRepository<Version> {
}