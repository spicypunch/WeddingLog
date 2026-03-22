package kr.jm.weddingledger.feature.money.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kr.jm.weddingledger.feature.money.data.dto.CategoryRowDto

class MoneyRepositoryOrderingTest {

    @Test
    fun `sortedForDisplay keeps 기타 at the end`() {
        val categories = listOf(
            CategoryRowDto(
                id = "cat-1",
                weddingId = "wedding-1",
                name = "웨딩홀",
                isDefault = true,
            ),
            CategoryRowDto(
                id = "cat-2",
                weddingId = "wedding-1",
                name = "기타",
                isDefault = true,
            ),
            CategoryRowDto(
                id = "cat-3",
                weddingId = "wedding-1",
                name = "식사/모임",
                isDefault = false,
            ),
        )

        val sorted = categories.sortedForDisplay()

        assertEquals(
            listOf("웨딩홀", "식사/모임", "기타"),
            sorted.map { it.name },
        )
    }
}
