package imurashov.graphcoin.repository.db.entity

import androidx.room.*
import imurashov.graphcoin.repository.network.Response

@Entity(indices = [Index(value = ["period"], unique = true)])
data class Graph(
    val period: String,
    val status: String,
    val name: String?,
    val unit: String?,
    val description: String
) {
    constructor(period: String, response: Response) : this(
        period,
        response.status,
        response.name,
        response.unit,
        response.description
    )

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    @Ignore
    val points = mutableListOf<Point>()
}