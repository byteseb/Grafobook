package com.byteseb.grafobook.sheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.byteseb.grafobook.R
import com.byteseb.grafobook.adapters.NotesAdapter
import com.byteseb.grafobook.models.Filter
import com.byteseb.grafobook.models.Note
import com.byteseb.grafobook.room.NoteViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.tagged_sheet.*

class TaggedSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tagged_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val specificTag = arguments?.getString("tag")

        taggedTitle.text = String.format(getString(R.string.notes_containing_tag, specificTag))
        val notesList = ArrayList<Note>()

        val viewModel = ViewModelProvider(this).get(NoteViewModel::class.java)
        viewModel.allNotes.observe(this) {
            for (note in it) {
                for (tag in note.tags) {
                    if (!notesList.contains(note) && note.tags.contains(specificTag)) {
                        notesList.add(note)
                    }
                }
            }
            val adapter = NotesAdapter(requireContext(), clickable = true, fragmentManager = null)
            adapter.maxSelectionSize = -1
            adapter.submitList(notesList)
            taggedRecycler.layoutManager =
                StaggeredGridLayoutManager(
                    resources.getInteger(R.integer.column_count),
                    StaggeredGridLayoutManager.VERTICAL
                )
            taggedRecycler.adapter = adapter
            adapter.notifyDataSetChanged()
        }
    }
}