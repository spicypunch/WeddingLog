package kr.jm.weddingledger.feature.tools.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kr.jm.weddingledger.feature.tools.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.tools.data.dto.ProfileRowDto
import kr.jm.weddingledger.feature.tools.data.dto.ValuesSurveyAnswerInsertDto
import kr.jm.weddingledger.feature.tools.data.dto.ValuesSurveyAnswerRowDto
import kr.jm.weddingledger.feature.tools.data.dto.VenueFavoriteInsertDto
import kr.jm.weddingledger.feature.tools.data.dto.VenueFavoriteRowDto
import kr.jm.weddingledger.feature.tools.data.dto.VenueRowDto
import kr.jm.weddingledger.feature.tools.domain.model.ToolsResult
import kr.jm.weddingledger.feature.tools.domain.model.ValuesSurveyAnswer
import kr.jm.weddingledger.feature.tools.domain.model.Venue
import kr.jm.weddingledger.feature.tools.domain.repository.ToolsRepository

class ToolsRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : ToolsRepository {

    private var cachedUserId: String? = null
    private var cachedWeddingId: String? = null

    override suspend fun fetchVenues(): ToolsResult<List<Venue>> {
        return try {
            val fallbackSeeds = seedVenues()
            val rows = supabaseClient
                .from("venues")
                .select {
                    order(column = "rating", order = Order.DESCENDING)
                    order(column = "price_per_guest", order = Order.ASCENDING)
                }
                .decodeList<VenueRowDto>()

            val venues = rows.map { row ->
                row.toDomain(
                    fallbackAccent = fallbackSeeds[row.id.hashCode().absoluteMod(fallbackSeeds.size)].accentHex,
                )
            }
            ToolsResult.Success(venues)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("venues")) {
                ToolsResult.Error("도구 기능 초기 설정이 필요합니다. venues 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ToolsResult.Error(t.toUserFriendlyMessage(defaultMessage = "웨딩홀 목록을 불러오지 못했습니다."))
            }
        }
    }

    override suspend fun fetchFavoriteVenueIds(): ToolsResult<Set<String>> {
        return try {
            val weddingId = getWeddingId()
            val rows = supabaseClient
                .from("venue_favorites")
                .select {
                    filter { eq("wedding_id", weddingId) }
                }
                .decodeList<VenueFavoriteRowDto>()

            ToolsResult.Success(rows.map { it.venueId }.toSet())
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("venue_favorites")) {
                ToolsResult.Error("도구 기능 초기 설정이 필요합니다. venue_favorites 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ToolsResult.Error(t.toUserFriendlyMessage(defaultMessage = "찜 목록을 불러오지 못했습니다."))
            }
        }
    }

    override suspend fun setVenueFavorite(venueId: String, favorite: Boolean): ToolsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            if (favorite) {
                try {
                    supabaseClient
                        .from("venue_favorites")
                        .insert(
                            VenueFavoriteInsertDto(
                                weddingId = weddingId,
                                venueId = venueId,
                            )
                        )
                } catch (e: RestException) {
                    if (!e.isUniqueViolation()) throw e
                }
            } else {
                supabaseClient
                    .from("venue_favorites")
                    .delete {
                        filter { eq("wedding_id", weddingId) }
                        filter { eq("venue_id", venueId) }
                    }
            }
            ToolsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("venue_favorites")) {
                ToolsResult.Error("도구 기능 초기 설정이 필요합니다. venue_favorites 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ToolsResult.Error(t.toUserFriendlyMessage(defaultMessage = "찜 상태 변경에 실패했습니다."))
            }
        }
    }

    override suspend fun fetchSurveyAnswers(): ToolsResult<List<ValuesSurveyAnswer>> {
        return try {
            val weddingId = getWeddingId()
            val rows = supabaseClient
                .from("values_survey_answers")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<ValuesSurveyAnswerRowDto>()

            ToolsResult.Success(rows.map { it.toDomain() })
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("values_survey_answers")) {
                ToolsResult.Error("도구 기능 초기 설정이 필요합니다. values_survey_answers 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ToolsResult.Error(t.toUserFriendlyMessage(defaultMessage = "저장된 설문 결과를 불러오지 못했습니다."))
            }
        }
    }

    override suspend fun saveSurveyAnswers(answers: List<ValuesSurveyAnswer>): ToolsResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("values_survey_answers")
                .delete {
                    filter { eq("wedding_id", weddingId) }
                }

            if (answers.isNotEmpty()) {
                supabaseClient
                    .from("values_survey_answers")
                    .insert(
                        answers.map { answer ->
                            ValuesSurveyAnswerInsertDto(
                                weddingId = weddingId,
                                questionId = answer.questionId,
                                optionId = answer.optionId,
                                score = answer.score,
                            )
                        }
                    )
            }

            ToolsResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            if (t is RestException && t.isMissingRelation("values_survey_answers")) {
                ToolsResult.Error("도구 기능 초기 설정이 필요합니다. values_survey_answers 테이블 SQL을 먼저 실행해 주세요.")
            } else {
                ToolsResult.Error(t.toUserFriendlyMessage(defaultMessage = "설문 결과 저장에 실패했습니다."))
            }
        }
    }

    private suspend fun getWeddingId(): String {
        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id
        if (cachedUserId == userId) {
            cachedWeddingId?.let { return it }
        }
        val profile = try {
            supabaseClient
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(count = 1)
                }
                .decodeList<ProfileRowDto>()
                .firstOrNull()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            null
        }

        val weddingId = profile?.weddingId ?: userId

        if (profile == null) {
            try {
                supabaseClient
                    .from("profiles")
                    .insert(
                        ProfileInsertDto(
                            id = userId,
                            email = user.email,
                            weddingId = weddingId,
                        )
                    )
            } catch (_: Throwable) {
                // Ignore: profile row may already be managed elsewhere.
            }
        }

        cachedUserId = userId
        cachedWeddingId = weddingId
        return weddingId
    }
}

