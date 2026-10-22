package id.umkm.pos.pro.ui.staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.core.model.Staff
import id.umkm.pos.pro.ui.common.EmptyState
import id.umkm.pos.pro.ui.common.LoadingState
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.ScreenHeader
import id.umkm.pos.pro.ui.theme.LocalAppTextStyles
import id.umkm.pos.pro.ui.theme.LocalPosSemanticPalette
import id.umkm.pos.pro.ui.theme.PosSizes
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme

const val STAFF_SCREEN_TEST_TAG = "staff-screen"
const val STAFF_ADD_ACTION_TEST_TAG = "staff-add-action"
const val STAFF_SEARCH_ACTION_TEST_TAG = "staff-search-action"
const val STAFF_SEARCH_FIELD_TEST_TAG = "staff-search-field"
const val STAFF_EDITOR_DIALOG_TEST_TAG = "staff-editor-dialog"
const val STAFF_NAME_FIELD_TEST_TAG = "staff-name-field"
const val STAFF_EDITOR_SAVE_TEST_TAG = "staff-editor-save"
const val STAFF_ROW_TEST_TAG_PREFIX = "staff-row-"

private val STAFF_CONTENT_MAX_WIDTH = 720.dp

@Composable
internal fun StaffScreen(
    state: StaffUiState,
    actions: StaffActions,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val strings = LocalPosStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }
    val successMessage = state.message?.resolve(strings)

    LaunchedEffect(successMessage) {
        if (successMessage == null) return@LaunchedEffect
        snackbarHostState.showSnackbar(successMessage)
        actions.onMessageConsumed()
    }
    LaunchedEffect(state.isSearchVisible) {
        if (state.isSearchVisible) focusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize().testTag(STAFF_SCREEN_TEST_TAG)) {
        Box(
            modifier = Modifier
                .widthIn(max = STAFF_CONTENT_MAX_WIDTH)
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenHeader(
                    title = strings.staffTitle,
                    supportingText = strings.staffSubtitle,
                    onBack = onBack,
                    actions = {
                        IconButton(
                            onClick = actions.onToggleSearch,
                            modifier = Modifier.testTag(STAFF_SEARCH_ACTION_TEST_TAG),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = strings.staffSearchAction,
                            )
                        }
                    },
                )
                if (state.isSearchVisible) {
                    StaffSearchField(
                        state = state,
                        actions = actions,
                        focusRequester = focusRequester,
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        state.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                        !state.hasStaff -> StaffEmptyContent(
                            state = state,
                            actions = actions,
                            modifier = Modifier.fillMaxSize(),
                        )

                        !state.hasVisibleStaff -> EmptyState(
                            iconRes = R.drawable.ic_search_x,
                            title = strings.staffSearchEmptyTitle,
                            supportingText = strings.staffSearchEmptyHint,
                            actionLabel = strings.actionClose,
                            onAction = actions.onClearSearch,
                            modifier = Modifier.fillMaxSize(),
                        )

                        else -> StaffList(
                            state = state,
                            actions = actions,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = actions.onOpenAddEditor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(PosSpacing.md)
                    .testTag(STAFF_ADD_ACTION_TEST_TAG),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = strings.staffAddAction,
                    modifier = Modifier.size(PosSizes.iconMedium),
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = PosSpacing.md, vertical = 96.dp),
        )
        state.editor?.let { editor ->
            StaffEditorDialog(
                editor = editor,
                isSubmitting = state.isSubmitting,
                actions = actions,
            )
        }
        state.pendingDeactivation?.let { staff ->
            StaffDeactivateDialog(
                staff = staff,
                actions = actions,
            )
        }
    }
}

@Composable
private fun StaffSearchField(
    state: StaffUiState,
    actions: StaffActions,
    focusRequester: FocusRequester,
) {
    val strings = LocalPosStrings.current
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = actions.onSearchQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PosSpacing.md)
            .padding(bottom = PosSpacing.xs)
            .focusRequester(focusRequester)
            .testTag(STAFF_SEARCH_FIELD_TEST_TAG),
        placeholder = { Text(strings.staffSearchPlaceholder) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null,
            )
        },
        trailingIcon = if (state.searchQuery.isNotEmpty()) {
            {
                IconButton(onClick = actions.onClearSearch) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = strings.actionClose,
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun StaffEmptyContent(
    state: StaffUiState,
    actions: StaffActions,
    modifier: Modifier = Modifier,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = modifier.padding(
            start = PosSpacing.md,
            end = PosSpacing.md,
            top = PosSpacing.xs,
        ),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        state.errorMessage?.let { error ->
            StaffErrorBanner(
                message = error.resolve(strings),
                onDismiss = actions.onMessageConsumed,
            )
        }
        EmptyState(
            iconRes = R.drawable.ic_users,
            title = strings.staffEmptyTitle,
            supportingText = strings.staffEmptyHint,
            actionLabel = strings.staffAddAction,
            onAction = actions.onOpenAddEditor,
        )
    }
}

@Composable
internal fun StaffErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    val strings = LocalPosStrings.current
    val palette = LocalPosSemanticPalette.current
    Surface(
        color = palette.warningContainer,
        contentColor = palette.warningText,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(
                start = PosSpacing.sm,
                top = PosSpacing.xs,
                bottom = PosSpacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_triangle_alert),
                contentDescription = null,
                modifier = Modifier.size(PosSizes.iconSmall),
            )
            Text(
                text = message,
                style = LocalAppTextStyles.current.supportingLabel,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = PosSizes.touchTarget),
            ) {
                Text(text = strings.actionClose)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun StaffScreenPreview() {
    UmkmPosTheme {
        StaffScreen(
            state = StaffUiState(
                staffList = listOf(
                    Staff(id = 1L, name = "Dini", isActive = true),
                    Staff(id = 2L, name = "Budi", isActive = true),
                    Staff(id = 3L, name = "Sari", isActive = false),
                ),
                activeStaffId = 1L,
                isLoading = false,
            ),
            actions = StaffActions(
                onOpenAddEditor = {},
                onOpenRenameEditor = {},
                onNameChange = {},
                onSaveEditor = {},
                onDismissEditor = {},
                onRequestDeactivate = {},
                onConfirmDeactivate = {},
                onDismissDeactivate = {},
                onActivateStaff = {},
                onMessageConsumed = {},
            ),
        )
    }
}
