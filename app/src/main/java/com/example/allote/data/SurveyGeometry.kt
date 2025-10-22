package com.example.allote.data

import org.json.JSONArray
import org.json.JSONObject

sealed class SurveyGeometry(val type: String) {
    data class MapPoint(val latitude: Double, val longitude: Double) : SurveyGeometry(TYPE_POINT)
    data class MapPolygon(val points: List<Pair<Double, Double>>, val rotation: Double = 0.0) : SurveyGeometry(TYPE_POLYGON)
    data class MapPolyline(val points: List<Pair<Double, Double>>) : SurveyGeometry(TYPE_POLYLINE)
    data class SketchPath(val points: List<Pair<Float, Float>>) : SurveyGeometry(TYPE_SKETCH_PATH)
    data class SketchShape(val shape: String, val points: List<Pair<Float, Float>>) : SurveyGeometry(TYPE_SKETCH_SHAPE)

    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        when (this) {
            is MapPoint -> {
                json.put("lat", latitude)
                json.put("lng", longitude)
            }
            is MapPolygon -> {
                json.put("points", JSONArray().apply {
                    points.forEach { (lat, lng) ->
                        put(JSONObject().apply {
                            put("lat", lat)
                            put("lng", lng)
                        })
                    }
                })
                if (rotation != 0.0) {
                    json.put("rotation", rotation)
                }
            }
            is MapPolyline -> {
                json.put("points", JSONArray().apply {
                    points.forEach { (lat, lng) ->
                        put(JSONObject().apply {
                            put("lat", lat)
                            put("lng", lng)
                        })
                    }
                })
            }
            is SketchPath -> {
                json.put("points", JSONArray().apply {
                    points.forEach { (x, y) ->
                        put(JSONObject().apply {
                            put("x", x)
                            put("y", y)
                        })
                    }
                })
            }
            is SketchShape -> {
                json.put("shape", shape)
                json.put("points", JSONArray().apply {
                    points.forEach { (x, y) ->
                        put(JSONObject().apply {
                            put("x", x)
                            put("y", y)
                        })
                    }
                })
            }
        }
        return json.toString()
    }

    companion object {
        const val TYPE_POINT = "POINT"
        const val TYPE_POLYGON = "POLYGON"
        const val TYPE_POLYLINE = "POLYLINE"
        const val TYPE_SKETCH_PATH = "SKETCH_PATH"
        const val TYPE_SKETCH_SHAPE = "SKETCH_SHAPE"

        fun fromJson(payload: String): SurveyGeometry? {
            return try {
                val json = JSONObject(payload)
                when (val type = json.optString("type")) {
                    TYPE_POINT -> MapPoint(
                        latitude = json.getDouble("lat"),
                        longitude = json.getDouble("lng")
                    )
                    TYPE_POLYGON -> {
                        val pointsJson = json.getJSONArray("points")
                        val points = mutableListOf<Pair<Double, Double>>()
                        for (i in 0 until pointsJson.length()) {
                            val point = pointsJson.getJSONObject(i)
                            points.add(point.getDouble("lat") to point.getDouble("lng"))
                        }
                        val rotation = json.optDouble("rotation", 0.0)
                        MapPolygon(points, rotation)
                    }
                    TYPE_POLYLINE -> {
                        val pointsJson = json.getJSONArray("points")
                        val points = mutableListOf<Pair<Double, Double>>()
                        for (i in 0 until pointsJson.length()) {
                            val point = pointsJson.getJSONObject(i)
                            points.add(point.getDouble("lat") to point.getDouble("lng"))
                        }
                        MapPolyline(points)
                    }
                    TYPE_SKETCH_PATH -> {
                        val pointsJson = json.getJSONArray("points")
                        val points = mutableListOf<Pair<Float, Float>>()
                        for (i in 0 until pointsJson.length()) {
                            val point = pointsJson.getJSONObject(i)
                            points.add(point.getDouble("x").toFloat() to point.getDouble("y").toFloat())
                        }
                        SketchPath(points)
                    }
                    TYPE_SKETCH_SHAPE -> {
                        val shape = json.optString("shape", "custom")
                        val pointsJson = json.getJSONArray("points")
                        val points = mutableListOf<Pair<Float, Float>>()
                        for (i in 0 until pointsJson.length()) {
                            val point = pointsJson.getJSONObject(i)
                            points.add(point.getDouble("x").toFloat() to point.getDouble("y").toFloat())
                        }
                        SketchShape(shape, points)
                    }
                    else -> {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
