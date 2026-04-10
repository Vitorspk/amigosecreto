package activity.amigosecreto.util

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class FormatUtilsTest {

    // --- formatDate ---

    @Test
    fun formatDate_withDdMmYyyyPattern_returnsExpectedString() {
        // Parse a known date via the same pattern to avoid timezone ambiguity
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
        val result = FormatUtils.formatDate(date, "dd/MM/yyyy")
        assertTrue(result.isNotEmpty())
    }

    // --- formatIsoDateTime ---

    @Test
    fun formatIsoDateTime_validIsoInput_returnsFormattedDate() {
        val result = FormatUtils.formatIsoDateTime("2024-12-25T14:30:00", "dd/MM/yyyy HH:mm")
        assertEquals("25/12/2024 14:30", result)
    }

    @Test
    fun formatIsoDateTime_invalidInput_returnsOriginalString() {
        val invalid = "not-a-date"
        val result = FormatUtils.formatIsoDateTime(invalid, "dd/MM/yyyy HH:mm")
        assertEquals(invalid, result)
    }

    @Test
    fun formatIsoDateTime_emptyInput_returnsEmpty() {
        val result = FormatUtils.formatIsoDateTime("", "dd/MM/yyyy")
        assertEquals("", result)
    }

    @Test
    fun formatIsoDateTime_withEnPattern_returnsUsFormat() {
        val result = FormatUtils.formatIsoDateTime("2024-06-05T09:00:00", "MM/dd/yyyy HH:mm")
        assertEquals("06/05/2024 09:00", result)
    }

    @Test
    fun formatIsoDateTime_midnightTime_formatsCorrectly() {
        val result = FormatUtils.formatIsoDateTime("2024-01-01T00:00:00", "dd/MM/yyyy HH:mm")
        assertEquals("01/01/2024 00:00", result)
    }
}
