package net.gini.android.core.api.authorization

import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.SessionTokenInfo
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import net.gini.android.core.api.authorization.apimodels.UserResponseModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

internal interface UserService {
    @FormUrlEncoded
    @POST("oauth/token?grant_type=password")
    suspend fun signIn(@HeaderMap basicAuthHeaders: Map<String, String>, @Field("username") userName:String, @Field("password") password:String): Response<SessionToken>

    @POST("oauth/token?grant_type=client_credentials")
    suspend fun loginClient(@HeaderMap basicAuthHeaders: Map<String, String>): Response<SessionToken>

    @POST("api/users")
    suspend fun createUser(@HeaderMap bearerHeaders: Map<String, String>, @Body userRequestModel: UserRequestModel): Response<ResponseBody>

    @GET("oauth/check_token")
    suspend fun getGiniApiSessionTokenInfo(@HeaderMap bearerHeaders: Map<String, String>, @Query("token") token: String): Response<SessionTokenInfo>

    @GET
    suspend fun getUserInfo(@HeaderMap bearerHeaders: Map<String, String>, @Url uri: String): Response<UserResponseModel>

    @PUT("api/users/{userId}")
    suspend fun updateEmail(@HeaderMap bearerHeaders: Map<String, String>, @Path("userId") userId: String, @Body userRequestModel: UserRequestModel): Response<ResponseBody>
}
