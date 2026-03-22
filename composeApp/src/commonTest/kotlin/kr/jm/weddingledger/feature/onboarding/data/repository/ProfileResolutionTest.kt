package kr.jm.weddingledger.feature.onboarding.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kr.jm.weddingledger.feature.onboarding.data.dto.ProfileRowDto

class ProfileResolutionTest {

    @Test
    fun `shared wedding date is used when my profile date is empty`() {
        val mine = profileRow(id = "me", weddingId = "wedding-1")
        val partner = profileRow(
            id = "partner",
            weddingId = "wedding-1",
            weddingDate = "2026-10-18",
        )

        val resolved = mine.toResolvedDomain(listOf(mine, partner))

        assertEquals("2026-10-18", resolved.weddingDate?.toString())
        assertFalse(resolved.isWeddingDateUndecided)
    }

    @Test
    fun `shared undecided state is used when no one has a wedding date`() {
        val mine = profileRow(id = "me", weddingId = "wedding-1")
        val partner = profileRow(
            id = "partner",
            weddingId = "wedding-1",
            isWeddingDateUndecided = true,
        )

        val resolved = mine.toResolvedDomain(listOf(mine, partner))

        assertNull(resolved.weddingDate)
        assertTrue(resolved.isWeddingDateUndecided)
    }

    @Test
    fun `explicit wedding date wins over undecided flags`() {
        val mine = profileRow(
            id = "me",
            weddingId = "wedding-1",
            weddingDate = "2026-11-09",
            isWeddingDateUndecided = true,
        )
        val partner = profileRow(
            id = "partner",
            weddingId = "wedding-1",
            isWeddingDateUndecided = true,
        )

        val resolved = mine.toResolvedDomain(listOf(mine, partner))

        assertEquals("2026-11-09", resolved.weddingDate?.toString())
        assertFalse(resolved.isWeddingDateUndecided)
    }
}

private fun profileRow(
    id: String,
    weddingId: String,
    weddingDate: String? = null,
    isWeddingDateUndecided: Boolean = false,
): ProfileRowDto = ProfileRowDto(
    id = id,
    weddingId = weddingId,
    weddingDate = weddingDate,
    isWeddingDateUndecided = isWeddingDateUndecided,
)
