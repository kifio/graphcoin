package imurashov.graphcoin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import imurashov.graphcoin.domain.Interactor
import imurashov.graphcoin.repository.db.entity.Graph
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GraphCoinViewModel(app: Application) : AndroidViewModel(app) {

    val graphData: MutableLiveData<Graph> = MutableLiveData()
    val errorData: MutableLiveData<Int> = MutableLiveData()

    private val interactor = Interactor(app)
    private var task: Disposable? = null

    fun setTimespan(period: String) {
        task?.dispose()
        task = interactor.getGraphForPeriod(period)
            .observeOn(Schedulers.io())
            .subscribeOn(Schedulers.io())
            .subscribe({
                val graph = it.getOrNull()
                if (graph != null) showGraph(graph)
            }, {
                showError()
            })
    }

    private fun showError() {
        errorData.postValue(R.string.err_message)
    }

    private fun showGraph(graph: Graph) {
        graphData.postValue(graph)
    }

}