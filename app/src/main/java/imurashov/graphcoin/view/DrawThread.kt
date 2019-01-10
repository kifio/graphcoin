package imurashov.graphcoin.view

import android.animation.ObjectAnimator
import android.graphics.*
import android.view.SurfaceHolder
import imurashov.graphcoin.repository.db.entity.Graph
import imurashov.graphcoin.repository.db.entity.Point
import java.util.*
import java.text.SimpleDateFormat

internal class DrawThread(private val surfaceHolder: SurfaceHolder) : Thread() {

    var running = false
    private var resetting = false
    private var points = mutableListOf<Point>()
    private var chartPath: Path = Path()
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
    private var top: Int = 0
    private var bottom: Int = 0
    private var pathLength = 0f
    private var dateStep = 3
    private var intervals: FloatArray = floatArrayOf(0f, 0f)
    private val textPaths = mutableListOf<Path>()
    private val prices = mutableListOf<String>()
    private val dates = mutableListOf<String>()
    private val textRect = Rect()

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

    fun setGraph(graph: Graph) {

        resetting = true

        // Clear lists of data for drawing
        points.clear()
        textPaths.clear()

        // Reset chart path
        chartPath.reset()

        if (graph.points.isNotEmpty()) {
            chartPath.moveTo(calcX(graph.points.first().x), canvasHeight - calcY(graph.points.first().y))

            // Fill lists of data with new values
            graph.points.forEach {
                textPaths.add(Path())
                points.add(it)
                lineTo(it)
            }

            calcPricesScale(graph)
            convertDates()
            measure.setPath(chartPath, false)
            pathLength = measure.length
            intervals[0] = measure.length
            intervals[1] = measure.length
            animate()
        }

        resetting = false
    }

    private fun lineTo(point: Point) = chartPath.lineTo(calcX(point.x), canvasHeight - calcY(point.y))

    private fun animate() {
        val animator = ObjectAnimator.ofFloat(1.0f, 0.0f)
        animator.duration = 1000
        animator.addUpdateListener {
            paint.pathEffect = DashPathEffect(
                intervals, Math.max(it.animatedValue as Float * pathLength, 0.0f)
            )
        }
        animator.start()
    }

    private fun calcPricesScale(graph: Graph) {
        val maxPrice = graph.points.maxBy { it.price } ?: return
        val minPrice = graph.points.minBy { it.price } ?: return

        var order = digitOrderNumber(maxPrice.price)
        top = ((maxPrice.price / (PRICES_SCALE_SIZE * order)) + 1) * (PRICES_SCALE_SIZE * order)

        order = digitOrderNumber(maxPrice.price)
        bottom = ((minPrice.price / (PRICES_SCALE_SIZE * order))) * (PRICES_SCALE_SIZE * order)

        val step = (top - bottom) / PRICES_SCALE_SIZE
        for (i in 0 .. PRICES_SCALE_SIZE) {
            prices.add("$${(bottom + (step * i))}")
        }
    }

    private fun convertDates() {
        dateStep = getDatesStep()
        val formatter = SimpleDateFormat(if (dateStep == 3) DATE_FORMAT_DAY else DATE_FORMAT_MONTH, Locale.getDefault())
        for (i in points.indices) {
            dates.add(formatter.format(Date(points[i].timestamp * 1000)))
        }
    }

    private fun drawChart(canvas: Canvas?) {
        if (canvas == null || resetting) return
        drawAxis(canvas)
        drawLegend(canvas)
        drawChartPath(canvas)
    }

    private fun drawAxis(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawLine(minX, canvasHeight - minY, minX, canvasHeight - maxY, paint)
        canvas.drawLine(minX, canvasHeight - minY, maxX, canvasHeight - minY, paint)
    }

    private fun drawLegend(canvas: Canvas) {
        paint.style = Paint.Style.FILL_AND_STROKE
        drawPrices(canvas)
        drawDates(canvas)
    }

