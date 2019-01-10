package imurashov.graphcoin.repository.network

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface BlockchainApi {
    @GET("charts/market-price?format=json")
    fun dataForPeriod(@Query("timespan") period: String): Single<Response>
}