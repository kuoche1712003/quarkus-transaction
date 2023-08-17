package io.kuoche.lock.resource

import io.kuoche.lock.service.EntityService
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

@Path("/api/entities")
class EntityResource(
    private val entityService: EntityService
) {

    @POST
    suspend fun create(): Response{
        val id = entityService.create()
        return Response.ok(mapOf("id" to id)).build()
    }

    @POST
    @Path("/{id}:test")
    suspend fun test(
        @PathParam("id") id: String
    ): Response{
        entityService.sadLock(id)
        return Response.noContent().build()
    }

    @POST
    @Path("/{id}:transaction")
    suspend fun testTransaction(
        @PathParam("id") id: String
    ): Response{
        entityService.transactionLock(id)
        return Response.noContent().build()
    }
}