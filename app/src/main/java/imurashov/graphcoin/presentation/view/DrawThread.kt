package imurashov.graphcoin.presentation.view

import android.animation.ObjectAnimator
import android.graphics.*
import android.view.SurfaceHolder
import imurashov.graphcoin.repository.db.entity.Graph
import imurashov.graphcoin.repository.db.entity.Point
import java.util.*
import java.text.SimpleDateFormat

internal class DrawThread(private val surfaceHolder: SurfaceHolder) : Thread() {

    var running = false
    private val chartPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var minX: Float = 0f
    private var minY: Float = 0f
    private var maxX: Float = 0f
    private var maxY: Float = 0f
    private var chartWidth: Float = 0f
    private var chartHeight: Float = 0f
    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f
    private var chart: Chart? = null
    private val textRect = Rect()

    init {
        chartPaint.style = Paint.Style.STROKE
        chartPaint.strokeWidth = 2f
        chartPaint.color = Color.BLUE

        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
    }

    override fun run() {
        var canvas: Canvas?
        while (running) {
            canvas = null
            try {
                canvas = surfaceHolder.lockCanvas(null)
                canvas?.drawColor(Color.WHITE)
                drawChart(canvas)
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    fun setGraph(graph: Graph?) {
        if (graph != null) {
            this.chart = Chart(graph)
            val measure = PathMeasure()
            measure.setPath(this.chart?.chartPath, false)
            animate(measure)
        } else {
            this.chart = null
            return
        }
    }

    private fun animate(measure: PathMeasure) {
        val pathLength = measure.length
        val intervals: FloatArray = floatArrayOf(measure.length, measure.length)
        val animator = ObjectAnimator.ofFloat(1.0f, 0.0f)
        animator.duration = 1000
        animator.addUpdateListener {
            chartPaint.pathEffect = DashPathEffect(
                intervals, Math.max(it.animatedValue as Float * pathLength, 0.0f)
            )
        }
        animator.start()
    }

    private fun drawChart(canvas: Canvas?) {
        if (canvas == null) return
        val chart = this.chart ?: return
        drawAxis(canvas)
        drawLegend(canvas, chart)
        drawChartPath(canvas, chart)
    }

    private fun drawAxis(canvas: Canvas) {
        canvas.drawLine(minX, canvasHeight - minY, minX, canvasHeight - maxY, paint)
        canvas.drawLine(minX, canvasHeight - minY, maxX, canvasHeight - minY, paint)
    }

    private fun drawLegend(canvas: Canvas, chart: Chart) {
        drawPrices(canvas, chart)
        drawDates(canvas, chart)
    }

    private fun drawPrices(canvas: Canvas, chart: Chart) {
        if (chart.prices.isEmpty()) return
        for (i in chart.prices.indices) {
            drawPrice(canvas, chart.prices[i].price, chart.prices[i].y)
            if (i > 0) {
                drawHorizontalLine(canvas, chart.prices[i].y)
            }
        }
    }

    private fun drawPrice(canvas: Canvas, price: String, y: Float) {
        resetTextSize(1.5f * (canvasWidth / VIEW_SIZE), price)
        paint.getTextBounds(price, 0, price.length, textRect)
        val textX = minX - textRect.width() - 0.25f * textRect.width()
        val textY = y + 0.25f * textRect.height()
        paint.color = Color.BLACK
        canvas.drawText(price, textX, textY, paint)
    }

    private fun drawHorizontalLine(canvas: Canvas, y: Float) {
        paint.color = Color.GRAY
        canvas.drawLine(minX, y, maxX, y, paint)
    }

    private fun drawDates(canvas: Canvas, chart: Chart) {
        if (chart.dates.isEmpty()) return
        for (i in chart.dates.indices) {
            // Last column on plot will be less then others, because 31 * 12 > 365
            if (i > 0) drawVerticalLine(canvas, chart.dates[i].x)
            drawDate(canvas, chart.dates[i].x, chart.dates[i].date, chart.dates[i].path)
        }
    }

    private fun drawDate(canvas: Canvas, x: Float, date: String, textPath: Path) {
        resetTextSize(1.5f * (canvasWidth / VIEW_SIZE), date)
        paint.getTextBounds(date, 0, date.length, textRect)
        textPath.reset()
        textPath.moveTo(x - (1.25f * textRect.height()), (canvasHeight - minY) + 1.25f * textRect.width())
        textPath.lineTo(x + (1.25f * textRect.height()), (canvasHeight - minY) + 0.25f * textRect.height())
        paint.color = Color.BLACK
        canvas.drawTextOnPath(date, textPath, 0f, 0f, paint)
    }

    private fun drawVerticalLine(canvas: Canvas, x: Float) {
        paint.color = Color.GRAY
        canvas.drawLine(x, canvasHeight - maxY, x, canvasHeight - minY, paint)
    }

    private fun drawChartPath(canvas: Canvas, chart: Chart) {
        canvas.drawPath(chart.chartPath, chartPaint)
    }

    fun setSize(w: Int, h: Int) {
        maxX = w /
                VIEW_SIZE * (VIEW_SIZE - TOP_RIGHT_OFFSET)
        maxY = h /
                VIEW_SIZE * (VIEW_SIZE - TOP_RIGHT_OFFSET)
        minX = w / VIEW_SIZE *
                BOTTOM_LEFT_OFFSET
        minY = h / VIEW_SIZE *
                BOTTOM_LEFT_OFFSET
        chartWidth = w / VIEW_SIZE *
                CHART_SIZE
        chartHeight = h / VIEW_SIZE *
                CHART_SIZE
        canvasWidth = w.toFloat()
        canvasHeight = h.toFloat()
    }

    private fun resetTextSize(desiredWidth: Float, text: String) {
        val testTextSize = 36f

        // Get the bounds of the text, using our testTextSize.
        paint.textSize = testTextSize
        paint.getTextBounds(text, 0, text.length, textRect)

        // Calculate the desired size as a proportion of our testTextSize.
        val desiredTextSize = testTextSize * desiredWidth / textRect.width()

        // Set the paint for that size.
        paint.textSize = desiredTextSize
    }

    private data class YDate(val x: Float, val date: String, val path: Path)
    private data class XPrice(val y: Float, val price: String)

    private inner class Chart(graph: Graph) {

        var chartPath: Path = Path()
        val prices = mutableListOf<XPrice>()
        val dates = mutableListOf<YDate>()
        var yOffset = 0f

        init {
            if (graph.points.isNotEmpty()) {
                chartPath.moveTo(calcX(graph.points.first().x), canvasHeight - calcY(graph.points.first().y))
                graph.points.forEach { lineTo(it) }
                calcPricesScale(graph)
                convertDates(graph.points)
            }
        }

        private fun calcPricesScale(graph: Graph) {

            val maxPrice = graph.points.maxBy { it.price } ?: return
            val minPrice = graph.points.minBy { it.price } ?: return

            val priceStep = (maxPrice.price - minPrice.price) / PRICES_SCALE_SIZE
            val a = calcY(maxPrice.y)
            val b = calcY(minPrice.y)
            val yStep = (a - b) / PRICES_SCALE_SIZE

            for (i in 0..PRICES_SCALE_SIZE) {
                val y = ((canvasHeight - minY) - yStep * i)
                prices.add(XPrice(y, "$${(minPrice.price + (priceStep * i))}"))
            }
        }

        private fun convertDates(points: List<Point>) {
            val dateStep = getDatesStep(points.size)
            val formatter =
                SimpleDateFormat(if (dateStep == 3) DATE_FORMAT_DAY else DATE_FORMAT_MONTH, Locale.getDefault())
            for (i in 0 until points.size step dateStep) {
                val date = formatter.format(Date(points[i].timestamp * 1000))
                val x = calcX(points[i].x)
                dates.add(YDate(x, date, Path()))
            }
        }

        private fun lineTo(point: Point) = chartPath.lineTo(calcX(point.x), canvasHeight - calcY(point.y))

        private fun getDatesStep(size: Int): Int {
            return when {
                size <= 31 -> 3
                size <= 31 * 6 -> 31
                else -> 31
            }
        }

        private fun calcX(x: Double): Float {
            return (x * chartWidth).toFloat() + minX
        }

        private fun calcY(y: Double): Float {
            return ((yOffset + y) * chartHeight).toFloat() + minY
        }
    }

    companion object {
        private const val BOTTOM_LEFT_OFFSET = 2.5f
        private const val TOP_RIGHT_OFFSET = 1f
        private const val CHART_SIZE = 10f
        private const val VIEW_SIZE = 14f
        private const val PRICES_SCALE_SIZE = 10
        private const val DATE_FORMAT_DAY = "dd.MM"
        private const val DATE_FORMAT_MONTH = "MM.yyyy"
    }
}