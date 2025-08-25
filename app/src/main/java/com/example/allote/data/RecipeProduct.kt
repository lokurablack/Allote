package com.example.allote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipe_products")
data class RecipeProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipeId: Int,
    val productId: Int,
    val dosis: Double,
    val cantidadTotal: Double,
    val ordenMezclado: Int,
    val unidadDosis: String // "L/ha", "Cc/ha", "Gr/ha", "Kg/ha"
)
