# quarkus-transaction

此專案展示 [Quarkus](https://quarkus.io/) 資料庫交易的使用案例。

## ＭongoDB
MongoDB 對於單一 Document 的操作是原子性，但有時候我們需要同時操作多個 Document 並保證這個操作是原子性。

MongoDB 從 4.0 版提供了 ACID 交易。

### 情境
我們在 MongoDB 使用 Account Collection 保存使用者帳戶資訊，在初始化時新增兩個使用者 George (帳戶金額 2000) 和 Mary (帳戶金額 0)。

每次操作轉帳時從 George 轉 1000 給 Mary ，但都會拋出例外模擬發生錯誤，測試不同寫法的差別。

#### 不使用交易
```kotlin
suspend fun transferWithoutTransaction(fromUUID: String, toUUID: String, amount: BigDecimal){
    nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount.negate())
        .where("${Account::uuid.name}", fromUUID)
        .awaitSuspending()
    throw RuntimeException("database error")
    nioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount)
        .where("${Account::uuid.name}", toUUID)
        .awaitSuspending()
}
```
執行後發現 George 的帳戶金額只剩下 1000。

### 使用 @Transactional
Quarkus ＭongoDB Panache [文件](https://quarkus.io/guides/mongodb-panache#transactions)有提到你可以在方法上使用這個 @Transactional 開啟交易。

要注意的是 MongoDB 的交易只有在 Replicaset 使用，幸好 Quarkus Dev Services MongoDB 會啟用單節點的 Replicaset。

```kotlin
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
```
執行後發生錯誤```Cannot start a JTA transaction from the IO thread.```

### 使用 @Transactional 和 BIO
剛剛的錯誤看起來跟 NIO 有關係改用 BIO 試試看。

```kotlin
@Transactional
fun transferWithBIO(fromUUID: String, toUUID: String, amount: BigDecimal){
    bioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount.negate())
        .where("${Account::uuid.name}", fromUUID)
    throw RuntimeException("database error")
    bioAccountRepository.update("{'\$inc': {'${Account::amount.name}': ?1}}", amount)
        .where("${Account::uuid.name}", toUUID)
}
```
執行後查看資料庫 George 的帳戶金額還有 1000 交易成功了。

### 使用 ReactiveMongoClient
既然 Panache 的 NIO 無法支援交易，改用 NIO 的 ReactiveMongoClient 試試看。

Quarkus 官網 MongoDB Client [文件](https://quarkus.io/guides/mongodb)完全沒有關於交易的說明，
只好參考 MongoDB Transaction API [文件](https://www.mongodb.com/docs/v4.4/core/transactions/#transactions-api)。

文件中提到每個交易中的操作需要與 session 關聯，因此手動使用 ReactiveMongoClient 開啟 session ，並自己開啟交易 try catch 決定 commit or abort。

寫起來跟 JDBC Driver 手動管理交易很像。

```kotlin
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
```
執行後查看資料庫 George 的帳戶金額還有 1000 交易成功了。


### 感想
在 MongoDB 還沒支援交易前要完成交易只能開發者自己實作 Two Phase Commit，在 MongoDB 4.0 之後在 ReplicaSet 支援交易方便了許多。

但 Quarkus 對此支援度不是很好，目前要使用 NIO API 只能使用 ReactiveMongoClient，另外 MongoDB 的交易層級的設定與 RDBMS 不太一樣，若是想再正式環境使用需要好好研究其中機制與效能議題。









## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/quarkus-transaction-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.