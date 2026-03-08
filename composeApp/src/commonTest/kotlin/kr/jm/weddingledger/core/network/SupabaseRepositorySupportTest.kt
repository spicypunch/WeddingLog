package kr.jm.weddingledger.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupabaseRepositorySupportTest {

    @Test
    fun `cache returns stored wedding id for same user`() {
        val cache = WeddingIdCache()

        cache.put(
            userId = "user-1",
            weddingId = "wedding-1",
        )

        assertEquals("wedding-1", cache.get("user-1"))
    }

    @Test
    fun `cache does not reuse wedding id after account switch`() {
        val cache = WeddingIdCache()

        cache.put(
            userId = "user-1",
            weddingId = "wedding-1",
        )

        assertNull(cache.get("user-2"))
    }

    @Test
    fun `cache updates stored wedding id when same user is overwritten`() {
        val cache = WeddingIdCache()

        cache.put(
            userId = "user-1",
            weddingId = "wedding-1",
        )
        cache.put(
            userId = "user-1",
            weddingId = "wedding-2",
        )

        assertEquals("wedding-2", cache.get("user-1"))
    }
}
