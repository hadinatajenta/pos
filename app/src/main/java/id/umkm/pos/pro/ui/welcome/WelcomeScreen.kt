package id.umkm.pos.pro.ui.welcome

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.umkm.pos.pro.R
import id.umkm.pos.pro.feature.reports.Rupiah
import id.umkm.pos.pro.ui.common.LocalPosStrings
import id.umkm.pos.pro.ui.common.PosStrings
import id.umkm.pos.pro.ui.components.PrimaryActionButton
import id.umkm.pos.pro.ui.theme.PosBreakpoints
import id.umkm.pos.pro.ui.theme.PosColors
import id.umkm.pos.pro.ui.theme.PosSpacing
import id.umkm.pos.pro.ui.theme.UmkmPosTheme
import kotlinx.coroutines.delay

const val WELCOME_SCREEN_TEST_TAG = "welcome-screen"

private enum class ShowcaseScene(
    @DrawableRes val iconRes: Int,
) {
    CASHIER(iconRes = R.drawable.ic_shopping_cart),
    INVENTORY(iconRes = R.drawable.ic_package),
    PROFIT(iconRes = R.drawable.ic_trending_up),
}

private fun ShowcaseScene.title(strings: PosStrings): String = when (this) {
    ShowcaseScene.CASHIER -> strings.welcomeFeatureCashier
    ShowcaseScene.INVENTORY -> strings.welcomeFeatureInventory
    ShowcaseScene.PROFIT -> strings.welcomeFeatureProfit
}

