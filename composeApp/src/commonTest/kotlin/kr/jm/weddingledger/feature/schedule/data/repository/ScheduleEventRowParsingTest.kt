package kr.jm.weddingledger.feature.schedule.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ScheduleEventRowParsingTest {

    @Test
    fun `schedule row parser ignores unknown fields while keeping known values`() {
        val row = buildJsonObject {
            put("id", "event-1")
            put("wedding_id", "wedding-1")
            put("created_by", "user-1")
            put("title", "드레스 피팅")
            put("event_date", "2026-11-01")
            put("start_time", "13:30")
            put("location", "강남")
            put("repeat_rule", "WEEKLY")
            put("memo", "리허설 포함")
            put("created_at", "2026-03-28T12:00:00Z")
            put("updated_at", "2026-03-28T12:30:00Z")
            put("unexpected_column", "should_be_ignored")
        }

        val parsed = row.toScheduleEventRowDtoOrNull()

        assertNotNull(parsed)
        assertEquals("event-1", parsed.id)
        assertEquals("wedding-1", parsed.weddingId)
        assertEquals("user-1", parsed.createdBy)
        assertEquals("드레스 피팅", parsed.title)
        assertEquals("2026-11-01", parsed.eventDate)
        assertEquals("13:30", parsed.startTime)
        assertEquals("강남", parsed.location)
        assertEquals("WEEKLY", parsed.repeatRule)
        assertEquals("리허설 포함", parsed.memo)
    }
}
