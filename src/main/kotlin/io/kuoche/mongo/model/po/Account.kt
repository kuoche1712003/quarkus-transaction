package io.kuoche.mongo.model.po

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.types.ObjectId
import java.math.BigDecimal

@MongoEntity(collection = "Account")
data class Account(
    var id: ObjectId? = null,
    var uuid: String,
    var name: String,
    var amount: BigDecimal = BigDecimal.ZERO
)
