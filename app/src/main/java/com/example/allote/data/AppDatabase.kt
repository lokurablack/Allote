package com.example.allote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Client::class,
        Job::class,
        AdministracionTrabajo::class,
        JobParametros::class,
        Product::class,
        Recipe::class,
        RecipeProduct::class,
        ImageEntity::class,
        DocumentoTrabajo::class,
        Formulacion::class,
        Lote::class,
        MovimientoContable::class,
        DocumentoMovimiento::class,
        Checklist::class,
        ChecklistItem::class
    ],
    version = 24, // Incrementado para añadir unidadDosis en recipe_products
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clientDao(): ClientDao
    abstract fun jobDao(): JobDao
    abstract fun administracionDao(): AdministracionDao
    abstract fun jobParametrosDao(): JobParametrosDao
    abstract fun productDao(): ProductDao
    abstract fun recipeDao(): RecipeDao
    abstract fun imageDao(): ImageDao
    abstract fun formulacionDao(): FormulacionDao
    abstract fun loteDao(): LoteDao
    abstract fun movimientoContableDao(): MovimientoContableDao
    abstract fun documentoMovimientoDao(): DocumentoMovimientoDao
    abstract fun checklistDao(): ChecklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN startDate INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE jobs ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agrega la nueva columna para persistir la unidad de dosis seleccionada
                db.execSQL("ALTER TABLE recipe_products ADD COLUMN unidadDosis TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN billingStatus TEXT NOT NULL DEFAULT 'No Facturado'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE jobs ADD COLUMN valuePerHectare REAL NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS job_parametros (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "jobId INTEGER NOT NULL, " +
                            "dosis REAL, " +
                            "tamanoGota REAL, " +
                            "interlineado REAL, " +
                            "velocidad REAL, " +
                            "altura REAL, " +
                            "discoUtilizado TEXT)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE job_parametros ADD COLUMN revoluciones REAL")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS products (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "nombreComercial TEXT NOT NULL, " +
                            "principioActivo TEXT NOT NULL, " +
                            "tipo TEXT NOT NULL, " +
                            "formulacion TEXT NOT NULL)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipes (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "jobId INTEGER NOT NULL, " +
                            "hectareas REAL NOT NULL, " +
                            "caudal REAL NOT NULL, " +
                            "totalCaldo REAL NOT NULL, " +
                            "fechaCreacion INTEGER NOT NULL)")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS recipe_products (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "recipeId INTEGER NOT NULL, " +
                            "productId INTEGER NOT NULL, " +
                            "dosis REAL NOT NULL, " +
                            "cantidadTotal REAL NOT NULL, " +
                            "ordenMezclado INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN numeroRegistroSenasa TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN concentracion TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN fabricante TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN bandaToxicologica TEXT")
                database.execSQL("ALTER TABLE products ADD COLUMN modoAccion TEXT")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE recipes ADD COLUMN resumen TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE clients ADD COLUMN localidad TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE clients ADD COLUMN direccion TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE administracion_trabajo ADD COLUMN documentUri TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add the new applicationType column, defaulting to PULVERIZACION for existing products
                database.execSQL("ALTER TABLE products ADD COLUMN applicationType TEXT NOT NULL DEFAULT 'PULVERIZACION'")
                // 2. Recreate the table with the new schema (making columns nullable)
                database.execSQL("""
                    CREATE TABLE products_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombreComercial TEXT NOT NULL,
                        tipo TEXT NOT NULL,
                        applicationType TEXT NOT NULL,
                        principioActivo TEXT,
                        formulacionId INTEGER,
                        numeroRegistroSenasa TEXT,
                        concentracion TEXT,
                        fabricante TEXT,
                        bandaToxicologica TEXT,
                        modoAccion TEXT
                    )
                """.trimIndent())
                // 3. Copy data from the old table to the new one
                database.execSQL("""
                    INSERT INTO products_new (id, nombreComercial, tipo, applicationType, principioActivo, formulacionId, numeroRegistroSenasa, concentracion, fabricante, bandaToxicologica, modoAccion)
                    SELECT id, nombreComercial, tipo, applicationType, principioActivo, formulacionId, numeroRegistroSenasa, concentracion, fabricante, bandaToxicologica, modoAccion FROM products
                """.trimIndent())
                // 4. Drop the old table
                database.execSQL("DROP TABLE products")
                // 5. Rename the new table to the original name
                database.execSQL("ALTER TABLE products_new RENAME TO products")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `lotes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jobId` INTEGER NOT NULL, `nombre` TEXT NOT NULL, `hectareas` REAL NOT NULL)")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE lotes ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE lotes ADD COLUMN longitude REAL")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `movimientos_contables` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clientId` INTEGER NOT NULL,
                        `jobId` INTEGER,
                        `fecha` INTEGER NOT NULL,
                        `descripcion` TEXT NOT NULL,
                        `debe` REAL NOT NULL,
                        `haber` REAL NOT NULL,
                        `tipoMovimiento` TEXT NOT NULL,
                        `detallesPago` TEXT,
                        `documentoUri` TEXT
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lotes ADD COLUMN hectareasReales REAL DEFAULT NULL")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movimientos_contables ADD COLUMN esAprobadoGeneral INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE movimientos_contables ADD COLUMN estadoAprobacion TEXT NOT NULL DEFAULT 'PENDIENTE'")
                db.execSQL("""
                    UPDATE movimientos_contables 
                    SET estadoAprobacion = CASE esAprobadoGeneral 
                                           WHEN 1 THEN 'APROBADO' 
                                           ELSE 'PENDIENTE' 
                                           END
                """)
                // Nota: La columna antigua 'esAprobadoGeneral' se mantiene por simplicidad de la migración.
                // En una futura migración podría ser eliminada si se recrea la tabla.
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `checklist_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `checklistId` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        `isDone` INTEGER NOT NULL,
                        `position` INTEGER NOT NULL,
                        FOREIGN KEY(`checklistId`) REFERENCES `checklists`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_items_checklistId ON checklist_items(checklistId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "client_job_app_database"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                // Eliminada la lista PREPOPULATE_DATA y su uso para precargar formulaciones
                            }
                        }
                    })
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_23_24
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