private fun VenueRowDto.toDomain(fallbackAccent: Long): Venue {
    return Venue(
        id = id,
        name = name,
        location = location,
        pricePerGuest = pricePerGuest,
        rating = rating,
        capacity = capacity,
        tags = tags,
        accentHex = accentHex.toColorLongOrNull() ?: fallbackAccent,
    )
}

private fun ValuesSurveyAnswerRowDto.toDomain(): ValuesSurveyAnswer {
    return ValuesSurveyAnswer(
        questionId = questionId,
        optionId = optionId,
        score = score,
    )
}

private fun seedVenues(): List<Venue> = listOf(
    Venue(
        id = "venue-1",
        name = "아펠가모 공덕",
        location = "서울 마포구",
        pricePerGuest = 89000,
        rating = 4.8,
        capacity = 220,
        tags = listOf("식대 포함", "교통 편리", "주차 우수"),
        accentHex = 0xFFEF3948,
    ),
    Venue(
        id = "venue-2",
        name = "더채플 앳 청담",
        location = "서울 강남구",
        pricePerGuest = 102000,
        rating = 4.9,
        capacity = 180,
        tags = listOf("프리미엄", "채플식", "포토존"),
        accentHex = 0xFF6366F1,
    ),
    Venue(
        id = "venue-3",
        name = "엘타워 그레이스홀",
        location = "서울 서초구",
        pricePerGuest = 96000,
        rating = 4.6,
        capacity = 260,
        tags = listOf("대규모", "강남권", "연회장 넓음"),
        accentHex = 0xFF10B981,
    ),
    Venue(
        id = "venue-4",
        name = "보테가마지오",
        location = "서울 성동구",
        pricePerGuest = 78000,
        rating = 4.5,
        capacity = 140,
        tags = listOf("가성비", "소규모", "야경"),
        accentHex = 0xFFF97316,
    ),
    Venue(
        id = "venue-5",
        name = "JW 메리어트 플라자",
        location = "서울 중구",
        pricePerGuest = 118000,
        rating = 4.7,
        capacity = 300,
        tags = listOf("호텔 예식", "대형 홀", "브랜드"),
        accentHex = 0xFF8B5CF6,
    ),
)

private fun String?.toColorLongOrNull(): Long? {
    if (this == null) return null
    val cleaned = trim().removePrefix("#")
    val normalized = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return null
    }
    return normalized.toLongOrNull(radix = 16)
}

private fun Int.absoluteMod(divisor: Int): Int {
    if (divisor <= 0) return 0
    val mod = this % divisor
    return if (mod < 0) mod + divisor else mod
}

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private fun RestException.isMissingRelation(@Suppress("UNUSED_PARAMETER") relationName: String): Boolean {
    return postgresCodeOrNull() == POSTGRES_UNDEFINED_TABLE
}

private fun RestException.isUniqueViolation(): Boolean {
    return postgresCodeOrNull() == POSTGRES_UNIQUE_VIOLATION || statusCode == 409
}

private fun RestException.postgresCodeOrNull(): String? {
    return (this as? PostgrestRestException)?.code?.trim()?.uppercase()
}

private fun logSupabaseThrowable(t: Throwable) {
    when (t) {
        is RestException -> println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("Tools error=${t::class.simpleName}")
    }
}

private const val POSTGRES_UNDEFINED_TABLE: String = "42P01"
private const val POSTGRES_UNIQUE_VIOLATION: String = "23505"
