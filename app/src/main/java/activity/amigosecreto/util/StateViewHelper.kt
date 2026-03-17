package activity.amigosecreto.util

import android.view.View
import android.view.ViewStub

/**
 * Manages three mutually-exclusive content states for a list screen:
 * loading, empty, and content (list visible).
 *
 * Uses [ViewStub] so the loading/empty layouts are only inflated on first use.
 *
 * Usage:
 * ```
 * val stateHelper = StateViewHelper(stubLoading, stubEmpty, listView)
 * stateHelper.showLoading()
 * stateHelper.showEmpty()
 * stateHelper.showContent()
 * ```
 */
class StateViewHelper(
    private val stubLoading: ViewStub,
    private val stubEmpty: ViewStub,
    private val contentView: View
) {
    init {
        requireNotNull(stubLoading) { "stubLoading must not be null" }
        requireNotNull(stubEmpty) { "stubEmpty must not be null" }
    }

    private var loadingView: View? = null
    private var emptyView: View? = null

    fun showLoading() {
        if (loadingView == null) loadingView = stubLoading.inflate()
        loadingView?.visibility = View.VISIBLE
        emptyView?.visibility = View.GONE
        contentView.visibility = View.GONE
    }

    fun showEmpty() {
        loadingView?.visibility = View.GONE
        if (emptyView == null) emptyView = stubEmpty.inflate()
        emptyView?.visibility = View.VISIBLE
        contentView.visibility = View.GONE
    }

    fun showContent() {
        loadingView?.visibility = View.GONE
        emptyView?.visibility = View.GONE
        contentView.visibility = View.VISIBLE
    }
}
