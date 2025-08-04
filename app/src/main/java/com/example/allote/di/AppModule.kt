package com.example.allote.di

import android.content.Context
import android.content.SharedPreferences
import com.example.allote.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import com.example.allote.data.ExchangeRateRepository
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()

    @Provides
    fun provideJobDao(db: AppDatabase): JobDao = db.jobDao()

    @Provides
    fun provideAdministracionDao(db: AppDatabase): AdministracionDao = db.administracionDao()

    @Provides
    fun provideJobParametrosDao(db: AppDatabase): JobParametrosDao = db.jobParametrosDao()

    @Provides
    fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()

    @Provides
    fun provideRecipeDao(db: AppDatabase): RecipeDao = db.recipeDao()

    @Provides
    fun provideImageDao(db: AppDatabase): ImageDao = db.imageDao()

    @Provides
    fun provideFormulacionDao(db: AppDatabase): FormulacionDao = db.formulacionDao()

    @Provides
    fun provideLoteDao(db: AppDatabase): LoteDao = db.loteDao()

    @Provides
    fun provideMovimientoContableDao(db: AppDatabase): MovimientoContableDao = db.movimientoContableDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(prefs: SharedPreferences): SettingsRepository {
        return SettingsRepository(prefs)
    }

    @Provides
    @Singleton
    fun provideAdministracionRepository(
        jobDao: JobDao,
        administracionDao: AdministracionDao,
        movimientoContableDao: MovimientoContableDao
    ): AdministracionRepository {
        return AdministracionRepository(jobDao, administracionDao, movimientoContableDao)
    }

    @Provides
    @Singleton
    fun provideProductsRepository(productDao: ProductDao, formulacionDao: FormulacionDao): ProductsRepository {
        return ProductsRepository(productDao, formulacionDao)
    }

    @Provides
    @Singleton
    fun provideClientsRepository(
        clientDao: ClientDao,
        jobDao: JobDao
    ): ClientsRepository {
        return ClientsRepository(clientDao, jobDao)
    }

    @Provides
    @Singleton
    fun provideJobsRepository(jobDao: JobDao, clientDao: ClientDao, imageDao: ImageDao): JobsRepository {
        return JobsRepository(jobDao, clientDao, imageDao)
    }

    @Provides
    @Singleton
    fun provideFormulacionesRepository(formulacionDao: FormulacionDao): FormulacionesRepository {
        return FormulacionesRepository(formulacionDao)
    }

    @Provides
    @Singleton
    fun provideGestionLotesRepository(
        loteDao: LoteDao,
        jobDao: JobDao,
        recipeDao: RecipeDao,
        productDao: ProductDao,
        formulacionDao: FormulacionDao
    ): GestionLotesRepository {
        return GestionLotesRepository(loteDao, jobDao, recipeDao, productDao, formulacionDao)
    }

    @Provides
    @Singleton
    fun provideJobDetailRepository(jobDao: JobDao, jobParametrosDao: JobParametrosDao): JobDetailRepository {
        return JobDetailRepository(jobDao, jobParametrosDao)
    }

    @Provides
    @Singleton
    fun provideParametrosRepository(jobDao: JobDao, jobParametrosDao: JobParametrosDao): ParametrosRepository {
        return ParametrosRepository(jobDao, jobParametrosDao)
    }

    @Provides
    @Singleton
    fun provideProductDetailRepository(productDao: ProductDao, formulacionDao: FormulacionDao): ProductDetailRepository {
        return ProductDetailRepository(productDao, formulacionDao)
    }

    @Provides
    @Singleton
    fun provideRecetasRepository(
        jobDao: JobDao,
        recipeDao: RecipeDao,
        productDao: ProductDao,
        formulacionDao: FormulacionDao,
        appDatabase: AppDatabase
    ): RecetasRepository {
        return RecetasRepository(jobDao, recipeDao, productDao, formulacionDao, appDatabase)
    }

    @Provides
    @Singleton
    fun provideAdministracionGeneralRepository(
        jobDao: JobDao,
        clientDao: ClientDao,
        administracionDao: AdministracionDao,
        movimientoContableDao: MovimientoContableDao,
        documentoMovimientoDao: DocumentoMovimientoDao,
        appDatabase: AppDatabase
    ): AdministracionGeneralRepository {
        return AdministracionGeneralRepository(jobDao, administracionDao, movimientoContableDao, clientDao, documentoMovimientoDao, appDatabase)
    }

    @Provides
    @Singleton
    fun provideAdministracionResumenRepository(
        jobDao: JobDao,
        administracionDao: AdministracionDao
    ): AdministracionResumenRepository {
        return AdministracionResumenRepository(jobDao, administracionDao)
    }

    @Provides
    @Singleton
    fun provideClientAdministracionRepository(clientDao: ClientDao): ClientAdministracionRepository {
        return ClientAdministracionRepository(clientDao)
    }

    @Provides
    @Singleton
    fun provideClientContabilidadRepository(
        appDatabase: AppDatabase,
        movimientoContableDao: MovimientoContableDao,
        clientDao: ClientDao,
        documentoMovimientoDao: DocumentoMovimientoDao,
        settingsRepository: SettingsRepository
    ): ClientContabilidadRepository {
        return ClientContabilidadRepository(
            appDatabase,
            movimientoContableDao,
            clientDao,
            documentoMovimientoDao,
            settingsRepository
        )
    }

    @Provides
    @Singleton
    fun provideClientJobsRepository(
        jobDao: JobDao,
        clientDao: ClientDao,
        imageDao: ImageDao
    ): ClientJobsRepository {
        return ClientJobsRepository(jobDao, clientDao, imageDao)
    }

    @Provides
    @Singleton
    fun provideImagesJobRepository(imageDao: ImageDao, @ApplicationContext context: Context): ImagesJobRepository {
        return ImagesJobRepository(imageDao, context)
    }

    @Provides
    @Singleton
    fun provideDocumentosRepository(
        administracionDao: AdministracionDao,
        @ApplicationContext context: Context
    ): DocumentosRepository {
        return DocumentosRepository(administracionDao, context)
    }

    @Provides
    @Singleton
    fun provideMainDashboardRepository(
        jobDao: JobDao,
        clientDao: ClientDao,
        movimientoContableDao: MovimientoContableDao,
        exchangeRateRepository: ExchangeRateRepository
    ): MainDashboardRepository {
        return MainDashboardRepository(
            jobDao,
            clientDao,
            movimientoContableDao,
            exchangeRateRepository
        )
    }

    @Provides
    fun provideDocumentoMovimientoDao(db: AppDatabase): DocumentoMovimientoDao = db.documentoMovimientoDao()

    @Provides
    @Singleton
    fun provideDocumentViewerRepository(
        movimientoContableDao: MovimientoContableDao,
        documentoMovimientoDao: DocumentoMovimientoDao
    ): DocumentViewerRepository {
        return DocumentViewerRepository(movimientoContableDao, documentoMovimientoDao)
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.bluelytics.com.ar/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // 2. Usando Retrofit, Hilt ahora sabe cómo crear nuestro servicio de API.
    @Provides
    @Singleton
    fun provideBluelyticsApiService(retrofit: Retrofit): BluelyticsApiService {
        return retrofit.create(BluelyticsApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("MeteoRetrofit")
    fun provideMeteoRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideMeteoApiService(@Named("MeteoRetrofit") retrofit: Retrofit): MeteoApiService {
        return retrofit.create(MeteoApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("NewsRetrofit")
    fun provideNewsRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://newsdata.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNewsApiService(@Named("NewsRetrofit") retrofit: Retrofit): NewsApiService {
        return retrofit.create(NewsApiService::class.java)
    }

    // 3. Usando el servicio de API, Hilt ahora sabe cómo crear nuestro nuevo repositorio.
    @Provides
    @Singleton
    fun provideExchangeRateRepository(apiService: BluelyticsApiService): ExchangeRateRepository {
        return ExchangeRateRepository(apiService)
    }
}