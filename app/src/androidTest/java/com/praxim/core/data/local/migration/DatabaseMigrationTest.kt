package com.praxim.core.data.local.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.praxim.core.data.local.database.PraximDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PraximDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO scan_audit_log (id, timestamp, raw_payload, entity_type) VALUES (1, 1633046400, 'payload', 'UPI')")
            close()
        }

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, PraximMigrations.MIGRATION_1_2)

        val cursor = db.query("SELECT * FROM scan_audit_log")
        assert(cursor.moveToFirst())
        val columnIdx = cursor.getColumnIndex("merchant_category_code")
        assert(columnIdx != -1)
        assert(cursor.getString(columnIdx) == null) // new column should be null for existing rows
    }
}
