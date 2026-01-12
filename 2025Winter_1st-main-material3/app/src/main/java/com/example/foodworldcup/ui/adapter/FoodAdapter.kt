package com.example.foodworldcup.ui.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.foodworldcup.R
import com.example.foodworldcup.data.Food
import java.util.concurrent.Executors

/**
 * 음식 리스트를 표시하는 RecyclerView 어댑터입니다.
 * 카테고리 내부의 음식들을 표시할 때 사용됩니다.
 */
class FoodAdapter(
    private var foods: List<Food>,
    private var selectedFoodIds: Set<Int>,
    private val onFoodCheckedChanged: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {
    
    companion object {
        // 이미지 캐시 (최대 10MB, 약 50개 이미지)
        private val imageCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                // Bitmap의 메모리 크기 반환 (바이트 단위)
                return bitmap.byteCount
            }
        }
        
        // 백그라운드 스레드 풀 (이미지 로딩용)
        private val executorService = Executors.newFixedThreadPool(4)
        
        // 메인 스레드 핸들러
        private val mainHandler = Handler(Looper.getMainLooper())
        
        /**
         * 캐시 키 생성
         */
        private fun getCacheKey(category: String, foodName: String): String {
            return "$category/$foodName"
        }
    }

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.foodCheckBox)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foods[position]
        val isSelected = food.id in selectedFoodIds

        holder.foodNameTextView.text = food.name

        // 체크박스 먼저 설정 (이미지 로딩 전에 빠르게 반응)
        holder.checkBox.setOnCheckedChangeListener(null) // 기존 리스너 제거
        holder.checkBox.isChecked = isSelected
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onFoodCheckedChanged(food.id, isChecked)
        }

        // 음식 설명은 표시하지 않음 (이름만 표시)
        holder.foodDescriptionTextView.visibility = View.GONE

        // 이미지 로딩을 post로 지연 (레이아웃 완료 후, 비동기 처리)
        holder.itemView.post {
            // ViewHolder가 재사용되지 않았는지 확인
            if (holder.adapterPosition == position) {
                if (!food.imagePath.isNullOrEmpty()) {
                    loadImageWithFallback(holder, food.imagePath, food.name, food.category, position)
                } else {
                    // 이미지 경로가 없으면 기본 이미지 표시
                    holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
                    holder.foodImageView.setBackgroundColor(Color.WHITE)
                }
            }
        }

        // 아이템 클릭 시 체크박스 토글
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }
    
    /**
     * 이미지를 로드하고, 실패 시 여러 경로를 시도하는 함수
     * Glide를 사용하여 비동기로 로드하여 메인 스레드 블로킹을 방지합니다.
     * 이미지 캐싱을 통해 성능을 최적화합니다.
     */
    private fun loadImageWithFallback(
        holder: FoodViewHolder,
        imagePath: String,
        foodName: String,
        category: String,
        position: Int
    ) {
        // 캐시 키 생성
        val cacheKey = getCacheKey(category, foodName)
        
        // 캐시에서 먼저 확인
        val cachedBitmap = imageCache.get(cacheKey)
        if (cachedBitmap != null && !cachedBitmap.isRecycled) {
            holder.foodImageView.setImageBitmap(cachedBitmap)
            holder.foodImageView.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.foodImageView.setBackgroundColor(Color.WHITE)
            return
        }
        
        // Glide RequestOptions 설정
        val requestOptions = RequestOptions()
            .placeholder(null)  // placeholder 제거 (깜빡임 방지)
            .error(ColorDrawable(Color.WHITE))
            .centerCrop()
            .dontAnimate()  // 애니메이션 비활성화 (성능 향상)
            .skipMemoryCache(false) // 메모리 캐시 사용
            .diskCacheStrategy(DiskCacheStrategy.NONE) // assets는 디스크 캐시 불필요
        
        // 시도할 경로 리스트
        val pathsToTry = mutableListOf<String>()
        pathsToTry.add(imagePath)
        
        if (imagePath.endsWith(".png")) {
            pathsToTry.add(imagePath.replace(".png", ".jpg"))
        } else if (imagePath.endsWith(".jpg")) {
            pathsToTry.add(imagePath.replace(".jpg", ".png"))
        }
        
        pathsToTry.add("food_images/$category/$foodName.png")
        pathsToTry.add("food_images/$category/$foodName.jpg")
        
        // Glide로 첫 번째 경로부터 비동기 시도
        loadImageWithGlide(holder, pathsToTry, 0, cacheKey, requestOptions, position)
    }
    
    /**
     * 백그라운드 스레드에서 이미지를 로드하는 함수
     * 메인 스레드 블로킹을 방지하고, 실패 시 다음 경로를 자동으로 시도합니다.
     */
    private fun loadImageWithGlide(
        holder: FoodViewHolder,
        paths: List<String>,
        index: Int,
        cacheKey: String,
        requestOptions: RequestOptions,
        position: Int
    ) {
        // ViewHolder가 재사용되었는지 확인
        if (holder.adapterPosition != position) {
            return
        }
        
        if (index >= paths.size) {
            // 모든 경로 실패 시 기본 이미지
            mainHandler.post {
                if (holder.adapterPosition == position) {
                    holder.foodImageView.setImageResource(R.drawable.ic_launcher_background)
                    holder.foodImageView.setBackgroundColor(Color.WHITE)
                }
            }
            return
        }
        
        val path = paths[index]
        val context = holder.itemView.context
        
        // 백그라운드 스레드에서 이미지 로드
        executorService.execute {
            try {
                // 백그라운드 스레드에서 assets에서 이미지 로드
                val inputStream = context.assets.open(path)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                
                if (bitmap != null) {
                    // 캐시에 저장
                    imageCache.put(cacheKey, bitmap)
                    
                    // 메인 스레드에서 ImageView에 설정
                    mainHandler.post {
                        // ViewHolder가 재사용되지 않았는지 다시 확인
                        if (holder.adapterPosition == position) {
                            // Glide를 사용하여 이미지 설정 (메모리 캐시 활용)
                            Glide.with(context)
                                .load(bitmap)
                                .apply(requestOptions)
                                .into(holder.foodImageView)
                        }
                    }
                } else {
                    // 다음 경로 시도
                    loadImageWithGlide(holder, paths, index + 1, cacheKey, requestOptions, position)
                }
            } catch (e: Exception) {
                // 실패 시 다음 경로 시도
                loadImageWithGlide(holder, paths, index + 1, cacheKey, requestOptions, position)
            }
        }
    }

    override fun getItemCount(): Int = foods.size
    
    /**
     * 데이터를 업데이트하는 메서드입니다.
     * DiffUtil을 사용하여 변경된 아이템만 업데이트하여 성능을 최적화합니다.
     */
    fun updateData(newFoods: List<Food>, newSelectedFoodIds: Set<Int>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = foods.size
            override fun getNewListSize() = newFoods.size
            
            override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
                return foods[oldPos].id == newFoods[newPos].id
            }
            
            override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
                val oldFood = foods[oldPos]
                val newFood = newFoods[newPos]
                val oldSelected = oldFood.id in selectedFoodIds
                val newSelected = newFood.id in newSelectedFoodIds
                // 음식 정보와 선택 상태가 모두 같으면 true
                return oldFood == newFood && oldSelected == newSelected
            }
        }
        
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        foods = newFoods
        selectedFoodIds = newSelectedFoodIds
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * 선택된 음식 ID 목록만 업데이트합니다.
     * 변경된 아이템만 업데이트하여 성능을 최적화합니다.
     */
    fun updateSelectedFoodIds(newSelectedFoodIds: Set<Int>) {
        val changedPositions = mutableListOf<Int>()
        
        // 변경된 아이템의 위치 찾기
        foods.forEachIndexed { index, food ->
            val wasSelected = food.id in selectedFoodIds
            val isSelected = food.id in newSelectedFoodIds
            if (wasSelected != isSelected) {
                changedPositions.add(index)
            }
        }
        
        selectedFoodIds = newSelectedFoodIds
        
        // 변경된 아이템만 업데이트 (전체 업데이트 방지)
        changedPositions.forEach { position ->
            notifyItemChanged(position)
        }
    }
}
