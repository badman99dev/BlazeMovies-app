package com.movie.app.best.di

import android.content.Context
import android.content.SharedPreferences
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.remote.AuthApiService
import com.movie.app.best.data.remote.BypassApiService
import com.movie.app.best.data.remote.ImdbApiService
import com.movie.app.best.data.remote.ImdxApiService
import com.movie.app.best.data.remote.MovieApiService
import com.movie.app.best.data.repository.Zee5TokenRepository
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson() = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(
            com.movie.app.best.data.model.Zee5SearchContent::class.java,
            com.movie.app.best.data.model.Zee5SearchContentDeserializer()
        )
        .create()

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    @Named("userAgent")
    fun provideUserAgentInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("X-App-Version-Code", com.movie.app.best.BuildConfig.VERSION_CODE.toString())
                .header("X-App-Version-Name", com.movie.app.best.BuildConfig.VERSION_NAME)
                .build()
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            val token = getLiveFirebaseToken(context)
            val request = if (token != null) {
                chain.request().newBuilder()
                    .header("X-Auth-Token", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    private fun getLiveFirebaseToken(context: Context): String? {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            try {
                return Tasks.await(user.getIdToken(false), 30, TimeUnit.SECONDS)?.token
            } catch (_: Exception) {}
        }
        return context.getSharedPreferences("app_auth", Context.MODE_PRIVATE)
            .getString("token", null)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        @Named("userAgent") userAgentInterceptor: Interceptor,
        @Named("auth") authInterceptor: Interceptor,
        @ApplicationContext context: Context
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(DebugInterceptor())
            .authenticator(FirebaseTokenAuthenticator(context))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("main")
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("bypass")
    fun provideBypassRetrofit(gson: com.google.gson.Gson): Retrofit {
        val bypassClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(DebugInterceptor())
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DL_AGENT_BASE_URL)
            .client(bypassClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("imdb")
    fun provideImdbRetrofit(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.IMDB1_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @Named("imdx")
    fun provideImdxRetrofit(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.IMDB2_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieApiService(@Named("main") retrofit: Retrofit): MovieApiService {
        return retrofit.create(MovieApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(@Named("main") retrofit: Retrofit): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBypassApiService(@Named("bypass") retrofit: Retrofit): BypassApiService {
        return retrofit.create(BypassApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImdbApiService(@Named("imdb") retrofit: Retrofit): ImdbApiService {
        return retrofit.create(ImdbApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImdxApiService(@Named("imdx") retrofit: Retrofit): ImdxApiService {
        return retrofit.create(ImdxApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("zee5")
    fun provideZee5Retrofit(okHttpClient: OkHttpClient, gson: com.google.gson.Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(com.movie.app.best.data.remote.Zee5ApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideZee5ApiService(@Named("zee5") retrofit: Retrofit): com.movie.app.best.data.remote.Zee5ApiService {
        return retrofit.create(com.movie.app.best.data.remote.Zee5ApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("zee5suggest")
    fun provideZee5SuggestionRetrofit(gson: com.google.gson.Gson): Retrofit {
        val plainClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://artemis.zee5.com/")
            .client(plainClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideZee5SuggestionApiService(@Named("zee5suggest") retrofit: Retrofit): com.movie.app.best.data.remote.Zee5SuggestionApiService {
        return retrofit.create(com.movie.app.best.data.remote.Zee5SuggestionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideZee5TokenRepository(
        zee5Api: com.movie.app.best.data.remote.Zee5ApiService
    ): Zee5TokenRepository {
        return Zee5TokenRepository(zee5Api)
    }
}

class FirebaseTokenAuthenticator(
    @ApplicationContext private val context: Context
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        val fresh = try {
            Tasks.await(user.getIdToken(true), 30, TimeUnit.SECONDS)?.token
        } catch (_: Exception) { null } ?: return null
        context.getSharedPreferences("app_auth", Context.MODE_PRIVATE)
            .edit().putString("token", fresh).apply()
        return response.request.newBuilder()
            .header("X-Auth-Token", "Bearer $fresh")
            .build()
    }
}
