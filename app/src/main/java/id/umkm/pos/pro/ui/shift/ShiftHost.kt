package id.umkm.pos.pro.ui.shift

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import id.umkm.pos.pro.ui.LocalAppContainer
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.asTextOrNull
import id.umkm.pos.pro.ui.theme.PosSpacing

@Composable
fun ShiftHost(
    modifier: Modifier = Modifier,
    onManageStaff: () -> Unit = {},
    snackbarBottomPadding: Dp = 96.dp,
    content: @Composable () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModelFactory = remember(container) {
        viewModelFactory {
            initializer {
                ShiftViewModel(
                    shiftRepository = container.shiftRepository,
                    staffRepository = container.staffRepository,
                )
            }
        }
    }
    val viewModel: ShiftViewModel = viewModel(factory = viewModelFactory)
    val state by viewModel.state.collectAsState()
    var pendingQuickOpen by remember { mutableStateOf<(() -> Unit)?>(null) }
    val actions = remember(viewModel) {
        val base = ShiftActions.from(viewModel)
        ShiftActions(
            onSelectStaff = base.onSelectStaff,
            onStartingCashChange = base.onStartingCashChange,
            onActualEndingCashChange = base.onActualEndingCashChange,
            onSelectNextStaff = base.onSelectNextStaff,
            onNextStartingCashChange = base.onNextStartingCashChange,
            onVarianceAcknowledgedChange = base.onVarianceAcknowledgedChange,
            onOpenShift = base.onOpenShift,
            onCloseShift = base.onCloseShift,
            onSwitchShift = base.onSwitchShift,
            onSwitchOperator = base.onSwitchOperator,
            onDismissForm = {
                pendingQuickOpen = null
                base.onDismissForm()
            },
            onDismissHistory = base.onDismissHistory,
            onRetryHistory = base.onRetryHistory,
            onMessageConsumed = base.onMessageConsumed,
            onOpenFullForm = base.onOpenFullForm,
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingCartClear by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isCartGuardVisible by remember { mutableStateOf(false) }
    val launcher = remember(viewModel) {
        object : ShiftLauncher {
            override fun openShift(cartItemCount: Int, onClearCart: () -> Unit) {
                pendingQuickOpen = null
                if (cartItemCount > 0 && viewModel.state.value.activeShift != null) {
                    pendingCartClear = onClearCart
                    isCartGuardVisible = true
                } else {
                    viewModel.onOpenShiftForm()
                }
            }

            override fun openQuickShift(onOpened: () -> Unit) {
                if (viewModel.state.value.activeShift != null) {
                    pendingQuickOpen = null
                    viewModel.onOpenShiftForm()
                } else {
                    pendingQuickOpen = onOpened
                    viewModel.onQuickOpenShiftForm()
                }
            }

            override fun openHistory() {
                pendingQuickOpen = null
                viewModel.onOpenHistory()
            }
        }
    }
    val messageText = state.message?.asTextOrNull()
    LaunchedEffect(state.message) {
        if (messageText == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(messageText)
        actions.onMessageConsumed()
    }
    LaunchedEffect(state.lastOpenedShiftId) {
        if (state.lastOpenedShiftId == null) return@LaunchedEffect
        val callback = pendingQuickOpen
        pendingQuickOpen = null
        callback?.invoke()
        viewModel.onOpenedShiftConsumed()
    }
    LaunchedEffect(state.mode) {
        if (state.mode == ShiftSheetMode.HIDDEN && state.lastOpenedShiftId == null) {
            pendingQuickOpen = null
        }
    }
    CompositionLocalProvider(LocalShiftLauncher provides launcher) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = PosSpacing.md,
                        end = PosSpacing.md,
                        bottom = snackbarBottomPadding,
                    ),
            )
            if (state.mode == ShiftSheetMode.SHIFT_FORM) {
                SwitchCashierSheet(
                    state = state,
                    actions = actions,
                    onManageStaff = {
                        pendingQuickOpen = null
                        viewModel.onDismissForm()
                        onManageStaff()
                    },
                )
            }
            if (state.mode == ShiftSheetMode.HISTORY) {
                ShiftHistorySheet(state = state, actions = actions)
            }
            if (isCartGuardVisible) {
                ShiftCartGuardDialog(
                    onConfirm = {
                        pendingCartClear?.invoke()
                        pendingCartClear = null
                        isCartGuardVisible = false
                        viewModel.onOpenShiftForm()
                    },
                    onDismiss = {
                        pendingCartClear = null
                        isCartGuardVisible = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ShiftCartGuardDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.shiftCartGuardTitle) },
        text = { Text(strings.shiftCartGuardMessage) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = strings.shiftCartGuardConfirm,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = strings.actionCancel)
            }
        },
    )
}
