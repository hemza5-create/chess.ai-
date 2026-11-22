package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.AppLang
import com.example.chess.ChessEngine
import com.example.chess.ChessMove
import com.example.chess.PvLine
import com.example.ui.AppStrings
import com.example.ui.ChessUiState
import com.example.ui.MoveQualityKind
import com.example.ui.StudioTab
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandLime
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInputSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EvalWhiteFill
import com.example.ui.theme.HeaderSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnBrandDark
import com.example.ui.theme.RoseBlunder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AnalysisPanelSection(
    state: ChessUiState,
    strings: AppStrings,
    evalStatusText: String,
    onDepthSelected: (Int) -> Unit,
    onPlayPvMove: (PvLine) -> Unit,
    onSelectTab: (StudioTab) -> Unit,
    onLoadNextPuzzle: () -> Unit,
    onShowPuzzleHint: () -> Unit,
    onFenChanged: (String) -> Unit,
    onImportFen: () -> Unit,
    onCopyFen: () -> Unit,
    onPgnChanged: (String) -> Unit,
    onImportPgn: () -> Unit,
    onSharePgn: () -> Unit,
    onCopyPgn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EngineAnalysisCard(
            state = state,
            strings = strings,
            evalStatusText = evalStatusText,
            onDepthSelected = onDepthSelected,
            onPlayPvMove = onPlayPvMove
        )

        StudioTabsCard(
            state = state,
            strings = strings,
            onSelectTab = onSelectTab,
            onLoadNextPuzzle = onLoadNextPuzzle,
            onShowPuzzleHint = onShowPuzzleHint,
            onFenChanged = onFenChanged,
            onImportFen = onImportFen,
            onCopyFen = onCopyFen,
            onPgnChanged = onPgnChanged,
            onImportPgn = onImportPgn,
            onSharePgn = onSharePgn,
            onCopyPgn = onCopyPgn
        )
    }
}

@Composable
private fun EngineAnalysisCard(
    state: ChessUiState,
    strings: AppStrings,
    evalStatusText: String,
    onDepthSelected: (Int) -> Unit,
    onPlayPvMove: (PvLine) -> Unit
) {
    var depthMenuExpanded by remember { mutableStateOf(false) }
    val pulseTransition = rememberInfiniteTransition(label = "engine_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(BrandLime)
                    )
                    Text(
                        text = strings.engineTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    Surface(
                        onClick = { depthMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        color = HeaderSurface,
                        border = BorderStroke(1.dp, BorderMuted),
                        modifier = Modifier.testTag("depth_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${strings.depth} ${state.depth}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandLime
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = BrandLime,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = depthMenuExpanded,
                        onDismissRequest = { depthMenuExpanded = false }
                    ) {
                        listOf(
                            1 to strings.depthFast,
                            2 to strings.depthBalanced,
                            3 to strings.depthDeep
                        ).forEach { (d, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = if (state.depth == d) BrandLime else TextPrimary
                                    )
                                },
                                onClick = {
                                    depthMenuExpanded = false
                                    onDepthSelected(d)
                                }
                            )
                        }
                    }
                }
            }

            // Evaluation & Best Move Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = HeaderSurface,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = strings.evalLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = ChessEngine.formatScore(state.engineEvalCp),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandLime,
                            modifier = Modifier.testTag("engine_eval_text")
                        )
                        Text(
                            text = if (state.isAnalyzing) strings.analyzing else evalStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = HeaderSurface,
                    border = BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = strings.bestMoveLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.bestMoveSan,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.testTag("engine_best_san")
                            )
                            Text(
                                text = state.bestMoveUci,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = BrandGold,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }

                        val (qualityLabel, qualityColor) = moveQualityDisplay(
                            kind = state.moveQualityKind,
                            lang = state.lang,
                            defaultLabel = strings.readyToAnalyze
                        )
                        Text(
                            text = qualityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = qualityColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // MultiPV Lines
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DeepInputSurface,
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.topLines,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${state.evaluatedNodes} nodes",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    state.pvLines.forEachIndexed { idx, line ->
                        val isPositive = line.scoreCp >= 0
                        Surface(
                            onClick = { onPlayPvMove(line) },
                            shape = RoundedCornerShape(8.dp),
                            color = HeaderSurface,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pv_line_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (isPositive) EvalWhiteFill else ElevatedSurface
                                ) {
                                    Text(
                                        text = ChessEngine.formatScore(line.scoreCp),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPositive) OnBrandDark else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = "${idx + 1}. ${line.pvText}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioTabsCard(
    state: ChessUiState,
    strings: AppStrings,
    onSelectTab: (StudioTab) -> Unit,
    onLoadNextPuzzle: () -> Unit,
    onShowPuzzleHint: () -> Unit,
    onFenChanged: (String) -> Unit,
    onImportFen: () -> Unit,
    onCopyFen: () -> Unit,
    onPgnChanged: (String) -> Unit,
    onImportPgn: () -> Unit,
    onSharePgn: () -> Unit,
    onCopyPgn: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderSurface)
            ) {
                StudioTabButton(
                    label = strings.tabMoves,
                    selected = state.activeTab == StudioTab.MOVES,
                    onClick = { onSelectTab(StudioTab.MOVES) },
                    modifier = Modifier.weight(1f).testTag("tab_moves")
                )
                StudioTabButton(
                    label = strings.tabTrainer,
                    selected = state.activeTab == StudioTab.TRAINER,
                    onClick = { onSelectTab(StudioTab.TRAINER) },
                    modifier = Modifier.weight(1f).testTag("tab_trainer")
                )
                StudioTabButton(
                    label = strings.tabIo,
                    selected = state.activeTab == StudioTab.IO,
                    onClick = { onSelectTab(StudioTab.IO) },
                    modifier = Modifier.weight(1f).testTag("tab_io")
                )
            }
            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            when (state.activeTab) {
                StudioTab.MOVES -> MovesTabContent(state = state, strings = strings)
                StudioTab.TRAINER -> TrainerTabContent(
                    state = state,
                    strings = strings,
                    onLoadNextPuzzle = onLoadNextPuzzle,
                    onShowPuzzleHint = onShowPuzzleHint
                )
                StudioTab.IO -> IoTabContent(
                    state = state,
                    strings = strings,
                    onFenChanged = onFenChanged,
                    onImportFen = onImportFen,
                    onCopyFen = onCopyFen,
                    onPgnChanged = onPgnChanged,
                    onImportPgn = onImportPgn,
                    onSharePgn = onSharePgn,
                    onCopyPgn = onCopyPgn
                )
            }
        }
    }
}

