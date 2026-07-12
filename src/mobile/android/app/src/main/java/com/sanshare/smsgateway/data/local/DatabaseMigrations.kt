package com.sanshare.smsgateway.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sms_segments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sentSmsId` INTEGER NOT NULL,
                    `segmentIndex` INTEGER NOT NULL,
                    `totalSegments` INTEGER NOT NULL,
                    `sentStatus` TEXT NOT NULL,
                    `deliveryStatus` TEXT NOT NULL,
                    `sentResultCode` INTEGER,
                    `deliveryResultCode` INTEGER,
                    `updatedAt` INTEGER NOT NULL,
                    FOREIGN KEY(`sentSmsId`) REFERENCES `sent_sms`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_segments_sentSmsId` ON `sms_segments` (`sentSmsId`)")
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_segments_sentSmsId_segmentIndex` ON `sms_segments` (`sentSmsId`, `segmentIndex`)",
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sms_segments_updatedAt` ON `sms_segments` (`updatedAt`)")
        }
    }
}