@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var activeSceneIndex by remember { mutableIntStateOf(0) }
    var cashierStep by remember { mutableIntStateOf(0) }
    var sceneReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sceneReady = true
    }

    LaunchedEffect(activeSceneIndex) {
        if (ShowcaseScene.entries[activeSceneIndex] == ShowcaseScene.CASHIER) {
            cashierStep = 0
            delay(180)
            cashierStep = 1
            delay(380)
            cashierStep = 2
            delay(380)
            cashierStep = 3
            delay(420)
            cashierStep = 4
        }
        delay(3600)
        activeSceneIndex = (activeSceneIndex + 1) % ShowcaseScene.entries.size
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (sceneReady) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "welcomeHeaderAlpha",
    )
    val headerSlidePx by animateFloatAsState(
        targetValue = if (sceneReady) 0f else 28f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "welcomeHeaderSlide",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(WELCOME_SCREEN_TEST_TAG),
    ) {
        val isExpanded = maxWidth >= PosBreakpoints.expandedNavigation
        if (isExpanded) {
            WelcomeTabletLayout(
                activeScene = ShowcaseScene.entries[activeSceneIndex],
                cashierStep = cashierStep,
                headerAlpha = headerAlpha,
                headerSlidePx = headerSlidePx,
                onSelectScene = { index -> activeSceneIndex = index },
                onStart = onStart,
            )
        } else {
            WelcomePhoneLayout(
                activeScene = ShowcaseScene.entries[activeSceneIndex],
                cashierStep = cashierStep,
                headerAlpha = headerAlpha,
                headerSlidePx = headerSlidePx,
                onSelectScene = { index -> activeSceneIndex = index },
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun WelcomePhoneLayout(
    activeScene: ShowcaseScene,
    cashierStep: Int,
    headerAlpha: Float,
    headerSlidePx: Float,
    onSelectScene: (Int) -> Unit,
    onStart: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PosSpacing.lg, vertical = PosSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = headerAlpha
                    translationY = headerSlidePx
                },
            verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Spacer(modifier = Modifier.height(PosSpacing.xs))
            WelcomeBrandPill()
            Text(
                text = strings.welcomeHeadline,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 38.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = strings.welcomeBodyPhone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = PosSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedPosShowcaseDeck(
                activeScene = activeScene,
                cashierStep = cashierStep,
                onSelectScene = onSelectScene,
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            PrimaryActionButton(
                text = strings.welcomeStart,
                onClick = onStart,
                shape = RoundedCornerShape(percent = 50),
            )
            Text(
                text = strings.welcomeOfflineBadge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WelcomeTabletLayout(
    activeScene: ShowcaseScene,
    cashierStep: Int,
    headerAlpha: Float,
    headerSlidePx: Float,
    onSelectScene: (Int) -> Unit,
    onStart: () -> Unit,
) {
    val strings = LocalPosStrings.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = headerAlpha
                        translationY = headerSlidePx
                    },
                verticalArrangement = Arrangement.spacedBy(PosSpacing.md),
            ) {
                WelcomeBrandPill()
                Text(
                    text = strings.welcomeHeadline,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 44.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = strings.welcomeBodyTablet,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(PosSpacing.xs),
                ) {
                    TabletFeatureBullet(strings.welcomeBulletTransactions)
                    TabletFeatureBullet(strings.welcomeBulletStock)
                    TabletFeatureBullet(strings.welcomeBulletProfit)
                }
                Spacer(modifier = Modifier.height(PosSpacing.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PosSpacing.md),
                ) {
                    Box(modifier = Modifier.width(260.dp)) {
                        PrimaryActionButton(
                            text = strings.welcomeStart,
                            onClick = onStart,
                            shape = RoundedCornerShape(percent = 50),
                        )
                    }
                    Text(
                        text = strings.welcomeOwnership,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Box(
                modifier = Modifier.weight(1.05f),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedPosShowcaseDeck(
                    activeScene = activeScene,
                    cashierStep = cashierStep,
                    onSelectScene = onSelectScene,
                )
            }
        }
    }
}

@Composable
private fun WelcomeBrandPill() {
    val strings = LocalPosStrings.current
    Surface(
        color = PosColors.GreenSoft,
        contentColor = PosColors.ForestGreen,
        shape = CircleShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_store),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = strings.welcomeReadyOffline,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun TabletFeatureBullet(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Surface(
            color = PosColors.GreenSoft,
            contentColor = PosColors.ForestGreen,
            shape = CircleShape,
            modifier = Modifier.size(22.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AnimatedPosShowcaseDeck(
    activeScene: ShowcaseScene,
    cashierStep: Int,
    onSelectScene: (Int) -> Unit,
) {
    val strings = LocalPosStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
        ) {
            ShowcaseScene.entries.forEachIndexed { index, scene ->
                val isSelected = scene == activeScene
                Surface(
                    color = if (isSelected) PosColors.ForestGreen else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .clickable { onSelectScene(index) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            painter = painterResource(scene.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = scene.title(strings),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AnimatedContent(
                targetState = activeScene,
                transitionSpec = {
                    (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 10 })
                        .togetherWith(fadeOut(tween(180)))
                },
                label = "showcaseSceneTransition",
            ) { scene ->
                when (scene) {
                    ShowcaseScene.CASHIER -> CashierSimulationCard(step = cashierStep)
                    ShowcaseScene.INVENTORY -> InventorySimulationCard()
                    ShowcaseScene.PROFIT -> ProfitSimulationCard()
                }
            }
        }
    }
}

@Composable
private fun CashierSimulationCard(step: Int) {
    val strings = LocalPosStrings.current
    val targetTotal = when {
        step >= 3 -> 43_000
        step == 2 -> 33_000
        step == 1 -> 18_000
        else -> 0
    }
    val animatedTotal by animateIntAsState(
        targetValue = targetTotal,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "animatedCartTotal",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.xs),
            ) {
                Surface(
                    color = PosColors.GreenSoft,
                    contentColor = PosColors.ForestGreen,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_receipt),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = strings.welcomeShowcaseReceiptActive,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "INV-20261018-001",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Surface(
                color = PosColors.SuccessContainer,
                contentColor = PosColors.Success,
                shape = CircleShape,
            ) {
                Text(
                    text = if (step >= 4) strings.welcomeShowcaseReceiptPaid else strings.welcomeShowcaseReceiptOpen,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        Column(verticalArrangement = Arrangement.spacedBy(PosSpacing.xs)) {
            AnimatedCartItemRow(
                visible = step >= 1,
                iconRes = R.drawable.ic_coffee,
                name = "Kopi Susu Gula Aren",
                qtyLabel = "1x • Gelas",
                priceLabel = "Rp 18.000",
            )
            AnimatedCartItemRow(
                visible = step >= 2,
                iconRes = R.drawable.ic_croissant,
                name = "Roti Bakar Cokelat",
                qtyLabel = "1x • Porsi",
                priceLabel = "Rp 15.000",
            )
            AnimatedCartItemRow(
                visible = step >= 3,
                iconRes = R.drawable.ic_cup_soda,
                name = "Es Teh Manis",
                qtyLabel = "2x • Gelas",
                priceLabel = "Rp 10.000",
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosSpacing.md, vertical = PosSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = strings.welcomeShowcaseTotalDue,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Rupiah.format(animatedTotal.toLong()),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PosColors.ForestGreen,
                    )
                }
                AnimatedVisibility(
                    visible = step >= 4,
                    enter = fadeIn(tween(260)) + slideInVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ) { it / 2 },
                ) {
                    Surface(
                        color = PosColors.ForestGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AnimatedCheckVector(progress = if (step >= 4) 1f else 0f)
                            Text(
                                text = strings.welcomeShowcaseChange,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedCartItemRow(
    visible: Boolean,
    @DrawableRes iconRes: Int,
    name: String,
    qtyLabel: String,
    priceLabel: String,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(260)) + expandVertically(tween(260)) + slideInVertically(tween(260)) { it / 3 },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = PosColors.ForestGreen,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = qtyLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = priceLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun InventorySimulationCard() {
    val strings = LocalPosStrings.current
    var animateBars by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        animateBars = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PosSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.welcomeShowcaseStockTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = strings.welcomeShowcaseStockHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = PosColors.GreenSoft,
                contentColor = PosColors.ForestGreen,
                shape = CircleShape,
            ) {
                Text(
                    text = strings.welcomeShowcaseWholesaleActive,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        AnimatedStockRow(
            name = "Beras Premium 5kg",
            unitDetail = "24 Sak • Harga Grosir",
            targetFill = if (animateBars) 0.82f else 0f,
            barColor = PosColors.ForestGreen,
        )
        AnimatedStockRow(
            name = "Indomie Goreng",
            unitDetail = "140 Bks (3 Dus + 20 Bks)",
            targetFill = if (animateBars) 0.68f else 0f,
            barColor = PosColors.Accent,
        )
        AnimatedStockRow(
            name = "Gula Pasir 1kg",
            unitDetail = "Sisa 4 kg • Stok Menipis",
            targetFill = if (animateBars) 0.24f else 0f,
            barColor = PosColors.Warning,
        )
    }
}

@Composable
private fun AnimatedStockRow(
    name: String,
    unitDetail: String,
    targetFill: Float,
    barColor: Color,
) {
    val fill by animateFloatAsState(
        targetValue = targetFill,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "stockFillAnimation",
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(PosSpacing.xs))
            Text(
                text = unitDetail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun ProfitSimulationCard() {
    val strings = LocalPosStrings.current
    var animateChart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        animateChart = true
    }
    val progress by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0f,
        animationSpec = tween(durationMillis = 560, easing = FastOutSlowInEasing),
        label = "profitChartProgress",
    )
    val bars = remember { listOf(0.42f, 0.58f, 0.51f, 0.74f, 0.66f, 0.88f, 1.0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PosSpacing.md),
        verticalArrangement = Arrangement.spacedBy(PosSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = strings.welcomeShowcaseProfitTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.welcomeShowcaseProfitAmount,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PosColors.ForestGreen,
                )
            }
            Surface(
                color = PosColors.SuccessContainer,
                contentColor = PosColors.Success,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trending_up),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = strings.welcomeShowcaseTurnover,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .padding(top = PosSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEachIndexed { index, ratio ->
                val isPeak = index == bars.lastIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .fillMaxHeight((ratio * progress).coerceIn(0.08f, 1f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(
                                    if (isPeak) PosColors.ForestGreen else PosColors.ChartBarMuted,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.dateWeekdayAbbreviations[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosSpacing.sm, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = strings.welcomeShowcaseProfitHint,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(PosSpacing.xs))
                Text(
                    text = strings.welcomeShowcaseReady,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = PosColors.ForestGreen,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AnimatedCheckVector(progress: Float) {
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "checkVectorProgress",
    )
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val p1 = Offset(w * 0.18f, h * 0.52f)
        val p2 = Offset(w * 0.44f, h * 0.78f)
        val p3 = Offset(w * 0.84f, h * 0.24f)
        val path = Path().apply {
            moveTo(p1.x, p1.y)
            if (animProgress <= 0.5f) {
                val t = (animProgress / 0.5f).coerceIn(0f, 1f)
                lineTo(
                    p1.x + (p2.x - p1.x) * t,
                    p1.y + (p2.y - p1.y) * t,
                )
            } else {
                lineTo(p2.x, p2.y)
                val t = ((animProgress - 0.5f) / 0.5f).coerceIn(0f, 1f)
                lineTo(
                    p2.x + (p3.x - p2.x) * t,
                    p2.y + (p3.y - p2.y) * t,
                )
            }
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Preview(name = "Welcome Phone", widthDp = 380, heightDp = 800, showBackground = true)
@Composable
private fun WelcomeScreenPhonePreview() {
    UmkmPosTheme {
        WelcomeScreen(onStart = {})
    }
}

@Preview(name = "Welcome Tablet", widthDp = 960, heightDp = 640, showBackground = true)
@Composable
private fun WelcomeScreenTabletPreview() {
    UmkmPosTheme {
        WelcomeScreen(onStart = {})
    }
}
