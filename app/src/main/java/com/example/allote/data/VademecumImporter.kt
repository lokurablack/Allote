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
        val todasFormulaciones = formulacionDao.getAllFormulaciones().toMutableList()
        fun normalizar(str: String): String = str.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("/", " ").replace("-", " ").replace("_", " ").replace("  ", " ").trim()
        fun normalizarRegistro(str: String?): String = (str ?: "")
            .lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
            .replace("[^a-z0-9]".toRegex(), "")
            .trim()

        // Registros que deben importarse sin formulación (forzar NULL)
        val registrosSinFormulacion = setOf(
            "30076","30080","30085","30240","30304","30318","30358","30406","30423","30425","30667","30850","31020","31022","31032","31070","31071","31148","31223","31230","31234","31333","31731","32045","32169","32229","32468","32566","32618","32655","32673","32736","32767","32815","32822","32847","32876","32903","32974","33063","33064","33097","33136","33343","33350","33353","33385","33421","33483","33488","33492","33512","33536","33551","33575","33596","33597","33603","33652","33708","33729","33756","33775","33784","33856","33930","33932","33967","36949","36950","37236","37588","38394","38704","40287","40310","40323","40395",
            "LJ 00068","LJ 00069","LJ 00082","LJ 00088","LJ 00096","LJ 00098","LJ 00113","LJ 00134","LJ 00139","LJ 00150","LJ 00160","LJ 00167","LJ 00168","LJ 00172","LJ 00178","LJ 00180","LJ 00191","LJ 00192","LJ 00198","LJ 00203","LJ 00216","LJ 00222","LJ 00227","LJ 00263","LJ 00271","LJ 00273","LJ 00274","LJ 00277","LJ 00283","LJ 00284","LJ 00286","LJ 00289","LJ 00290","LJ 00295","LJ 00298","LJ 00304","LJ 00316","LJ 00317","LJ 00323","LJ 00334","LJ 00335","LJ 00337","LJ 00345","LJ 00348","LJ 00357","LJ 00358","LJ 00369","LJ 00389","LJ 00396","LJ 00399","LJ 00405","LJ 00408","LJ 00411","LJ 00412","LJ 00413","LJ 00417","LJ 00418","LJ 00422","LJ 00427","LJ 00434","LJ 00435","LJ 00437","LJ 00442","LJ 00444","LJ 00445","LJ 00448","LJ 00449","LJ 00452","LJ 00453","LJ 00454","LJ 00455","LJ 00460","LJ 00462","LJ 00463","LJ 00465","LJ 00466","LJ 00467","LJ 00468","LJ 00469","LJ 00470","LJ 00472","LJ 00473","LJ 00474","LJ 00475","LJ 00476","LJ 00477","LJ 00478","LJ 00479","LJ 00481","LJ 00482","LJ 00484","LJ 00486","LJ 00487","LJ 00494","LJ 00495","LJ 00497","LJ 00498","LJ 00511",
            "SE-003","SE-014","SE-023","SE-031","SE-080","SE-081","SE-102","SE-111","SE-134","SE-141","SE-162","SE-172","SE-220","SE-222","SE-223","SE-238","SE-271","SE-279","SE-286","SE-287","SE-292","SE-298","SE-310","SE-313","SE-315","SE-331","SE-335","SE-336","SE-348","SE-354","SE-361","SE-365","SE-371","SE-372","SE-374","SE-375","SE-376","SE-378","SE-379","SE-384","SE-388","SE-389","SE-434"
        ).map { normalizarRegistro(it) }.toSet()
        // Log de formulaciones en BD
        println("Formulaciones en BD:")
        todasFormulaciones.forEach { println(normalizar(it.nombre)) }
        // Log de formulaciones en CSV (primeras 50)
        println("Formulaciones en CSV (primeras 50):")
        // Probar con codificación UTF-8 y mostrar la primera línea para depuración
        val inputStream = context.assets.open("Vademecum_Senasa.csv")
        val readerUtf8 = inputStream.bufferedReader(charset = Charsets.UTF_8)
        val primeraLinea = readerUtf8.readLine()
        println("[IMPORTADOR] Primera línea CSV (UTF-8): $primeraLinea")
        readerUtf8.close()
        // Ahora leer el archivo normalmente con UTF-8
        context.assets.open("Vademecum_Senasa.csv").bufferedReader(charset = Charsets.UTF_8).use { readerLog ->
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
        // Abrir el archivo CSV con codificación ISO-8859-1 para evitar problemas de caracteres
        val reader = context.assets.open("Vademecum_Senasa.csv").bufferedReader(charset = Charsets.UTF_8)
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
                    var formulacionEntity = if (formulacionNormalizada.isNotBlank()) {
                        // Evitar contains("") que matchea todo; solo si hay contenido real
                        todasFormulaciones.find {
                            val n = normalizar(it.nombre)
                            n == formulacionNormalizada || (formulacionNormalizada.length >= 2 && n.contains(formulacionNormalizada))
                        }
                    } else null
                    // Si no existe, crearla y agregarla a la BD y a la lista local
                    if (formulacionEntity == null && formulacionCsv.isNotBlank()) {
                        val nuevaFormulacion = Formulacion(
                            nombre = formulacionCsv,
                            ordenMezcla = todasFormulaciones.size + 1,
                            tipoUnidad = "LIQUIDO"
                        )
                        formulacionDao.insertAll(listOf(nuevaFormulacion))
                        // Recuperar la formulación insertada (puede que el id se asigne automáticamente)
                        val formulacionInsertada = formulacionDao.getByName(formulacionCsv)
                        if (formulacionInsertada != null) {
                            formulacionEntity = formulacionInsertada
                            todasFormulaciones.add(formulacionEntity)
                        }
                    }
                    // Forzar NULL si el registro está en la lista de sin formulación
                    val regNorm = normalizarRegistro(numeroRegistro)
                    val forcedNull = registrosSinFormulacion.contains(regNorm)
                    val formulacionId = if (forcedNull) null else formulacionEntity?.id
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
        println("Productos importados desde Vademecum: $importados")
        println("Productos sin formulación asociada: $sinFormulacion")
        println("Tipos de applicationType importados: ${tiposAplicacionDebug}")
        productos.forEach { productDao.insert(it) }
    }
}
