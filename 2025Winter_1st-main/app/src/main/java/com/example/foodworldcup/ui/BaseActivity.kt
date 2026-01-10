package com.example.foodworldcup.ui

import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.foodworldcup.R
import com.example.foodworldcup.utils.PreferenceManager

/**
 * 모든 Activity의 기본 클래스입니다.
 * 하단 네비게이션 바의 공통 로직을 처리합니다.
 */
abstract class BaseActivity : AppCompatActivity() {

    /**
     * 현재 Activity가 어떤 화면인지 나타내는 enum입니다.
     * 각 Activity에서 이 값을 설정해야 합니다.
     */
    enum class Screen {
        HOME,      // IntroActivity
        LIST,      // FoodListActivity
        SWIPE,     // GameActivity
        ACCEPTED,  // ResultActivity 또는 MyPageActivity
        MAP        // MapActivity
    }

    /**
     * 현재 화면을 나타내는 값입니다.
     * 각 Activity의 onCreate()에서 setCurrentScreen()을 호출하여 설정해야 합니다.
     */
    protected var currentScreen: Screen = Screen.HOME

    /**
     * 하단 네비게이션 바의 뷰 참조입니다.
     */
    private var bottomNavView: View? = null
    
    /**
     * 하단 네비게이션 바가 초기화되었는지 확인하는 플래그입니다.
     */
    private var isBottomNavInitialized = false
    
    /**
     * PreferenceManager 인스턴스 (필요시 사용)
     */
    protected val preferenceManager: PreferenceManager by lazy {
        PreferenceManager(this)
    }

    /**
     * 현재 화면을 설정하고 네비게이션 바를 업데이트합니다.
     * 각 Activity의 onCreate()에서 super.onCreate() 호출 후 이 함수를 호출해야 합니다.
     */
    protected fun setupBottomNavigation(screen: Screen) {
        currentScreen = screen
        
        try {
            // 하단 네비게이션 바 찾기
            bottomNavView = findViewById<View>(R.id.bottomNavigation)
            Log.d("BaseActivity", "findViewById 결과: ${bottomNavView != null}")
            
            if (bottomNavView != null) {
                isBottomNavInitialized = true
                Log.d("BaseActivity", "하단 네비게이션 바 찾음, 클릭 리스너 설정 시작")
                setupBottomNavigationClickListeners()
                Log.d("BaseActivity", "상태 업데이트 시작")
                updateBottomNavigationState()
                Log.d("BaseActivity", "하단 네비게이션 바 설정 완료: $screen")
            } else {
                Log.w("BaseActivity", "하단 네비게이션 바를 찾을 수 없습니다 (ID: bottomNavigation)")
                Log.w("BaseActivity", "레이아웃에 include된 bottomNavigation이 있는지 확인하세요")
                isBottomNavInitialized = false
            }
        } catch (e: Exception) {
            Log.e("BaseActivity", "setupBottomNavigation 오류", e)
            Log.e("BaseActivity", "오류 타입: ${e.javaClass.simpleName}")
            Log.e("BaseActivity", "오류 메시지: ${e.message}")
            e.printStackTrace()
            isBottomNavInitialized = false
        }
    }

