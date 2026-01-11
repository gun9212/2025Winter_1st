package com.example.foodworldcup.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import com.example.foodworldcup.data.Food
import com.example.foodworldcup.data.FoodRepository
import com.example.foodworldcup.databinding.ActivityGameBinding
import com.example.foodworldcup.game.GameStateManager
import com.example.foodworldcup.ui.adapter.CardStackAdapter
import com.example.foodworldcup.utils.ImagePreloader
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.Duration
import com.yuyakaido.android.cardstackview.RewindAnimationSetting
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting

/**
 * 스와이프 게임 화면을 담당하는 Activity입니다.
 * CardStackView를 사용하여 Tinder 스타일의 스와이프 게임을 구현합니다.
 * 
 * 주요 기능:
 * - 좌우 스와이프로 음식 선택 (Like/Nope)
 *  * - Overlay로 Like/Nope 텍스트 표시
 * - 버튼 클릭으로 자동 스와이프
 * - 이미지 프리로딩
 * 
 * 레이아웃 파일: res/layout/activity_game.xml
 */
class GameActivity : BaseActivity() {

    private lateinit var binding: ActivityGameBinding
    
    // 게임 상태 관리
    private lateinit var gameStateManager: GameStateManager
    
    // 이미지 프리로더
    private lateinit var imagePreloader: ImagePreloader
    
    // CardStackView 어댑터
    private lateinit var cardStackAdapter: CardStackAdapter
    
    // CardStackLayoutManager
    private lateinit var layoutManager: CardStackLayoutManager
    
    
    // 초기 총 음식 개수 (카운팅 정확성을 위해 저장)
    private var initialTotalCount: Int = 0
    
    // 스와이프 전 topPosition 저장 (스와이프된 음식을 찾기 위해)
    private var topPositionBeforeSwipe: Int = 0
    
    // Handler for delayed tasks (메모리 누수 방지)
    private val handler = Handler(Looper.getMainLooper())
    
    // Pending runnables for cleanup
    private val pendingRunnables = mutableListOf<Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 하단 네비게이션 바 설정
        setupBottomNavigation(BaseActivity.Screen.SWIPE)

        // 이미지 프리로더 초기화
        imagePreloader = ImagePreloader(this)

