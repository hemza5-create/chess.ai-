package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.AppLang
import com.example.chess.AppMode
import com.example.chess.ChessEngine
import com.example.chess.ChessGame
import com.example.chess.ChessMove
import com.example.chess.ChessPiece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.PvLine
import com.example.chess.Square
import com.example.chess.TacticalPuzzle
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class StudioTab {
    MOVES,
    TRAINER,
    IO
}

enum class MoveQualityKind {
    NONE,
    EXCELLENT,
    GOOD,
    INACCURACY,
    BLUNDER
}

enum class ToastKind {
    INFO,
    SUCCESS,
    WARN
}

data class ToastEvent(
    val message: String,
    val kind: ToastKind = ToastKind.INFO
)

data class ChessUiState(
    val board: List<ChessPiece?> = emptyList(),
    val turn: PieceColor = PieceColor.WHITE,
    val boardOrientation: PieceColor = PieceColor.WHITE,
    val selectedSquare: Square? = null,
    val legalTargets: Set<Square> = emptySet(),
    val lastMove: ChessMove? = null,
    val checkSquare: Square? = null,
    val isCheckmate: Boolean = false,
    val isDraw: Boolean = false,
    val isGameOver: Boolean = false,
    val history: List<ChessMove> = emptyList(),
    val capturedByWhite: List<PieceType> = emptyList(),
    val capturedByBlack: List<PieceType> = emptyList(),
    val materialDiff: Int = 0,
    val appMode: AppMode = AppMode.ANALYSIS,
    val activeTab: StudioTab = StudioTab.MOVES,
    val lang: AppLang = AppLang.AR,
    val depth: Int = 2,
    val showVisionArrows: Boolean = true,
    val isAnalyzing: Boolean = false,
    val engineEvalCp: Int = 25,
    val bestMove: ChessMove? = null,
    val bestMoveSan: String = "e4",
    val bestMoveUci: String = "(e2e4)",
    val pvLines: List<PvLine> = emptyList(),
    val evaluatedNodes: Int = 0,
    val moveQualityKind: MoveQualityKind = MoveQualityKind.NONE,
    val lastEvaluatedMoveSan: String? = null,
    val accuracyPercent: Int = 100,
    val currentPuzzle: TacticalPuzzle? = null,
    val openingNameAr: String = "وضعية البداية القياسية",
    val openingNameEn: String = "Standard Starting Position",
    val fenInput: String = ChessGame.START_FEN,
    val pgnInput: String = "",
    val pendingPromotion: Pair<Square, Square>? = null
)

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = UserPreferencesRepository(application.applicationContext)
    private val game = ChessGame()

    private var analysisJob: Job? = null
    private var aiResponseJob: Job? = null
    private var lastEvalBeforeMove: Int = 25
    private val moveAccuracies = mutableListOf<Int>()
    private var puzzleIndex = -1

    private val _uiState = MutableStateFlow(ChessUiState())
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastEvent>(extraBufferCapacity = 4)
    val toastEvents: SharedFlow<ToastEvent> = _toastEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    lang = prefs.lang,
                    showVisionArrows = prefs.showVisionArrows,
                    depth = prefs.searchDepth
                )
            }
            syncBoardState()
            analyzeCurrentPosition()
        }
    }

    fun onSquareClick(square: Square) {
        val state = _uiState.value
        if (game.isGameOver()) {
            emitToast(
                if (state.lang == AppLang.AR) "انتهت المباراة! اضغط على \"جديد\" للبدء من جديد."
                else "Game over! Tap \"New\" to restart.",
                ToastKind.WARN
            )
            return
        }

        val currentSelected = state.selectedSquare
        if (currentSelected != null && square in state.legalTargets) {
            val piece = game.pieceAt(currentSelected)
            val isPromotion = piece?.type == PieceType.PAWN &&
                ((piece.color == PieceColor.WHITE && square.rank == 0) ||
                    (piece.color == PieceColor.BLACK && square.rank == 7))

            if (isPromotion) {
                _uiState.update { it.copy(pendingPromotion = currentSelected to square) }
                return
            }

            executePlayerMove(currentSelected, square, null)
            return
        }

        val clickedPiece = game.pieceAt(square)
        if (clickedPiece != null && clickedPiece.color == game.turn) {
            val targets = game.legalMoves(square).map { it.to }.toSet()
            _uiState.update {
                it.copy(selectedSquare = square, legalTargets = targets)
            }
        } else {
            _uiState.update {
                it.copy(selectedSquare = null, legalTargets = emptySet())
            }
        }
    }

    fun confirmPromotion(pieceType: PieceType) {
        val pending = _uiState.value.pendingPromotion ?: return
        _uiState.update { it.copy(pendingPromotion = null) }
        executePlayerMove(pending.first, pending.second, pieceType)
    }

    fun dismissPromotion() {
        _uiState.update {
            it.copy(
                pendingPromotion = null,
                selectedSquare = null,
                legalTargets = emptySet()
            )
        }
    }

    private fun executePlayerMove(from: Square, to: Square, promotion: PieceType?) {
        val moverColor = game.turn
        val moveResult = game.makeMove(from, to, promotion) ?: run {
            _uiState.update { it.copy(selectedSquare = null, legalTargets = emptySet()) }
            return
        }

        _uiState.update { it.copy(selectedSquare = null, legalTargets = emptySet()) }
        syncBoardState()

        val state = _uiState.value
        if (state.appMode == AppMode.PUZZLE && state.currentPuzzle != null) {
            val playedUci = moveResult.uci
            if (playedUci == state.currentPuzzle.solutionUci) {
                emitToast(
                    if (state.lang == AppLang.AR) "🎯 ممتاز! نقلة تكتيكية حاسمة وصحيحة!"
                    else "🎯 Brilliant! Correct tactical solution!",
                    ToastKind.SUCCESS
                )
            } else {
                emitToast(
                    if (state.lang == AppLang.AR) "❌ ليست النقلة الأدق في هذا اللغز، جرب التراجع!"
                    else "❌ Not the best tactical move, try Undo!",
                    ToastKind.WARN
                )
            }
            analyzeCurrentPosition(moverColor, moveResult)
            return
        }

        if (state.appMode == AppMode.VS_ENGINE && !game.isGameOver()) {
            analyzeCurrentPosition(moverColor, moveResult, triggerAiReply = true)
        } else {
            analyzeCurrentPosition(moverColor, moveResult)
        }
    }

    fun playPvMove(pvLine: PvLine) {
        if (game.isGameOver()) return
        val moverColor = game.turn
        val move = game.makeMove(pvLine.move.from, pvLine.move.to, pvLine.move.promotion) ?: return
        _uiState.update { it.copy(selectedSquare = null, legalTargets = emptySet()) }
        syncBoardState()
        analyzeCurrentPosition(moverColor, move)
    }

    fun playEngineBestMove() {
        if (game.isGameOver()) return
        val currentBest = _uiState.value.bestMove
        if (currentBest != null) {
            val move = game.makeMove(currentBest.from, currentBest.to, currentBest.promotion)
            if (move != null) {
                _uiState.update { it.copy(selectedSquare = null, legalTargets = emptySet()) }
                syncBoardState()
                analyzeCurrentPosition()
                return
            }
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                ChessEngine.computeMultiPv(game.fen(), _uiState.value.depth)
            }
            val best = result.lines.firstOrNull()?.move ?: return@launch
            game.makeMove(best.from, best.to, best.promotion)
            _uiState.update { it.copy(selectedSquare = null, legalTargets = emptySet()) }
            syncBoardState()
            analyzeCurrentPosition()
        }
    }

    fun resetGame() {
        aiResponseJob?.cancel()
        game.reset()
        moveAccuracies.clear()
        lastEvalBeforeMove = 25
        _uiState.update {
            it.copy(
                selectedSquare = null,
                legalTargets = emptySet(),
                currentPuzzle = null,
                moveQualityKind = MoveQualityKind.NONE,
                lastEvaluatedMoveSan = null,
                accuracyPercent = 100,
                pendingPromotion = null
            )
        }
        syncBoardState()
        analyzeCurrentPosition()
        val lang = _uiState.value.lang
        emitToast(
            if (lang == AppLang.AR) "تم بدء مباراة جديدة" else "New game started",
            ToastKind.INFO
        )
    }

    fun flipBoard() {
        _uiState.update {
            it.copy(boardOrientation = it.boardOrientation.opposite())
        }
    }

    fun undoMove() {
        aiResponseJob?.cancel()
        val undone = game.undo() ?: return
        if (_uiState.value.appMode == AppMode.VS_ENGINE && undone.piece.color != _uiState.value.boardOrientation) {
            game.undo()
        }
        _uiState.update {
            it.copy(selectedSquare = null, legalTargets = emptySet(), pendingPromotion = null)
        }
        syncBoardState()
        analyzeCurrentPosition()
    }

    fun toggleVisionArrows() {
        val next = !_uiState.value.showVisionArrows
        _uiState.update { it.copy(showVisionArrows = next) }
        viewModelScope.launch { prefsRepo.setShowVisionArrows(next) }
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.lang == AppLang.AR) AppLang.EN else AppLang.AR
        _uiState.update { it.copy(lang = next) }
        viewModelScope.launch { prefsRepo.setLanguage(next) }
    }

    fun setAppMode(mode: AppMode) {
        aiResponseJob?.cancel()
        _uiState.update { it.copy(appMode = mode) }
    }

    fun setActiveTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setSearchDepth(depth: Int) {
        val clamped = depth.coerceIn(1, 3)
        _uiState.update { it.copy(depth = clamped) }
        viewModelScope.launch { prefsRepo.setSearchDepth(clamped) }
        analyzeCurrentPosition()
    }

    fun loadNextPuzzle() {
        aiResponseJob?.cancel()
        val puzzles = ChessEngine.TACTICAL_PUZZLES
        puzzleIndex = (puzzleIndex + 1) % puzzles.size
        val puzzle = puzzles[puzzleIndex]

        game.loadFen(puzzle.fen)
        moveAccuracies.clear()
        _uiState.update {
            it.copy(
                appMode = AppMode.PUZZLE,
                activeTab = StudioTab.TRAINER,
                currentPuzzle = puzzle,
                boardOrientation = game.turn,
                selectedSquare = null,
                legalTargets = emptySet(),
                moveQualityKind = MoveQualityKind.NONE,
                lastEvaluatedMoveSan = null,
                accuracyPercent = 100
            )
        }
        syncBoardState()
        analyzeCurrentPosition()
        val lang = _uiState.value.lang
        emitToast(
            if (lang == AppLang.AR) "تم تحميل لغز تكتيكي جديد!" else "New tactical puzzle loaded!",
            ToastKind.SUCCESS
        )
    }

    fun showPuzzleHint() {
        val puzzle = _uiState.value.currentPuzzle
        if (puzzle == null) {
            loadNextPuzzle()
            return
        }
        val fromStr = puzzle.solutionUci.take(2)
        val fromSq = Square.fromAlgebraic(fromStr) ?: return
        val targets = game.legalMoves(fromSq).map { it.to }.toSet()
        _uiState.update {
            it.copy(selectedSquare = fromSq, legalTargets = targets)
        }
        val lang = _uiState.value.lang
        emitToast(
            if (lang == AppLang.AR) "تلميح: حرك القطعة الموجودة في المربع $fromStr"
            else "Hint: Move the piece on square $fromStr",
            ToastKind.INFO
        )
    }

    fun onFenInputChanged(value: String) {
        _uiState.update { it.copy(fenInput = value) }
    }

    fun onPgnInputChanged(value: String) {
        _uiState.update { it.copy(pgnInput = value) }
    }

    fun importFen() {
        val fenStr = _uiState.value.fenInput.trim()
        val loaded = game.loadFen(fenStr)
        val lang = _uiState.value.lang
        if (!loaded) {
            emitToast(
                if (lang == AppLang.AR) "صيغة FEN غير صالحة!" else "Invalid FEN string!",
                ToastKind.WARN
            )
            return
        }
        aiResponseJob?.cancel()
        _uiState.update {
            it.copy(
                selectedSquare = null,
                legalTargets = emptySet(),
                currentPuzzle = null,
                moveQualityKind = MoveQualityKind.NONE
            )
        }
        syncBoardState()
        analyzeCurrentPosition()
        emitToast(
            if (lang == AppLang.AR) "تم تحميل وضعية FEN بنجاح" else "FEN position loaded successfully",
            ToastKind.SUCCESS
        )
    }

    fun importPgn() {
        val pgnStr = _uiState.value.pgnInput.trim()
        if (pgnStr.isEmpty()) return
        val loaded = game.loadPgn(pgnStr)
        val lang = _uiState.value.lang
        if (!loaded) {
            emitToast(
                if (lang == AppLang.AR) "تعذر قراءة نص PGN، تأكد من صحة النقلات" else "Invalid PGN format",
                ToastKind.WARN
            )
            return
        }
        aiResponseJob?.cancel()
        _uiState.update {
            it.copy(
                selectedSquare = null,
                legalTargets = emptySet(),
                currentPuzzle = null,
                activeTab = StudioTab.MOVES
            )
        }
        syncBoardState()
        analyzeCurrentPosition()
        emitToast(
            if (lang == AppLang.AR) "تم استيراد المباراة بنجاح" else "PGN game imported successfully",
            ToastKind.SUCCESS
        )
    }

    fun currentPgnForExport(): String = game.pgn()

    fun notifyCopied() {
        val lang = _uiState.value.lang
        emitToast(
            if (lang == AppLang.AR) "تم النسخ إلى الحافظة" else "Copied to clipboard",
            ToastKind.SUCCESS
        )
    }

    private fun syncBoardState() {
        val snapshot = game.boardSnapshot()
        val history = game.history()
        val inCheck = game.inCheck()
        val checkSq = if (inCheck) game.findKing(game.turn) else null

        // Calculate material & captured pieces
        val initialCounts = mapOf(
            PieceType.PAWN to 8,
            PieceType.KNIGHT to 2,
            PieceType.BISHOP to 2,
            PieceType.ROOK to 2,
            PieceType.QUEEN to 1
        )
        val whiteCounts = mutableMapOf<PieceType, Int>()
        val blackCounts = mutableMapOf<PieceType, Int>()
        for (p in snapshot) {
            if (p != null && p.type != PieceType.KING) {
                if (p.color == PieceColor.WHITE) {
                    whiteCounts[p.type] = (whiteCounts[p.type] ?: 0) + 1
                } else {
                    blackCounts[p.type] = (blackCounts[p.type] ?: 0) + 1
                }
            }
        }

        val orderedTypes = listOf(
            PieceType.QUEEN,
            PieceType.ROOK,
            PieceType.BISHOP,
            PieceType.KNIGHT,
            PieceType.PAWN
        )
        var whiteScore = 0
        var blackScore = 0
        val capturedByWhite = mutableListOf<PieceType>()
        val capturedByBlack = mutableListOf<PieceType>()

        for (type in orderedTypes) {
            val wCount = whiteCounts[type] ?: 0
            val bCount = blackCounts[type] ?: 0
            whiteScore += wCount * (type.valueCp / 100)
            blackScore += bCount * (type.valueCp / 100)

            val missingBlack = (initialCounts[type]!! - bCount).coerceAtLeast(0)
            val missingWhite = (initialCounts[type]!! - wCount).coerceAtLeast(0)
            repeat(missingBlack) { capturedByWhite.add(type) }
            repeat(missingWhite) { capturedByBlack.add(type) }
        }

        // Detect opening
        val joinedMoves = history.joinToString(" ") { it.san }
        var openingAr = if (history.isEmpty()) "وضعية البداية القياسية" else "وضعية حرة / منتصف اللعب"
        var openingEn = if (history.isEmpty()) "Standard Starting Position" else "Custom / Middlegame Position"
        for (op in ChessEngine.OPENINGS_DB) {
            if (joinedMoves.startsWith(op.movesPrefix)) {
                openingAr = op.nameAr
                openingEn = op.nameEn
                break
            }
        }

        _uiState.update {
            it.copy(
                board = snapshot,
                turn = game.turn,
                lastMove = game.lastMove(),
                checkSquare = checkSq,
                isCheckmate = game.inCheckmate(),
                isDraw = game.inDraw(),
                isGameOver = game.isGameOver(),
                history = history,
                capturedByWhite = capturedByWhite,
                capturedByBlack = capturedByBlack,
                materialDiff = whiteScore - blackScore,
                openingNameAr = openingAr,
                openingNameEn = openingEn,
                fenInput = game.fen(),
                pgnInput = game.pgn()
            )
        }
    }

    private fun analyzeCurrentPosition(
        lastMoverColor: PieceColor? = null,
        lastMoveObj: ChessMove? = null,
        triggerAiReply: Boolean = false
    ) {
        analysisJob?.cancel()
        val fenSnapshot = game.fen()
        val depthSnapshot = _uiState.value.depth

        analysisJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true) }
            val result = withContext(Dispatchers.Default) {
                ChessEngine.computeMultiPv(fenSnapshot, depthSnapshot)
            }

            // Ensure FEN hasn't changed while computing
            if (game.fen() != fenSnapshot) return@launch

            val topLine = result.lines.firstOrNull()
            var qualityKind = _uiState.value.moveQualityKind
            var qualitySan = _uiState.value.lastEvaluatedMoveSan

            if (lastMoverColor != null && lastMoveObj != null) {
                val diff = if (lastMoverColor == PieceColor.WHITE) {
                    lastEvalBeforeMove - result.scoreCp
                } else {
                    result.scoreCp - lastEvalBeforeMove
                }
                val (kind, acc) = when {
                    diff <= 25 -> MoveQualityKind.EXCELLENT to 100
                    diff <= 90 -> MoveQualityKind.GOOD to 82
                    diff <= 200 -> MoveQualityKind.INACCURACY to 55
                    else -> MoveQualityKind.BLUNDER to 20
                }
                qualityKind = kind
                qualitySan = lastMoveObj.san
                moveAccuracies.add(acc)
            }
            lastEvalBeforeMove = result.scoreCp

            val avgAcc = if (moveAccuracies.isEmpty()) 100 else {
                (moveAccuracies.sum().toFloat() / moveAccuracies.size).toInt()
            }

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    engineEvalCp = result.scoreCp,
                    bestMove = topLine?.move,
                    bestMoveSan = topLine?.san ?: "-",
                    bestMoveUci = topLine?.let { line -> "(${line.uci})" } ?: "",
                    pvLines = result.lines,
                    evaluatedNodes = result.evaluatedNodes,
                    moveQualityKind = qualityKind,
                    lastEvaluatedMoveSan = qualitySan,
                    accuracyPercent = avgAcc
                )
            }

            if (triggerAiReply && _uiState.value.appMode == AppMode.VS_ENGINE && !game.isGameOver()) {
                val best = topLine?.move ?: return@launch
                aiResponseJob?.cancel()
                aiResponseJob = viewModelScope.launch {
                    delay(280)
                    if (game.fen() == fenSnapshot && !game.isGameOver()) {
                        game.makeMove(best.from, best.to, best.promotion)
                        syncBoardState()
                        analyzeCurrentPosition()
                    }
                }
            }
        }
    }

    private fun emitToast(message: String, kind: ToastKind) {
        _toastEvents.tryEmit(ToastEvent(message, kind))
    }

    fun formatEvalStatus(scoreCp: Int, lang: AppLang): String = if (lang == AppLang.AR) {
        when {
            abs(scoreCp) >= 90000 -> "كش مات إجباري"
            scoreCp > 120 -> "أفضلية واضحة للأبيض"
            scoreCp > 30 -> "أفضلية طفيفة للأبيض"
            scoreCp < -120 -> "أفضلية واضحة للأسود"
            scoreCp < -30 -> "أفضلية طفيفة للأسود"
            else -> "موقف متعادل"
        }
    } else {
        when {
            abs(scoreCp) >= 90000 -> "Forced Checkmate"
            scoreCp > 120 -> "Clear White Advantage"
            scoreCp > 30 -> "Slight White Edge"
            scoreCp < -120 -> "Clear Black Advantage"
            scoreCp < -30 -> "Slight Black Edge"
            else -> "Equal Position"
        }
    }
}
