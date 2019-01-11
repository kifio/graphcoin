package imurashov.graphcoin.presentation

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import imurashov.graphcoin.R
import imurashov.graphcoin.domain.Interactor
import imurashov.graphcoin.repository.db.entity.Graph
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GraphCoinViewModel(app: Application) : AndroidViewModel(app) {

    val graphData: MutableLiveData<Graph> = MutableLiveData()
    val errorData: MutableLiveData<Int> = MutableLiveData()

    private val interactor = Interactor(app)
    private var task: Disposable? = null

    fun setPeriod() {
        val savedPeriod = readPeriod()
        if (savedPeriod != null) {
            setPeriod(savedPeriod)
        }
    }

    fun setPeriod(period: String) {
        savePeriod(period)
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

    private fun savePeriod(period: String) {
        val ctx: Context = getApplication()
        val preferences = ctx.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        preferences.edit().putString(PERIOD_KEY, period).apply()
    }

    private fun readPeriod(): String? {
        val ctx: Context = getApplication()
        val preferences = ctx.getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        return if (preferences.all.isEmpty()) {
            DEFAULT_PERIOD
        } else {
            preferences.getString(PERIOD_KEY, null)
        }
    }

    private fun showError() {
        errorData.postValue(R.string.err_message)
    }

    private fun showGraph(graph: Graph) {
        graphData.postValue(graph)
    }

    companion object {
        private const val PREFERENCES = "imurashov.graphcoin.preferences"
        private const val PERIOD_KEY = "imurashov.graphcoin.preferences.period"
        private const val DEFAULT_PERIOD = "1months"
    }

}