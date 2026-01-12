package com.example.foodworldcup.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodworldcup.R
import com.example.foodworldcup.api.Place

/** 검색 결과를 표시하는 RecyclerView 어댑터 (음식종류별 헤더 포함) */
class PlaceAdapter(
        private var _places: List<Place>,
        selectedIndexParam: Int = -1,
        private val onItemClick: (Place, Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PLACE = 1
    }

    // 아이템 리스트 구성: 헤더와 Place 아이템을 순서대로 배치 (ItemTouchHelper에서 접근 필요)
    val items = mutableListOf<AdapterItem>()

    // Place 리스트에 직접 접근 가능하도록 (ItemDecoration에서 사용)
    val places: List<Place>
        get() = _places

    // 선택 상태 업데이트 (생성자 파라미터로 초기화)
    var selectedIndex: Int = selectedIndexParam
        private set

    init {
        buildItemsList()
    }

    private fun buildItemsList() {
        items.clear()
        if (_places.isEmpty()) return

        // 첫 번째 음식종류의 헤더 추가
        items.add(AdapterItem.Header(_places[0].foodType ?: "기타"))
        items.add(AdapterItem.PlaceItem(0))

        // 나머지 Place 아이템 추가 (음식종류가 바뀔 때마다 헤더 추가)
        for (i in 1 until _places.size) {
            if (_places[i].foodType != _places[i - 1].foodType) {
                items.add(AdapterItem.Header(_places[i].foodType ?: "기타"))
            }
            items.add(AdapterItem.PlaceItem(i))
        }
    }

    // 데이터 업데이트
    fun updatePlaces(newPlaces: List<Place>) {
        _places = newPlaces
        buildItemsList()
        notifyDataSetChanged()
    }

    // 선택 상태 업데이트
    fun updateSelectedIndex(newSelectedIndex: Int) {
        val oldIndex = selectedIndex
        selectedIndex = newSelectedIndex

        // 모든 Place 아이템의 위치를 찾아서 업데이트 (실시간 반영)
        val positionsToUpdate = mutableSetOf<Int>()

        // 이전 선택 항목과 새 선택 항목 모두 업데이트
        if (oldIndex >= 0 && oldIndex < _places.size) {
            val oldAdapterPosition = getAdapterPositionForPlace(oldIndex)
            if (oldAdapterPosition >= 0) {
                positionsToUpdate.add(oldAdapterPosition)
            }
        }

        if (selectedIndex >= 0 && selectedIndex < _places.size) {
            val newAdapterPosition = getAdapterPositionForPlace(selectedIndex)
            if (newAdapterPosition >= 0) {
                positionsToUpdate.add(newAdapterPosition)
            }
        }

        // 선택 상태가 변경되면 모든 Place 아이템의 색상이 변경되어야 함
        // 모든 Place 아이템 위치 추가
        items.forEachIndexed { index, item ->
            if (item is AdapterItem.PlaceItem) {
                positionsToUpdate.add(index)
            }
        }

        // 모든 변경된 위치 업데이트
        positionsToUpdate.forEach { position -> notifyItemChanged(position) }
    }

    sealed class AdapterItem {
        data class Header(val foodType: String) : AdapterItem()
        data class PlaceItem(val index: Int) : AdapterItem()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodTypeText: TextView = itemView.findViewById(R.id.foodTypeText)
    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val placeNameText: TextView = itemView.findViewById(R.id.placeNameText)
        val categoryText: TextView = itemView.findViewById(R.id.categoryText)
        val addressText: TextView = itemView.findViewById(R.id.addressText)
        val phoneText: TextView = itemView.findViewById(R.id.phoneText)
        val distanceText: TextView = itemView.findViewById(R.id.distanceText)
        val itemCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.itemCard)
        val itemContent: LinearLayout = itemView.findViewById(R.id.itemContent)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && position < items.size) {
                    when (val item = items[position]) {
                        is AdapterItem.PlaceItem -> {
                            if (item.index < _places.size) {
                                onItemClick(_places[item.index], item.index)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AdapterItem.Header -> VIEW_TYPE_HEADER
            is AdapterItem.PlaceItem -> VIEW_TYPE_PLACE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view =
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.item_food_type_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view =
                        LayoutInflater.from(parent.context)
                                .inflate(R.layout.item_place, parent, false)
                PlaceViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, mutableListOf<Any>())
    }

    override fun onBindViewHolder(
            holder: RecyclerView.ViewHolder,
            position: Int,
            payloads: MutableList<Any>
    ) {
        when (holder) {
            is HeaderViewHolder -> {
                when (val item = items[position]) {
                    is AdapterItem.Header -> {
                        holder.foodTypeText.text = "🍽️ ${item.foodType}"
                    }
                    else -> {}
                }
            }
            is PlaceViewHolder -> {
                when (val item = items[position]) {
                    is AdapterItem.PlaceItem -> {
                        if (item.index < _places.size) {
                            val place = _places[item.index]
                            val isSelected = item.index == selectedIndex

                            // 선택 상태에 따라 배경색 변경 (실시간 반영)
                            if (selectedIndex >= 0) {
                                if (isSelected) {
                                    // 선택된 항목: 흰색 배경
                                    holder.itemCard.setCardBackgroundColor(
                                            ContextCompat.getColor(
                                                    holder.itemView.context,
                                                    android.R.color.white
                                            )
                                    )
                                    holder.itemContent.setBackgroundColor(
                                            ContextCompat.getColor(
                                                    holder.itemView.context,
                                                    android.R.color.white
                                            )
                                    )
                                } else {
                                    // 비선택 항목: 회색 배경
                                    holder.itemCard.setCardBackgroundColor(
                                            ContextCompat.getColor(
                                                    holder.itemView.context,
                                                    android.R.color.darker_gray
                                            )
                                    )
                                    holder.itemContent.setBackgroundColor(
                                            ContextCompat.getColor(
                                                    holder.itemView.context,
                                                    android.R.color.darker_gray
                                            )
                                    )
                                }
                            } else {
                                // 선택 상태가 아니면 모두 흰색 (원래 색상)
                                holder.itemCard.setCardBackgroundColor(
                                        ContextCompat.getColor(
                                                holder.itemView.context,
                                                android.R.color.white
                                        )
                                )
                                holder.itemContent.setBackgroundColor(
                                        ContextCompat.getColor(
                                                holder.itemView.context,
                                                android.R.color.white
                                        )
                                )
                            }

                            // 전체 데이터 업데이트
                            holder.placeNameText.text = place.place_name

                            // 카테고리 이름 파싱 ("음식점>" 제거)
                            val categoryName = place.category_name ?: "카테고리 정보 없음"
                            val parsedCategory =
                                    if (categoryName.startsWith("음식점 >")) {
                                        categoryName.substring(5) // "음식점 >" 제거
                                    } else {
                                        categoryName
                                    }
                            holder.categoryText.text = parsedCategory

                            // 주소 표시 (도로명 주소 우선, 없으면 지번 주소)
                            val address =
                                    place.road_address_name ?: place.address_name ?: "주소 정보 없음"
                            holder.addressText.text = address

                            // 전화번호 표시
                            holder.phoneText.text = place.phone ?: "전화번호 없음"

                            // 거리 표시 (미터 단위를 km로 변환)
                            if (!place.distance.isNullOrBlank()) {
                                val distanceMeter = place.distance.toDoubleOrNull()
                                if (distanceMeter != null) {
                                    val distanceKm = distanceMeter / 1000.0
                                    holder.distanceText.text =
                                            if (distanceKm < 1.0) {
                                                "${distanceMeter.toInt()}m"
                                            } else {
                                                String.format("%.1fkm", distanceKm)
                                            }
                                } else {
                                    holder.distanceText.text = ""
                                }
                            } else {
                                holder.distanceText.text = ""
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // Place 인덱스를 어댑터 position으로 변환 (스크롤용)
    fun getAdapterPositionForPlace(placeIndex: Int): Int {
        return items.indexOfFirst {
            it is AdapterItem.PlaceItem && (it as AdapterItem.PlaceItem).index == placeIndex
        }
    }
}

/** Sticky Header ItemDecoration - 선택된 음식 종류가 상단에 고정되도록 함 */
class StickyHeaderItemDecoration(private val adapter: PlaceAdapter) :
        RecyclerView.ItemDecoration() {

    private var cachedHeaderView: View? = null
    private var cachedHeaderPosition = -1
    private var headerAlpha: Float = 1f

    override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: android.view.View,
            parent: RecyclerView,
            state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION && position < adapter.items.size) {
            // 헤더 아이템에 대해서만 RecyclerView의 paddingStart만큼 음수 마진을 주어 x=0부터 시작하도록 함
            if (adapter.items[position] is PlaceAdapter.AdapterItem.Header) {
                outRect.left = -parent.paddingStart
                outRect.right = -parent.paddingEnd
            }
        }
    }

    override fun onDrawOver(
            c: android.graphics.Canvas,
            parent: RecyclerView,
            state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return

        // 현재 화면에 보이는 첫 번째 아이템
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePosition == RecyclerView.NO_POSITION) return

        // 현재 화면에 보이는 첫 번째 헤더 찾기
        var currentHeaderPosition = -1
        for (i in firstVisiblePosition downTo 0) {
            if (i < adapter.items.size && adapter.items[i] is PlaceAdapter.AdapterItem.Header) {
                currentHeaderPosition = i
                break
            }
        }

        // 첫 번째 헤더가 없으면 선택된 항목의 헤더를 사용 (선택된 항목이 있는 경우)
        if (currentHeaderPosition < 0 &&
                        adapter.selectedIndex >= 0 &&
                        adapter.selectedIndex < adapter.places.size
        ) {
            val selectedPlace = adapter.places[adapter.selectedIndex]
            val selectedFoodType = selectedPlace.foodType
            if (selectedFoodType != null) {
                // 선택된 음식 종류의 헤더 위치 찾기
                for (i in adapter.items.indices) {
                    when (val item = adapter.items[i]) {
                        is PlaceAdapter.AdapterItem.Header -> {
                            if (item.foodType == selectedFoodType) {
                                currentHeaderPosition = i
                                break
                            }
                        }
                        is PlaceAdapter.AdapterItem.PlaceItem -> {
                            if (item.index == adapter.selectedIndex) {
                                // 선택된 Place가 속한 헤더를 찾기 위해 위로 거슬러 올라감
                                for (j in i downTo 0) {
                                    if (adapter.items[j] is PlaceAdapter.AdapterItem.Header) {
                                        currentHeaderPosition = j
                                        break
                                    }
                                }
                                break
                            }
                        }
                    }
                }
            }
        }

        val headerPosition = currentHeaderPosition

        // 실제 헤더 뷰가 화면에 있는지 확인 (스크롤 전)
        val actualHeaderView = layoutManager.findViewByPosition(headerPosition)
        // RecyclerView의 padding을 고려하지 않고 실제 헤더 위치 사용 (RecyclerView 내부 좌표계)
        val headerTop = actualHeaderView?.top ?: Int.MAX_VALUE

        // 헤더가 화면 상단에 닿거나 지나갔으면 sticky header 표시
        val needsSticky = headerPosition >= 0 && headerTop < 0

        if (needsSticky || (currentHeaderPosition >= 0 && firstVisiblePosition >= headerPosition)) {
            var headerView = cachedHeaderView

            // 캐시된 뷰가 없거나 다른 헤더가 필요하면 새로 생성
            if (headerView == null || cachedHeaderPosition != headerPosition) {
                val headerItem =
                        adapter.items[headerPosition] as? PlaceAdapter.AdapterItem.Header ?: return

                // 뷰 홀더 생성 및 바인딩 (수동)
                val newHeaderViewHolder =
                        adapter.onCreateViewHolder(parent, PlaceAdapter.VIEW_TYPE_HEADER) as
                                PlaceAdapter.HeaderViewHolder
                adapter.onBindViewHolder(newHeaderViewHolder, headerPosition)
                headerView = newHeaderViewHolder.itemView

                // 크기 측정
                val widthSpec =
                        View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY)
                val heightSpec =
                        View.MeasureSpec.makeMeasureSpec(
                                parent.height,
                                View.MeasureSpec.UNSPECIFIED
                        )

                // 뷰에 레이아웃 파라미터가 없으면 설정
                if (headerView.layoutParams == null) {
                    headerView.layoutParams =
                            ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                }

                headerView.measure(widthSpec, heightSpec)
                headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)

                cachedHeaderView = headerView
                cachedHeaderPosition = headerPosition
                headerAlpha = 1f
            }

            headerView?.let { view ->
                // 다음 헤더가 올라오면서 스택 효과 적용
                var nextHeaderPosition = -1
                for (i in (headerPosition + 1) until adapter.items.size) {
                    if (adapter.items[i] is PlaceAdapter.AdapterItem.Header) {
                        nextHeaderPosition = i
                        break
                    }
                }

                var topOffset = 0
                if (nextHeaderPosition > 0 && firstVisiblePosition >= nextHeaderPosition) {
                    val nextHeaderView = layoutManager.findViewByPosition(nextHeaderPosition)
                    if (nextHeaderView != null) {
                        // paddingTop이 0이므로 nextHeaderView.top을 그대로 사용
                        // 다음 헤더가 올라오고 있으면 스택 효과로 현재 헤더를 위로 밀어냄
                        if (nextHeaderView.top < view.height) {
                            topOffset = nextHeaderView.top - view.height
                        }
                    }
                }

                // 헤더를 상단에 그리기 (실제 헤더와 같은 위치, x=0부터 시작)
                c.save()
                val xOffset = 0f // getItemOffsets로 padding을 상쇄했으므로 x=0부터 시작
                val yOffset = topOffset.toFloat()
                c.translate(xOffset, yOffset)
                parent.drawChild(c, view, parent.drawingTime)
                c.restore()
            }
        } else {
            // 실제 헤더가 화면에 보이면 sticky header는 표시하지 않음
            cachedHeaderView = null
            cachedHeaderPosition = -1
            headerAlpha = 0f
        }
    }
}
