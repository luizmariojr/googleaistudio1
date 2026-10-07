package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DeliveryConfirmationEntity::class,
        RomaneioEntity::class,
        NotaFiscalEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deliveryConfirmationDao(): DeliveryConfirmationDao
    abstract fun romaneioDao(): RomaneioDao
    abstract fun notaFiscalDao(): NotaFiscalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS romaneios_cache")
                db.execSQL("DROP TABLE IF EXISTS notas_fiscais_cache")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `romaneios_cache` (
                        `id` TEXT NOT NULL, 
                        `numeroRomaneio` TEXT NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `clientePrincipal` TEXT NOT NULL, 
                        `cidade` TEXT NOT NULL, 
                        `uf` TEXT NOT NULL, 
                        `quantidadeNfs` INTEGER NOT NULL, 
                        `quantidadeVolumes` INTEGER NOT NULL, 
                        `pesoTotalKg` REAL NOT NULL, 
                        `motoristaNome` TEXT NOT NULL, 
                        `prazoEntrega` TEXT NOT NULL, 
                        `permiteCanhotoUnico` INTEGER NOT NULL, 
                        `entregueEm` INTEGER, 
                        `entregueEmFormatado` TEXT, 
                        `retiradaConfirmadaEm` INTEGER, 
                        `observacaoGeral` TEXT, 
                        `createdAt` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notas_fiscais_cache` (
                        `id` TEXT NOT NULL, 
                        `romaneioId` TEXT NOT NULL, 
                        `numeroNf` TEXT NOT NULL, 
                        `destinatario` TEXT NOT NULL, 
                        `cidade` TEXT NOT NULL, 
                        `uf` TEXT NOT NULL, 
                        `volumes` INTEGER NOT NULL, 
                        `pesoKg` REAL NOT NULL, 
                        `valor` REAL NOT NULL, 
                        `status` TEXT NOT NULL, 
                        `canhotoFotoUri` TEXT, 
                        `entregueEm` INTEGER, 
                        `entregueEmFormatado` TEXT, 
                        `avariaRegistrada` INTEGER NOT NULL, 
                        `redespachoRegistrado` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `romaneios_cache` ADD COLUMN `createdAt` TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bicicletao_logistica_v3.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
