package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.chess.ChessEngine
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.PvLine
import com.example.chess.Square
import com.example.ui.AppStrings
import com.example.ui.ChessUiState
import com.example.ui.theme.BoardDarkSquare
import com.example.ui.theme.BoardLastMoveOverlay
import com.example.ui.theme.BoardLegalCaptureRing
import com.example.ui.theme.BoardLegalMoveDot
import com.example.ui.theme.BoardLightSquare
import com.example.ui.theme.BoardSelectedOverlay
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandLime
import com.example.ui.theme.CardSurface
import com.example.ui.theme.CrimsonCheck
import com.example.ui.theme.CrimsonDeep
import com.example.ui.theme.DeepInputSurface
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EvalBlackFill
import com.example.ui.theme.EvalWhiteFill
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OnBrandDark
import com.example.ui.theme.RoseBlunder
import com.example.ui.theme.SkyVision
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardSection(
    state: ChessUiState,
    strings: AppStrings,
    onSquareClick: (Square) -> Unit,
    onResetGame: () -> Unit,
    onFlipBoard: () -> Unit,
    onUndoMove: () -> Unit,
    onToggleVisionArrows: () -> Unit,
    onPlayBestMove: () -> Unit,
    onLoadPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWhiteBottom = state.boardOrientation == PieceColor.WHITE
    val topIsWhite = !isWhiteBottom

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Player Bar
        PlayerInfoBar(
            isWhiteSide = topIsWhite,
            name = if (topIsWhite) strings.playerYou else strings.engineName,
            capturedPieces = if (topIsWhite) state.capturedByWhite else state.capturedByBlack,
            trailingContent = {
                TurnIndicatorBadge(state = state, strings = strings)
            }
        )

        // Evaluation Bar + Interactive Chessboard
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvaluationBar(
                scoreCp = state.engineEvalCp,
                modifier = Modifier
                    .width(26.dp)
                    .fillMaxHeight()
                    .testTag("evaluation_bar")
            )

            ChessBoardBox(
                state = state,
                onSquareClick = onSquareClick,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .testTag("chess_board")
            )
        }

        // Bottom Player Bar
        PlayerInfoBar(
            isWhiteSide = isWhiteBottom,
            name = if (isWhiteBottom) strings.playerYou else strings.engineName,
            capturedPieces = if (isWhiteBottom) state.capturedByWhite else state.capturedByBlack,
            trailingContent = {
                val diffStr = if (state.materialDiff > 0) "+${state.materialDiff}" else "${state.materialDiff}"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ElevatedSurface
                ) {
                    Text(
                        text = "${strings.materialLabel} $diffStr",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        )

        // Quick Action Controls
        QuickActionControls(
            showVision = state.showVisionArrows,
            strings = strings,
            onResetGame = onResetGame,
            onFlipBoard = onFlipBoard,
            onUndoMove = onUndoMove,
            onToggleVisionArrows = onToggleVisionArrows,
            onPlayBestMove = onPlayBestMove,
            onLoadPuzzle = onLoadPuzzle
        )
    }
}

