package io.kuoche.lock.model.po

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@MongoEntity(collection = "vc")
data class Version(
    @field:BsonId
    var id: ObjectId? = null,
    var entityId: ObjectId,
    var detail: String,
)
