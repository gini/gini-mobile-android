package net.gini.android.core.api.authorization

import net.gini.android.core.api.authorization.apimodels.SessionResponseModel
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface UserService {
    @FormUrlEncoded
    @POST("oauth/token?grant_type=password")
    suspend fun signIn(@Field("username") userName:String, @Field("password") password:String): Response<UserResponseModel>

    @POST("oauth/token?grant_type=client_credentials")
    suspend fun loginClient(): Response<Session>

    @POST("api/users")
    suspend fun createUser(@Body userRequestModel: UserRequestModel): Response<UserResponseModel>

    @GET("oauth/check_token?token={token}")
    suspend fun getGiniApiSessionTokenInfo(@Path("token") token: String): Response<SessionResponseModel>

    @GET("{uri}")
    suspend fun getUserInfo(@Path("uri") uri: String): Response<UserResponseModel>

    @PUT("api/users/{userId}")
    suspend fun updateEmail(@Path("userId") userId: String, @Body userRequestModel: UserRequestModel): Response<ResponseBody>
}
