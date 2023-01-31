package io.kuoche.mongo.resource

import io.kuoche.mongo.service.AccountService
import org.jboss.logging.Logger
import java.math.BigDecimal
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Response

@Path("/mongodb")
class AccountResource(
    private val accountService: AccountService,
    private val logger: Logger
) {
    @GET
    @Path("/transfer-without-transaction")
    suspend fun transferWithoutTransaction(): Response{
        try {
            accountService.transferWithoutTransaction("1", "2", BigDecimal.valueOf(1000))
        }catch (e: Exception){
            logger.error(e.message)
        }
        return Response.noContent().build()
    }

    @GET
    @Path("/transfer-nio-annotation")
    suspend fun transferWithNIOAndAnnotation(): Response{
        try {
            accountService.transferWithNIOAndAnnotation("1", "2", BigDecimal.valueOf(1000))
        }catch (e: Exception){
            logger.error(e.message)
        }
        return Response.noContent().build()
    }

    @GET
    @Path("/transfer-bio")
    fun transferWithBIO(): Response{
        try{
            accountService.transferWithBIO("1", "2", BigDecimal.valueOf(1000))
        }catch (e: Exception){
            logger.error(e.message)
        }
        return Response.noContent().build()
    }

    @GET
    @Path("/transfer-nio-driver")
    suspend fun transferWithNIOAndDriver(): Response{
        try{
            accountService.transferWithAIOAndDriver("1","2", BigDecimal.valueOf(1000))
        }catch (e: Exception){
            logger.error(e.message)
        }
        return Response.noContent().build()
    }
}