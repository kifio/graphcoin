package imurashov.graphcoin.presentation.view

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import imurashov.graphcoin.presentation.GraphCoinViewModel
import imurashov.graphcoin.R
import imurashov.graphcoin.repository.db.entity.Graph
import kotlinx.android.synthetic.main.fragment_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainFragment: Fragment(), SurfaceHolder.Callback {

    private lateinit var drawThread: DrawThread
    private lateinit var viewModel: GraphCoinViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProviders.of(this).get(GraphCoinViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        surfaceView.holder.addCallback(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.month -> viewModel.setPeriod(MONTH)
            R.id.half_year -> viewModel.setPeriod(HALF_YEAR)
            R.id.year -> viewModel.setPeriod(YEAR)
        }
        priceProgress.visibility = View.VISIBLE
        surfaceProgress.visibility = View.VISIBLE
        return false
    }

    private fun setPrice(graph: Graph) {
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val last = graph.points.maxBy { it.timestamp } ?: return
        priceProgress.visibility = View.GONE
        price.visibility = View.VISIBLE
        price.text = String.format(Locale.getDefault(),
            getString(R.string.last_known_price), formatter.format(last.timestamp  * 1000), "$${last.price}")
    }

    private fun setTitle(period: String) {
        when (period) {
            MONTH -> activity?.setTitle(R.string.month)
            HALF_YEAR -> activity?.setTitle(R.string.half_year)
            YEAR -> activity?.setTitle(R.string.year)
        }
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d("kifio", "surfaceChanged: w: $width; h: $height")
        drawThread.setSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("kifio", "surfaceCreated")

        drawThread = DrawThread(surfaceView.holder)
        drawThread.running = true
        drawThread.start()

        viewModel.graphData.observe(this, Observer {
            setTitle(it.second.period)
            surfaceProgress.visibility = View.GONE
            drawThread.setGraph(it.second)
            setPrice(it.second)
                if (!it.first) {
                description.visibility = View.GONE
            } else {
                description.visibility = View.VISIBLE
            }
        })

        viewModel.errorData.observe(this, Observer {
            drawThread.setGraph(null)
            price.visibility = View.GONE
            priceProgress.visibility = View.GONE
            surfaceProgress.visibility = View.GONE
            description.visibility = View.GONE
            setTitle(it.second)
            Snackbar.make(coordinator, it.first, Snackbar.LENGTH_SHORT).show()
        })

        viewModel.registerNetworkCallback()
        if (!viewModel.isNetworkAvailable()) {
            viewModel.setPeriod()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("kifio", "surfaceDestroyed")
        viewModel.unregisterNetworkCallback()

        var retry = true
        drawThread.running = false
        while (retry) {
            try {
                drawThread.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val MONTH = "1months"
        private const val HALF_YEAR = "6months"
        private const val YEAR = "1year"
        private const val DATE_FORMAT = "dd.MM.yyyy"
    }
}