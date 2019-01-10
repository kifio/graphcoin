package imurashov.graphcoin.repository.network

import io.reactivex.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class BlockchainService {

    private val api: BlockchainApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build().create(BlockchainApi::class.java)
    }

    fun getData(period: String): Single<Response> {
        return api.dataForPeriod(period)
    }

    companion object {
        private const val BASE_URL = "https://api.blockchain.info/"
    }
}