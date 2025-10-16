package com.example.allote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FieldSurveyRepository @Inject constructor(
    private val fieldSurveyDao: FieldSurveyDao,
    private val annotationDao: SurveyAnnotationDao,
    private val mediaDao: AnnotationMediaDao,
    private val jobDao: JobDao,
    private val loteDao: LoteDao
) {

    suspend fun ensureSurvey(jobId: Int, loteId: Int?): FieldSurvey {
        val existing = when (loteId) {
            null -> fieldSurveyDao.observeLatestSurveyForJob(jobId).firstOrNull()?.survey
            else -> fieldSurveyDao.observeLatestSurveyForLote(jobId, loteId).firstOrNull()?.survey
        }
        if (existing != null) {
            return existing
        }
        val now = System.currentTimeMillis()
        val survey = FieldSurvey(
            jobId = jobId,
            loteId = loteId,
            createdAt = now,
            updatedAt = now
        )
        val id = fieldSurveyDao.insert(survey).toInt()
        return survey.copy(id = id)
    }

    fun observeSurveyWithAnnotations(surveyId: Int): Flow<FieldSurveyWithAnnotations?> {
        return fieldSurveyDao.observeSurveyWithAnnotations(surveyId)
    }

    suspend fun getSurveyById(surveyId: Int): FieldSurvey? = fieldSurveyDao.getSurveyById(surveyId)

    suspend fun getSurveySnapshot(surveyId: Int): FieldSurveyWithAnnotations? =
        fieldSurveyDao.getSurveyWithAnnotations(surveyId)

    suspend fun updateSurvey(survey: FieldSurvey) {
        fieldSurveyDao.update(survey.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateSurveyBaseLayer(surveyId: Int, baseLayer: String) {
        val survey = fieldSurveyDao.getSurveyById(surveyId) ?: return
        fieldSurveyDao.update(
            survey.copy(
                baseLayer = baseLayer,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSurveyBoundary(surveyId: Int, boundaryGeoJson: String?) {
        val survey = fieldSurveyDao.getSurveyById(surveyId) ?: return
        fieldSurveyDao.update(
            survey.copy(
                boundaryGeoJson = boundaryGeoJson,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSurveyCustomCategories(surveyId: Int, customCategoriesJson: String) {
        val survey = fieldSurveyDao.getSurveyById(surveyId) ?: return
        fieldSurveyDao.update(
            survey.copy(
                customCategoriesJson = customCategoriesJson,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSurvey(survey: FieldSurvey) {
        fieldSurveyDao.delete(survey)
    }

    suspend fun addAnnotation(annotation: SurveyAnnotation, media: List<AnnotationMedia> = emptyList()): SurveyAnnotation {
        val now = System.currentTimeMillis()
        val annotationId = annotationDao.insert(
            annotation.copy(
                createdAt = annotation.createdAt,
                updatedAt = now
            )
        ).toInt()
        if (media.isNotEmpty()) {
            mediaDao.insertAll(
                media.map { item -> item.copy(annotationId = annotationId, createdAt = now) }
            )
        }
        fieldSurveyDao.getSurveyById(annotation.surveyId)?.let { survey ->
            fieldSurveyDao.update(survey.copy(updatedAt = now))
        }
        return annotation.copy(id = annotationId)
    }

    suspend fun updateAnnotation(annotation: SurveyAnnotation) {
        val now = System.currentTimeMillis()
        annotationDao.update(annotation.copy(updatedAt = now))
        fieldSurveyDao.getSurveyById(annotation.surveyId)?.let { survey ->
            fieldSurveyDao.update(survey.copy(updatedAt = now))
        }
    }

    suspend fun deleteAnnotation(annotation: SurveyAnnotation) {
        annotationDao.delete(annotation)
        fieldSurveyDao.getSurveyById(annotation.surveyId)?.let { survey ->
            fieldSurveyDao.update(survey.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteAnnotationById(annotationId: Int) {
        annotationDao.getAnnotationById(annotationId)?.let { deleteAnnotation(it) }
    }

    suspend fun getAnnotationById(annotationId: Int): SurveyAnnotation? =
        annotationDao.getAnnotationById(annotationId)

    fun observeAnnotationsWithMedia(surveyId: Int): Flow<List<AnnotationWithMedia>> {
        return annotationDao.observeAnnotationsForSurvey(surveyId)
    }

    suspend fun addMedia(annotationId: Int, media: AnnotationMedia): AnnotationMedia {
        val now = System.currentTimeMillis()
        val id = mediaDao.insert(
            media.copy(
                annotationId = annotationId,
                createdAt = now
            )
        ).toInt()
        annotationDao.getAnnotationById(annotationId)?.let { annotation ->
            annotationDao.update(annotation.copy(updatedAt = now))
            fieldSurveyDao.getSurveyById(annotation.surveyId)?.let { survey ->
                fieldSurveyDao.update(survey.copy(updatedAt = now))
            }
        }
        return media.copy(id = id, annotationId = annotationId, createdAt = now)
    }

    suspend fun removeMedia(mediaId: Int) {
        val media = mediaDao.getById(mediaId) ?: return
        mediaDao.delete(media)
        annotationDao.getAnnotationById(media.annotationId)?.let { annotation ->
            val now = System.currentTimeMillis()
            annotationDao.update(annotation.copy(updatedAt = now))
            fieldSurveyDao.getSurveyById(annotation.surveyId)?.let { survey ->
                fieldSurveyDao.update(survey.copy(updatedAt = now))
            }
        }
    }

    suspend fun getJob(jobId: Int): Job? = jobDao.getJobByIdSync(jobId)

    suspend fun getLote(loteId: Int): Lote? = loteDao.getLoteByIdSync(loteId)
}
