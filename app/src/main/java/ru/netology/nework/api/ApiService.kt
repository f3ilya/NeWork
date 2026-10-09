package ru.netology.nework.api

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import ru.netology.nework.BuildConfig
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.Token

private const val BASE_URL = "${BuildConfig.BASE_URL}/api/"

fun okhttp(vararg interceptor: Interceptor): OkHttpClient = OkHttpClient.Builder()
    .apply {
        interceptor.forEach {
            this.addInterceptor(it)
        }
    }
    .build()

fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .client(client)
    .build()

interface ApiService {
    /** === AUTHENTICATION === **/

    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun authentication(
        @Field("login") login: String,
        @Field("pass") pass: String,
    ): Response<Token>

    @FormUrlEncoded
    @POST("users/registration")
    suspend fun registration(
        @Field("login") login: String,
        @Field("pass") pass: String,
        @Field("name") name: String,
    ): Response<Token>

    @Multipart
    @POST("users/registration")
    suspend fun registrationWithAvatar(
        @Part("login") login: RequestBody,
        @Part("pass") pass: RequestBody,
        @Part("name") name: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<Token>

    /** === POSTS === **/

    @GET("posts")
    suspend fun getAll(): Response<List<Post>>

    @GET("posts/{id}/before")
    suspend fun getBeforePosts(
        @Path("id") id: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @GET("posts/{id}/after")
    suspend fun getAfterPosts(
        @Path("id") id: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatestPosts(@Query("count") count: Int): Response<List<Post>>

    @POST("posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikePost(@Path("id") id: Long): Response<Post>
}