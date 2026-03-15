package kr.jm.weddingledger.feature.money.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.jm.weddingledger.core.network.WeddingIdResolver
import kr.jm.weddingledger.core.network.logSupabaseRepositoryThrowable
import kr.jm.weddingledger.feature.money.data.dto.BudgetItemInsertDto
import kr.jm.weddingledger.feature.money.data.dto.BudgetItemRowDto
import kr.jm.weddingledger.feature.money.data.dto.CategoryInsertDto
import kr.jm.weddingledger.feature.money.data.dto.CategoryRowDto
import kr.jm.weddingledger.feature.money.data.dto.ExpenseInsertDto
import kr.jm.weddingledger.feature.money.data.dto.ExpenseRowDto
import kr.jm.weddingledger.feature.money.domain.model.BudgetItem
import kr.jm.weddingledger.feature.money.domain.model.BudgetStageType
import kr.jm.weddingledger.feature.money.domain.model.Expense
import kr.jm.weddingledger.feature.money.domain.model.MoneyCategory
import kr.jm.weddingledger.feature.money.domain.model.MoneyResult
import kr.jm.weddingledger.feature.money.domain.model.MoneySnapshot
import kr.jm.weddingledger.feature.money.domain.model.NewBudgetItem
import kr.jm.weddingledger.feature.money.domain.model.NewExpense
import kr.jm.weddingledger.feature.money.domain.model.Payer
import kr.jm.weddingledger.feature.money.domain.model.PaymentMethod
import kr.jm.weddingledger.feature.money.domain.repository.MoneyRepository

