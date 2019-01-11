package imurashov.graphcoin.presentation.view

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import imurashov.graphcoin.presentation.GraphCoinViewModel
import imurashov.graphcoin.R
import kotlinx.android.synthetic.main.fragment_main.*

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
        return false
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawThread.setSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawThread = DrawThread(surfaceView.holder)
        drawThread.running = true
        drawThread.start()

        viewModel.graphData.observe(this, Observer {
            drawThread.setGraph(it)
        })

        viewModel.setPeriod()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
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
    }
}