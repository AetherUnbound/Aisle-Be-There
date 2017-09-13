package com.aether.aislebethere

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.ListView
import android.widget.RelativeLayout
import com.aether.aislebethere.SectionAdapter

class HeaderListView : RelativeLayout {

    private var mListView: InternalListView? = null
    private var mAdapter: SectionAdapter? = null
    private var mHeader: RelativeLayout? = null
    private var mHeaderConvertView: View? = null
    private var mScrollView: FrameLayout? = null
    private var mExternalOnScrollListener: AbsListView.OnScrollListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        mListView = InternalListView(getContext(), attrs)
        val listParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        listParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        mListView!!.layoutParams = listParams
        mListView!!.setOnScrollListener(HeaderListViewOnScrollListener())
        mListView!!.isVerticalScrollBarEnabled = false
        mListView!!.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (mAdapter != null) {
                mAdapter!!.onItemClick(parent, view, position, id)
            }
        }
        addView(mListView)

        mHeader = RelativeLayout(getContext())
        val headerParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        headerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        mHeader!!.layoutParams = headerParams
        mHeader!!.gravity = Gravity.BOTTOM
        addView(mHeader)

        // The list view's scroll bar can be hidden by the header, so we display our own scroll bar instead
        val scrollBarDrawable = resources.getDrawable(R.drawable.scrollbar_handle_holo_light)
        mScrollView = FrameLayout(getContext())
        val scrollParams = RelativeLayout.LayoutParams(scrollBarDrawable.intrinsicWidth, RelativeLayout.LayoutParams.MATCH_PARENT)
        scrollParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        scrollParams.rightMargin = dpToPx(2f).toInt()
        mScrollView!!.layoutParams = scrollParams

        val scrollIndicator = ImageView(context)
        scrollIndicator.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
        scrollIndicator.setImageDrawable(scrollBarDrawable)
        scrollIndicator.scaleType = ScaleType.FIT_XY
        mScrollView!!.addView(scrollIndicator)
        mScrollView!!.visibility = View.INVISIBLE

        addView(mScrollView)
    }

    fun setAdapter(adapter: SectionAdapter) {
        mAdapter = adapter
        mListView!!.adapter = adapter
    }

    fun setOnScrollListener(l: AbsListView.OnScrollListener) {
        mExternalOnScrollListener = l
    }

    private inner class HeaderListViewOnScrollListener : AbsListView.OnScrollListener {

        private var previousFirstVisibleItem = -1
        private var direction = 0
        private var actualSection = 0
        private var scrollingStart = false
        private var doneMeasuring = false
        private var lastResetSection = -1
        private var nextH: Int = 0
        private var prevH: Int = 0
        private var previous: View? = null
        private var next: View? = null
        private val fadeOut = AlphaAnimation(1f, 0f)
        private var noHeaderUpToHeader = false
        private var didScroll = false

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
            if (mExternalOnScrollListener != null) {
                mExternalOnScrollListener!!.onScrollStateChanged(view, scrollState)
            }
            didScroll = true
        }

        override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
            var firstVisibleItem = firstVisibleItem
            if (mExternalOnScrollListener != null) {
                mExternalOnScrollListener!!.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount)
            }

            if (!didScroll) {
                return
            }

            firstVisibleItem -= mListView!!.headerViewsCount
            if (firstVisibleItem < 0) {
                mHeader!!.removeAllViews()
                return
            }

            updateScrollBar()
            if (visibleItemCount > 0 && firstVisibleItem == 0 && mHeader!!.getChildAt(0) == null) {
                addSectionHeader(0)
                lastResetSection = 0
            }

            val realFirstVisibleItem = getRealFirstVisibleItem(firstVisibleItem, visibleItemCount)
            if (totalItemCount > 0 && previousFirstVisibleItem != realFirstVisibleItem) {
                direction = realFirstVisibleItem - previousFirstVisibleItem

                actualSection = mAdapter!!.getSection(realFirstVisibleItem)

                val currIsHeader = mAdapter!!.isSectionHeader(realFirstVisibleItem)
                val prevHasHeader = mAdapter!!.hasSectionHeaderView(actualSection - 1)
                val nextHasHeader = mAdapter!!.hasSectionHeaderView(actualSection + 1)
                val currHasHeader = mAdapter!!.hasSectionHeaderView(actualSection)
                val currIsLast = mAdapter!!.getRowInSection(realFirstVisibleItem) === mAdapter!!.numberOfRows(actualSection) - 1
                val prevHasRows = mAdapter!!.numberOfRows(actualSection - 1) > 0
                val currIsFirst = mAdapter!!.getRowInSection(realFirstVisibleItem) === 0

                val needScrolling = currIsFirst && !currHasHeader && prevHasHeader && realFirstVisibleItem != firstVisibleItem
                val needNoHeaderUpToHeader = currIsLast && currHasHeader && !nextHasHeader && realFirstVisibleItem == firstVisibleItem && Math.abs(mListView!!.getChildAt(0).top) >= mListView!!.getChildAt(0).height / 2

                noHeaderUpToHeader = false
                if (currIsHeader && !prevHasHeader && firstVisibleItem >= 0) {
                    resetHeader(if (direction < 0) actualSection - 1 else actualSection)
                } else if (currIsHeader && firstVisibleItem > 0 || needScrolling) {
                    if (!prevHasRows) {
                        resetHeader(actualSection - 1)
                    }
                    startScrolling()
                } else if (needNoHeaderUpToHeader) {
                    noHeaderUpToHeader = true
                } else if (lastResetSection != actualSection) {
                    resetHeader(actualSection)
                }

                previousFirstVisibleItem = realFirstVisibleItem
            }

            if (scrollingStart) {
                val scrolled = if (realFirstVisibleItem >= firstVisibleItem) mListView!!.getChildAt(realFirstVisibleItem - firstVisibleItem).top else 0

                if (!doneMeasuring) {
                    setMeasurements(realFirstVisibleItem, firstVisibleItem)
                }

                val headerH = if (doneMeasuring) (prevH - nextH) * direction * Math.abs(scrolled) / (if (direction < 0) nextH else prevH) + (if (direction > 0) nextH else prevH) else 0

                mHeader!!.scrollTo(0, -Math.min(0, scrolled - headerH))
                if (doneMeasuring && headerH != mHeader!!.layoutParams.height) {
                    val p = (if (direction < 0) next!!.layoutParams else previous!!.layoutParams) as RelativeLayout.LayoutParams
                    p.topMargin = headerH - p.height
                    mHeader!!.layoutParams.height = headerH
                    mHeader!!.requestLayout()
                }
            }

            if (noHeaderUpToHeader) {
                if (lastResetSection != actualSection) {
                    addSectionHeader(actualSection)
                    lastResetSection = actualSection + 1
                }
                mHeader!!.scrollTo(0, mHeader!!.layoutParams.height - (mListView!!.getChildAt(0).height + mListView!!.getChildAt(0).top))
            }
        }

        private fun startScrolling() {
            scrollingStart = true
            doneMeasuring = false
            lastResetSection = -1
        }

        private fun resetHeader(section: Int) {
            scrollingStart = false
            addSectionHeader(section)
            mHeader!!.requestLayout()
            lastResetSection = section
        }

        private fun setMeasurements(realFirstVisibleItem: Int, firstVisibleItem: Int) {

            if (direction > 0) {
                nextH = if (realFirstVisibleItem >= firstVisibleItem) mListView!!.getChildAt(realFirstVisibleItem - firstVisibleItem).measuredHeight else 0
            }

            previous = mHeader!!.getChildAt(0)
            prevH = if (previous != null) previous!!.measuredHeight else mHeader!!.height

            if (direction < 0) {
                if (lastResetSection != actualSection - 1) {
                    addSectionHeader(Math.max(0, actualSection - 1))
                    next = mHeader!!.getChildAt(0)
                }
                nextH = if (mHeader!!.childCount > 0) mHeader!!.getChildAt(0).measuredHeight else 0
                mHeader!!.scrollTo(0, prevH)
            }
            doneMeasuring = previous != null && prevH > 0 && nextH > 0
        }

        private fun updateScrollBar() {
            if (mHeader != null && mListView != null && mScrollView != null) {
                val offset = mListView!!.computeVerticalScrollOffset()
                val range = mListView!!.computeVerticalScrollRange()
                val extent = mListView!!.computeVerticalScrollExtent()
                mScrollView!!.visibility = if (extent >= range) View.INVISIBLE else View.VISIBLE
                if (extent >= range) {
                    return
                }
                val top = if (range == 0) mListView!!.height else mListView!!.height * offset / range
                val bottom = if (range == 0) 0 else mListView!!.height - mListView!!.height * (offset + extent) / range
                mScrollView!!.setPadding(0, top, 0, bottom)
                fadeOut.reset()
                fadeOut.fillBefore = true
                fadeOut.fillAfter = true
                fadeOut.startOffset = FADE_DELAY.toLong()
                fadeOut.duration = FADE_DURATION.toLong()
                mScrollView!!.clearAnimation()
                mScrollView!!.startAnimation(fadeOut)
            }
        }

        private fun addSectionHeader(actualSection: Int) {
            val previousHeader = mHeader!!.getChildAt(0)
            if (previousHeader != null) {
                mHeader!!.removeViewAt(0)
            }

            if (mAdapter!!.hasSectionHeaderView(actualSection)) {
                mHeaderConvertView = mAdapter!!.getSectionHeaderView(actualSection, mHeaderConvertView, mHeader)
                mHeaderConvertView!!.layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

                mHeaderConvertView!!.measure(View.MeasureSpec.makeMeasureSpec(mHeader!!.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

                mHeader!!.layoutParams.height = mHeaderConvertView!!.measuredHeight
                mHeaderConvertView!!.scrollTo(0, 0)
                mHeader!!.scrollTo(0, 0)
                mHeader!!.addView(mHeaderConvertView, 0)
            } else {
                mHeader!!.layoutParams.height = 0
                mHeader!!.scrollTo(0, 0)
            }

            mScrollView!!.bringToFront()
        }

        private fun getRealFirstVisibleItem(firstVisibleItem: Int, visibleItemCount: Int): Int {
            if (visibleItemCount == 0) {
                return -1
            }
            var relativeIndex = 0
            var totalHeight = mListView!!.getChildAt(0).top
            relativeIndex = 0
            while (relativeIndex < visibleItemCount && totalHeight < mHeader!!.height) {
                totalHeight += mListView!!.getChildAt(relativeIndex).height
                relativeIndex++
            }
            val realFVI = Math.max(firstVisibleItem, firstVisibleItem + relativeIndex - 1)
            return realFVI
        }
    }

    val listView: ListView?
        get() = mListView

    fun addHeaderView(v: View) {
        mListView!!.addHeaderView(v)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    protected inner class InternalListView(context: Context, attrs: AttributeSet?) : ListView(context, attrs) {

        public override fun computeVerticalScrollExtent(): Int {
            return super.computeVerticalScrollExtent()
        }

        public override fun computeVerticalScrollOffset(): Int {
            return super.computeVerticalScrollOffset()
        }

        public override fun computeVerticalScrollRange(): Int {
            return super.computeVerticalScrollRange()
        }
    }

    companion object {

        // TODO: Handle listViews with fast scroll
        // TODO: See if there are methods to dispatch to mListView

        private val FADE_DELAY = 1000
        private val FADE_DURATION = 2000
    }
}
