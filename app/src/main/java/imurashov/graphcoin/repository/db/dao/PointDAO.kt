package imurashov.graphcoin.repository.db.dao

import androidx.room.*
import imurashov.graphcoin.repository.db.entity.Graph
import imurashov.graphcoin.repository.db.entity.Point
import io.reactivex.Single

@Dao
interface PointDAO {

    @Query("SELECT * FROM point WHERE period = :period")
    fun getByTimespan(period: String): List<Point>

    @Insert
    fun insert(employee: List<Point>)
}