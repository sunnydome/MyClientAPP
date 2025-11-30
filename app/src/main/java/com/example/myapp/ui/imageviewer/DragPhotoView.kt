package com.example.myapp.ui.imageviewer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 可拖拽关闭 + 缩放的 PhotoView
 *
 * 功能：
 * 1. 双指缩放
 * 2. 双击缩放
 * 3. 拖拽浏览
 * 4. 下滑关闭（带透明度渐变）
 */
class DragPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val MIN_SCALE = 1.0f
        private const val MAX_SCALE = 3.0f
        private const val DOUBLE_TAP_SCALE = 2.0f

        // 触发关闭的阈值
        private const val DISMISS_THRESHOLD = 200f
        private const val DISMISS_VELOCITY_THRESHOLD = 800f
        private const val ALPHA_MIN = 0.3f
    }

    // 矩阵相关
    private val imageMatrix = Matrix()
    private val savedMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    // 手势检测器
    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    // 触摸状态
    private var mode = Mode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var downX = 0f
    private var downY = 0f

    // 拖拽关闭相关
    private var isDraggingToClose = false
    private var dragTranslationY = 0f
    private var currentAlpha = 1f

    // 回调
    private var onDismissListener: (() -> Unit)? = null
    private var onDragListener: ((Float, Float) -> Unit)? = null
    private var onSingleTapListener: (() -> Unit)? = null

    private enum class Mode {
        NONE, DRAG, ZOOM
    }

    init {
        scaleType = ScaleType.MATRIX

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    fun setOnDismissListener(listener: () -> Unit) {
        onDismissListener = listener
    }

    fun setOnDragListener(listener: (Float, Float) -> Unit) {
        onDragListener = listener
    }

    fun setOnSingleTapListener(listener: () -> Unit) {
        onSingleTapListener = listener
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        drawable?.let { initImageMatrix() }
    }

    private fun initImageMatrix() {
        val drawable = drawable ?: return

        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        val vWidth = width.toFloat()
        val vHeight = height.toFloat()

        if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) {
            post { initImageMatrix() }
            return
        }

        imageMatrix.reset()

        // 计算缩放比例，使图片适应View
        val scale = min(vWidth / dWidth, vHeight / dHeight)

        // 计算居中偏移
        val dx = (vWidth - dWidth * scale) / 2f
        val dy = (vHeight - dHeight * scale) / 2f

        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)

        setImageMatrix(imageMatrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initImageMatrix()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(imageMatrix)
                lastTouchX = event.x
                lastTouchY = event.y
                downX = event.x
                downY = event.y
                mode = Mode.DRAG
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(imageMatrix)
                mode = Mode.ZOOM
                isDraggingToClose = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    // 判断是否应该触发下滑关闭
                    if (!isDraggingToClose && isAtNormalScale() && dy > 0 && abs(dy) > abs(dx) * 1.5f) {
                        // 检查图片是否已经在顶部
                        if (isImageAtTop()) {
                            isDraggingToClose = true
                        }
                    }

                    if (isDraggingToClose) {
                        // 下滑关闭模式
                        handleDragToClose(event.y - downY)
                    } else {
                        // 普通拖拽模式
                        handleDrag(dx, dy)
                    }

                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingToClose) {
                    handleDragEnd()
                }
                mode = Mode.NONE
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_POINTER_UP -> {
                // 当一个手指抬起时，更新最后触摸位置
                val remainingPointerIndex = if (event.actionIndex == 0) 1 else 0
                if (remainingPointerIndex < event.pointerCount) {
                    lastTouchX = event.getX(remainingPointerIndex)
                    lastTouchY = event.getY(remainingPointerIndex)
                }
                mode = Mode.DRAG
            }
        }

        return true
    }

    private fun handleDrag(dx: Float, dy: Float) {
        val rect = getImageBounds()

        var actualDx = dx
        var actualDy = dy

        // 限制水平拖拽范围
        if (rect.width() <= width) {
            actualDx = 0f
        } else {
            if (rect.left + dx > 0) actualDx = -rect.left
            if (rect.right + dx < width) actualDx = width - rect.right
        }

        // 限制垂直拖拽范围
        if (rect.height() <= height) {
            actualDy = 0f
        } else {
            if (rect.top + dy > 0) actualDy = -rect.top
            if (rect.bottom + dy < height) actualDy = height - rect.bottom
        }

        // 判断是否应该让父View处理
        if (actualDx == 0f && dx != 0f) {
            parent?.requestDisallowInterceptTouchEvent(false)
        }

        imageMatrix.postTranslate(actualDx, actualDy)
        setImageMatrix(imageMatrix)
    }

    private fun handleDragToClose(totalDy: Float) {
        dragTranslationY = totalDy

        // 计算透明度
        val dismissProgress = min(1f, abs(totalDy) / (height * 0.5f))
        currentAlpha = max(ALPHA_MIN, 1f - dismissProgress * 0.7f)

        // 计算缩放
        val scale = max(0.5f, 1f - dismissProgress * 0.3f)

        // 更新变换
        translationY = totalDy
        scaleX = scale
        scaleY = scale

        // 通知外部更新背景
        onDragListener?.invoke(totalDy, currentAlpha)
    }

    private fun handleDragEnd() {
        val shouldDismiss = abs(dragTranslationY) > DISMISS_THRESHOLD

        if (shouldDismiss) {
            // 既然要执行 Activity 的共享元素退出，就不要在这里执行 animateDismiss() 把图片移出屏幕
            // 直接触发回调，让 System Transition 接管当前的 dragTranslationY 位置
            onDismissListener?.invoke()

            // 注意：不要再调用 animateDismiss()，否则会和系统动画冲突
        } else {
            // 恢复原位逻辑保持不变
            animateRestore()
        }

        isDraggingToClose = false
    }

    private fun animateDismiss() {
        val targetY = if (dragTranslationY > 0) height.toFloat() else -height.toFloat()

        ValueAnimator.ofFloat(dragTranslationY, targetY).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                translationY = value
                val alpha = max(0f, 1f - abs(value) / height)
                onDragListener?.invoke(value, alpha)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismissListener?.invoke()
                }
            })
            start()
        }
    }

    private fun animateRestore() {
        ValueAnimator.ofFloat(dragTranslationY, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                translationY = value
                scaleX = 1f - abs(value) / height * 0.3f
                scaleY = scaleX
                val alpha = max(ALPHA_MIN, 1f - abs(value) / height * 0.7f)
                onDragListener?.invoke(value, alpha)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    translationY = 0f
                    scaleX = 1f
                    scaleY = 1f
                    onDragListener?.invoke(0f, 1f)
                }
            })
            start()
        }

        dragTranslationY = 0f
    }

    private fun getImageBounds(): RectF {
        val rect = RectF()
        drawable?.let {
            rect.set(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())
            imageMatrix.mapRect(rect)
        }
        return rect
    }

    private fun getCurrentScale(): Float {
        imageMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    private fun isAtNormalScale(): Boolean {
        return abs(getCurrentScale() - getMinScale()) < 0.1f
    }

    private fun isImageAtTop(): Boolean {
        val rect = getImageBounds()
        return rect.top >= 0
    }

    private fun getMinScale(): Float {
        val drawable = drawable ?: return MIN_SCALE
        val dWidth = drawable.intrinsicWidth.toFloat()
        val dHeight = drawable.intrinsicHeight.toFloat()
        return min(width / dWidth, height / dHeight)
    }

    fun reset() {
        translationY = 0f
        scaleX = 1f
        scaleY = 1f
        currentAlpha = 1f
        dragTranslationY = 0f
        isDraggingToClose = false
        initImageMatrix()
    }

    // 缩放手势监听
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val currentScale = getCurrentScale()
            val minScale = getMinScale()

            // 限制缩放范围
            val newScale = currentScale * scaleFactor
            if (newScale < minScale * 0.5f || newScale > MAX_SCALE * 1.5f) {
                return true
            }

            imageMatrix.postScale(
                scaleFactor, scaleFactor,
                detector.focusX, detector.focusY
            )
            setImageMatrix(imageMatrix)

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            // 缩放结束后，如果超出范围则回弹
            val currentScale = getCurrentScale()
            val minScale = getMinScale()

            val targetScale = when {
                currentScale < minScale -> minScale
                currentScale > MAX_SCALE -> MAX_SCALE
                else -> return
            }

            animateScale(currentScale, targetScale)
        }
    }

    private fun animateScale(from: Float, to: Float) {
        ValueAnimator.ofFloat(from, to).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                val factor = scale / getCurrentScale()
                imageMatrix.postScale(factor, factor, width / 2f, height / 2f)
                setImageMatrix(imageMatrix)
            }
            start()
        }
    }

    // 手势监听
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTapListener?.invoke()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val currentScale = getCurrentScale()
            val minScale = getMinScale()

            val targetScale = if (currentScale < DOUBLE_TAP_SCALE - 0.1f) {
                DOUBLE_TAP_SCALE
            } else {
                minScale
            }

            animateScaleTo(targetScale, e.x, e.y)
            return true
        }
    }

    private fun animateScaleTo(targetScale: Float, focusX: Float, focusY: Float) {
        val startScale = getCurrentScale()

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val fraction = it.animatedValue as Float
                val scale = startScale + (targetScale - startScale) * fraction
                val factor = scale / getCurrentScale()
                imageMatrix.postScale(factor, factor, focusX, focusY)
                setImageMatrix(imageMatrix)
            }
            start()
        }
    }
}