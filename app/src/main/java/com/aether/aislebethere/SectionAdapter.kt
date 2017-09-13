package com.aether.aislebethere

import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.BaseAdapter

abstract class SectionAdapter : BaseAdapter(), OnItemClickListener {

    private var mCount = -1

    abstract fun numberOfSections(): Int

    abstract fun numberOfRows(section: Int): Int

    abstract fun getRowView(section: Int, row: Int, convertView: View?, parent: ViewGroup): View

    abstract fun getRowItem(section: Int, row: Int): Any

    fun hasSectionHeaderView(section: Int): Boolean {
        return false
    }

    fun getSectionHeaderView(section: Int, convertView: View?, parent: ViewGroup?): View? {
        return null
    }

    fun getSectionHeaderItem(section: Int): Any? {
        return null
    }

    val rowViewTypeCount: Int
        get() = 1

    val sectionHeaderViewTypeCount: Int
        get() = 1

    /**
     * Must return a value between 0 and getRowViewTypeCount() (excluded)
     */
    fun getRowItemViewType(section: Int, row: Int): Int {
        return 0
    }

    /**
     * Must return a value between 0 and getSectionHeaderViewTypeCount() (excluded, if > 0)
     */
    fun getSectionHeaderItemViewType(section: Int): Int {
        return 0
    }

    override
            /**
             * Dispatched to call onRowItemClick
             */
    fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        onRowItemClick(parent, view, getSection(position), getRowInSection(position), id)
    }

    fun onRowItemClick(parent: AdapterView<*>, view: View, section: Int, row: Int, id: Long) {

    }

    override
            /**
             * Counts the amount of cells = headers + rows
             */
    fun getCount(): Int {
        if (mCount < 0) {
            mCount = numberOfCellsBeforeSection(numberOfSections())
        }
        return mCount
    }

    override fun isEmpty(): Boolean {
        return count == 0
    }

    override
            /**
             * Dispatched to call getRowItem or getSectionHeaderItem
             */
    fun getItem(position: Int): Any? {
        val section = getSection(position)
        if (isSectionHeader(position)) {
            if (hasSectionHeaderView(section)) {
                return getSectionHeaderItem(section)
            }
            return null
        }
        return getRowItem(section, getRowInSection(position))
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override
            /**
             * Dispatched to call getRowView or getSectionHeaderView
             */
    fun getView(position: Int, convertView: View, parent: ViewGroup): View? {
        val section = getSection(position)
        if (isSectionHeader(position)) {
            if (hasSectionHeaderView(section)) {
                return getSectionHeaderView(section, convertView, parent)
            }
            return null
        }
        return getRowView(section, getRowInSection(position), convertView, parent)
    }

    /**
     * Returns the section number of the indicated cell
     */
    fun getSection(position: Int): Int {
        var section = 0
        var cellCounter = 0
        while (cellCounter <= position && section <= numberOfSections()) {
            cellCounter += numberOfCellsInSection(section)
            section++
        }
        return section - 1
    }

    /**
     * Returns the row index of the indicated cell Should not be call with
     * positions directing to section headers
     */
    fun getRowInSection(position: Int): Int {
        val section = getSection(position)
        val row = position - numberOfCellsBeforeSection(section)
        if (hasSectionHeaderView(section)) {
            return row - 1
        } else {
            return row
        }
    }

    /**
     * Returns true if the cell at this index is a section header
     */
    fun isSectionHeader(position: Int): Boolean {
        val section = getSection(position)
        return hasSectionHeaderView(section) && numberOfCellsBeforeSection(section) == position
    }

    /**
     * Returns the number of cells (= headers + rows) before the indicated
     * section
     */
    protected fun numberOfCellsBeforeSection(section: Int): Int {
        var count = 0
        for (i in 0..Math.min(numberOfSections(), section) - 1) {
            count += numberOfCellsInSection(i)
        }
        return count
    }

    private fun numberOfCellsInSection(section: Int): Int {
        return numberOfRows(section) + if (hasSectionHeaderView(section)) 1 else 0
    }

    override fun notifyDataSetChanged() {
        mCount = numberOfCellsBeforeSection(numberOfSections())
        super.notifyDataSetChanged()
    }

    override fun notifyDataSetInvalidated() {
        mCount = numberOfCellsBeforeSection(numberOfSections())
        super.notifyDataSetInvalidated()
    }

    override
            /**
             * Dispatched to call getRowItemViewType or getSectionHeaderItemViewType
             */
    fun getItemViewType(position: Int): Int {
        val section = getSection(position)
        if (isSectionHeader(position)) {
            return rowViewTypeCount + getSectionHeaderItemViewType(section)
        } else {
            return getRowItemViewType(section, getRowInSection(position))
        }
    }

    override
            /**
             * Dispatched to call getRowViewTypeCount and getSectionHeaderViewTypeCount
             */
    fun getViewTypeCount(): Int {
        return rowViewTypeCount + sectionHeaderViewTypeCount
    }

    override
            /**
             * By default, disables section headers
             */
    fun isEnabled(position: Int): Boolean {
        return (disableHeaders() || !isSectionHeader(position)) && isRowEnabled(getSection(position), getRowInSection(position))
    }

    fun disableHeaders(): Boolean {
        return false
    }

    fun isRowEnabled(section: Int, row: Int): Boolean {
        return true
    }
}
