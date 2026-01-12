package de.meply.meply.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android:view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import de.meply.meply.R
import de.meply.meply.data.profile.InviteCode
import java.text.SimpleDateFormat
import java.util.*

class InviteCodeAdapter(
    private val codes: List<InviteCode>
) : RecyclerView.Adapter<InviteCodeAdapter.InviteCodeViewHolder>() {

    inner class InviteCodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusIcon: TextView = view.findViewById(R.id.code_status_icon)
        val codeText: TextView = view.findViewById(R.id.code_text)
        val codeMeta: TextView = view.findViewById(R.id.code_meta)
        val copyButton: ImageView = view.findViewById(R.id.code_copy_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteCodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite_code, parent, false)
        return InviteCodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteCodeViewHolder, position: Int) {
        val code = codes[position]

        // Status icon
        if (code.used) {
            holder.statusIcon.text = "✓"
            holder.statusIcon.setTextColor(holder.itemView.context.getColor(R.color.success))
            holder.codeText.alpha = 0.5f
            holder.codeText.paintFlags = holder.codeText.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.statusIcon.text = "○"
            holder.statusIcon.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
            holder.codeText.alpha = 1.0f
            holder.codeText.paintFlags = holder.codeText.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // Code text
        holder.codeText.text = code.code

        // Meta info
        if (code.used && !code.usedAt.isNullOrEmpty()) {
            val formattedDate = formatDate(code.usedAt)
            holder.codeMeta.text = "Verwendet $formattedDate"
            holder.codeMeta.visibility = View.VISIBLE
        } else if (code.used) {
            holder.codeMeta.text = "Verwendet"
            holder.codeMeta.visibility = View.VISIBLE
        } else {
            holder.codeMeta.text = "Verfügbar"
            holder.codeMeta.visibility = View.VISIBLE
        }

        // Copy button
        if (code.used) {
            holder.copyButton.visibility = View.GONE
        } else {
            holder.copyButton.visibility = View.VISIBLE
            holder.copyButton.setOnClickListener {
                copyToClipboard(holder.itemView.context, code.code)
            }
        }
    }

    override fun getItemCount(): Int = codes.size

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Einladungscode", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Code kopiert: $text", Toast.LENGTH_SHORT).show()
    }

    private fun formatDate(timestamp: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val date = format.parse(timestamp) ?: return timestamp

            val displayFormat = SimpleDateFormat("am dd.MM.yyyy", Locale.GERMAN)
            displayFormat.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }
}
