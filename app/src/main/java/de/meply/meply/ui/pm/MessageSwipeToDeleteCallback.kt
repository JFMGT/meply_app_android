package de.meply.meply.ui.pm

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.messages.Message

/**
 * ItemTouchHelper callback for swipe-to-delete messages.
 * Only allows swiping own messages that haven't been deleted yet.
 */
class MessageSwipeToDeleteCallback(
    private val context: Context,
    private val currentUserId: String?,
    private val getMessageAt: (Int) -> Message?,
    private val onSwipedAction: (position: Int, message: Message) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val deleteIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val backgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.error)
    }
    private val cornerRadius = context.resources.getDimension(R.dimen.card_corner_radius)

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return 0

        val message = getMessageAt(position) ?: return 0

        // Only allow swiping own messages that haven't been deleted
        val isOwnMessage = message.author.documentId == currentUserId
        val isNotDeleted = !message.deletedByUser

        return if (isOwnMessage && isNotDeleted) {
            super.getSwipeDirs(recyclerView, viewHolder)
        } else {
            0 // Disable swipe for other messages
        }
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            val message = getMessageAt(position)
            if (message != null) {
                onSwipedAction(position, message)
            }
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top

        // Draw red background with rounded corners
        val background = RectF(
            itemView.right.toFloat() + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )
        c.drawRoundRect(background, cornerRadius, cornerRadius, backgroundPaint)

        // Draw delete icon
        deleteIcon?.let { icon ->
            val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + icon.intrinsicHeight
            val iconRight = itemView.right - iconMargin
            val iconLeft = iconRight - icon.intrinsicWidth

            if (iconLeft > itemView.right + dX) {
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.colorFilter = PorterDuffColorFilter(
                    ContextCompat.getColor(context, android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
                icon.draw(c)
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.5f
    }
}
