package com.example.foodworldcup.api

import com.example.foodworldcup.BuildConfig
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// --- [1] 데이터 모델 (Kakao Local API JSON 응답용) ---
data class KakaoSearchResponse(val documents: List<Place>, val meta: Meta)

data class Meta(val total_count: Int, val pageable_count: Int, val is_end: Boolean)

data class Place(
        val id: String? = null, // place_id (상세 정보 조회용)
        val place_name: String,
        val category_name: String? = null,
        val category_group_code: String? = null,
        val phone: String? = null,
        val address_name: String? = null,
        val road_address_name: String? = null,
        val x: String, // 경도 (Longitude)
        val y: String, // 위도 (Latitude)
        val place_url: String? = null,
        val distance: String? = null,
        val foodType: String? = null // 음식 종류 (검색 시 설정)
)

// 상세 정보 응답 모델
data class PlaceDetailResponse(val documents: List<PlaceDetail>)

data class PlaceDetail(
        val id: String? = null,
        val place_name: String? = null,
        val category_name: String? = null,
        val phone: String? = null,
        val address_name: String? = null,
        val road_address_name: String? = null,
        val x: String? = null,
        val y: String? = null,
        val place_url: String? = null,
        val home_page: String? = null,
        val bcode: String? = null,
        val hcode: String? = null
)

// --- [2] API 인터페이스 (Retrofit) ---
interface KakaoApiService {
    @GET("v2/local/search/keyword.json")
    fun searchPlace(
            @Header("Authorization") apiKey: String,
            @Query("query") query: String,
            @Query("x") longitude: String? = null,
            @Query("y") latitude: String? = null,
            @Query("radius") radius: Int? = null, // 미터 단위
            @Query("page") page: Int = 1,
            @Query("size") size: Int = 15
    ): Call<KakaoSearchResponse>

    // 상세 정보 조회 (place_id 사용)
    @GET("v2/local/search/detail.json")
    fun getPlaceDetail(
            @Header("Authorization") apiKey: String,
            @Query("id") placeId: String
    ): Call<PlaceDetailResponse>
}

class MapApiHelper {

    companion object {
        private const val SEARCH_RADIUS = 4000 // 4000m (4km)
        private const val BASE_URL = "https://dapi.kakao.com/"
    }

    private val retrofit =
            Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

    private val apiService = retrofit.create(KakaoApiService::class.java)

    // API 키는 BuildConfig에서 가져옴
    private val restApiKey: String = "KakaoAK ${BuildConfig.KAKAO_REST_API_KEY}"

    /** Kakao Local API로 음식점 검색 latitude, longitude가 null이면 위치 정보 없이 검색합니다. */
    fun searchPlaces(
            query: String,
            foodType: String,
            latitude: Double? = null,
            longitude: Double? = null,
            onSuccess: (List<Place>) -> Unit,
            onError: (String) -> Unit
    ) {
        // 위치 정보가 있으면 radius 설정
        val radius = if (latitude != null && longitude != null) SEARCH_RADIUS else null

        apiService
                .searchPlace(
                        apiKey = restApiKey,
                        query = query,
                        longitude = longitude?.toString(),
                        latitude = latitude?.toString(),
                        radius = radius,
                        page = 1,
                        size = 15
                )
                .enqueue(
                        object : Callback<KakaoSearchResponse> {
                            override fun onResponse(
                                    call: Call<KakaoSearchResponse>,
                                    response: Response<KakaoSearchResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val places = response.body()?.documents ?: emptyList()
                                    // 음식 종류를 Place에 추가
                                    val placesWithFoodType =
                                            places.map { it.copy(foodType = foodType) }
                                    onSuccess(placesWithFoodType)
                                } else {
                                    val errorMsg = "검색 실패: ${response.code()} ${response.message()}"
                                    if (response.code() == 401) {
                                        onError(
                                                "API 키가 유효하지 않습니다. local.properties의 KAKAO_REST_API_KEY를 확인해주세요."
                                        )
                                    } else {
                                        onError(errorMsg)
                                    }
                                }
                            }

                            override fun onFailure(call: Call<KakaoSearchResponse>, t: Throwable) {
                                onError("네트워크 오류: ${t.message}")
                            }
                        }
                )
    }
}
