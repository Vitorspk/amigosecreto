package activity.amigosecreto.util

import android.view.View
import android.view.ViewStub
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StateViewHelperTest {

    private lateinit var stubLoading: ViewStub
    private lateinit var stubEmpty: ViewStub
    private lateinit var loadingView: View
    private lateinit var emptyView: View
    private lateinit var contentView: View
    private lateinit var helper: StateViewHelper

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        loadingView = View(context)
        emptyView = View(context)
        contentView = View(context)

        stubLoading = mock(ViewStub::class.java)
        stubEmpty = mock(ViewStub::class.java)
        `when`(stubLoading.inflate()).thenReturn(loadingView)
        `when`(stubEmpty.inflate()).thenReturn(emptyView)

        helper = StateViewHelper(stubLoading, stubEmpty, contentView)
    }

    // ── showLoading ──────────────────────────────────────────────────────────

    @Test
    fun `showLoading inflates loading view on first call`() {
        helper.showLoading()
        assertEquals(View.VISIBLE, loadingView.visibility)
    }

    @Test
    fun `showLoading hides content view`() {
        helper.showLoading()
        assertEquals(View.GONE, contentView.visibility)
    }

    @Test
    fun `showLoading hides empty view when already inflated`() {
        helper.showEmpty()   // inflate emptyView
        helper.showLoading()
        assertEquals(View.GONE, emptyView.visibility)
    }

    // ── showEmpty ────────────────────────────────────────────────────────────

    @Test
    fun `showEmpty inflates empty view on first call`() {
        helper.showEmpty()
        assertEquals(View.VISIBLE, emptyView.visibility)
    }

    @Test
    fun `showEmpty hides content view`() {
        helper.showEmpty()
        assertEquals(View.GONE, contentView.visibility)
    }

    @Test
    fun `showEmpty hides loading view when already inflated`() {
        helper.showLoading()  // inflate loadingView
        helper.showEmpty()
        assertEquals(View.GONE, loadingView.visibility)
    }

    // ── showContent ──────────────────────────────────────────────────────────

    @Test
    fun `showContent shows content view`() {
        helper.showContent()
        assertEquals(View.VISIBLE, contentView.visibility)
    }

    @Test
    fun `showContent hides loading view when already inflated`() {
        helper.showLoading()
        helper.showContent()
        assertEquals(View.GONE, loadingView.visibility)
    }

    @Test
    fun `showContent hides empty view when already inflated`() {
        helper.showEmpty()
        helper.showContent()
        assertEquals(View.GONE, emptyView.visibility)
    }

    // ── State transitions ────────────────────────────────────────────────────

    @Test
    fun `loading then empty then content shows only content`() {
        helper.showLoading()
        helper.showEmpty()
        helper.showContent()

        assertEquals(View.VISIBLE, contentView.visibility)
        assertEquals(View.GONE, loadingView.visibility)
        assertEquals(View.GONE, emptyView.visibility)
    }

    @Test
    fun `content then loading shows only loading`() {
        helper.showContent()
        helper.showLoading()

        assertEquals(View.GONE, contentView.visibility)
        assertEquals(View.VISIBLE, loadingView.visibility)
    }

    // ── Construction ─────────────────────────────────────────────────────────

    @Test
    fun `instance is created with valid arguments`() {
        assertNotNull(helper)
    }
}
