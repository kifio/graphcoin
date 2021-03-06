package imurashov.graphcoin.domain

import android.app.Application
import androidx.room.Room
import imurashov.graphcoin.repository.db.database.AppDatabase
import imurashov.graphcoin.repository.db.entity.Graph
import imurashov.graphcoin.repository.network.BlockchainService
import imurashov.graphcoin.repository.network.Response
import imurashov.graphcoin.utils.Normalization
import imurashov.graphcoin.utils.Optional
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import java.io.IOException

class Interactor(app: Application) {

    private var networkTask: Disposable? = null

    private val database = Room.databaseBuilder(
            app, AppDatabase::class.java, DB_NAME).build()

    private val blockchainService = BlockchainService()

    fun getGraphForPeriod(period: String): Observable<Pair<Boolean, Optional<Graph>>> =
        Observable.create { emitter ->
            var isCached = false
            val graph = getCachedGraph(period)
            if (graph != null) {
                isCached = true
                emitter.onNext(Pair(true, Optional.of(graph)))
            }
            networkTask?.dispose()
            networkTask = blockchainService.getData(period)
                .subscribe({
                    if (!emitter.isDisposed) {
                        emitter.onNext(Pair(false, parseResponse(period, it)))
                    }
                }, {
                    it.printStackTrace()
                    if (!emitter.isDisposed && !isCached) {
                        emitter.onError(IOException("Some server error"))
                    }
                })
        }

    private fun parseResponse(period: String, response: Response): Optional<Graph> {
        val graph = createGraph(period, response)
        if (graph != null) cacheGraph(graph)
        return Optional(graph)
    }

    private fun createGraph(period: String, response: Response): Graph? {
        val graph = Graph(period, response)
        graph.points.addAll(Normalization.normalizeValues(period, response.values))
        return graph
    }

    private fun cacheGraph(graph: Graph) {
        database.graphDAO().insert(graph)
        database.pointDAO().insert(graph.points)
    }

    private fun getCachedGraph(period: String): Graph? {
        val graph = database.graphDAO().query(period)
        graph?.points?.addAll(database.pointDAO().getForPeriod(period))
        return graph
    }

    companion object {
        private const val DB_NAME = "database"
    }
}