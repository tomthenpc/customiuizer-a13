package tv.withaibuild.customiuizer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ListView
import com.miui.system.internal.R
import kotlin.math.max
import kotlin.math.min

class SortableListView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.listViewStyle
) : ListView(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 200
        private const val SCROLL_BOUND = 0.25f
        private const val SCROLL_SPEED_MAX = 16
        private const val SNAPSHOT_ALPHA = 153
        private const val TAG = "SortableListView"
    }

    private var mDraggingFrom = -1
    private var mDraggingItemHeight = 0
    private var mDraggingItemWidth = 0
    private var mDraggingTo = -1
    private var mDraggingY = 0
    private var mInterceptTouchForSorting = false
    private var mItemUpperBound = -1
    private var mOffsetYInDraggingItem = 0
    private var mOnOrderChangedListener: OnOrderChangedListener? = null
    private var mOnTouchListener: View.OnTouchListener
    private var mScrollBound = 0
    private var mScrollLowerBound = 0
    private var mScrollUpperBound = 0
    private var mSnapshot: BitmapDrawable? = null
    private var mSnapshotBackgroundForOverUpperBound: Drawable? = null
    private var mSnapshotShadow: Drawable? = null
    private var mSnapshotShadowPaddingBottom = 0
    private var mSnapshotShadowPaddingTop = 0
    private val mTmpLocation = IntArray(2)

    fun interface OnOrderChangedListener {
        fun OnOrderChanged(oldPos: Int, newPos: Int)
    }

    init {
        val shadow = context.getDrawable(R.drawable.sortable_list_dragging_item_shadow)
        mSnapshotShadow = shadow
        shadow?.setAlpha(SNAPSHOT_ALPHA)
        val rect = Rect()
        shadow?.getPadding(rect)
        mSnapshotShadowPaddingTop = rect.top
        mSnapshotShadowPaddingBottom = rect.bottom

        mOnTouchListener = View.OnTouchListener { _, motionEvent ->
            if (mOnOrderChangedListener != null && (motionEvent.action and 0xff) == MotionEvent.ACTION_DOWN) {
                val hittenItemPosition = getHittenItemPosition(motionEvent)
                if (hittenItemPosition >= 0) {
                    mDraggingFrom = hittenItemPosition
                    mDraggingTo = hittenItemPosition
                    mInterceptTouchForSorting = true
                    val childAt = getChildAt(hittenItemPosition - firstVisiblePosition)
                    if (childAt != null) {
                        mDraggingItemWidth = childAt.width
                        mDraggingItemHeight = childAt.height
                        getLocationOnScreen(mTmpLocation)
                        mDraggingY = motionEvent.rawY.toInt() - mTmpLocation[1]
                        mOffsetYInDraggingItem = mDraggingY - childAt.top

                        val createBitmap = Bitmap.createBitmap(mDraggingItemWidth, mDraggingItemHeight, Bitmap.Config.ARGB_8888)
                        childAt.draw(Canvas(createBitmap))
                        mSnapshot = BitmapDrawable(resources, createBitmap)
                        mSnapshot?.setAlpha(SNAPSHOT_ALPHA)
                        mSnapshot?.setBounds(childAt.left, 0, childAt.right, mDraggingItemHeight)

                        mSnapshotBackgroundForOverUpperBound?.setAlpha(SNAPSHOT_ALPHA)
                        mSnapshotBackgroundForOverUpperBound?.setBounds(childAt.left, 0, childAt.right, mDraggingItemHeight)

                        mSnapshotShadow?.setBounds(
                            childAt.left,
                            -mSnapshotShadowPaddingTop,
                            childAt.right,
                            mDraggingItemHeight + mSnapshotShadowPaddingBottom
                        )
                        childAt.startAnimation(createAnimation(mDraggingItemWidth, mDraggingItemWidth, 0, 0))
                    }
                }
            }
            mInterceptTouchForSorting
        }
    }

    fun createAnimation(fromX: Int, toX: Int, fromY: Int, toY: Int): Animation {
        val translateAnimation = TranslateAnimation(fromX.toFloat(), toX.toFloat(), fromY.toFloat(), toY.toFloat())
        translateAnimation.duration = ANIMATION_DURATION.toLong()
        translateAnimation.fillAfter = true
        return translateAnimation
    }

    fun getHittenItemPosition(motionEvent: MotionEvent): Int {
        val rawX = motionEvent.rawX
        val rawY = motionEvent.rawY
        val firstVisiblePosition = firstVisiblePosition
        val lastVisiblePosition = lastVisiblePosition
        for (pos in lastVisiblePosition downTo firstVisiblePosition) {
            val childAt = getChildAt(pos - firstVisiblePosition)
            if (childAt != null) {
                childAt.getLocationOnScreen(mTmpLocation)
                if (mTmpLocation[0] <= rawX && mTmpLocation[0] + childAt.width >= rawX) {
                    if (mTmpLocation[1] <= rawY && mTmpLocation[1] + childAt.height >= rawY) {
                        return pos
                    }
                }
            }
        }
        return -1
    }

    private fun setViewAnimation(view: View?, animation: Animation?) {
        if (view == null) return
        if (animation != null) {
            view.startAnimation(animation)
        } else {
            view.clearAnimation()
        }
    }

    private fun setViewAnimationByPisition(position: Int, animation: Animation?) {
        setViewAnimation(getChildAt(position - firstVisiblePosition), animation)
    }

    private fun updateDraggingToPisition(target: Int) {
        if (target == mDraggingTo || target < 0) return
        Log.d(TAG, "sort item from $mDraggingFrom To $target")

        if (mDraggingFrom < max(mDraggingTo, target)) {
            while (mDraggingTo > target && mDraggingTo > mDraggingFrom) {
                Log.d(TAG, "item $mDraggingTo set move down reverse animation")
                val pos = mDraggingTo
                mDraggingTo = pos - 1
                setViewAnimationByPisition(pos, createAnimation(0, 0, -mDraggingItemHeight, 0))
            }
        }

        if (mDraggingFrom > min(mDraggingTo, target)) {
            while (mDraggingTo < target && mDraggingTo < mDraggingFrom) {
                Log.d(TAG, "item $mDraggingTo set move up reverse animation")
                val pos = mDraggingTo
                mDraggingTo = pos + 1
                setViewAnimationByPisition(pos, createAnimation(0, 0, mDraggingItemHeight, 0))
            }
        }

        if (mDraggingFrom < max(mDraggingTo, target)) {
            while (mDraggingTo < target) {
                val pos = mDraggingTo + 1
                mDraggingTo = pos
                setViewAnimationByPisition(pos, createAnimation(0, 0, 0, -mDraggingItemHeight))
                Log.d(TAG, "item $mDraggingTo set move up animation")
            }
        }

        if (mDraggingFrom <= min(mDraggingTo, target)) return

        while (mDraggingTo > target) {
            val pos = mDraggingTo - 1
            mDraggingTo = pos
            setViewAnimationByPisition(pos, createAnimation(0, 0, 0, mDraggingItemHeight))
            Log.d(TAG, "item $mDraggingTo set move down animation")
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mDraggingFrom < 0) return

        var top = mDraggingY - mOffsetYInDraggingItem
        var header = headerViewsCount
        if (header < firstVisiblePosition || header > lastVisiblePosition) {
            header = firstVisiblePosition
        }
        val headerChild = getChildAt(header - firstVisiblePosition)
        top = max(top, headerChild?.top ?: 0)

        var bottom = (count - 1) - footerViewsCount
        if (bottom < firstVisiblePosition || bottom > lastVisiblePosition) {
            bottom = lastVisiblePosition
        }
        val bottomChild = getChildAt(bottom - firstVisiblePosition)
        val min = min(top, (bottomChild?.bottom ?: 0) - mDraggingItemHeight)

        canvas.translate(0f, min.toFloat())
        mSnapshotShadow?.draw(canvas)
        mSnapshot?.draw(canvas)
        mSnapshotBackgroundForOverUpperBound?.let {
            if (mDraggingTo < mItemUpperBound) it.draw(canvas)
        }
        canvas.translate(0f, -min.toFloat())
    }

    fun getListenerForStartingSort(): View.OnTouchListener = mOnTouchListener

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (mInterceptTouchForSorting) {
            requestDisallowInterceptTouchEvent(true)
            onTouchEvent(motionEvent)
            true
        } else {
            super.onInterceptTouchEvent(motionEvent)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val bound = max(1, (h * SCROLL_BOUND).toInt())
        mScrollBound = bound
        mScrollUpperBound = bound
        mScrollLowerBound = h - bound
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        if (mInterceptTouchForSorting) {
            when (motionEvent.action and 0xff) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (mDraggingFrom >= 0) {
                        val listener = mOnOrderChangedListener
                        if (listener == null || mDraggingFrom == mDraggingTo || mDraggingTo < 0) {
                            setViewAnimationByPisition(mDraggingFrom, null)
                        } else {
                            listener.OnOrderChanged(mDraggingFrom - headerViewsCount, mDraggingTo - headerViewsCount)
                        }
                    }
                    mInterceptTouchForSorting = false
                    mDraggingFrom = -1
                    mDraggingTo = -1
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val y = motionEvent.y.toInt()
                    if (mInterceptTouchForSorting || y != mDraggingY) {
                        var hittenItemPosition = getHittenItemPosition(motionEvent)
                        if (hittenItemPosition < headerViewsCount || hittenItemPosition > count - footerViewsCount) {
                            hittenItemPosition = mDraggingTo
                        }
                        updateDraggingToPisition(hittenItemPosition)
                        mDraggingY = y
                        invalidate()
                        var scroll = 0
                        if (y > mScrollLowerBound) {
                            scroll = ((mScrollLowerBound - y) * SCROLL_SPEED_MAX) / mScrollBound
                        } else if (y < mScrollUpperBound) {
                            scroll = ((mScrollUpperBound - y) * SCROLL_SPEED_MAX) / mScrollBound
                        }
                        if (scroll == 0) return true
                        val childAt = getChildAt(hittenItemPosition - firstVisiblePosition) ?: return true
                        setSelectionFromTop(hittenItemPosition, childAt.top + scroll)
                        return true
                    }
                    return true
                }
                else -> return true
            }
        }
        return super.onTouchEvent(motionEvent)
    }

    fun setItemUpperBound(upperBound: Int, background: Drawable?) {
        mItemUpperBound = upperBound
        mSnapshotBackgroundForOverUpperBound = background
    }

    fun setOnOrderChangedListener(listener: OnOrderChangedListener?) {
        mOnOrderChangedListener = listener
    }
}
