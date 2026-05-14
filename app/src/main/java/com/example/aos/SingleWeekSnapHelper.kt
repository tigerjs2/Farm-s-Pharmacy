package com.example.aos

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max

/**
 * 한 페이지를 한 주(월~일)로 넘기는 SnapHelper.
 *
 * - 빠르게 밀어도 한 번에 1주만 이동한다.
 * - 살짝 건드린 정도의 짧은 드래그/약한 플링은 다음 주로 넘어가지 않고 현재 주로 돌아온다.
 */
class SingleWeekSnapHelper : PagerSnapHelper() {

    companion object {
        // 값이 클수록 '휙' 넘겨도 덜 민감하다. 너무 둔하면 2800~3200 정도로 낮추면 된다.
        private const val MIN_FLING_VELOCITY = 3600

        // 손으로 끌 때 화면 폭의 이 비율 이상 움직여야 다음/이전 주로 넘어간다.
        private const val MIN_DRAG_RATIO = 0.35f

        // 화면이 좁아도 최소 이 정도는 밀어야 넘어가게 한다.
        private const val MIN_DRAG_DP = 96
    }

    private var recyclerView: RecyclerView? = null
    private var dragStartPosition: Int = RecyclerView.NO_POSITION
    private var totalDragDistancePx: Int = 0
    private var flingTargetPosition: Int = RecyclerView.NO_POSITION

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
            super.onScrollStateChanged(rv, newState)

            when (newState) {
                RecyclerView.SCROLL_STATE_DRAGGING -> {
                    val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
                    dragStartPosition = findCenterPosition(layoutManager)
                    totalDragDistancePx = 0
                    flingTargetPosition = RecyclerView.NO_POSITION
                }

                RecyclerView.SCROLL_STATE_IDLE -> {
                    // SnapHelper가 먼저 스냅 대상을 계산한 뒤 초기화되도록 post 처리한다.
                    rv.post {
                        dragStartPosition = RecyclerView.NO_POSITION
                        totalDragDistancePx = 0
                        flingTargetPosition = RecyclerView.NO_POSITION
                    }
                }
            }
        }

        override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(rv, dx, dy)

            if (dragStartPosition == RecyclerView.NO_POSITION) return

            val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return
            totalDragDistancePx += if (layoutManager.orientation == RecyclerView.HORIZONTAL) dx else dy
        }
    }

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView?.removeOnScrollListener(scrollListener)

        super.attachToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
        recyclerView?.addOnScrollListener(scrollListener)
    }

    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        val linearLayoutManager = layoutManager as? LinearLayoutManager
            ?: return RecyclerView.NO_POSITION

        val itemCount = linearLayoutManager.itemCount
        if (itemCount == 0) return RecyclerView.NO_POSITION

        val currentPosition = findCenterPosition(linearLayoutManager)
        if (currentPosition == RecyclerView.NO_POSITION) return RecyclerView.NO_POSITION

        val mainVelocity = if (linearLayoutManager.orientation == RecyclerView.HORIZONTAL) {
            velocityX
        } else {
            velocityY
        }

        // 손으로 충분히 끌었다면 속도가 낮아도 한 주 넘긴다.
        // 반대로 살짝만 끌고 놓은 약한 플링은 현재 주로 되돌린다.
        val dragThresholdPx = dragThresholdPx(linearLayoutManager)
        val dragDelta = when {
            dragStartPosition != RecyclerView.NO_POSITION && totalDragDistancePx > dragThresholdPx -> 1
            dragStartPosition != RecyclerView.NO_POSITION && totalDragDistancePx < -dragThresholdPx -> -1
            else -> 0
        }

        val target = when {
            dragDelta != 0 -> (dragStartPosition + dragDelta).coerceIn(0, itemCount - 1)
            abs(mainVelocity) < MIN_FLING_VELOCITY -> currentPosition
            else -> {
                val velocityDelta = if (mainVelocity > 0) 1 else -1
                (currentPosition + velocityDelta).coerceIn(0, itemCount - 1)
            }
        }

        flingTargetPosition = target
        return target
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        val linearLayoutManager = layoutManager as? LinearLayoutManager
            ?: return super.findSnapView(layoutManager)

        if (flingTargetPosition != RecyclerView.NO_POSITION) {
            return linearLayoutManager.findViewByPosition(flingTargetPosition)
                ?: super.findSnapView(layoutManager)
        }

        if (dragStartPosition != RecyclerView.NO_POSITION) {
            val thresholdPx = dragThresholdPx(linearLayoutManager)

            val delta = when {
                totalDragDistancePx > thresholdPx -> 1
                totalDragDistancePx < -thresholdPx -> -1
                else -> 0
            }

            val target = (dragStartPosition + delta)
                .coerceIn(0, linearLayoutManager.itemCount - 1)

            return linearLayoutManager.findViewByPosition(target)
                ?: super.findSnapView(layoutManager)
        }

        return super.findSnapView(layoutManager)
    }

    private fun dragThresholdPx(layoutManager: LinearLayoutManager): Int {
        val rv = recyclerView ?: return 0
        val pageSizePx = if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            rv.width - rv.paddingStart - rv.paddingEnd
        } else {
            rv.height - rv.paddingTop - rv.paddingBottom
        }

        val minByRatio = (pageSizePx * MIN_DRAG_RATIO).toInt()
        val minByDp = (MIN_DRAG_DP * rv.resources.displayMetrics.density + 0.5f).toInt()

        return max(minByRatio, minByDp)
    }

    private fun findCenterPosition(layoutManager: LinearLayoutManager): Int {
        val rv = recyclerView ?: return layoutManager.findFirstVisibleItemPosition()
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()

        if (first == RecyclerView.NO_POSITION || last == RecyclerView.NO_POSITION) {
            return RecyclerView.NO_POSITION
        }

        val rvCenter = if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
            rv.paddingStart + (rv.width - rv.paddingStart - rv.paddingEnd) / 2
        } else {
            rv.paddingTop + (rv.height - rv.paddingTop - rv.paddingBottom) / 2
        }

        var closestPosition = first
        var closestDistance = Int.MAX_VALUE

        for (position in first..last) {
            val child = layoutManager.findViewByPosition(position) ?: continue

            val childCenter = if (layoutManager.orientation == RecyclerView.HORIZONTAL) {
                child.left + child.width / 2
            } else {
                child.top + child.height / 2
            }

            val distance = abs(childCenter - rvCenter)
            if (distance < closestDistance) {
                closestDistance = distance
                closestPosition = position
            }
        }

        return closestPosition
    }
}
