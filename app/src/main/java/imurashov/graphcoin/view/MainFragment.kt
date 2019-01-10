package imurashov.graphcoin.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import imurashov.graphcoin.GraphCoinViewModel
import imurashov.graphcoin.R
import imurashov.graphcoin.repository.db.entity.Graph
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment: Fragment(), SurfaceHolder.Callback {

    private lateinit var drawThread: DrawThread

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        surfaceView.holder.addCallback(this)
    }

    override fun surfaceChanged(h: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawThread.setSize(width, height)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawThread = DrawThread(surfaceView.holder)
        drawThread.running = true
        drawThread.start()

        val viewModel = ViewModelProviders.of(this).get(GraphCoinViewModel::class.java)
        viewModel.graphData.observe(this, Observer {
            drawThread.setGraph(it)
        })

        viewModel.setTimespan("365days")
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
}