    private fun drawPrices(canvas: Canvas) {
        if (prices.isEmpty()) return
        val step = (maxY - minY) / PRICES_SCALE_SIZE
        for (i in 0 .. PRICES_SCALE_SIZE) {
            val y = canvasHeight - (minY + step * i)
            drawPrice(canvas, prices[i], y)
            if (i > 0) {
                drawHorizontalLine(canvas, y)
            }
        }
    }

    private fun drawPrice(canvas: Canvas, price: String, y: Float) {
        resetTextSize(canvasWidth / VIEW_SIZE, price)
        paint.getTextBounds(price, 0, price.length, textRect)
        val textX = minX - textRect.width() - 0.25f * textRect.width()
        val textY = y + 0.25f * textRect.height()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawText(price, textX, textY, paint)
    }

    private fun drawHorizontalLine(canvas: Canvas, y: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.GRAY
        canvas.drawLine(minX, y, maxX, y, paint)
    }

    private fun drawDates(canvas: Canvas) {
        if (prices.isEmpty()) return
        for (i in points.indices) {
            // Last column on plot will be less then others, because 31 * 12 > 365
            if (i % dateStep == 0 || i == points.size - 1) {
                drawDate(canvas, points[i], dates[i], textPaths[i])
                if (i > 0) {
                    drawVerticalLine(canvas, calcX(points[i].x))
                }
            }
        }
    }

    private fun getDatesStep(): Int {
        return when {
            points.size <= 31 -> 3
            points.size <= 31 * 6 -> 31
            else -> 31
        }
    }

    private fun digitOrderNumber(x: Int): Int {
        for (i in SIZES.indices) {
            if (x <= SIZES[i]) {
                return i + 1
            }
        }
        return SIZES.size - 1
    }

    private fun drawDate(canvas: Canvas, point: Point, date: String, textPath: Path) {
        resetTextSize(canvasWidth / VIEW_SIZE, date)
        paint.getTextBounds(date, 0, date.length, textRect)
        textPath.reset()
        textPath.moveTo(calcX(point.x) - (1.25f * textRect.height()), (canvasHeight - minX) + 1.25f * textRect.width())
        textPath.lineTo(calcX(point.x) + (1.25f * textRect.height()), (canvasHeight - minY) + 0.25f * textRect.height())
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeWidth = 1f
        paint.color = Color.BLACK
        canvas.drawTextOnPath(date, textPath, 0f, 0f, paint)
    }

    private fun drawVerticalLine(canvas: Canvas, x: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.GRAY
        canvas.drawLine(x, canvasHeight - maxY, x, canvasHeight - minY, paint)
    }

    private fun drawChartPath(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.BLUE
        canvas.drawPath(chartPath, paint)
    }

    private fun calcX(x: Double): Float {
        return (x * chartWidth).toFloat() + minX
    }

    private fun calcY(y: Double): Float {
        return (y * chartHeight).toFloat() + minY
    }

    fun setSize(w: Int, h: Int) {
        maxX = w / VIEW_SIZE * (VIEW_SIZE - TOP_RIGHT_OFFSET)
        maxY = h / VIEW_SIZE * (VIEW_SIZE - TOP_RIGHT_OFFSET)
        minX = w / VIEW_SIZE * BOTTOM_LEFT_OFFSET
        minY = h / VIEW_SIZE * BOTTOM_LEFT_OFFSET
        chartWidth = w / VIEW_SIZE * CHART_SIZE
        chartHeight = h / VIEW_SIZE * CHART_SIZE
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

    companion object {
        private const val BOTTOM_LEFT_OFFSET = 2.5f
        private const val TOP_RIGHT_OFFSET = 1f
        private const val CHART_SIZE = 10f
        private const val VIEW_SIZE = 14f
        private const val PRICES_SCALE_SIZE = 10
        private const val DATE_FORMAT_DAY = "dd.MM"
        private const val DATE_FORMAT_MONTH = "MM.yyyy"
        private val SIZES = arrayOf(
            9, 99, 999, 9999, 99999, 999999, 9999999,
            99999999, 999999999, Int.MAX_VALUE
        )
    }
}