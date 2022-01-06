package ir.mahdiparastesh.mobinaexplorer.room

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.*
import androidx.room.Database
import ir.mahdiparastesh.mobinaexplorer.Explorer
import java.io.File

@Database(
    entities = [Nominee::class, Candidate::class, Session::class],
    version = 1, exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun dao(): DAO

    @Dao
    interface DAO {
        @Query("SELECT * FROM Session")
        fun sessions(): List<Session>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun addSession(item: Session): Long


        @Query("SELECT * FROM Nominee")
        fun nominees(): List<Nominee>

        @Query("SELECT * FROM nominee WHERE user LIKE :user LIMIT 1")
        fun nominee(user: String): Nominee

        @Query("SELECT * FROM nominee WHERE id LIKE :id LIMIT 1")
        fun nomineeById(id: Long): Nominee

        @Insert(onConflict = OnConflictStrategy.ABORT)
        fun addNominee(item: Nominee)

        @Update
        fun updateNominee(item: Nominee)


        @Query("SELECT * FROM Candidate")
        fun candidates(): List<Candidate>

        @Insert(onConflict = OnConflictStrategy.IGNORE) // DON'T REPLACE!!!
        fun addCandidate(item: Candidate)
    }

    @SuppressLint("SdCardPath")
    class DbFile(which: Triple) : File(
        "/data/data/" + Explorer::class.java.`package`!!.name + "/databases/" + DATABASE + which.s
    ) {
        companion object {
            const val DATABASE = "primary.db"

            fun build(c: Context, mainThread: Boolean = false) = Room.databaseBuilder(
                c, ir.mahdiparastesh.mobinaexplorer.room.Database::class.java, DATABASE
            ).apply { if (mainThread) allowMainThreadQueries() }.build()
        }

        enum class Triple(val s: String) {
            MAIN(""), SHARED_MEMORY("-shm"), WRITE_AHEAD_LOG("-wal")
        }
    }
}
