package com.example.foodworldcup.ui

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
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
 * - 아래로 스와이프로 Rewind 기능
 * - Overlay로 Like/Nope 텍스트 표시
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
    
    // 아래로 스와이프 감지를 위한 변수
    private var isDraggingDown = false
    private var downSwipeRatio = 0f
    
    // 초기 총 음식 개수 (카운팅 정확성을 위해 저장)
    private var initialTotalCount: Int = 0
    
    // 스와이프 전 topPosition 저장 (스와이프된 음식을 찾기 위해)
    private var topPositionBeforeSwipe: Int = 0

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
        val initialFoodList = selectedFoodIds.mapNotNull { id ->
            FoodRepository.getFoodById(id)
        }
        
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
                isDraggingDown = false
                downSwipeRatio = 0f
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
        // 아래로 스와이프는 수동으로 감지하여 Rewind 처리

        // Swipe 애니메이션 설정
        val swipeSetting = SwipeAnimationSetting.Builder()
            .setDirection(Direction.Right)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(AccelerateInterpolator())
            .build()
        layoutManager.setSwipeAnimationSetting(swipeSetting)

        // Rewind 애니메이션 설정
        val rewindSetting = RewindAnimationSetting.Builder()
            .setDirection(Direction.Bottom)
            .setDuration(Duration.Normal.duration)
            .setInterpolator(DecelerateInterpolator())
            .build()
        layoutManager.setRewindAnimationSetting(rewindSetting)

        // 어댑터 설정
        val foodList = gameStateManager.getRemainingFoods().toMutableList()
        cardStackAdapter = CardStackAdapter(foodList)
        binding.cardStackView.layoutManager = layoutManager
        binding.cardStackView.adapter = cardStackAdapter
        
        // 아래로 스와이프 Rewind를 위한 터치 리스너 추가
        setupSwipeDownListener()
    }
    
    /**
     * 아래로 스와이프 제스처를 감지하여 Rewind 기능을 처리합니다.
     */
    private var touchStartY = 0f
    private var touchStartX = 0f
    
    private fun setupSwipeDownListener() {
        // dp를 픽셀로 변환하는 함수
        val dpToPx = { dp: Float ->
            resources.displayMetrics.density * dp
        }
        
        val swipeThreshold = dpToPx(100f) // 100dp
        val dragThreshold = dpToPx(50f) // 50dp
        
        binding.cardStackView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 터치 시작 위치 저장
                    touchStartY = event.y
                    touchStartX = event.x
                    isDraggingDown = false
                    downSwipeRatio = 0f
                }
                MotionEvent.ACTION_MOVE -> {
                    // 아래로 드래그하는지 확인
                    val deltaY = event.y - touchStartY
                    val deltaX = kotlin.math.abs(event.x - touchStartX)
                    
                    // 세로 이동이 가로 이동보다 크고, 아래로 이동한 경우
                    if (deltaY > dragThreshold && deltaY > deltaX) {
                        isDraggingDown = true
                        val maxDrag = dpToPx(300f)
                        downSwipeRatio = minOf(1f, (deltaY / maxDrag))
                    } else {
                        // 좌우로 드래그하거나 위로 드래그하는 경우는 무시
                        if (deltaY <= 0 || deltaX > deltaY) {
                            isDraggingDown = false
                            downSwipeRatio = 0f
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 아래로 스와이프 완료 확인
                    val deltaY = event.y - touchStartY
                    val deltaX = kotlin.math.abs(event.x - touchStartX)
                    
                    // 아래로 스와이프 임계값 이상이고, 가로 이동보다 세로 이동이 큰 경우
                    if (isDraggingDown && deltaY > swipeThreshold && deltaY > deltaX) {
                        // Rewind 처리
                        if (gameStateManager.canRewind()) {
                            handleRewindWithAnimation()
                        } else {
                            Toast.makeText(this, "되돌릴 수 있는 카드가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                        isDraggingDown = false
                        downSwipeRatio = 0f
                        return@setOnTouchListener true
                    }
                    isDraggingDown = false
                    downSwipeRatio = 0f
                }
            }
            false // 다른 터치 이벤트는 CardStackView가 처리하도록
        }
    }

    /**
     * 카드 드래그 중 처리 (Overlay 표시 및 아래로 스와이프 감지)
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
                isDraggingDown = false
            }
            Direction.Left -> {
                // 왼쪽 스와이프: Nope Overlay 표시
                cardStackAdapter.setOverlayAlpha(holder, ratio, Direction.Left)
                isDraggingDown = false
            }
            Direction.Bottom -> {
                // 아래로 스와이프: Rewind 준비
                isDraggingDown = true
                downSwipeRatio = ratio
            }
            else -> {
                // 다른 방향은 처리하지 않음
                if (!isDraggingDown) {
                    cardStackAdapter.hideOverlay(holder)
                }
            }
        }
    }

    /**
     * 카드 스와이프 완료 시 처리
     */
    private fun onCardSwipedInternal(direction: Direction?) {
        // 아래로 스와이프는 별도 터치 리스너에서 처리하므로 여기서는 처리하지 않음
        
        // 좌우 스와이프인 경우만 처리
        when (direction) {
            Direction.Right, Direction.Left -> {
                // 스와이프 전 topPosition을 기준으로 스와이프된 음식 찾기
                val remainingFoods = gameStateManager.getRemainingFoods()
                
                if (remainingFoods.isEmpty()) {
                    // 더 이상 남은 음식이 없으면 게임 종료
                    binding.cardStackView.postDelayed({
                        finishGame()
                    }, 500) // 마지막 카드 표시를 위해 지연
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
                    binding.cardStackView.postDelayed({
                        finishGame()
                    }, 500) // 스와이프 애니메이션 완료 후 게임 종료 (마지막 카드 표시)
                    return
                }
                
                // 어댑터 업데이트
                cardStackAdapter.setFoods(updatedRemainingFoods)
                
                // topPosition을 0으로 리셋하여 첫 번째 카드부터 시작
                binding.cardStackView.postDelayed({
                    layoutManager.topPosition = 0
                    // 카드가 제대로 표시되도록 강제 새로고침
                    binding.cardStackView.invalidate()
                    binding.cardStackView.requestLayout()
                }, 50) // 약간의 지연을 두어 어댑터 업데이트 완료 보장
                
                // 다음 이미지 프리로드
                preloadNextImages()
                
                // 진행 상황 업데이트
                updateProgress()
            }
            else -> {
                // 다른 방향은 처리하지 않음
            }
        }

        isDraggingDown = false
        downSwipeRatio = 0f
    }

    /**
     * Rewind 처리 (애니메이션 포함)
     */
    private fun handleRewindWithAnimation() {
        if (!gameStateManager.canRewind()) {
            Toast.makeText(this, "되돌릴 수 있는 카드가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // GameStateManager에서 Rewind (상태 먼저 업데이트)
        val rewindAction = gameStateManager.rewind()
        if (rewindAction != null) {
            // 어댑터 데이터 업데이트 (Rewind 후 남은 음식 리스트로 업데이트)
            val remainingFoods = gameStateManager.getRemainingFoods()
            if (remainingFoods.isNotEmpty()) {
                // 어댑터 데이터만 업데이트 (새로 생성하지 않음)
                cardStackAdapter.setFoods(remainingFoods)
                
                // CardStackView의 rewind() 메서드 호출하여 애니메이션 수행
                binding.cardStackView.post {
                    // rewind() 호출 (CardStackView가 내부적으로 처리)
                    binding.cardStackView.rewind()
                }
                
                // 약간의 지연 후 topPosition을 0으로 리셋
                binding.cardStackView.postDelayed({
                    layoutManager.topPosition = 0
                    // 카드가 제대로 표시되도록 강제 새로고침
                    binding.cardStackView.invalidate()
                }, 100)
            }
            
            // 진행 상황 업데이트
            updateProgress()
        }
    }
    
    /**
     * Rewind 처리 (기존 메서드, 호환성 유지)
     */
    private fun handleRewind() {
        handleRewindWithAnimation()
    }

    /**
     * Rewind 완료 시 처리
     */
    private fun onCardRewoundInternal() {
        // Rewind 애니메이션 완료 후 추가 처리
        binding.cardStackView.post {
            // topPosition을 0으로 확실히 리셋
            layoutManager.topPosition = 0
            
            // 어댑터가 제대로 업데이트되었는지 확인
            val remainingFoods = gameStateManager.getRemainingFoods()
            if (remainingFoods.isNotEmpty()) {
                // 어댑터 아이템 개수가 맞지 않으면 다시 업데이트
                if (cardStackAdapter.itemCount != remainingFoods.size) {
                    cardStackAdapter.setFoods(remainingFoods)
                }
                
                // 카드가 제대로 표시되도록 강제 새로고침
                binding.cardStackView.invalidate()
                binding.cardStackView.requestLayout()
            }
        }
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
        binding.cardStackView.postDelayed({
            val intent = Intent(this, ResultActivity::class.java)
            val passedFoodIds = passedFoods.map { it.id }
            intent.putIntegerArrayListExtra("passed_food_ids", ArrayList(passedFoodIds))
            startActivity(intent)
            finish()
        }, 300) // 마지막 카드가 보이도록 약간의 지연
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
        // 리소스 정리
    }
}
