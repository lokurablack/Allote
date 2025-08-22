package com.example.allote.data

class VademecumImporter(private val context: android.content.Context, private val productDao: ProductDao, private val formulacionDao: FormulacionDao) {

    data class VademecumEntry(
        val numeroRegistro: String,    // -> numeroRegistroSenasa
        val marca: String,             // -> nombreComercial (CORRECCIÓN)
        val activos: String,           // -> principioActivo
        val bandaTox: String,          // -> bandaToxicologica
        val aptitudes: String,         // -> tipo
        val formulacion: String,       // -> formulacionId (buscar en BD)
        val tipoAplicacion: String     // -> applicationType
    )

    // ...campos si son necesarios...

    suspend fun importVademecumIfNeeded() {
        if (productDao.getAll().isNotEmpty()) return

        val productos = mutableListOf<Product>()
        val tiposAplicacionDebug = mutableSetOf<String>()
        val todasFormulaciones = formulacionDao.getAllFormulaciones()
        fun normalizar(str: String): String = str.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("/", " ").replace("-", " ").replace("_", " ").replace("  ", " ").trim()
        // Log de formulaciones en BD
        println("Formulaciones en BD:")
        todasFormulaciones.forEach { println(normalizar(it.nombre)) }
        // Log de formulaciones en CSV (primeras 50)
        println("Formulaciones en CSV (primeras 50):")
        context.assets.open("Vademecum_Senasa.csv").bufferedReader().use { readerLog ->
            readerLog.useLines { lines ->
                lines.drop(1).take(50).forEach { line ->
                    val cols = line.split(';')
                    if (cols.size >= 6) {
                        val formulacionCsv = cols[5].trim()
                        println(normalizar(formulacionCsv))
                    }
                }
            }
        }
        var sinFormulacion = 0
        var importados = 0
        context.assets.open("Vademecum_Senasa.csv").bufferedReader().use { reader ->
            reader.useLines { lines ->
                lines.drop(1).forEach { line ->
                    val cols = line.split(';')
                    if (cols.size >= 7) {
                        val numeroRegistro = cols[0].trim()
                        val marca = cols[1].trim()
                        val activos = cols[2].trim()
                        val bandaTox = cols[3].trim()
                        val aptitudes = cols[4].trim()
                        val formulacionCsv = cols[5].trim()
                        var tipoAplicacion = cols[6].trim().uppercase()
                        tipoAplicacion = tipoAplicacion.replace("Á", "A").replace("Í", "I").replace("É", "E").replace("Ó", "O").replace("Ú", "U")
                        tipoAplicacion = when {
                            tipoAplicacion == "PULVERIZACION" -> "PULVERIZACION"
                            tipoAplicacion == "ESPARCIDO" -> "ESPARCIDO"
                            tipoAplicacion.isBlank() -> "AMBOS"
                            else -> "AMBOS"
                        }
                        tiposAplicacionDebug.add(tipoAplicacion)
                        // Buscar la formulación por similitud
                        val formulacionNormalizada = normalizar(formulacionCsv)
                        val formulacionEntity = todasFormulaciones.find {
                            normalizar(it.nombre) == formulacionNormalizada || normalizar(it.nombre).contains(formulacionNormalizada)
                        }
                        val formulacionId = formulacionEntity?.id
                        if (formulacionId == null && formulacionCsv.isNotBlank()) sinFormulacion++
                        val product = Product(
                            nombreComercial = marca,
                            principioActivo = if (activos.isNotBlank()) activos else null,
                            tipo = aptitudes,
                            formulacionId = formulacionId,
                            numeroRegistroSenasa = if (numeroRegistro.isNotBlank()) numeroRegistro else null,
                            bandaToxicologica = if (bandaTox.isNotBlank()) bandaTox else null,
                            applicationType = tipoAplicacion,
                            fabricante = marca,
                            isFromVademecum = true
                        )
                        productos.add(product)
                        importados++
                    }
                }
            }
        }
        println("Productos importados desde Vademecum: $importados")
        println("Productos sin formulación asociada: $sinFormulacion")
        println("Tipos de applicationType importados: ${tiposAplicacionDebug}")
        productos.forEach { productDao.insert(it) }
    }
}
