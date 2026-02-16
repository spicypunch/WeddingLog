package kr.jm.weddingledger.feature.money.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.feature.money.data.dto.BudgetItemInsertDto
import kr.jm.weddingledger.feature.money.data.dto.BudgetItemRowDto
import kr.jm.weddingledger.feature.money.data.dto.CategoryInsertDto
import kr.jm.weddingledger.feature.money.data.dto.CategoryRowDto
import kr.jm.weddingledger.feature.money.data.dto.ExpenseInsertDto
import kr.jm.weddingledger.feature.money.data.dto.ExpenseRowDto
import kr.jm.weddingledger.feature.money.domain.model.BudgetItem
import kr.jm.weddingledger.feature.money.domain.model.Expense
import kr.jm.weddingledger.feature.money.domain.model.MoneyCategory
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository
import kr.jm.weddingledger.feature.schedule.data.dto.ProfileInsertDto
import kr.jm.weddingledger.feature.schedule.data.dto.ProfileRowDto

class MoneyRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : MoneyRepository {

    private var cachedWeddingId: String? = null

    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> {
        return try {
            val weddingId = getWeddingId()
            val categories = getOrSeedCategories(weddingId)

            val budgetItemRows = supabaseClient
                .from("budget_items")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<BudgetItemRowDto>()

            val expenseRows = supabaseClient
                .from("expenses")
                .select {
                    filter { eq("wedding_id", weddingId) }
                    order(column = "date", order = Order.DESCENDING)
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ExpenseRowDto>()

            MoneyResult.Success(
                MoneySnapshot(
                    categories = categories.map { it.toDomain() },
                    budgetItems = budgetItemRows.map { it.toDomain() },
                    expenses = expenseRows.mapNotNull { it.toDomainOrNull() },
                )
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "돈 데이터를 불러오지 못했습니다."))
        }
    }

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("budget_items")
                .insert(
                    BudgetItemInsertDto(
                        weddingId = weddingId,
                        categoryId = item.categoryId,
                        name = item.name.trim(),
                        targetAmount = item.targetAmount,
                        memo = item.memo?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 저장에 실패했습니다."))
        }
    }

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("expenses")
                .insert(
                    ExpenseInsertDto(
                        weddingId = weddingId,
                        budgetItemId = expense.budgetItemId,
                        amount = expense.amount,
                        date = expense.date.toString(),
                        paymentMethod = expense.paymentMethod.toDbValue(),
                        payer = expense.payer.toDbValue(),
                        memo = expense.memo?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "지출 저장에 실패했습니다."))
        }
    }

    override suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            val payload = buildJsonObject {
                put("category_id", JsonPrimitive(item.categoryId))
                put("name", JsonPrimitive(item.name.trim()))
                put("target_amount", JsonPrimitive(item.targetAmount))

                val memo = item.memo?.trim()?.takeIf { it.isNotEmpty() }
                if (memo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(memo))
            }

            supabaseClient
                .from("budget_items")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 수정에 실패했습니다."))
        }
    }

    override suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            val payload = buildJsonObject {
                put("budget_item_id", JsonPrimitive(expense.budgetItemId))
                put("amount", JsonPrimitive(expense.amount))
                put("date", JsonPrimitive(expense.date.toString()))
                put("payment_method", JsonPrimitive(expense.paymentMethod.toDbValue()))
                put("payer", JsonPrimitive(expense.payer.toDbValue()))

                val memo = expense.memo?.trim()?.takeIf { it.isNotEmpty() }
                if (memo == null) put("memo", JsonNull) else put("memo", JsonPrimitive(memo))
            }

            supabaseClient
                .from("expenses")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "지출 수정에 실패했습니다."))
        }
    }

    override suspend fun deleteBudgetItem(id: String): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("budget_items")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 삭제에 실패했습니다."))
        }
    }

    override suspend fun deleteExpense(id: String): MoneyResult<Unit> {
        return try {
            val weddingId = getWeddingId()
            supabaseClient
                .from("expenses")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseThrowable(t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "지출 삭제에 실패했습니다."))
        }
    }

    private suspend fun getOrSeedCategories(weddingId: String): List<CategoryRowDto> {
        val rows = supabaseClient
            .from("categories")
            .select {
                filter { eq("wedding_id", weddingId) }
                order(column = "created_at", order = Order.ASCENDING)
            }
            .decodeList<CategoryRowDto>()

        if (rows.isNotEmpty()) return rows

        val defaults = defaultCategoryNames().map { name ->
            CategoryInsertDto(
                weddingId = weddingId,
                name = name,
                isDefault = true,
            )
        }

        // Best-effort: 중복 생성 가능성이 있어도 UX 차원에서 seed를 시도합니다.
        runCatching {
            supabaseClient
                .from("categories")
                .insert(defaults)
        }

        return supabaseClient
            .from("categories")
            .select {
                filter { eq("wedding_id", weddingId) }
                order(column = "created_at", order = Order.ASCENDING)
            }
            .decodeList()
    }

    private fun defaultCategoryNames(): List<String> = listOf(
        "웨딩홀",
        "스드메",
        "예물/예단",
        "혼수/가전",
        "신혼여행",
        "기타",
    )

    private suspend fun getWeddingId(): String {
        cachedWeddingId?.let { return it }

        val user = supabaseClient.auth.currentUserOrNull()
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val userId = user.id

        val profile = try {
            val result = supabaseClient
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(count = 1)
                }
            result.decodeList<ProfileRowDto>().firstOrNull()
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
                // ignore
            }
        }

        cachedWeddingId = weddingId
        return weddingId
    }
}

private fun CategoryRowDto.toDomain(): MoneyCategory = MoneyCategory(
    id = id,
    name = name,
    isDefault = isDefault,
)

private fun BudgetItemRowDto.toDomain(): BudgetItem = BudgetItem(
    id = id,
    categoryId = categoryId,
    name = name,
    targetAmount = targetAmount,
    memo = memo,
)

private fun ExpenseRowDto.toDomainOrNull(): Expense? {
    val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return Expense(
        id = id,
        budgetItemId = budgetItemId,
        amount = amount,
        date = date,
        paymentMethod = paymentMethod.parsePaymentMethodOrNull() ?: return null,
        payer = payer.parsePayerOrNull() ?: return null,
        memo = memo,
        imageUrl = imageUrl,
    )
}

private fun String.parsePaymentMethodOrNull(): PaymentMethod? = when (trim().lowercase()) {
    "card" -> PaymentMethod.Card
    "cash" -> PaymentMethod.Cash
    else -> null
}

private fun String.parsePayerOrNull(): Payer? = when (trim().lowercase()) {
    "groom" -> Payer.Groom
    "bride" -> Payer.Bride
    else -> null
}

private fun PaymentMethod.toDbValue(): String = name.uppercase()
private fun Payer.toDbValue(): String = name.uppercase()

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private fun logSupabaseThrowable(t: Throwable) {
    // Avoid printing t.message (RestException may include URL + request headers).
    when (t) {
        is RestException -> println("Supabase error=${t.error} description=${t.description} status=${t.statusCode}")
        else -> println("Money error=${t::class.simpleName}")
    }
}
