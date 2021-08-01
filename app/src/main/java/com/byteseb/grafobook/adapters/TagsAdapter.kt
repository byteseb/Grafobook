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
import com.byteseb.grafobook.utils.ColorUtils.Companion.darkenColor
import com.byteseb.grafobook.utils.ColorUtils.Companion.isDarkColor
import com.google.android.material.chip.Chip
import kotlin.math.roundToInt

class TagsAdapter(
    val fragmentManager: FragmentManager,
    val canClose: Boolean = false,
    val tagInterface: TagInterface? = null,
    var color: Int? = null,
    val context: Context

) : ListAdapter<String, TagsAdapter.TagHolder>(TagDiffUtilCallback()) {

    fun forceRefresh() {
        for (note in currentList) {
            notifyItemChanged(currentList.indexOf(note))
        }
    }

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

            refreshColor()
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

        fun refreshColor() {

            val backColor: Int
            if(color != null){
                backColor =
             darkenColor(color!!, 0.6f)}
            else{
                backColor = context.getColor(R.color.chipBack)
            }
            chip.chipBackgroundColor = ColorStateList.valueOf(backColor)
            if (isDarkColor(backColor)) {
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setTextColor(Color.BLACK)
            }
        }
    }
}

class TagDiffUtilCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
        areItemsTheSame(oldItem, newItem)
}