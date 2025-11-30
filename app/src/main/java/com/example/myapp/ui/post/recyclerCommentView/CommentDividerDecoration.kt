package com.example.myapp.ui.post.recyclerCommentView

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapp.R

/**
 * 评论列表分割线
 *
 * 使用 ItemDecoration 代替在布局中添加分割线 View 的优势：
 * 1. 减少 View 数量，提升性能
 * 2. 不参与布局测量，更高效
 * 3. 可以灵活控制显示逻辑（如最后一项不显示）
 */
class CommentDividerDecoration(
    context: Context,
    @ColorInt dividerColor: Int = ContextCompat.getColor(context, R.color.divider_color),
    private val dividerHeight: Float = 0.5f,
    private val marginStart: Float = 16f,
    private val marginEnd: Float = 16f,
    private val showLastDivider: Boolean = false
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = dividerColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val density = context.resources.displayMetrics.density
    private val dividerHeightPx = (dividerHeight * density).toInt()
    private val marginStartPx = (marginStart * density).toInt()
    private val marginEndPx = (marginEnd * density).toInt()

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0

        // 最后一项不添加间距（除非明确要求显示）
        if (position < itemCount - 1 || showLastDivider) {
            outRect.bottom = dividerHeightPx
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        val itemCount = parent.adapter?.itemCount ?: 0

        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            // 最后一项不绘制分割线（除非明确要求显示）
            if (position >= itemCount - 1 && !showLastDivider) continue

            val left = parent.paddingLeft + marginStartPx
            val right = parent.width - parent.paddingRight - marginEndPx
            val top = child.bottom + (child.layoutParams as RecyclerView.LayoutParams).bottomMargin
            val bottom = top + dividerHeightPx

            c.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        }
    }

    companion object {
        /**
         * 创建默认样式的分割线
         */
        fun create(context: Context): CommentDividerDecoration {
            return CommentDividerDecoration(context)
        }

        /**
         * 创建自定义样式的分割线
         */
        fun create(
            context: Context,
            @ColorInt color: Int,
            heightDp: Float = 0.5f,
            marginStartDp: Float = 16f,
            marginEndDp: Float = 16f
        ): CommentDividerDecoration {
            return CommentDividerDecoration(
                context = context,
                dividerColor = color,
                dividerHeight = heightDp,
                marginStart = marginStartDp,
                marginEnd = marginEndDp
            )
        }
    }
}