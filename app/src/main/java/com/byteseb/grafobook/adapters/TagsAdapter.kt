package com.byteseb.grafobook.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.alpha
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.byteseb.grafobook.R
import com.byteseb.grafobook.interfaces.TagInterface
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.byteseb.grafobook.sheets.TaggedSheet
import com.google.android.material.chip.Chip
import kotlin.math.roundToInt

class TagsAdapter(
    val fragmentManager: FragmentManager,
    val canClose: Boolean = false,
    val tagInterface: TagInterface? = null,
    val color: Int? = null

) : ListAdapter<String, TagsAdapter.TagHolder>(TagDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder =
        TagHolder(LayoutInflater.from(parent.context).inflate(R.layout.chip_layout, parent, false))

    override fun onBindViewHolder(holder: TagHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class TagHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chip = view.findViewById<Chip>(R.id.filterChip)

        fun bind(string: String, position: Int) {
            chip.text = string
            chip.isCloseIconVisible = canClose
            chip.isCheckable = false

            if (color != null) {
                val color = darkenColor(color, 0.6f)
                chip.chipBackgroundColor = ColorStateList.valueOf(color)
                if(isDarkColor(color)){
                    chip.setTextColor(Color.WHITE)
                }
                else{
                    chip.setTextColor(Color.BLACK)
                }
            }

            chip.setOnSingleClickListener {
                val sheet = TaggedSheet()
                val bundle = Bundle()
                bundle.putString("tag", string)
                sheet.arguments = bundle
                sheet.show(fragmentManager, "taggedSheet")
            }

            chip.setOnCloseIconClickListener {
                tagInterface?.onCloseTag(string)
            }
        }

        fun darkenColor(color: Int, factor: Float): Int {
            val a = Color.alpha(color)
            val r = (Color.red(color) * factor).roundToInt()
            val g = (Color.green(color) * factor).roundToInt()
            val b = (Color.blue(color) * factor).roundToInt()
            return Color.argb(
                a,
                r.coerceAtMost(255),
                g.coerceAtMost(255),
                b.coerceAtMost(255)
            )
        }

        fun isDarkColor(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < 0.25f
        }
    }
}

class TagDiffUtilCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
        areItemsTheSame(oldItem, newItem)
}