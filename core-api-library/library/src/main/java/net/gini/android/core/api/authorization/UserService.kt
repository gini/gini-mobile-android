package net.gini.android.core.api.authorization

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface UserService {
    @FormUrlEncoded
    @POST("oauth/token?grant_type=password")
    suspend fun signIn(@Field("username") userName:String, @Field("password") password:String): Response<User>

    @POST("oauth/token?grant_type=client_credentials")
    suspend fun loginClient(): Response<Session>

    @POST("api/users")
    suspend fun createUser(@Body userRequestModel: UserRequestModel): Response<User>
}