@Composable
private fun PlayerInfoBar(
    isWhiteSide: Boolean,
    name: String,
    capturedPieces: List<PieceType>,
    trailingContent: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isWhiteSide) EvalWhiteFill else ElevatedSurface)
                        .border(
                            width = 1.dp,
                            color = if (isWhiteSide) Color.Transparent else BorderMuted,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isWhiteSide) "♔" else "♚",
                        fontSize = 17.sp,
                        color = if (isWhiteSide) OnBrandDark else TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val capturedText = capturedPieces.joinToString("") { pieceGlyph(it) }
                    Text(
                        text = capturedText.ifEmpty { " " },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun TurnIndicatorBadge(
    state: ChessUiState,
    strings: AppStrings
) {
    val (label, bg, fg, borderColor) = when {
        state.isCheckmate -> Quad(strings.checkmate, CrimsonDeep, Color.White, Color.Transparent)
        state.isDraw -> Quad(strings.draw, BrandGold, OnBrandDark, Color.Transparent)
        state.turn == PieceColor.WHITE -> Quad(strings.whiteTurn, EvalWhiteFill, OnBrandDark, Color.Transparent)
        else -> Quad(strings.blackTurn, ElevatedSurface, TextPrimary, BorderMuted)
    }

    Surface(
        shape = CircleShape,
        color = bg,
        border = if (borderColor != Color.Transparent) BorderStroke(1.dp, borderColor) else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private data class Quad(
    val label: String,
    val bg: Color,
    val fg: Color,
    val border: Color
)

@Composable
private fun EvaluationBar(
    scoreCp: Int,
    modifier: Modifier = Modifier
) {
    val clamped = scoreCp.coerceIn(-1000, 1000)
    val targetWhiteFraction = (0.5f + (clamped / 1000f) * 0.44f).coerceIn(0.06f, 0.94f)
    val animatedWhiteFraction by animateFloatAsState(
        targetValue = targetWhiteFraction,
        animationSpec = tween(durationMillis = 420),
        label = "eval_bar_anim"
    )
    val scoreText = ChessEngine.formatScore(scoreCp)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DeepInputSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f - animatedWhiteFraction)
                .background(EvalBlackFill),
            contentAlignment = Alignment.TopCenter
        ) {
            if (scoreCp < 0) {
                Text(
                    text = scoreText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(animatedWhiteFraction)
                .background(EvalWhiteFill),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (scoreCp >= 0) {
                Text(
                    text = scoreText,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBrandDark,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChessBoardBox(
    state: ChessUiState,
    onSquareClick: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWhiteBottom = state.boardOrientation == PieceColor.WHITE

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(14.dp))
                .border(2.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .background(CardSurface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (displayRow in 0 until 8) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (displayCol in 0 until 8) {
                            val rank = if (isWhiteBottom) displayRow else 7 - displayRow
                            val file = if (isWhiteBottom) displayCol else 7 - displayCol
                            val sq = Square.of(file, rank)
                            val isLight = (file + rank) % 2 == 0
                            val piece = state.board.getOrNull(sq.index)
                            val isSelected = state.selectedSquare == sq
                            val isLastMove = state.lastMove?.let { it.from == sq || it.to == sq } == true
                            val isCheckSquare = state.checkSquare == sq
                            val isLegalTarget = sq in state.legalTargets

                            BoardSquareCell(
                                square = sq,
                                isLight = isLight,
                                piece = piece,
                                isSelected = isSelected,
                                isLastMove = isLastMove,
                                isCheckSquare = isCheckSquare,
                                isLegalTarget = isLegalTarget,
                                showRankLabel = displayCol == 0,
                                showFileLabel = displayRow == 7,
                                onClick = { onSquareClick(sq) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                }
            }

            if (state.showVisionArrows && state.pvLines.isNotEmpty() && !state.isGameOver) {
                VisionArrowsOverlay(
                    lines = state.pvLines,
                    isWhiteBottom = isWhiteBottom,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun BoardSquareCell(
    square: Square,
    isLight: Boolean,
    piece: ChessPiece?,
    isSelected: Boolean,
    isLastMove: Boolean,
    isCheckSquare: Boolean,
    isLegalTarget: Boolean,
    showRankLabel: Boolean,
    showFileLabel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isLight) BoardLightSquare else BoardDarkSquare
    val coordColor = if (isLight) BoardDarkSquare else BoardLightSquare

    Box(
        modifier = modifier
            .background(baseColor)
            .clickable(onClick = onClick)
            .testTag("square_${square.toAlgebraic()}"),
        contentAlignment = Alignment.Center
    ) {
        if (isLastMove) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BoardLastMoveOverlay)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BoardSelectedOverlay)
            )
        }
        if (isCheckSquare) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CrimsonCheck, CrimsonDeep.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )
        }

        if (showRankLabel) {
            Text(
                text = square.rankChar.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = coordColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 1.dp)
            )
        }

        if (showFileLabel) {
            Text(
                text = square.fileChar.toString(),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = coordColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 1.dp)
            )
        }

        if (piece != null) {
            Image(
                painter = painterResource(pieceDrawableRes(piece)),
                contentDescription = pieceContentDescription(piece),
                modifier = Modifier.fillMaxSize(0.84f)
            )
        }

        if (isLegalTarget) {
            if (piece != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.84f)
                        .border(3.dp, BoardLegalCaptureRing, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.28f)
                        .clip(CircleShape)
                        .background(BoardLegalMoveDot)
                )
            }
        }
    }
}

@Composable
private fun VisionArrowsOverlay(
    lines: List<PvLine>,
    isWhiteBottom: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sqSize = size.width / 8f

        fun centerOf(sq: Square): Offset {
            val col = if (isWhiteBottom) sq.file else 7 - sq.file
            val row = if (isWhiteBottom) sq.rank else 7 - sq.rank
            return Offset((col + 0.5f) * sqSize, (row + 0.5f) * sqSize)
        }

        lines.take(2).reversed().forEachIndexed { revIdx, line ->
            val isPrimary = (lines.take(2).size == 1) || (revIdx == 1)
            val from = centerOf(line.move.from)
            val to = centerOf(line.move.to)
            val dx = to.x - from.x
            val dy = to.y - from.y
            val len = kotlin.math.hypot(dx, dy)
            if (len < 1f) return@forEachIndexed

            val arrowColor = if (isPrimary) BrandLime.copy(alpha = 0.86f) else SkyVision.copy(alpha = 0.52f)
            val strokeWidth = if (isPrimary) sqSize * 0.16f else sqSize * 0.11f
            val headLength = if (isPrimary) sqSize * 0.34f else sqSize * 0.27f
            val headWidth = if (isPrimary) sqSize * 0.24f else sqSize * 0.19f

            val angle = atan2(dy, dx)
            val lineEnd = Offset(
                to.x - cos(angle) * (headLength * 0.72f),
                to.y - sin(angle) * (headLength * 0.72f)
            )

            drawLine(
                color = arrowColor,
                start = from,
                end = lineEnd,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            val p1 = Offset(
                to.x - cos(angle) * headLength + sin(angle) * headWidth,
                to.y - sin(angle) * headLength - cos(angle) * headWidth
            )
            val p2 = Offset(
                to.x - cos(angle) * headLength - sin(angle) * headWidth,
                to.y - sin(angle) * headLength + cos(angle) * headWidth
            )

            val headPath = Path().apply {
                moveTo(to.x, to.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            drawPath(path = headPath, color = arrowColor)
        }
    }
}

@Composable
private fun QuickActionControls(
    showVision: Boolean,
    strings: AppStrings,
    onResetGame: () -> Unit,
    onFlipBoard: () -> Unit,
    onUndoMove: () -> Unit,
    onToggleVisionArrows: () -> Unit,
    onPlayBestMove: () -> Unit,
    onLoadPuzzle: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useSingleRow = maxWidth >= 520.dp
        if (useSingleRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Refresh,
                    label = strings.newGame,
                    onClick = onResetGame,
                    modifier = Modifier.weight(1f).testTag("btn_new_game")
                )
                QuickActionButton(
                    icon = Icons.Default.SwapVert,
                    label = strings.flip,
                    onClick = onFlipBoard,
                    modifier = Modifier.weight(1f).testTag("btn_flip_board")
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    label = strings.undo,
                    onClick = onUndoMove,
                    modifier = Modifier.weight(1f).testTag("btn_undo")
                )
                QuickActionButton(
                    icon = if (showVision) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    label = strings.vision,
                    isActive = showVision,
                    accentColor = BrandLime,
                    onClick = onToggleVisionArrows,
                    modifier = Modifier.weight(1f).testTag("btn_toggle_vision")
                )
                QuickActionButton(
                    icon = Icons.Default.Bolt,
                    label = strings.playBest,
                    accentColor = BrandGold,
                    onClick = onPlayBestMove,
                    modifier = Modifier.weight(1f).testTag("btn_play_best")
                )
                QuickActionButton(
                    icon = Icons.Default.CenterFocusStrong,
                    label = strings.puzzle,
                    accentColor = EmeraldAccent,
                    onClick = onLoadPuzzle,
                    modifier = Modifier.weight(1f).testTag("btn_puzzle")
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Refresh,
                        label = strings.newGame,
                        onClick = onResetGame,
                        modifier = Modifier.weight(1f).testTag("btn_new_game")
                    )
                    QuickActionButton(
                        icon = Icons.Default.SwapVert,
                        label = strings.flip,
                        onClick = onFlipBoard,
                        modifier = Modifier.weight(1f).testTag("btn_flip_board")
                    )
                    QuickActionButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        label = strings.undo,
                        onClick = onUndoMove,
                        modifier = Modifier.weight(1f).testTag("btn_undo")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = if (showVision) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        label = strings.vision,
                        isActive = showVision,
                        accentColor = BrandLime,
                        onClick = onToggleVisionArrows,
                        modifier = Modifier.weight(1f).testTag("btn_toggle_vision")
                    )
                    QuickActionButton(
                        icon = Icons.Default.Bolt,
                        label = strings.playBest,
                        accentColor = BrandGold,
                        onClick = onPlayBestMove,
                        modifier = Modifier.weight(1f).testTag("btn_play_best")
                    )
                    QuickActionButton(
                        icon = Icons.Default.CenterFocusStrong,
                        label = strings.puzzle,
                        accentColor = EmeraldAccent,
                        onClick = onLoadPuzzle,
                        modifier = Modifier.weight(1f).testTag("btn_puzzle")
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    accentColor: Color = TextPrimary
) {
    val bg = if (isActive) accentColor.copy(alpha = 0.16f) else CardSurface
    val border = if (isActive) accentColor.copy(alpha = 0.45f) else BorderSubtle
    val contentColor = if (isActive || accentColor != TextPrimary) accentColor else TextPrimary

    Surface(
        onClick = onClick,
        modifier = modifier.minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PromotionPickerDialog(
    color: PieceColor,
    title: String,
    onSelect: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    val choices = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(1.dp, BorderMuted)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    choices.forEach { pieceType ->
                        val piece = ChessPiece(color, pieceType)
                        Surface(
                            onClick = { onSelect(pieceType) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .testTag("promote_${pieceType.fenChar}"),
                            shape = RoundedCornerShape(12.dp),
                            color = ElevatedSurface,
                            border = BorderStroke(1.dp, BorderMuted)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(PaddingValues(6.dp))
                            ) {
                                Image(
                                    painter = painterResource(pieceDrawableRes(piece)),
                                    contentDescription = pieceContentDescription(piece),
                                    modifier = Modifier.fillMaxSize(0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@DrawableRes
fun pieceDrawableRes(piece: ChessPiece): Int = when (piece.key) {
    "wP" -> R.drawable.ic_piece_wp
    "wN" -> R.drawable.ic_piece_wn
    "wB" -> R.drawable.ic_piece_wb
    "wR" -> R.drawable.ic_piece_wr
    "wQ" -> R.drawable.ic_piece_wq
    "wK" -> R.drawable.ic_piece_wk
    "bP" -> R.drawable.ic_piece_bp
    "bN" -> R.drawable.ic_piece_bn
    "bB" -> R.drawable.ic_piece_bb
    "bR" -> R.drawable.ic_piece_br
    "bQ" -> R.drawable.ic_piece_bq
    else -> R.drawable.ic_piece_bk
}

@Composable
private fun pieceContentDescription(piece: ChessPiece): String = when (piece.key) {
    "wP" -> stringResource(R.string.cd_white_pawn)
    "wN" -> stringResource(R.string.cd_white_knight)
    "wB" -> stringResource(R.string.cd_white_bishop)
    "wR" -> stringResource(R.string.cd_white_rook)
    "wQ" -> stringResource(R.string.cd_white_queen)
    "wK" -> stringResource(R.string.cd_white_king)
    "bP" -> stringResource(R.string.cd_black_pawn)
    "bN" -> stringResource(R.string.cd_black_knight)
    "bB" -> stringResource(R.string.cd_black_bishop)
    "bR" -> stringResource(R.string.cd_black_rook)
    "bQ" -> stringResource(R.string.cd_black_queen)
    else -> stringResource(R.string.cd_black_king)
}

private fun pieceGlyph(type: PieceType): String = when (type) {
    PieceType.PAWN -> "♟"
    PieceType.KNIGHT -> "♞"
    PieceType.BISHOP -> "♝"
    PieceType.ROOK -> "♜"
    PieceType.QUEEN -> "♛"
    PieceType.KING -> "♚"
}
