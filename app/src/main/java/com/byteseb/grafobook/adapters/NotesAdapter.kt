package com.byteseb.grafobook.adapters

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.byteseb.grafobook.R
import com.byteseb.grafobook.activities.MainInterface
import com.byteseb.grafobook.activities.NoteActivity
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.byteseb.grafobook.utils.HtmlUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class NotesAdapter(
    val context: Context,
    val interactable: Boolean = true,
    val fragmentManager: FragmentManager?,
    val viewModel: NoteViewModel? = null,
    val listener: MainInterface? = null
) :
    ListAdapter<Note, NotesAdapter.NoteHolder>(NoteDiffUtilCallback()) {

    var selecting: Boolean = false
    val selection = ArrayList<Note>()

    fun setSelection(selectedNotes: ArrayList<Int>){
        runBlocking {
            selection.clear()
            for(noteIndex in selectedNotes){
                selection.add(currentList[noteIndex])
                notifyItemChanged(noteIndex)
            }
        }
    }

    fun selectAll() {
        selection.clear()
        selecting = true
        for (note in currentList) {
            selection.add(note)
            notifyItemChanged(currentList.indexOf(note))
        }
        listener?.onSelectionUpdated(selecting, selection)
    }

    fun clearSelection() {
        for (item in selection) {
            notifyItemChanged(currentList.indexOf(item))
        }
        selection.clear()
        selecting = false
        listener?.onSelectionUpdated(selecting, selection)

    }

    fun deleteSelection() {
        for(i in selection.size -1 downTo 0){
            val note = selection[i]
            deleteNote(note)
        }
        selection.clear()
        selecting = false
        listener?.onSelectionUpdated(selecting, selection)
    }

    fun getDuplicated(note: Note): Note{
        return Note(
            name = note.name,
            color = note.color,
            favorite = note.favorite,
            lastDate = note.lastDate,
            creationDate = System.currentTimeMillis(),
            tags = note.tags,
            reminder = note.reminder,
            content = note.content
        )
    }

    fun duplicateSelection() {
        for (note in selection) {
            insertNote(getDuplicated(note))
        }
        clearSelection()
    }

    fun deleteNote(note: Note) {
        viewModel?.delete(note)
    }

    fun insertNote(note: Note){
        viewModel?.insert(note)
    }

    fun strokeColor(): Int {
        return ContextCompat.getColor(context, R.color.selectionColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder =
        NoteHolder(LayoutInflater.from(parent.context).inflate(R.layout.note_card, parent, false))

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView = view.findViewById<MaterialCardView>(R.id.noteCard)
        val nameView = view.findViewById<TextView>(R.id.noteName)
        val favView = view.findViewById<CheckBox>(R.id.editFav)
        val remindView = view.findViewById<ImageView>(R.id.noteReminder)
        val dateIcon = view.findViewById<ImageView>(R.id.noteDateIcon)
        val dateView = view.findViewById<TextView>(R.id.noteDate)
        val tagsRecycler = view.findViewById<RecyclerView>(R.id.noteTags)
        val contentView = view.findViewById<TextView>(R.id.noteContent)

        fun bind(note: Note) {

            //Card
            refreshSelected(note, cardView)
            cardView.setOnClickListener {
                if (interactable) {
                    if (selecting) {
                        //Selection mode is enabled. Toggle the selection on this note
                        if (selection.contains(note)) {
                            selection.remove(note)
                            if(selection.isEmpty()){
                                selecting = false
                                clearSelection()
                            }
                        } else {
                            selection.add(note)
                        }
                        refreshSelected(note, cardView)
                    } else {
                        //Open Note Activity
                        val intent = Intent(context, NoteActivity::class.java)
                        intent.putExtras(getBundle(note))
                        context.startActivity(intent)
                    }
                }
            }

            cardView.setOnLongClickListener {
                if (!selecting) {
                    selecting = true
                    selection.add(note)
                    listener?.onSelectionUpdated(selecting, selection)
                    refreshSelected(note, cardView)
                }
                true
            }

            if (note.color != null) {
                cardView.setCardBackgroundColor(Color.parseColor(note.color))
            } else {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.cardBackground
                    )
                )
            }

            val backColor = cardView.cardBackgroundColor.defaultColor

            //Name
            nameView.setText(note.name)
            tintText(nameView, backColor)

            //Reminder
            tintImg(remindView, backColor)
            if (note.reminder == -1L) {
                remindView.visibility = View.GONE
            } else {
                remindView.visibility = View.VISIBLE
                if (note.reminder < System.currentTimeMillis()) {
                    remindView.setImageResource(R.drawable.ic_baseline_alarm_on_24)
                } else {
                    remindView.setImageResource(R.drawable.ic_round_notifications_active_24)
                }
            }

            //Favorite
            favView.isChecked = note.favorite
            favView.isEnabled = interactable
            tintCheckbox(favView, backColor)
            favView.setOnClickListener {
                //Toggle Note's favorite
                runBlocking {
                    note.favorite = favView.isChecked
                    viewModel?.update(note)
                }
            }

            //Date
            tintImg(dateIcon, backColor)
            val formattedDate =
                SimpleDateFormat(context.getString(R.string.year_month_date_hour_minute), Locale.getDefault()).format(note.lastDate)
            dateView.setText(formattedDate)
            tintText(dateView, backColor)

            //Tags
            if (note.tags.isEmpty()) {
                tagsRecycler.visibility = View.GONE
            } else {
                tagsRecycler.visibility = View.VISIBLE
                val manager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

                val adapter: TagsAdapter
                if (fragmentManager != null) {
                    if (note.color != null) {
                        adapter = TagsAdapter(
                            fragmentManager = fragmentManager,
                            color = Color.parseColor(note.color)
                        )
                    } else {
                        adapter = TagsAdapter(fragmentManager = fragmentManager)
                    }
                    adapter.submitList(note.tags)
                    tagsRecycler.layoutManager = manager
                    tagsRecycler.adapter = adapter
                }
            }

            //Content
            tintText(contentView, backColor)
            if (note.content.isNotEmpty()) {
                contentView.text = HtmlUtils.fromHtml(note.content)
            } else {
                contentView.text = ""
            }
        }

        fun refreshSelected(note: Note, cardView: MaterialCardView) {
            if(interactable){
                if (selection.contains(note)) {
                    //Has been selected, show it with a stroke
                    cardView.strokeColor = strokeColor()
                } else {
                    cardView.strokeColor = ContextCompat.getColor(context, R.color.transparent)
                }
            }
            listener?.onSelectionUpdated(selecting, selection)
        }

        fun tintText(view: TextView, color: Int) {
            val finalColor: Int

            if (isDarkColor(color)) {
                finalColor = Color.WHITE
            } else {
                finalColor = Color.BLACK
            }

            view.setTextColor(finalColor)
        }

        fun tintImg(view: ImageView, color: Int) {

            val finalColor: Int

            if (isDarkColor(color)) {
                finalColor = Color.WHITE
            } else {
                finalColor = Color.BLACK
            }

            view.setColorFilter(finalColor)
        }

        fun tintCheckbox(view: CheckBox, color: Int) {

            val finalColor: Int


            if (isDarkColor(color)) {
                finalColor = Color.WHITE
            } else {
                finalColor = Color.BLACK
            }

            view.buttonTintList = ColorStateList.valueOf(finalColor)
        }

        fun isDarkColor(color: Int): Boolean {
            return ColorUtils.calculateLuminance(color) < 0.25f
        }

        fun getBundle(note: Note): Bundle {
            val bundle = Bundle()
            bundle.putInt("id", note.id)
            bundle.putString("name", note.name)
            bundle.putString("content", note.content)
            bundle.putString("color", note.color)
            bundle.putBoolean("favorite", note.favorite)
            bundle.putLong("creationDate", note.creationDate)
            bundle.putLong("lastDate", note.lastDate)
            bundle.putLong("reminder", note.reminder)
            val tags = ArrayList<String>()
            tags.addAll(note.tags)
            bundle.putStringArrayList("tags", tags)
            return bundle
        }
    }
}

class NoteDiffUtilCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
        areItemsTheSame(oldItem, newItem)
}