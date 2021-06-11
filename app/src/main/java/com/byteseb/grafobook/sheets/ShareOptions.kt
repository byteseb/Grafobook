package com.byteseb.grafobook.sheets

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.byteseb.grafobook.R
import com.byteseb.grafobook.listeners.setOnSingleClickListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.share_options.*
import java.lang.ClassCastException

class ShareOptions: BottomSheetDialogFragment() {

    enum class Options{
        GRAFO_FILE, HTML_FILE
    }

    interface ShareOptionsInterface{
        fun onOptionSelected(option: Int)
    }

    private var callback: ShareOptionsInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callback = context as ShareOptionsInterface
        }
        catch (ex: ClassCastException){
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.share_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        htmlCard.setOnSingleClickListener {
            callback?.onOptionSelected(Options.HTML_FILE.ordinal)
            dismiss()
        }
        grafoCard.setOnSingleClickListener {
            callback?.onOptionSelected(Options.GRAFO_FILE.ordinal)
            dismiss()
        }
    }
}