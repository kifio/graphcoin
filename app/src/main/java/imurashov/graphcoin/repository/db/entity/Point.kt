package imurashov.graphcoin.repository.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(foreignKeys = [ForeignKey(
    entity = Graph::class,
    parentColumns = ["period"],
    childColumns = ["period"],
    onDelete = CASCADE
)])
class Point(val period: String, val x: Double, val y: Double, val timestamp: Long, val price: Int) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}