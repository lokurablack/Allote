package com.example.allote

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// 1. Anotamos la clase con @HiltAndroidApp
@HiltAndroidApp
class ClientJobApplication : Application() {
    // 2. Normalmente, puedes dejar el cuerpo de la clase vacío.
    // Hilt se encarga de generar todo el código necesario.
}