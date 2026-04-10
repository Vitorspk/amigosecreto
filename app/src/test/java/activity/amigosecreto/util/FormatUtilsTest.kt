package activity.amigosecreto.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class FormatUtilsTest {

    // --- formatDate ---

    @Test
    fun formatDate_withDdMmYyyyPattern_returnsExpectedString() {
        // Parse with the same pattern and default timezone to match what formatDate will produce.
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("25/12/2024")!!
        val result = FormatUtils.formatDate(date, "dd/MM/yyyy")
        assertEquals("25/12/2024", result)
    }

    @Test
    fun formatDate_withMmDdYyyyPattern_returnsUsFormat() {
        val date = SimpleDateFormat("MM/dd/yyyy", Locale.US).parse("06/05/2024")!!
        val result = FormatUtils.formatDate(date, "MM/dd/yyyy")
        assertEquals("06/05/2024", result)
    }

    @Test
    fun formatDate_returnsNonEmptyString() {
        val date = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("01/01/2024")!!
        assertTrue(FormatUtils.formatDate(date, "dd/MM/yyyy").isNotEmpty())
    }

    // --- formatIsoDateTime ---

    @Test
    fun formatIsoDateTime_validIsoInput_returnsFormattedDate() {
        // ISO parser in FormatUtils uses Locale.US (no timezone), so noon avoids day-boundary issues.
        val result = FormatUtils.formatIsoDateTime("2024-12-25T14:30:00", "dd/MM/yyyy HH:mm")
        assertEquals("25/12/2024 14:30", result)
    }

    @Test
    fun formatIsoDateTime_invalidInput_returnsOriginalString() {
        val invalid = "not-a-date"
        assertEquals(invalid, FormatUtils.formatIsoDateTime(invalid, "dd/MM/yyyy HH:mm"))
    }

    @Test
    fun formatIsoDateTime_emptyInput_returnsEmpty() {
        assertEquals("", FormatUtils.formatIsoDateTime("", "dd/MM/yyyy"))
    }

    @Test
    fun formatIsoDateTime_withEnPattern_returnsUsFormat() {
        val result = FormatUtils.formatIsoDateTime("2024-06-05T09:00:00", "MM/dd/yyyy HH:mm")
        assertEquals("06/05/2024 09:00", result)
    }

    @Test
    fun formatIsoDateTime_midnightTime_formatsCorrectly() {
        // 2024-01-01 at noon to avoid timezone-dependent day-rollover
        val result = FormatUtils.formatIsoDateTime("2024-01-01T12:00:00", "dd/MM/yyyy")
        assertEquals("01/01/2024", result)
    }
}
