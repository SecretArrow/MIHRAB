package id.or.kemenag.yasintahlil.core.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class HijriCalcTest {
    @Test fun ramadan1447SekitarFeb2026() {
        // 1 Ramadan 1447H = 19 Feb 2026 (Sidang Isbat). Tabular boleh selisih ±2 hari.
        val c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).apply {
            set(2026, 1, 19, 12, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        val (d, m, y) = HijriCalc.from(c)
        assertEquals(1447, y)
        assertTrue("Bulan Ramadan (9) ±1, got $m/$d", m in 8..10)
    }
}
