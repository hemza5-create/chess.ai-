package com.example.chess

import kotlin.math.abs

object ChessEngine {

    private val PST_PAWN = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5, 5, 10, 25, 25, 10, 5, 5,
        0, 0, 0, 20, 20, 0, 0, 0,
        5, -5, -10, 0, 0, -10, -5, 5,
        5, 10, 10, -20, -20, 10, 10, 5,
        0, 0, 0, 0, 0, 0, 0, 0
    )

    private val PST_KNIGHT = intArrayOf(
        -50, -40, -30, -30, -30, -30, -40, -50,
        -40, -20, 0, 0, 0, 0, -20, -40,
        -30, 0, 10, 15, 15, 10, 0, -30,
        -30, 5, 15, 20, 20, 15, 5, -30,
        -30, 0, 15, 20, 20, 15, 0, -30,
        -30, 5, 10, 15, 15, 10, 5, -30,
        -40, -20, 0, 5, 5, 0, -20, -40,
        -50, -40, -30, -30, -30, -30, -40, -50
    )

    private val PST_BISHOP = intArrayOf(
        -20, -10, -10, -10, -10, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 10, 10, 5, 0, -10,
        -10, 5, 5, 10, 10, 5, 5, -10,
        -10, 0, 10, 10, 10, 10, 0, -10,
        -10, 10, 10, 10, 10, 10, 10, -10,
        -10, 5, 0, 0, 0, 0, 5, -10,
        -20, -10, -10, -10, -10, -10, -10, -20
    )

    private val PST_ROOK = intArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0,
        5, 10, 10, 10, 10, 10, 10, 5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        0, 0, 0, 5, 5, 0, 0, 0
    )

    private val PST_QUEEN = intArrayOf(
        -20, -10, -10, -5, -5, -10, -10, -20,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -20, -10, -10, -5, -5, -10, -10, -20
    )

    private class NodeCounter(var count: Int = 0)

    fun evaluateStatic(game: ChessGame): Int {
        if (game.inCheckmate()) {
            return if (game.turn == PieceColor.WHITE) -99999 else 99999
        }
        if (game.inDraw()) return 0

        var totalEval = 0
        val board = game.boardSnapshot()
        for (i in 0 until 64) {
            val piece = board[i] ?: continue
            val r = i / 8
            val c = i % 8
            val idx = if (piece.color == PieceColor.WHITE) r * 8 + c else (7 - r) * 8 + c
            val positional = when (piece.type) {
                PieceType.PAWN -> PST_PAWN[idx]
                PieceType.KNIGHT -> PST_KNIGHT[idx]
                PieceType.BISHOP -> PST_BISHOP[idx]
                PieceType.ROOK -> PST_ROOK[idx]
                PieceType.QUEEN -> PST_QUEEN[idx]
                PieceType.KING -> 0
            }
            val value = piece.type.valueCp + positional
            totalEval += if (piece.color == PieceColor.WHITE) value else -value
        }
        return totalEval
    }

    private fun minimax(
        game: ChessGame,
        depth: Int,
        alphaInit: Int,
        betaInit: Int,
        isMaximizing: Boolean,
        counter: NodeCounter
    ): Int {
        counter.count++
        if (depth == 0 || game.isGameOver()) {
            val raw = evaluateStatic(game)
            return if (abs(raw) >= 99000) {
                if (raw > 0) raw + depth else raw - depth
            } else {
                raw
            }
        }

        val moves = game.legalMoves().sortedByDescending { moveOrderScore(it) }
        var alpha = alphaInit
        var beta = betaInit

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE / 2
            for (move in moves) {
                game.makeMoveFast(move)
                val ev = minimax(game, depth - 1, alpha, beta, false, counter)
                game.undo()
                if (ev > maxEval) maxEval = ev
                if (ev > alpha) alpha = ev
                if (beta <= alpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE / 2
            for (move in moves) {
                game.makeMoveFast(move)
                val ev = minimax(game, depth - 1, alpha, beta, true, counter)
                game.undo()
                if (ev < minEval) minEval = ev
                if (ev < beta) beta = ev
                if (beta <= alpha) break
            }
            return minEval
        }
    }

    private fun moveOrderScore(move: ChessMove): Int {
        var s = 0
        if (move.captured != null) {
            s += 1000 + move.captured.type.valueCp - move.piece.type.valueCp / 10
        }
        if (move.promotion != null) {
            s += 800 + move.promotion.valueCp
        }
        if (move.isCastleKingside || move.isCastleQueenside) s += 60
        return s
    }

    fun computeMultiPv(fen: String, depth: Int): EngineAnalysisResult {
        val counter = NodeCounter()
        val tempGame = ChessGame(fen)
        val isWhite = tempGame.turn == PieceColor.WHITE
        val legalMoves = tempGame.legalMovesWithSan()

        if (legalMoves.isEmpty()) {
            return EngineAnalysisResult(
                scoreCp = evaluateStatic(tempGame),
                lines = emptyList(),
                evaluatedNodes = 1
            )
        }

        val candidates = ArrayList<PvLine>(legalMoves.size)
        val searchDepth = (depth - 1).coerceAtLeast(0)

        for (move in legalMoves) {
            tempGame.makeMoveFast(move)
            val score = minimax(
                game = tempGame,
                depth = searchDepth,
                alphaInit = Int.MIN_VALUE / 2,
                betaInit = Int.MAX_VALUE / 2,
                isMaximizing = !isWhite,
                counter = counter
            )

            // 3-ply quick tactical rollout for Principal Variation display
            val pvSan = mutableListOf(move.san)
            val rollout = tempGame.copy()
            repeat(3) {
                val nextMoves = rollout.legalMovesWithSan()
                if (nextMoves.isEmpty()) return@repeat
                val bestNext = nextMoves.maxByOrNull { moveOrderScore(it) } ?: return@repeat
                rollout.makeMoveFast(bestNext)
                pvSan.add(bestNext.san)
            }

            tempGame.undo()
            candidates.add(
                PvLine(
                    move = move,
                    uci = move.uci,
                    san = move.san,
                    scoreCp = score,
                    pvText = pvSan.joinToString(" ")
                )
            )
        }

        val sorted = if (isWhite) {
            candidates.sortedByDescending { it.scoreCp }
        } else {
            candidates.sortedBy { it.scoreCp }
        }

        return EngineAnalysisResult(
            scoreCp = sorted.first().scoreCp,
            lines = sorted.take(3),
            evaluatedNodes = counter.count
        )
    }

    fun formatScore(cp: Int): String {
        if (abs(cp) >= 90000) {
            return if (cp > 0) "+M" else "-M"
        }
        val pawns = cp / 100.0
        val formatted = String.format(java.util.Locale.US, "%.2f", pawns)
        return if (cp > 0) "+$formatted" else formatted
    }

    val OPENINGS_DB = listOf(
        OpeningEntry("e4 e5 Nf3 Nc6 Bb5", "افتتاحية روي لوبيز (الإسباني)", "Ruy Lopez Opening"),
        OpeningEntry("e4 e5 Nf3 Nc6 Bc4", "الافتتاحية الإيطالية", "Italian Game"),
        OpeningEntry("e4 c5", "الدفاع الصقلي", "Sicilian Defense"),
        OpeningEntry("e4 e6", "الدفاع الفرنسي", "French Defense"),
        OpeningEntry("e4 c6", "دفاع كارو-كان", "Caro-Kann Defense"),
        OpeningEntry("d4 d5 c4", "مناورة الوزير (Queen's Gambit)", "Queen's Gambit"),
        OpeningEntry("d4 Nf6 c4 g6", "الدفاع الملكي الهندي", "King's Indian Defense"),
        OpeningEntry("e4 e5", "افتتاحية بيدق الملك المفتوحة", "Open King's Pawn Game"),
        OpeningEntry("d4 d5", "افتتاحية بيدق الوزير المغلقة", "Closed Queen's Pawn Game")
    )

    val TACTICAL_PUZZLES = listOf(
        TacticalPuzzle(
            id = "scholars_mate",
            fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
            solutionUci = "h5f7",
            rating = "950 ELO",
            descriptionAr = "كش مات الباحث (Scholar's Mate): الأبيض يلعب ويكسب فوراً!",
            descriptionEn = "Scholar's Mate: White to play and checkmate immediately!"
        ),
        TacticalPuzzle(
            id = "legal_trap",
            fen = "r2qkb1r/pp2nppp/3p4/2pNN1B1/2BnP3/3P4/PPP2PPP/R2bK2R w KQkq - 1 10",
            solutionUci = "d5f6",
            rating = "1650 ELO",
            descriptionAr = "تكتيك ليغال الشهير: تضحية مذهلة بالحصان تؤدي لكش مات محققة!",
            descriptionEn = "Légal Trap Motif: A dazzling knight jump leads to forced mate!"
        ),
        TacticalPuzzle(
            id = "back_rank",
            fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            solutionUci = "d1d8",
            rating = "1100 ELO",
            descriptionAr = "كش مات الصف الخلفي (Back Rank Mate): استغل ضعف الصف الأخير للأسود.",
            descriptionEn = "Back Rank Mate: Exploit Black's trapped king on the back rank."
        ),
        TacticalPuzzle(
            id = "tactical_pin",
            fen = "r1b1k2r/ppppqppp/2n2n2/4p3/1bB1P3/2NP1N2/PPP2PPP/R1BQK2R w KQkq - 3 6",
            solutionUci = "c1g5",
            rating = "1350 ELO",
            descriptionAr = "الربط التكتيكي (Pin): اربط حصان الخصم بالوزير لزيادة الضغط على الوسط.",
            descriptionEn = "Tactical Pin: Pin the knight to the queen to increase central pressure."
        )
    )
}
