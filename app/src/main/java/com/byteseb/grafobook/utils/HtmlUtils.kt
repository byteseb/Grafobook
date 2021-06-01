package com.byteseb.grafobook.utils

import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.Spanned

class HtmlUtils {
    companion object{
        @SuppressWarnings
        fun fromHtml(html: String) : Spanned{
            if(html.isEmpty()){
                return SpannableString("")
            }
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            }
            else{
                return Html.fromHtml(html)
            }
        }

        @SuppressWarnings
        fun toHtml(spanned: Spanned): String{
            if(spanned.isEmpty()){
                return ""
            }
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                return Html.toHtml(spanned, Html.FROM_HTML_MODE_LEGACY)
            }
            else{
                return Html.toHtml(spanned)
            }
        }
    }
}