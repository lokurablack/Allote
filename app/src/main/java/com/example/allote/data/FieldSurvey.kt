package com.example.allote.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "field_surveys",
    foreignKeys = [
        ForeignKey(
            entity = Job::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Lote::class,
            parentColumns = ["id"],
            childColumns = ["loteId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["jobId"]), Index(value = ["loteId"])]
)
data class FieldSurvey(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobId: Int,
    val loteId: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val title: String? = null,
    val notes: String? = null,
    val baseLayer: String = "SATELLITE",
    val referenceImageUri: String? = null,
    val boundaryGeoJson: String? = null,
    val customCategoriesJson: String? = null
)

@Entity(
    tableName = "survey_annotations",
    foreignKeys = [
        ForeignKey(
            entity = FieldSurvey::class,
            parentColumns = ["id"],
            childColumns = ["surveyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["surveyId"]), Index(value = ["category"])]
)
data class SurveyAnnotation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surveyId: Int,
    val category: String,
    val title: String? = null,
    val description: String? = null,
    val geometryType: String,
    val geometryPayload: String,
    val colorHex: String = "#FF5722",
    val icon: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    @ColumnInfo(defaultValue = "0") val isCritical: Boolean = false,
    val metadataJson: String? = null
)

@Entity(
    tableName = "annotation_media",
    foreignKeys = [
        ForeignKey(
            entity = SurveyAnnotation::class,
            parentColumns = ["id"],
            childColumns = ["annotationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["annotationId"])]
)
data class AnnotationMedia(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val annotationId: Int,
    val uri: String,
    val type: String,
    val description: String? = null,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val isUploaded: Boolean = false
)

data class AnnotationWithMedia(
    @Embedded val annotation: SurveyAnnotation,
    @Relation(
        parentColumn = "id",
        entityColumn = "annotationId"
    )
    val media: List<AnnotationMedia>
)

data class FieldSurveyWithAnnotations(
    @Embedded val survey: FieldSurvey,
    @Relation(
        entity = SurveyAnnotation::class,
        parentColumn = "id",
        entityColumn = "surveyId"
    )
    val annotations: List<AnnotationWithMedia>
)
