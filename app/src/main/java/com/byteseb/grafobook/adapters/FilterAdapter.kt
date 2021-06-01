package com.byteseb.grafobook.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.MainInterface
import com.byteseb.grafobook.models.Filter
import com.google.android.material.chip.Chip

class FilterAdapter(val listener: MainInterface) : ListAdapter<Filter, FilterAdapter.FilterHolder>(FilterDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterHolder =
        FilterHolder(LayoutInflater.from(parent.context).inflate(R.layout.chip_layout, parent, false))

    override fun onBindViewHolder(holder: FilterHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FilterHolder(view: View) : RecyclerView.ViewHolder(view) {
        val chip = view.findViewById<Chip>(R.id.filterChip)

        fun bind(filter: Filter){
            chip.text = filter.text
            chip.isChecked = filter.checked

            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if(buttonView.isPressed){
                    filter.checked = isChecked
                    if(filter.favFilter){
                        //If it is the favorites filter, call onFavFilterChecked
                        listener.onFavFilterChecked(filter.checked)
                    }
                    else{
                        //If it is a tag filter, call the corresponding function
                        listener.onTagFilterChecked(filter.text, filter.checked)
                    }
                }
            }
        }
    }
}

class FilterDiffUtilCallback : DiffUtil.ItemCallback<Filter>(){
    override fun areItemsTheSame(oldItem: Filter, newItem: Filter): Boolean =
        oldItem.text == newItem.text

    override fun areContentsTheSame(oldItem: Filter, newItem: Filter): Boolean =
        areItemsTheSame(oldItem, newItem)
}