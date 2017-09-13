package com.aether.aislebethere

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText

/**
 * Created by Liet-Kynes on 9/12/2017.
 */

class ShoppingSectionAdapter constructor(context: Context): SectionAdapter()  {
    val context = context

    override fun numberOfSections(): Int {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return 4
    }

    override fun numberOfRows(section: Int): Int {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return 40
    }

    override fun getRowView(section: Int, row: Int, convertView: View?, parent: ViewGroup): View {
        var returnView = convertView
        if (convertView == null) {
//            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//            returnView = inflater.inflate(R.layout.list_item, parent) as LinearLayout
            View.inflate(context, R.layout.list_item, parent)
        }
        returnView!!.findViewById<EditText>(R.id.listEditText).setText("Hello")
        return returnView
    }

    override fun getRowItem(section: Int, row: Int): Any {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return 0
    }

}