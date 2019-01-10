package imurashov.graphcoin.view

import android.animation.ObjectAnimator
import android.graphics.*
import android.view.SurfaceHolder
import imurashov.graphcoin.repository.db.entity.Graph

internal class DrawThread(private val surfaceHolder: SurfaceHolder) : Thread() {

    var running = false
    private var path: Path = Path()
    private var measure = PathMeasure()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var minX: Float = 0f
    private var minY: Float = 0f
    private var maxX: Float = 0f
    private var maxY: Float = 0f
    private var chartWidth: Float = 0f
    private var chartHeight: Float = 0f
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f
    private var pathLength: Float = 0f

    init {
        paint.style = Paint.Style.STROKE
    }

    override fun run() {
        var canvas: Canvas?
        while (running) {
            canvas = null
            try {
                canvas = surfaceHolder.lockCanvas(null)
                drawChart(canvas)
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    fun setGraph(graph: Graph) {
        val points = graph.points
        path.reset()
        path.moveTo(calcX(points.first().x), canvasHeight - calcY(points.first().y))
        points.forEach {
            path.lineTo(
                calcX(it.x),
                canvasHeight - calcY(it.y)
            )
        }
        measure.setPath(path, false)
        pathLength = measure.length
        val animator = ObjectAnimator.ofFloat(this, "phase", 1.0f, 0.0f)
        animator.duration = 1000
        animator.start()
    }

    //is called by animtor object
    fun setPhase(phase: Float) {
        paint.pathEffect = createPathEffect(pathLength, phase, 0.0f)
    }

    private fun createPathEffect(pathLength: Float, phase: Float, offset: Float): PathEffect {
        return DashPathEffect(
            floatArrayOf(pathLength, pathLength),
            Math.max(phase * pathLength, offset)
        )
    }


    private fun drawChart(canvas: Canvas?) {
        if (canvas == null) return
        drawAxis(canvas)
        drawChartPath(canvas)
    }

    private fun drawAxis(canvas: Canvas) {
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawLine(minX, canvasHeight - minY, minX, canvasHeight - maxY, paint)
        canvas.drawLine(minX, canvasHeight - minY, maxX, canvasHeight - minY, paint)
    }

    private fun drawChartPath(canvas: Canvas) {
        paint.strokeWidth = 3f
        paint.color = Color.BLUE
        canvas.drawPath(path, paint)
    }

    private fun calcX(x: Double): Float {
        return (x * chartWidth).toFloat() + minX
    }

    private fun calcY(y: Double): Float {
        return (y * chartHeight).toFloat() + minY
    }

    fun setSize(w: Int, h: Int) {
        maxX = w / VIEW_WIDTH * (VIEW_WIDTH - CHART_OFFSET)
        maxY = h / VIEW_WIDTH * (VIEW_WIDTH - CHART_OFFSET)
        minX = w / VIEW_WIDTH * CHART_OFFSET
        minY = h / VIEW_WIDTH * CHART_OFFSET
        chartWidth = w / VIEW_WIDTH * CHART_WIDTH
        chartHeight = h / VIEW_WIDTH * CHART_WIDTH
        canvasWidth = w.toFloat()
        canvasHeight = h.toFloat()
    }

    companion object {
        private const val CHART_OFFSET = 2f
        private const val CHART_WIDTH = 10f
        private const val VIEW_WIDTH = 15f
    }
}