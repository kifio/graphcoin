package imurashov.graphcoin.repository.db.dao

import androidx.room.*
import imurashov.graphcoin.repository.db.entity.Graph

@Dao
interface GraphDAO {

    @Query("SELECT * FROM graph WHERE period = :period")
    fun query(period: String): Graph?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(graph: Graph)

    @Update
    fun update(graph: Graph)

    @Delete
    fun delete(graph: Graph)
}