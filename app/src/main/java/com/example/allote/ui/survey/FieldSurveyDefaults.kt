package com.example.allote.ui.survey

import org.json.JSONArray
import org.json.JSONObject

object FieldSurveyDefaults {

    val defaultCategories: List<AnnotationCategory> = listOf(
        AnnotationCategory(
            id = "access",
            label = "Accesos",
            colorHex = "#1E88E5",
            icon = "access",
            isDefault = true
        ),
        AnnotationCategory(
            id = "replenish",
            label = "Reabastecimiento",
            colorHex = "#6A1B9A",
            icon = "refuelling",
            isDefault = true
        ),
        AnnotationCategory(
            id = "restricted",
            label = "Zonas prohibidas",
            colorHex = "#E53935",
            icon = "no_entry",
            isDefault = true
        ),
        AnnotationCategory(
            id = "obstacle",
            label = "Obstáculos",
            colorHex = "#FDD835",
            icon = "warning",
            isDefault = true
        ),
        AnnotationCategory(
            id = "neighbor",
            label = "Estado vecinos",
            colorHex = "#43A047",
            icon = "neighbor",
            isDefault = true
        ),
        AnnotationCategory(
            id = "hazard",
            label = "Peligros",
            colorHex = "#FB8C00",
            icon = "hazard",
            isDefault = true
        ),
        AnnotationCategory(
            id = "notes",
            label = "Observaciones",
            colorHex = "#00838F",
            icon = "note",
            isDefault = true
        )
    )

    fun parseCustomCategories(json: String?): List<AnnotationCategory> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        AnnotationCategory(
                            id = obj.getString("id"),
                            label = obj.getString("label"),
                            colorHex = obj.optString("color", "#00796B"),
                            icon = obj.optString("icon", "custom"),
                            isDefault = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeCustomCategories(categories: List<AnnotationCategory>): String {
        val array = JSONArray()
        categories.forEach { category ->
            array.put(
                JSONObject().apply {
                    put("id", category.id)
                    put("label", category.label)
                    put("color", category.colorHex)
                    put("icon", category.icon)
                }
            )
        }
        return array.toString()
    }
}