        // 게임 초기화
        initializeGame()
    }

    /**
     * 게임을 초기화하는 함수입니다.
     */
    private fun initializeGame() {
        // FoodListActivity에서 받은 음식 ID 리스트로 음식 리스트를 구성합니다.
        val selectedFoodIds = intent.getIntegerArrayListExtra("selected_food_ids") ?: emptyList()
        // 음식 리스트를 랜덤 순서로 섞어서 게임의 재미를 높입니다.
        val initialFoodList = selectedFoodIds.mapNotNull { id ->
            FoodRepository.getFoodById(id)
        }.shuffled()
        
        // 음식 리스트가 비어있으면 FoodListActivity로 이동
        if (initialFoodList.isEmpty()) {
            Toast.makeText(this, "선택된 음식이 없습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, FoodListActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // GameStateManager 초기화
        gameStateManager = GameStateManager(initialFoodList)
        
        // 초기 총 개수 저장 (카운팅 정확성을 위해)
        initialTotalCount = initialFoodList.size
        
        // CardStackView 초기화
        setupCardStackView()
        
        // 초기 이미지 프리로드
        preloadNextImages()
        
        // 진행 상황 업데이트
        updateProgress()
        
        // 버튼 이벤트 설정
        setupButtons()
    }

    /**
     * CardStackView를 초기화하고 설정합니다.
     */
    private fun setupCardStackView() {
        // LayoutManager 생성 및 설정
        layoutManager = CardStackLayoutManager(this, object : CardStackListener {
            override fun onCardDragging(direction: Direction?, ratio: Float) {
                // 드래그 시작 시 현재 topPosition 저장
                if (ratio == 0f) {
                    topPositionBeforeSwipe = layoutManager.topPosition
                }
                // 드래그 중 Overlay 표시 및 투명도 조절
                onCardDraggingInternal(direction, ratio)
            }

            override fun onCardSwiped(direction: Direction?) {
                // 스와이프 완료 시 처리
                onCardSwipedInternal(direction)
            }

            override fun onCardRewound() {
                // Rewind 완료 시 처리
                onCardRewoundInternal()
            }

            override fun onCardCanceled() {
                // 스와이프 취소 시 Overlay 숨김
                val childCount = binding.cardStackView.childCount
                if (childCount > 0) {
                    val view = binding.cardStackView.getChildAt(0)
                    val holder = binding.cardStackView.getChildViewHolder(view) as? CardStackAdapter.ViewHolder
                    holder?.let {
                        cardStackAdapter.hideOverlay(it)
                    }
                }
            }

            override fun onCardAppeared(view: View?, position: Int) {
                // 카드 등장 시 처리 (필요시)
            }

            override fun onCardDisappeared(view: View?, position: Int) {
                // 카드 사라질 때 처리 (필요시)
            }
        })

        // 카드 여백 및 크기 설정
        layoutManager.setTranslationInterval(8.0f) // 카드 간 간격
        layoutManager.setScaleInterval(0.95f) // 뒤 카드 크기 비율
        layoutManager.setSwipeThreshold(0.3f) // 스와이프 임계값
        layoutManager.setMaxDegree(20.0f) // 카드 회전 최대 각도
        // 좌우 스와이프만 허용 (기본값)

        // Swipe 애니메이션 설정
        val swipeSetting = SwipeAnimationSetting.Builder()
            .setDirection(Direction.Right)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(AccelerateInterpolator())
            .build()
        layoutManager.setSwipeAnimationSetting(swipeSetting)

        // Rewind 애니메이션 설정 (Direction.Top으로 설정하여 위로 올라가는 자연스러운 애니메이션)
        val rewindSetting = RewindAnimationSetting.Builder()
            .setDirection(Direction.Top)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(DecelerateInterpolator())
            .build()
        layoutManager.setRewindAnimationSetting(rewindSetting)

        // 어댑터 설정
        val foodList = gameStateManager.getRemainingFoods().toMutableList()
        cardStackAdapter = CardStackAdapter(foodList)
        binding.cardStackView.layoutManager = layoutManager
        binding.cardStackView.adapter = cardStackAdapter
        
    }
    
    /**
     * 카드 드래그 중 처리 (Overlay 표시)
     */
    private fun onCardDraggingInternal(direction: Direction?, ratio: Float) {
        val topPosition = layoutManager.topPosition
        if (topPosition >= cardStackAdapter.itemCount || topPosition < 0) {
            return
        }

        // CardStackView에서 현재 표시 중인 첫 번째 카드의 뷰를 가져옴
        val childCount = binding.cardStackView.childCount
        if (childCount == 0) {
            return
        }

        val view = binding.cardStackView.getChildAt(0)
        val holder = binding.cardStackView.getChildViewHolder(view) as? CardStackAdapter.ViewHolder
        if (holder == null) {
            return
        }

        when (direction) {
            Direction.Right -> {
                // 오른쪽 스와이프: Like Overlay 표시
                cardStackAdapter.setOverlayAlpha(holder, ratio, Direction.Right)
            }
            Direction.Left -> {
                // 왼쪽 스와이프: Nope Overlay 표시
                cardStackAdapter.setOverlayAlpha(holder, ratio, Direction.Left)
            }
            Direction.Top -> {
                // 위로 스와이프: 처리하지 않음
                cardStackAdapter.hideOverlay(holder)
            }
            Direction.Bottom -> {
                // 아래로 스와이프: 처리하지 않음
                cardStackAdapter.hideOverlay(holder)
            }
            null -> {
                // null: 처리하지 않음
                cardStackAdapter.hideOverlay(holder)
            }
        }
        }

    /**
     * 카드 스와이프 완료 시 처리
     */
    private fun onCardSwipedInternal(direction: Direction?) {
        
        // 좌우 스와이프인 경우만 처리
        when (direction) {
            Direction.Right, Direction.Left -> {
                // 스와이프 전 topPosition을 기준으로 스와이프된 음식 찾기
                val remainingFoods = gameStateManager.getRemainingFoods()
                
                if (remainingFoods.isEmpty()) {
                    // 더 이상 남은 음식이 없으면 게임 종료
                    val finishRunnable = Runnable { finishGame() }
                    pendingRunnables.add(finishRunnable)
                    handler.postDelayed(finishRunnable, 500) // 마지막 카드 표시를 위해 지연
                    return
                }
                
                // 첫 번째 음식이 방금 스와이프된 음식 (topPosition이 0일 때)
                // topPositionBeforeSwipe를 기준으로 찾기
                val swipedIndex = if (topPositionBeforeSwipe < remainingFoods.size) {
                    topPositionBeforeSwipe
                } else {
                    0 // 안전을 위해 첫 번째 음식 사용
                }
                
                val swipedFood = remainingFoods[swipedIndex]
                
                // 스와이프 방향에 따라 GameStateManager 업데이트
                when (direction) {
                    Direction.Right -> {
                        // 오른쪽 스와이프: 합격 (Like)
                        gameStateManager.swipeCard(Direction.Right, swipedFood)
                    }
                    Direction.Left -> {
                        // 왼쪽 스와이프: 탈락 (Nope)
                        gameStateManager.swipeCard(Direction.Left, swipedFood)
                    }
                    else -> {
                        return
                    }
                }
                
                // 어댑터 데이터 업데이트 (기존 어댑터의 데이터만 업데이트)
                val updatedRemainingFoods = gameStateManager.getRemainingFoods()
                
                // 게임 종료 확인 (업데이트 전에 확인)
                if (updatedRemainingFoods.isEmpty() || gameStateManager.isGameFinished()) {
                    // 남은 음식이 없으면 게임 종료 (마지막 카드를 표시하기 위해 약간의 지연)
                    val finishRunnable = Runnable { finishGame() }
                    pendingRunnables.add(finishRunnable)
                    handler.postDelayed(finishRunnable, 500) // 스와이프 애니메이션 완료 후 게임 종료 (마지막 카드 표시)
                    return
                }
                
                // 어댑터 업데이트
                cardStackAdapter.setFoods(updatedRemainingFoods)
                
                // topPosition을 0으로 리셋하여 첫 번째 카드부터 시작
                val resetPositionRunnable = Runnable {
                    layoutManager.topPosition = 0
                    // 카드가 제대로 표시되도록 강제 새로고침
                    binding.cardStackView.invalidate()
                    binding.cardStackView.requestLayout()
                }
                pendingRunnables.add(resetPositionRunnable)
                handler.postDelayed(resetPositionRunnable, 50) // 약간의 지연을 두어 어댑터 업데이트 완료 보장
                
                // 다음 이미지 프리로드
                preloadNextImages()
                
                // 진행 상황 업데이트
                updateProgress()
            }
            else -> {
                // 다른 방향은 처리하지 않음
            }
        }

    }

    /**
     * Rewind 완료 시 처리
     */
    private fun onCardRewoundInternal() {
        // Rewind 애니메이션 완료 후 topPosition을 0으로 설정하여 카드가 똑바로 정리된 상태로 표시
        val rewindCompleteRunnable = Runnable {
            // topPosition을 0으로 설정하여 첫 번째 카드(되돌려진 카드)가 제대로 표시되도록 함
            layoutManager.topPosition = 0
            
            // 카드가 똑바로 정리된 상태로 표시되도록 강제 새로고침
            binding.cardStackView.invalidate()
            binding.cardStackView.requestLayout()
            
            // 다음 이미지 프리로드
            preloadNextImages()
        }
        pendingRunnables.add(rewindCompleteRunnable)
        // 애니메이션 완료 후 지연을 두어 부드럽게 처리
        handler.postDelayed(rewindCompleteRunnable, 100)
    }

    /**
     * 버튼 이벤트를 설정합니다.
     */
    private fun setupButtons() {
        // Like 버튼: 오른쪽 스와이프
        binding.likeButton.setOnClickListener {
            val topPosition = layoutManager.topPosition
            if (topPosition >= cardStackAdapter.itemCount) {
                return@setOnClickListener
            }

            val swipeSetting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Right)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            layoutManager.setSwipeAnimationSetting(swipeSetting)
            binding.cardStackView.swipe()
        }

        // Nope 버튼: 왼쪽 스와이프
        binding.nopeButton.setOnClickListener {
            val topPosition = layoutManager.topPosition
            if (topPosition >= cardStackAdapter.itemCount) {
                return@setOnClickListener
            }

            val swipeSetting = SwipeAnimationSetting.Builder()
                .setDirection(Direction.Left)
                .setDuration(Duration.Normal.duration)
                .setInterpolator(AccelerateInterpolator())
                .build()
            layoutManager.setSwipeAnimationSetting(swipeSetting)
            binding.cardStackView.swipe()
        }

        // Rewind 버튼: 이전 카드로 되돌리기
        binding.rewindButton.setOnClickListener {
            if (!gameStateManager.canRewind()) {
                Toast.makeText(this, "되돌릴 수 있는 카드가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // GameStateManager에서 Rewind (상태 먼저 업데이트하여 이전 카드가 어댑터에 포함되도록)
            val rewindAction = gameStateManager.rewind()
            if (rewindAction != null) {
                // 어댑터 데이터 업데이트 (Rewind 후 남은 음식 리스트로 업데이트, 이전 카드가 포함됨)
                val remainingFoods = gameStateManager.getRemainingFoods()
                if (remainingFoods.isNotEmpty()) {
                    // 어댑터 데이터 업데이트 (이전 카드가 다시 포함됨)
                    cardStackAdapter.setFoods(remainingFoods)
                    
                    // 약간의 지연 후 rewind() 호출하여 어댑터 업데이트 완료 보장
                    val rewindRunnable = Runnable {
                        // CardStackView의 rewind() 메서드 호출하여 애니메이션 수행
                        // rewind()는 topPosition을 감소시켜 이전 카드를 표시합니다
                        binding.cardStackView.rewind()
                    }
                    pendingRunnables.add(rewindRunnable)
                    handler.postDelayed(rewindRunnable, 50) // 어댑터 업데이트 완료 보장을 위한 지연
                }
                
                // 진행 상황 업데이트
                updateProgress()
            }
        }
    }

    /**
     * 다음 2-3장의 이미지를 프리로드합니다.
     */
    private fun preloadNextImages() {
        val remainingFoods = gameStateManager.getRemainingFoods()
        if (remainingFoods.size > 1) {
            // 첫 번째 카드는 이미 표시 중이므로 두 번째부터 프리로드
            val foodsToPreload = remainingFoods.subList(1, minOf(4, remainingFoods.size))
            imagePreloader.preloadNext(foodsToPreload, 3)
        }
    }

    /**
     * 진행 상황을 업데이트합니다.
     */
    private fun updateProgress() {
        // 초기 총 개수와 현재 남은 개수로 진행 상황 계산
        val total = initialTotalCount
        val remaining = gameStateManager.getRemainingCount()
        val completed = total - remaining
        binding.progressTextView.text = "$completed/$total"
    }

    /**
     * 게임이 끝났을 때 호출되는 함수입니다.
     * 합격된 음식 리스트를 ResultActivity로 전달합니다.
     */
    private fun finishGame() {
        // 진행 상황을 최종 상태로 업데이트
        updateProgress()
        
        val passedFoods = gameStateManager.getPassedFoods()
        
        if (passedFoods.isEmpty()) {
            Toast.makeText(this, "합격된 음식이 없습니다. 다시 시작하세요.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 약간의 지연 후 ResultActivity로 이동 (마지막 카드 표시를 위해)
        val navigateToResultRunnable = Runnable {
            val intent = Intent(this, ResultActivity::class.java)
            val passedFoodIds = passedFoods.map { it.id }
            intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
            startActivity(intent)
            finish()
        }
        pendingRunnables.add(navigateToResultRunnable)
        handler.postDelayed(navigateToResultRunnable, 300) // 마지막 카드가 보이도록 약간의 지연
    }

    override fun onResume() {
        super.onResume()
        // 필요시 게임 화면이 다시 보일 때 실행할 로직
    }

    override fun onPause() {
        super.onPause()
        // 필요시 게임 화면이 가려질 때 실행할 로직
    }

    override fun onDestroy() {
        super.onDestroy()
        // 모든 pending 작업 취소 (메모리 누수 방지)
        pendingRunnables.forEach { handler.removeCallbacks(it) }
        pendingRunnables.clear()
    }
}
