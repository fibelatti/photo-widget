package com.fibelatti.photowidget.widget.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class PhotoWidgetDatabaseMigrationTests {

    @get:Rule
    val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        databaseClass = PhotoWidgetDatabase::class.java,
    )

    private val latestVersion: Int by lazy {
        val database: PhotoWidgetDatabase = Room.inMemoryDatabaseBuilder(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            klass = PhotoWidgetDatabase::class.java,
        ).build()

        try {
            database.openHelper.readableDatabase.version
        } finally {
            database.close()
        }
    }

    @Test
    fun everyVersionMigratesToTheLatestSchema() {
        for (version: Int in FIRST_VERSION until latestVersion) {
            val name = "$TEST_DATABASE-$version"

            migrationTestHelper.createDatabase(name = name, version = version).close()
            migrationTestHelper.runMigrationsAndValidate(
                name = name,
                version = latestVersion,
                validateDroppedTables = true,
            ).close()
        }
    }

    @Test
    fun photoOrderIsKeptWhenMigratingFromTheFirstVersion() {
        migrationTestHelper.createDatabase(name = TEST_DATABASE, version = FIRST_VERSION).use { database ->
            database.execSQL(
                "INSERT INTO photo_widget_order (widgetId, photoIndex, photoId) " +
                    "VALUES (1, 0, 'first.jpg'), (1, 1, 'second.jpg'), (2, 0, 'other.jpg')",
            )
        }

        val expected: List<Triple<Int, Int, String>> = listOf(
            Triple(1, 0, "first.jpg"),
            Triple(1, 1, "second.jpg"),
            Triple(2, 0, "other.jpg"),
        )
        val actual: List<Triple<Int, Int, String>> = migrationTestHelper
            .runMigrationsAndValidate(name = TEST_DATABASE, version = latestVersion, validateDroppedTables = true)
            .use { database ->
                database.query(
                    "SELECT widgetId, photoIndex, photoId FROM photo_widget_order ORDER BY widgetId, photoIndex",
                )
                    .use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) {
                                add(Triple(cursor.getInt(0), cursor.getInt(1), cursor.getString(2)))
                            }
                        }
                    }
            }

        assertThat(actual).isEqualTo(expected)
    }

    private companion object {

        const val TEST_DATABASE = "migration-test"
        const val FIRST_VERSION = 1
    }
}
