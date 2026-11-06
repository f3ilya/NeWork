package ru.netology.nework.api

import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
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
import ru.netology.nework.dto.Job
import ru.netology.nework.dto.Media
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.Token
import ru.netology.nework.dto.UserResponse

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
    @Multipart
    @POST("media")
    suspend fun saveMedia(@Part media: MultipartBody.Part): Response<Media>

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

    @POST("posts")
    suspend fun savePost(@Body post: Post): Response<Post>

    @DELETE("posts/{id}")
    suspend fun removePost(@Path("id") id: Long): Response<Unit>

    @GET("posts/{id}/before")
    suspend fun getBeforePosts(
        @Path("id") id: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @GET("posts/latest")
    suspend fun getLatestPosts(@Query("count") count: Int): Response<List<Post>>

    @POST("posts/{id}/likes")
    suspend fun likePost(@Path("id") id: Long): Response<Post>

    @DELETE("posts/{id}/likes")
    suspend fun dislikePost(@Path("id") id: Long): Response<Post>

    /** === USERS === **/

    @GET("users")
    suspend fun getAllUsers(): Response<List<UserResponse>>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): Response<UserResponse>

    /** === JOBS === **/

    @GET("my/jobs")
    suspend fun getMyJobs(): Response<List<Job>>

    @GET("{id}/jobs")
    suspend fun getUserJobs(@Path("id") id: Long): Response<List<Job>>

    @POST("my/jobs")
    suspend fun saveJob(@Body job: Job): Response<Job>

    @DELETE("my/jobs/{id}")
    suspend fun removeJob(@Path("id") id: Long): Response<Unit>

    /** === WALL === **/

    @GET("{authorId}/wall/{id}/before")
    suspend fun getBeforeAuthorWall(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @GET("{authorId}/wall/latest")
    suspend fun getLatestAuthorWall(
        @Path("authorId") authorId: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @POST("{authorId}/wall/{id}/likes")
    suspend fun likeAuthorWall(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long,
    ): Response<Post>

    @DELETE("{authorId}/wall/{id}/likes")
    suspend fun dislikeAuthorWall(
        @Path("authorId") authorId: Long,
        @Path("id") id: Long,
    ): Response<Post>

    /** === MY WALL === **/

    @GET("my/wall/{id}/before")
    suspend fun getBeforeMyWall(
        @Path("id") id: Long,
        @Query("count") count: Int,
    ): Response<List<Post>>

    @GET("my/wall/latest")
    suspend fun getLatestMyWall(
        @Query("count") count: Int,
    ): Response<List<Post>>

    @POST("my/wall/{id}/likes")
    suspend fun likeMyWall(
        @Path("id") id: Long,
    ): Response<Post>

    @DELETE("my/wall/{id}/likes")
    suspend fun dislikeMyWall(
        @Path("id") id: Long,
    ): Response<Post>
}