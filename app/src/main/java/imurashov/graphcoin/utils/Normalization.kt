package imurashov.graphcoin.utils

import imurashov.graphcoin.repository.network.Response
import imurashov.graphcoin.repository.db.entity.Point
import java.util.*
import kotlin.math.roundToInt

object Normalization {

    // Normalize values of x and y.
    fun normalizeValues(period: String, values: List<Response.Value>): List<(Point)> {
        val points = ArrayList<Point>(values.size)
        if (!values.isEmpty()) {
            val maxPrice = values.maxBy { it.y }?.y ?: return points
            val minPrice = values.minBy { it.y }?.y ?: return points
            val firstDate = values.first().x
            val lastDate = values.last().x

            var x: Double
            var y: Double

            for (value in values) {
                x = (value.x.toDouble() - firstDate) / (lastDate - firstDate)
                y = (value.y - minPrice) / (maxPrice - minPrice)
                points.add(Point(period, x, y,
                    value.x,
                    value.y.roundToInt()))
            }
        }

        return points
    }
}