class MoneyRepositoryImpl(
    private val supabaseClient: SupabaseClient,
) : MoneyRepository {

    private val weddingIdResolver = WeddingIdResolver(supabaseClient)

    override suspend fun fetchSnapshot(): MoneyResult<MoneySnapshot> {
        return try {
            val weddingId = weddingIdResolver.resolve()
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
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "돈 데이터를 불러오지 못했습니다."))
        }
    }

    override suspend fun addBudgetItem(item: NewBudgetItem): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("budget_items")
                .insert(
                    BudgetItemInsertDto(
                        weddingId = weddingId,
                        categoryId = item.categoryId,
                        name = item.name.trim(),
                        targetAmount = item.targetAmount,
                        stage = item.stage.name,
                        dueDate = item.dueDate?.toString(),
                        memo = item.memo.trimToNull(),
                    )
                )
            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 저장에 실패했습니다."))
        }
    }

    override suspend fun addCategory(name: String): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("categories")
                .insert(
                    CategoryInsertDto(
                        weddingId = weddingId,
                        name = name.trim(),
                        isDefault = false,
                    )
                )
            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "카테고리 저장에 실패했습니다."))
        }
    }

    override suspend fun addExpense(expense: NewExpense): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
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
                        vendorName = expense.vendorName?.trim()?.takeIf { it.isNotEmpty() },
                        memo = expense.memo?.trim()?.takeIf { it.isNotEmpty() },
                    )
                )
            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "지출 저장에 실패했습니다."))
        }
    }

    override suspend fun updateBudgetItem(id: String, item: NewBudgetItem): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("category_id", JsonPrimitive(item.categoryId))
                put("name", JsonPrimitive(item.name.trim()))
                put("target_amount", JsonPrimitive(item.targetAmount))
                put("stage", JsonPrimitive(item.stage.name))
                if (item.dueDate == null) put("due_date", JsonNull) else put("due_date", JsonPrimitive(item.dueDate.toString()))

                val memo = item.memo.trimToNull()
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
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 수정에 실패했습니다."))
        }
    }

    override suspend fun updateCategory(id: String, name: String): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("name", JsonPrimitive(name.trim()))
            }

            supabaseClient
                .from("categories")
                .update(payload) {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "카테고리 수정에 실패했습니다."))
        }
    }

    override suspend fun updateExpense(id: String, expense: NewExpense): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            val payload = buildJsonObject {
                put("budget_item_id", JsonPrimitive(expense.budgetItemId))
                put("amount", JsonPrimitive(expense.amount))
                put("date", JsonPrimitive(expense.date.toString()))
                put("payment_method", JsonPrimitive(expense.paymentMethod.toDbValue()))
                put("payer", JsonPrimitive(expense.payer.toDbValue()))

                val vendorName = expense.vendorName?.trim()?.takeIf { it.isNotEmpty() }
                if (vendorName == null) put("vendor_name", JsonNull) else put("vendor_name", JsonPrimitive(vendorName))

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
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "지출 수정에 실패했습니다."))
        }
    }

    override suspend fun deleteBudgetItem(id: String): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("budget_items")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
            MoneyResult.Error(t.toUserFriendlyMessage(defaultMessage = "예산 항목 삭제에 실패했습니다."))
        }
    }

    override suspend fun deleteExpense(id: String): MoneyResult<Unit> {
        return try {
            val weddingId = weddingIdResolver.resolve()
            supabaseClient
                .from("expenses")
                .delete {
                    filter { eq("id", id) }
                    filter { eq("wedding_id", weddingId) }
                }

            MoneyResult.Success(Unit)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            logSupabaseRepositoryThrowable(FEATURE_NAME, t)
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

}

private fun CategoryRowDto.toDomain(): MoneyCategory = MoneyCategory(
    id = id,
    name = name,
    isDefault = isDefault,
)

private fun BudgetItemRowDto.toDomain(): BudgetItem {
    val decodedMemo = decodeBudgetItemMemo(name = name, rawMemo = memo)
    val storedStage = BudgetStageType.fromStoredValueOrNull(stage) ?: decodedMemo.stage
    val storedDueDate = dueDate.parseLocalDateOrNull() ?: decodedMemo.dueDate
    return BudgetItem(
        id = id,
        categoryId = categoryId,
        name = name,
        targetAmount = targetAmount,
        stage = storedStage,
        dueDate = storedDueDate,
        memo = decodedMemo.memo,
    )
}

private fun ExpenseRowDto.toDomainOrNull(): Expense? {
    val date = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    return Expense(
        id = id,
        budgetItemId = budgetItemId,
        amount = amount,
        date = date,
        paymentMethod = paymentMethod.parsePaymentMethodOrNull() ?: return null,
        payer = payer.parsePayerOrNull() ?: return null,
        vendorName = vendorName,
        memo = memo,
        imageUrl = imageUrl,
    )
}

private fun String.parsePaymentMethodOrNull(): PaymentMethod? = when (trim().lowercase()) {
    "card" -> PaymentMethod.Card
    "cash" -> PaymentMethod.Cash
    "joint_account", "jointaccount", "shared_account", "sharedaccount" -> PaymentMethod.JointAccount
    else -> null
}

private fun String.parsePayerOrNull(): Payer? = when (trim().lowercase()) {
    "groom" -> Payer.Groom
    "bride" -> Payer.Bride
    "joint", "shared" -> Payer.Joint
    else -> null
}

private fun PaymentMethod.toDbValue(): String = when (this) {
    PaymentMethod.Card -> "CARD"
    PaymentMethod.Cash -> "CASH"
    PaymentMethod.JointAccount -> "JOINT_ACCOUNT"
}
private fun Payer.toDbValue(): String = name.uppercase()

private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.parseLocalDateOrNull(): LocalDate? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

private data class DecodedBudgetMemo(
    val stage: BudgetStageType,
    val dueDate: LocalDate?,
    val memo: String?,
)

private fun decodeBudgetItemMemo(
    name: String,
    rawMemo: String?,
): DecodedBudgetMemo {
    if (rawMemo.isNullOrBlank()) {
        return DecodedBudgetMemo(
            stage = BudgetStageType.inferFromName(name),
            dueDate = null,
            memo = null,
        )
    }

    val lines = rawMemo.lines()
    var storedStage: BudgetStageType? = null
    var storedDueDate: LocalDate? = null
    var markerLineCount = 0

    while (markerLineCount < lines.size) {
        val line = lines[markerLineCount].trim()
        when {
            line.startsWith(BUDGET_STAGE_PREFIX) && line.endsWith(BUDGET_STAGE_SUFFIX) -> {
                storedStage = line
                    .removePrefix(BUDGET_STAGE_PREFIX)
                    .removeSuffix(BUDGET_STAGE_SUFFIX)
                    .let(BudgetStageType::fromStoredValueOrNull)
                markerLineCount += 1
            }

            line.startsWith(BUDGET_DUE_DATE_PREFIX) && line.endsWith(BUDGET_DUE_DATE_SUFFIX) -> {
                storedDueDate = line
                    .removePrefix(BUDGET_DUE_DATE_PREFIX)
                    .removeSuffix(BUDGET_DUE_DATE_SUFFIX)
                    .let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
                markerLineCount += 1
            }

            else -> break
        }
    }

    if (markerLineCount > 0) {
        val userMemo = lines
            .drop(markerLineCount)
            .joinToString(separator = "\n")
            .trim()
            .takeIf { it.isNotEmpty() }

        return DecodedBudgetMemo(
            stage = storedStage ?: BudgetStageType.inferFromName(name),
            dueDate = storedDueDate,
            memo = userMemo,
        )
    }

    return DecodedBudgetMemo(
        stage = BudgetStageType.inferFromName(name),
        dueDate = null,
        memo = rawMemo,
    )
}

private fun Throwable.toUserFriendlyMessage(defaultMessage: String): String {
    return when (this) {
        is RestException -> defaultMessage
        else -> defaultMessage
    }
}

private const val FEATURE_NAME: String = "Money"
private const val BUDGET_STAGE_PREFIX: String = "[[wl-stage:"
private const val BUDGET_STAGE_SUFFIX: String = "]]"
private const val BUDGET_DUE_DATE_PREFIX: String = "[[wl-due-date:"
private const val BUDGET_DUE_DATE_SUFFIX: String = "]]"
