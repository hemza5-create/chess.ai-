package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.chess.AppLang
import com.example.chess.AppMode
import com.example.ui.components.AnalysisPanelSection
import com.example.ui.components.ChessBoardSection
import com.example.ui.components.PromotionPickerDialog
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandLime
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepInputSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.HeaderSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OnBrandDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun ChessVisionScreen(
    viewModel: ChessViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = remember(state.lang) { stringsFor(state.lang) }
    val layoutDirection = if (state.lang == AppLang.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
    val snackbarHostState = remember { SnackbarHostState() }
    var lastToastKind by remember { mutableStateOf(ToastKind.INFO) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { event ->
            lastToastKind = event.kind
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = event.message,
                duration = SnackbarDuration.Short
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    val (bg, border, text) = when (lastToastKind) {
                        ToastKind.SUCCESS -> Triple(EmeraldContainer, EmeraldAccent.copy(alpha = 0.5f), TextPrimary)
                        ToastKind.WARN -> Triple(Color(0xFF451A03), AmberWarn.copy(alpha = 0.6f), AmberWarn)
                        ToastKind.INFO -> Triple(ElevatedSurface, BorderMuted, TextPrimary)
                    }
                    Snackbar(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        containerColor = bg,
                        contentColor = text,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.labelLarge,
                                color = text
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                StudioHeaderBar(
                    state = state,
                    strings = strings,
                    onModeSelected = viewModel::setAppMode,
                    onToggleLanguage = viewModel::toggleLanguage
                )
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ObsidianBg)
                ) {
                    val isWideTwoPane = maxWidth >= 840.dp
                    val evalStatus = viewModel.formatEvalStatus(state.engineEvalCp, state.lang)

                    if (isWideTwoPane) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(7f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ChessBoardSection(
                                    state = state,
                                    strings = strings,
                                    onSquareClick = viewModel::onSquareClick,
                                    onResetGame = viewModel::resetGame,
                                    onFlipBoard = viewModel::flipBoard,
                                    onUndoMove = viewModel::undoMove,
                                    onToggleVisionArrows = viewModel::toggleVisionArrows,
                                    onPlayBestMove = viewModel::playEngineBestMove,
                                    onLoadPuzzle = viewModel::loadNextPuzzle,
                                    modifier = Modifier.widthIn(max = 580.dp)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(5f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                AnalysisPanelSection(
                                    state = state,
                                    strings = strings,
                                    evalStatusText = evalStatus,
                                    onDepthSelected = viewModel::setSearchDepth,
                                    onPlayPvMove = viewModel::playPvMove,
                                    onSelectTab = viewModel::setActiveTab,
                                    onLoadNextPuzzle = viewModel::loadNextPuzzle,
                                    onShowPuzzleHint = viewModel::showPuzzleHint,
                                    onFenChanged = viewModel::onFenInputChanged,
                                    onImportFen = viewModel::importFen,
                                    onCopyFen = {
                                        clipboardManager.setText(AnnotatedString(state.fenInput))
                                        viewModel.notifyCopied()
                                    },
                                    onPgnChanged = viewModel::onPgnInputChanged,
                                    onImportPgn = viewModel::importPgn,
                                    onSharePgn = {
                                        sharePgnFile(context, viewModel.currentPgnForExport())
                                    },
                                    onCopyPgn = {
                                        clipboardManager.setText(AnnotatedString(state.pgnInput))
                                        viewModel.notifyCopied()
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 600.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ChessBoardSection(
                                    state = state,
                                    strings = strings,
                                    onSquareClick = viewModel::onSquareClick,
                                    onResetGame = viewModel::resetGame,
                                    onFlipBoard = viewModel::flipBoard,
                                    onUndoMove = viewModel::undoMove,
                                    onToggleVisionArrows = viewModel::toggleVisionArrows,
                                    onPlayBestMove = viewModel::playEngineBestMove,
                                    onLoadPuzzle = viewModel::loadNextPuzzle
                                )

                                AnalysisPanelSection(
                                    state = state,
                                    strings = strings,
                                    evalStatusText = evalStatus,
                                    onDepthSelected = viewModel::setSearchDepth,
                                    onPlayPvMove = viewModel::playPvMove,
                                    onSelectTab = viewModel::setActiveTab,
                                    onLoadNextPuzzle = viewModel::loadNextPuzzle,
                                    onShowPuzzleHint = viewModel::showPuzzleHint,
                                    onFenChanged = viewModel::onFenInputChanged,
                                    onImportFen = viewModel::importFen,
                                    onCopyFen = {
                                        clipboardManager.setText(AnnotatedString(state.fenInput))
                                        viewModel.notifyCopied()
                                    },
                                    onPgnChanged = viewModel::onPgnInputChanged,
                                    onImportPgn = viewModel::importPgn,
                                    onSharePgn = {
                                        sharePgnFile(context, viewModel.currentPgnForExport())
                                    },
                                    onCopyPgn = {
                                        clipboardManager.setText(AnnotatedString(state.pgnInput))
                                        viewModel.notifyCopied()
                                    }
                                )

                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }

        if (state.pendingPromotion != null) {
            PromotionPickerDialog(
                color = state.turn,
                title = strings.choosePromotion,
                onSelect = viewModel::confirmPromotion,
                onDismiss = viewModel::dismissPromotion
            )
        }
    }
}

@Composable
private fun StudioHeaderBar(
    state: ChessUiState,
    strings: AppStrings,
    onModeSelected: (AppMode) -> Unit,
    onToggleLanguage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HeaderSurface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val isWideHeader = maxWidth >= 700.dp

            if (isWideHeader) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BrandIdentityRow(subtitle = strings.subtitle, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    ModeAndLanguageControls(
                        state = state,
                        strings = strings,
                        onModeSelected = onModeSelected,
                        onToggleLanguage = onToggleLanguage
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BrandIdentityRow(subtitle = strings.subtitle, modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        LanguageToggleButton(
                            label = strings.langToggleLabel,
                            onClick = onToggleLanguage
                        )
                    }
                    ModeSegmentedRow(
                        currentMode = state.appMode,
                        strings = strings,
                        onModeSelected = onModeSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandIdentityRow(
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(BrandLime, BrandGold))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_piece_wn),
                contentDescription = stringResource(R.string.cd_app_logo),
                tint = OnBrandDark,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Chess-Vision Stockfish UCI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = BrandLime.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, BrandLime.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "UCI v17",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandLime,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModeAndLanguageControls(
    state: ChessUiState,
    strings: AppStrings,
    onModeSelected: (AppMode) -> Unit,
    onToggleLanguage: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeSegmentedRow(
            currentMode = state.appMode,
            strings = strings,
            onModeSelected = onModeSelected
        )
        LanguageToggleButton(
            label = strings.langToggleLabel,
            onClick = onToggleLanguage
        )
    }
}

@Composable
private fun ModeSegmentedRow(
    currentMode: AppMode,
    strings: AppStrings,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DeepInputSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModePillButton(
                label = strings.analysisMode,
                selected = currentMode == AppMode.ANALYSIS,
                onClick = { onModeSelected(AppMode.ANALYSIS) },
                modifier = Modifier.weight(1f, fill = false).testTag("mode_analysis")
            )
            ModePillButton(
                label = strings.aiMode,
                selected = currentMode == AppMode.VS_ENGINE,
                onClick = { onModeSelected(AppMode.VS_ENGINE) },
                modifier = Modifier.weight(1f, fill = false).testTag("mode_vs_engine")
            )
            ModePillButton(
                label = strings.puzzleMode,
                selected = currentMode == AppMode.PUZZLE,
                onClick = { onModeSelected(AppMode.PUZZLE) },
                modifier = Modifier.weight(1f, fill = false).testTag("mode_puzzle")
            )
        }
    }
}

@Composable
private fun ModePillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BrandLime else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) OnBrandDark else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LanguageToggleButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = ElevatedSurface,
        border = BorderStroke(1.dp, BorderMuted),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("btn_lang_toggle")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = BrandGold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        )
    }
}

private fun sharePgnFile(context: Context, pgnContent: String) {
    val content = pgnContent.ifBlank { "[Event \"Chess-Vision Stockfish UCI Analysis\"]\n\n*" }
    runCatching {
        val dir = File(context.cacheDir, "pgn").apply { mkdirs() }
        val file = File(dir, "chess-vision-analysis.pgn")
        file.writeText(content)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/x-chess-pgn"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, "chess-vision-analysis.pgn")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PGN"))
    }.onFailure {
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Share PGN"))
    }
}
