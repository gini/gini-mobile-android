package net.gini.android.core.api.authorization.apimodels

import com.google.gson.annotations.SerializedName

data class SessionResponseModel(
    @field:SerializedName("user_name")
    val userName: String? = null
)