    /**
     * 하단 네비게이션 바의 클릭 이벤트를 설정합니다.
     */
    private fun setupBottomNavigationClickListeners() {
        val navView = bottomNavView
        if (navView == null) {
            Log.w("BaseActivity", "bottomNavView가 null입니다. 클릭 리스너를 설정할 수 없습니다.")
            return
        }
        
        try {
            // 홈 (IntroActivity)
            val navHome = navView.findViewById<LinearLayout>(R.id.navHome)
            if (navHome != null) {
                navHome.setOnClickListener {
                    Log.d("BaseActivity", "홈 버튼 클릭")
                    if (currentScreen != Screen.HOME) {
                        try {
                            val intent = Intent(this, IntroActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e("BaseActivity", "홈으로 이동 실패", e)
                        }
                    }
                }
            } else {
                Log.w("BaseActivity", "navHome을 찾을 수 없습니다")
            }

            // 목록 (FoodListActivity)
            val navList = navView.findViewById<LinearLayout>(R.id.navList)
            if (navList != null) {
                navList.setOnClickListener {
                    Log.d("BaseActivity", "목록 버튼 클릭")
                    if (currentScreen != Screen.LIST) {
                        try {
                            Log.d("BaseActivity", "FoodListActivity로 이동 시도")
                            val intent = Intent(this, FoodListActivity::class.java)
                            startActivity(intent)
                            Log.d("BaseActivity", "FoodListActivity로 이동 완료")
                            finish()
                        } catch (e: Exception) {
                            Log.e("BaseActivity", "목록으로 이동 실패", e)
                            Log.e("BaseActivity", "오류 타입: ${e.javaClass.simpleName}")
                            Log.e("BaseActivity", "오류 메시지: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                Log.w("BaseActivity", "navList를 찾을 수 없습니다")
            }

            // 스와이프 (GameActivity)
            val navSwipe = navView.findViewById<LinearLayout>(R.id.navSwipe)
            if (navSwipe != null) {
                navSwipe.setOnClickListener {
                    Log.d("BaseActivity", "스와이프 버튼 클릭")
                    if (currentScreen != Screen.SWIPE) {
                        try {
                            // 저장된 선택된 음식 ID를 불러와서 GameActivity로 전달
                            val selectedFoodIds = preferenceManager.getSelectedFoodIds()
                            
                            if (selectedFoodIds.isNotEmpty()) {
                                val intent = Intent(this, GameActivity::class.java)
                                intent.putIntegerArrayListExtra("selected_food_ids", ArrayList(selectedFoodIds))
                                startActivity(intent)
                                finish()
                            } else {
                                // 선택된 음식이 없으면 FoodListActivity로 이동
                                Toast.makeText(this, "먼저 음식을 선택해주세요", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, FoodListActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            Log.e("BaseActivity", "스와이프로 이동 실패", e)
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                Log.w("BaseActivity", "navSwipe를 찾을 수 없습니다")
            }

            // 합격 (MyPageActivity - 우승 기록 목록)
            val navAccepted = navView.findViewById<LinearLayout>(R.id.navAccepted)
            if (navAccepted != null) {
                navAccepted.setOnClickListener {
                    Log.d("BaseActivity", "합격 버튼 클릭")
                    if (currentScreen != Screen.ACCEPTED) {
                        try {
                            val intent = Intent(this, MyPageActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e("BaseActivity", "합격으로 이동 실패", e)
                        }
                    }
                }
            } else {
                Log.w("BaseActivity", "navAccepted를 찾을 수 없습니다")
            }

            // 지도 (MapActivity)
            val navMap = navView.findViewById<LinearLayout>(R.id.navMap)
            if (navMap != null) {
                navMap.setOnClickListener {
                    Log.d("BaseActivity", "지도 버튼 클릭")
                    if (currentScreen != Screen.MAP) {
                        try {
                            val intent = Intent(this, MapActivity::class.java)
                            startActivity(intent)
                            finish()
                        } catch (e: Exception) {
                            Log.e("BaseActivity", "지도로 이동 실패", e)
                        }
                    }
                }
            } else {
                Log.w("BaseActivity", "navMap을 찾을 수 없습니다")
            }
            
            Log.d("BaseActivity", "모든 네비게이션 클릭 리스너 설정 완료")
        } catch (e: Exception) {
            Log.e("BaseActivity", "setupBottomNavigationClickListeners 오류", e)
            e.printStackTrace()
        }
    }

    /**
     * 현재 화면에 따라 하단 네비게이션 바의 선택 상태를 업데이트합니다.
     * 선택된 항목은 오렌지색, 선택되지 않은 항목은 회색으로 표시됩니다.
     */
    private fun updateBottomNavigationState() {
        val navView = bottomNavView ?: return
        
        // 모든 항목을 기본 상태로 초기화
        resetNavigationItems()

        // 현재 화면에 따라 선택된 항목 표시
        when (currentScreen) {
            Screen.HOME -> {
                val navHome = navView.findViewById<LinearLayout>(R.id.navHome)
                navHome?.let { setNavigationItemSelected(it, true) }
            }
            Screen.LIST -> {
                val navList = navView.findViewById<LinearLayout>(R.id.navList)
                navList?.let { setNavigationItemSelected(it, true) }
            }
            Screen.SWIPE -> {
                val navSwipe = navView.findViewById<LinearLayout>(R.id.navSwipe)
                navSwipe?.let { setNavigationItemSelected(it, true) }
            }
            Screen.ACCEPTED -> {
                val navAccepted = navView.findViewById<LinearLayout>(R.id.navAccepted)
                navAccepted?.let { setNavigationItemSelected(it, true) }
            }
            Screen.MAP -> {
                val navMap = navView.findViewById<LinearLayout>(R.id.navMap)
                navMap?.let { setNavigationItemSelected(it, true) }
            }
        }
    }

    /**
     * 모든 네비게이션 항목을 기본 상태(선택되지 않음)로 초기화합니다.
     */
    private fun resetNavigationItems() {
        val navView = bottomNavView ?: return
        
        navView.findViewById<LinearLayout>(R.id.navHome)?.let { setNavigationItemSelected(it, false) }
        navView.findViewById<LinearLayout>(R.id.navList)?.let { setNavigationItemSelected(it, false) }
        navView.findViewById<LinearLayout>(R.id.navSwipe)?.let { setNavigationItemSelected(it, false) }
        navView.findViewById<LinearLayout>(R.id.navAccepted)?.let { setNavigationItemSelected(it, false) }
        navView.findViewById<LinearLayout>(R.id.navMap)?.let { setNavigationItemSelected(it, false) }
    }

    /**
     * 네비게이션 항목의 선택 상태를 설정합니다.
     * 
     * @param item 네비게이션 항목의 LinearLayout
     * @param isSelected 선택 여부
     */
    private fun setNavigationItemSelected(item: View, isSelected: Boolean) {
        try {
            // TextView 찾기 (LinearLayout의 두 번째 자식)
            val linearLayout = item as? android.widget.LinearLayout
            if (linearLayout != null && linearLayout.childCount > 1) {
                val textView = linearLayout.getChildAt(1) as? android.widget.TextView
                
                if (textView != null) {
                    if (isSelected) {
                        // 선택된 상태: 오렌지색
                        textView.setTextColor(ContextCompat.getColor(this, R.color.primary_orange))
                    } else {
                        // 선택되지 않은 상태: 회색
                        textView.setTextColor(ContextCompat.getColor(this, R.color.navigation_unselected))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BaseActivity", "setNavigationItemSelected 오류", e)
        }
    }
}