@Composable
private fun StudioTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) BrandLime else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 13.dp, horizontal = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .background(if (selected) BrandLime else Color.Transparent)
        )
    }
}

@Composable
private fun MovesTabContent(
    state: ChessUiState,
    strings: AppStrings
) {
    val openingName = if (state.lang == AppLang.AR) state.openingNameAr else state.openingNameEn
    val pairs = remember(state.history) {
        state.history.chunked(2).mapIndexed { index, pair ->
            Triple(index + 1, pair[0], pair.getOrNull(1))
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(pairs.size) {
        if (pairs.isNotEmpty()) {
            listState.animateScrollToItem(pairs.lastIndex)
        }
    }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${strings.openingLabel} $openingName",
                style = MaterialTheme.typography.labelMedium,
                color = BrandGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${state.history.size} ${strings.movesUnit}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp, max = 230.dp),
            shape = RoundedCornerShape(12.dp),
            color = DeepInputSurface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            if (pairs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noMovesYet,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(pairs) { _, (num, whiteMove, blackMove) ->
                            MoveHistoryRow(num = num, whiteMove = whiteMove, blackMove = blackMove)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveHistoryRow(
    num: Int,
    whiteMove: ChessMove,
    blackMove: ChessMove?
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = HeaderSurface.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$num.",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.width(36.dp)
            )
            Text(
                text = whiteMove.san,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = blackMove?.san ?: "",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandLime,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrainerTabContent(
    state: ChessUiState,
    strings: AppStrings,
    onLoadNextPuzzle: () -> Unit,
    onShowPuzzleHint: () -> Unit
) {
    val adviceText = remember(state.moveQualityKind, state.lastEvaluatedMoveSan, state.lang) {
        buildCoachAdvice(state.moveQualityKind, state.lastEvaluatedMoveSan, state.lang, strings.defaultCoachAdvice)
    }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Real-Time Coach Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HeaderSurface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = BrandLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.coachFeedback,
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandLime
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldAccent.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${strings.accuracyLabel} ${state.accuracyPercent}%",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = adviceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
            }
        }

        // Tactical Challenge Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HeaderSurface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.tacticalChallenge,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = state.currentPuzzle?.rating ?: "1450 ELO",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGold
                    )
                }

                val puzzleDesc = state.currentPuzzle?.let {
                    if (state.lang == AppLang.AR) it.descriptionAr else it.descriptionEn
                } ?: strings.defaultPuzzleDesc

                Text(
                    text = puzzleDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onLoadNextPuzzle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandLime,
                            contentColor = OnBrandDark
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("btn_load_puzzle")
                    ) {
                        Text(
                            text = strings.loadPuzzleBtn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = onShowPuzzleHint,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElevatedSurface,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_puzzle_hint")
                    ) {
                        Text(
                            text = strings.hintBtn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IoTabContent(
    state: ChessUiState,
    strings: AppStrings,
    onFenChanged: (String) -> Unit,
    onImportFen: () -> Unit,
    onCopyFen: () -> Unit,
    onPgnChanged: (String) -> Unit,
    onImportPgn: () -> Unit,
    onSharePgn: () -> Unit,
    onCopyPgn: () -> Unit
) {
    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // FEN Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.fenLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                TextButton(
                    onClick = onCopyFen,
                    modifier = Modifier.testTag("btn_copy_fen")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = strings.copy, color = BrandLime, style = MaterialTheme.typography.labelMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = state.fenInput,
                        onValueChange = onFenChanged,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DeepInputSurface,
                            unfocusedContainerColor = DeepInputSurface,
                            focusedBorderColor = BrandLime,
                            unfocusedBorderColor = BorderSubtle
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fen_input")
                    )
                }

                Button(
                    onClick = onImportFen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("btn_load_fen")
                ) {
                    Text(text = strings.load, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // PGN Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.pgnLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                TextButton(
                    onClick = onCopyPgn,
                    modifier = Modifier.testTag("btn_copy_pgn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = BrandLime,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(text = strings.copy, color = BrandLime, style = MaterialTheme.typography.labelMedium)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = state.pgnInput,
                    onValueChange = onPgnChanged,
                    minLines = 3,
                    maxLines = 5,
                    placeholder = {
                        Text(
                            text = "1. e4 e5 2. Nf3 Nc6 3. Bb5...",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DeepInputSurface,
                        unfocusedContainerColor = DeepInputSurface,
                        focusedBorderColor = BrandLime,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pgn_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onImportPgn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandLime,
                        contentColor = OnBrandDark
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_import_pgn")
                ) {
                    Text(text = strings.importPgn, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = onSharePgn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElevatedSurface,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("btn_share_pgn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = strings.downloadPgn, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun moveQualityDisplay(
    kind: MoveQualityKind,
    lang: AppLang,
    defaultLabel: String
): Pair<String, Color> = when (kind) {
    MoveQualityKind.NONE -> defaultLabel to EmeraldAccent
    MoveQualityKind.EXCELLENT ->
        (if (lang == AppLang.AR) "★ نقلة ممتازة (Best)" else "★ Excellent Move") to BrandLime
    MoveQualityKind.GOOD ->
        (if (lang == AppLang.AR) "✓ نقلة جيدة" else "✓ Good Move") to EmeraldAccent
    MoveQualityKind.INACCURACY ->
        (if (lang == AppLang.AR) "?! عدم دقة (Inaccuracy)" else "?! Inaccuracy") to AmberWarn
    MoveQualityKind.BLUNDER ->
        (if (lang == AppLang.AR) "?? خطأ فادح (Blunder)" else "?? Blunder") to RoseBlunder
}

private fun buildCoachAdvice(
    kind: MoveQualityKind,
    moveSan: String?,
    lang: AppLang,
    defaultAdvice: String
): String {
    if (moveSan == null) return defaultAdvice
    return when (kind) {
        MoveQualityKind.EXCELLENT, MoveQualityKind.GOOD -> if (lang == AppLang.AR) {
            "نقلة دقيقة ($moveSan)! حافظت على توازنك أو عززت نشاط قطعك."
        } else {
            "Accurate move ($moveSan)! You maintained control and piece activity."
        }
        MoveQualityKind.INACCURACY -> if (lang == AppLang.AR) {
            "النقلة ($moveSan) سمحت للخصم بفرصة مضادة. راقب أسهم الرؤية الخضراء لاكتشاف البدائل الأقوى."
        } else {
            "The move ($moveSan) gave your opponent counterplay. Check the vision arrows for stronger options."
        }
        MoveQualityKind.BLUNDER -> if (lang == AppLang.AR) {
            "انتبه! النقلة ($moveSan) أفقدتك أفضلية مادية أو تكتيكية كبيرة. يمكنك التراجع وتجربة نقلة أخرى."
        } else {
            "Watch out! ($moveSan) lost significant material or tactical advantage. Use Undo to retry."
        }
        MoveQualityKind.NONE -> defaultAdvice
    }
}
