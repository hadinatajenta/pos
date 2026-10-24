package id.umkm.pos.pro.ui.more

sealed interface ExpectedCashState {
    object Loading : ExpectedCashState
    data class Ready(val amount: Long) : ExpectedCashState
    object Error : ExpectedCashState
}

object MoreHubTestTags {
    const val HUB = "more_hub"
    const val HERO = "more_hero"
    const val CASH_VALUE = "more_cash_value"
    const val CASH_ERROR = "more_cash_error"
    const val CASH_RETRY = "more_cash_retry"
    const val SHIFT_ACTION = "more_shift_action"
    const val HISTORY_ACTION = "more_history_action"
    const val SETTINGS_ROW = "more_settings_row"
    const val DEBUG_SECTION = "more_debug_section"
    const val EMPTY_DETAIL = "more_empty_detail"
}
