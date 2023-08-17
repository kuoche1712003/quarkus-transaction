package io.kuoche.mongo.config

import io.kuoche.mongo.model.po.Account
import io.kuoche.mongo.repository.BIOAccountRepository
import io.quarkus.runtime.Startup
import java.math.BigDecimal
import javax.annotation.PostConstruct
import javax.enterprise.context.ApplicationScoped

//@Startup
//@ApplicationScoped
class DataStartup(
    private val bioAccountRepository: BIOAccountRepository
) {

    @PostConstruct
    fun onStart() {
        val accountAmount = bioAccountRepository.count()
        if(accountAmount == 0L){
            val george = Account(uuid = "1", name = "George", amount = BigDecimal.valueOf(2000))
            val mary = Account(uuid = "2", name = "Mary")
            bioAccountRepository.persist(george, mary)
        }
    }


}