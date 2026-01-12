package com.example.foodworldcup.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.foodworldcup.R
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.data.MapSelectedFood
import com.example.foodworldcup.databinding.ActivityIntroBinding
import com.example.foodworldcup.utils.PreferenceManager
import com.kakao.sdk.common.util.Utility

/**
 * 앱의 첫 화면(인트로 화면)을 담당하는 Activity입니다.
 * 앱 소개 및 게임 시작 버튼이 있는 화면입니다.
 * 
 * 레이아웃 파일: res/layout/activity_intro.xml
 */
class IntroActivity : BaseActivity() {

    // ViewBinding 변수 선언. lateinit으로 나중에 초기화할 것을 약속합니다.
    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 뷰 바인딩 객체를 생성합니다.
        // XML 레이아웃 파일을 메모리에 올리고(inflate) 실제 뷰 객체로 만듭니다.
        binding = ActivityIntroBinding.inflate(layoutInflater)

        // 2. 생성된 뷰의 최상위(root) 뷰를 화면에 표시합니다.
        // 기존의 setContentView(R.layout.activity_intro)를 대체합니다.
        setContentView(binding.root)

        // FoodRepository 초기화 (JSON 파일에서 데이터 로드)
        FoodRepository.initialize(this)

        // 해시 키 추출 및 로그 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KakaoKeyHash", "현재 키 해시값: $keyHash")

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.HOME)

        // '게임 시작' 버튼 클릭 시 FoodListActivity로 이동
        binding.startButton.setOnClickListener { 
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
        }
        
        // Recent Winner 카드 클릭 시 마이페이지로 이동
        binding.recentWinnerCard.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            startActivity(intent)
        }
        
        // 하단 네비게이션 바는 BaseActivity에서 처리됨
    }

    override fun onResume() {
        super.onResume()
        // 인트로 화면이 다시 보일 때 최근 우승 기록을 불러와서 표시합니다.
        loadRecentWinner()
    }

    override fun onPause() {
        super.onPause()
    }

    /**
     * PreferenceManager에서 최근 선택된 음식을 불러와서 Recent Winner 섹션에 표시하는 함수입니다.
     * 기록이 없으면 Recent Winner 섹션을 숨깁니다.
     */
    private fun loadRecentWinner() {
        val mapSelectedFoods = preferenceManager.getMapSelectedFoods()
        
        if (mapSelectedFoods.isEmpty()) {
            // 기록이 없으면 Recent Winner 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
            return
        }
        
        // 가장 최근 기록 가져오기 (날짜 기준 내림차순 정렬)
        val recentFood = mapSelectedFoods.sortedByDescending { it.selectedDate }.first()
        
        // 음식 정보 가져오기
        val food = FoodRepository.getFoodById(recentFood.foodId)
        
        if (food != null) {
            // Recent Winner 섹션 표시
            binding.recentWinnerCard.visibility = View.VISIBLE
            binding.recentWinnerNameTextView.text = food.name
            
            // 캐릭터 이미지 로드 (characterImagePath 사용)
            if (!food.characterImagePath.isNullOrEmpty()) {
                // 시도할 경로 리스트
                val pathsToTry = mutableListOf<String>()
                
                // 1. 원본 경로 (캐릭터 이미지 경로)
                pathsToTry.add(food.characterImagePath)
                
                // 2. 확장자 변경 (.png <-> .jpg)
                if (food.characterImagePath.endsWith(".png")) {
                    pathsToTry.add(food.characterImagePath.replace(".png", ".jpg"))
                } else if (food.characterImagePath.endsWith(".jpg")) {
                    pathsToTry.add(food.characterImagePath.replace(".jpg", ".png"))
                }
                
                // 3. 음식 이름으로 직접 찾기 (캐릭터 이미지 경로)
                pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.png")
                pathsToTry.add("food_character_images/${food.category}/${food.name}_캐릭터누끼.jpg")
                
                // 각 경로를 시도
                var loaded = false
                for (path in pathsToTry) {
                    try {
                        val inputStream = assets.open(path)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            // ImageView의 크기를 측정한 후 이미지 스케일링
                            val viewTreeObserver = binding.recentWinnerImageView.viewTreeObserver
                            viewTreeObserver.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
                                override fun onPreDraw(): Boolean {
                                    binding.recentWinnerImageView.viewTreeObserver.removeOnPreDrawListener(this)
                                    
                                    // ImageView의 실제 크기 가져오기
                                    val targetWidth = binding.recentWinnerImageView.width
                                    val targetHeight = binding.recentWinnerImageView.height
                                    
                                    if (targetWidth > 0 && targetHeight > 0) {
                                        // MapActivity와 동일한 방식으로 이미지 크기 통일
                                        val scaledBitmap = scaleBitmapToFitContent(
                                            bitmap,
                                            targetWidth,
                                            targetHeight
                                        )
                                        
                                        if (scaledBitmap != null) {
                                            binding.recentWinnerImageView.setImageBitmap(scaledBitmap)
                                            binding.recentWinnerImageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                            binding.recentWinnerImageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        } else {
                                            // 스케일링 실패 시 원본 사용
                                            binding.recentWinnerImageView.setImageBitmap(bitmap)
                                            binding.recentWinnerImageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                            binding.recentWinnerImageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        }
                                    } else {
                                        // 크기를 측정할 수 없으면 원본 사용
                                        binding.recentWinnerImageView.setImageBitmap(bitmap)
                                        binding.recentWinnerImageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                                        binding.recentWinnerImageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    }
                                    
                                    return true
                                }
                            })
                            loaded = true
                            break
                        }
                    } catch (e: Exception) {
                        // 다음 경로 시도
                        continue
                    }
                }
                
                if (!loaded) {
                    binding.recentWinnerImageView.setImageResource(R.drawable.ic_launcher_background)
                }
            } else {
                binding.recentWinnerImageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            // 음식을 찾을 수 없으면 섹션 숨김
            binding.recentWinnerCard.visibility = View.GONE
        }
    }

    /**
     * MapActivity의 getContentBounds와 동일한 방식으로
     * 비트맵에서 투명하지 않은 실질적 영역을 구합니다.
     */
    private fun getContentBounds(bitmap: Bitmap): Rect {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var maxX = -1
        var minY = height
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Alpha 값 확인 (MSB 8bit)
                val alpha = (pixels[y * width + x] shr 24) and 0xFF
                if (alpha > 50) { // 약간의 투명도는 무시 (노이즈 방지)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            // 내용이 없으면 전체 반환
            return Rect(0, 0, width, height)
        }

        return Rect(minX, minY, maxX + 1, maxY + 1)
    }

    /**
     * MapActivity와 동일한 방식으로 이미지를 스케일링합니다.
     * 실질적 내용 영역을 기준으로 크기를 통일합니다.
     * 
     * @param originalBitmap 원본 비트맵
     * @param targetWidth 목표 너비
     * @param targetHeight 목표 높이
     * @return 스케일링된 비트맵
     */
    private fun scaleBitmapToFitContent(
        originalBitmap: Bitmap,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        try {
            // 이미지의 실질적 내용(누끼 부분)의 크기를 구함
            val contentBounds = getContentBounds(originalBitmap)
            val contentWidth = contentBounds.width()
            val contentHeight = contentBounds.height()

            // 타겟 영역 설정 (ImageView 크기의 90% 정도 사용)
            val targetAreaWidth = targetWidth * 0.9f
            val targetAreaHeight = targetHeight * 0.9f

            // 스케일 계산 (비율 유지)
            val scale = Math.min(
                targetAreaWidth / contentWidth,
                targetAreaHeight / contentHeight
            )

            // Matrix를 사용하여 스케일링
            val matrix = Matrix()
            // 먼저 내용 영역의 왼쪽 상단을 원점으로 이동
            matrix.postTranslate(
                -contentBounds.left.toFloat(),
                -contentBounds.top.toFloat()
            )
            // 스케일 적용
            matrix.postScale(scale, scale)
            
            // 중앙 정렬을 위한 이동
            val scaledContentWidth = contentWidth * scale
            val scaledContentHeight = contentHeight * scale
            val tx = (targetWidth - scaledContentWidth) / 2f
            val ty = (targetHeight - scaledContentHeight) / 2f
            matrix.postTranslate(tx, ty)

            // 스케일링된 비트맵 생성
            val scaledBitmap = Bitmap.createBitmap(
                targetWidth,
                targetHeight,
                Bitmap.Config.ARGB_8888
            )
            
            val canvas = Canvas(scaledBitmap)
            val paint = Paint()
            paint.isAntiAlias = true
            paint.isFilterBitmap = true
            
            canvas.drawBitmap(originalBitmap, matrix, paint)
            
            return scaledBitmap
        } catch (e: Exception) {
            Log.e("IntroActivity", "비트맵 스케일링 실패: ${e.message}", e)
            return null
        }
    }
}
