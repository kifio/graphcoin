package imurashov.graphcoin.repository.db.database

import androidx.room.RoomDatabase
import androidx.room.Database
import imurashov.graphcoin.repository.db.dao.GraphDAO
import imurashov.graphcoin.repository.db.dao.PointDAO
import imurashov.graphcoin.repository.db.entity.Graph
import imurashov.graphcoin.repository.db.entity.Point

@Database(entities = [Graph::class, Point::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun graphDAO(): GraphDAO
    abstract fun pointDAO(): PointDAO
}