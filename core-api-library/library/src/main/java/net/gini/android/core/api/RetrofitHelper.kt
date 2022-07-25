package net.gini.android.core.api

import net.gini.android.core.api.authorization.UserService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitHelper {
    val baseUrl = "https://user.gini.net/"
    private const val DEFAULT_TIMEOUT = 60L

    val setAuthorizationHeaderUseCase = SetAuthorizationHeaderUseCase()

    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                    .addInterceptor {
                        it.proceed(setAuthorizationHeaderUseCase(it.request()))
                    }.build())
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun provideUserService(): UserService {
        return getInstance().create(UserService::class.java)
    }

    class SetAuthorizationHeaderUseCase {

        operator fun invoke(request: Request): Request {
            return request.newBuilder().addHeader(AUTHORIZATION, "Basic Z2luaS1tb2JpbGUtdGVzdDp3VDRvNGtYUEFZdERrbm5PWXdXZjR3NXM=")
                .addHeader("accept", "application/json")
                .build()
        }

        companion object {
            private const val AUTHORIZATION = "Authorization"
        }
    }
}
