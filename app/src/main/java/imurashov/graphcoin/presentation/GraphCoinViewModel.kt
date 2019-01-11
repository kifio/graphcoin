package imurashov.graphcoin.presentation

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import imurashov.graphcoin.R
import imurashov.graphcoin.domain.Interactor
import imurashov.graphcoin.repository.db.entity.Graph
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class GraphCoinViewModel(app: Application) : AndroidViewModel(app) {

    val graphData: MutableLiveData<Pair<Boolean, Graph>> = MutableLiveData()
    val errorData: MutableLiveData<Pair<Int, String>> = MutableLiveData()

    private val connectivityManager = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    private val interactor = Interactor(app)
    private var task: Disposable? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("kifio", "onAvailable")
            setPeriod()
        }

        override fun onLost(network: Network) {
            Log.d("kifio", "onLost")
        }
    }

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
                val graph = it.second.getOrNull()
                if (graph != null) {
                    showGraph(it.first, graph)
                }
            }, {
                showError(period)
            })
    }

    fun registerNetworkCallback() {
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
    }

    fun unregisterNetworkCallback() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    fun isNetworkAvailable() : Boolean {
        val connected = connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting ?: false
        Log.d("kifio", "isNetworkAvailable: $connected")
        return connected
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

    private fun showError(period: String) {
        errorData.postValue(Pair(R.string.err_message, period))
    }

    private fun showGraph(cached: Boolean, graph: Graph) {
        graphData.postValue(Pair(cached, graph))
    }

    companion object {
        private const val PREFERENCES = "imurashov.graphcoin.preferences"
        private const val PERIOD_KEY = "imurashov.graphcoin.preferences.period"
        private const val DEFAULT_PERIOD = "1months"
    }

}