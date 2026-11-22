package com.example.chess

import kotlin.math.abs

class ChessGame(initialFen: String = START_FEN) {

    private val board = arrayOfNulls<ChessPiece>(64)
    var turn: PieceColor = PieceColor.WHITE
        private set

    private var whiteCastleK = true
    private var whiteCastleQ = true
    private var blackCastleK = true
    private var blackCastleQ = true
    private var epSquare: Square? = null
    private var halfMoveClock = 0
    private var fullMoveNumber = 1

    private val moveHistory = mutableListOf<ChessMove>()
    private val undoStack = mutableListOf<GameStateSnapshot>()
    private val repetitionCounts = mutableMapOf<String, Int>()
    private val pgnHeaders = linkedMapOf<String, String>()

    private data class GameStateSnapshot(
        val board: Array<ChessPiece?>,
        val turn: PieceColor,
        val whiteCastleK: Boolean,
        val whiteCastleQ: Boolean,
        val blackCastleK: Boolean,
        val blackCastleQ: Boolean,
        val epSquare: Square?,
        val halfMoveClock: Int,
        val fullMoveNumber: Int,
        val positionKey: String
    )

    init {
        loadFen(initialFen)
    }

    fun reset() {
        loadFen(START_FEN)
    }

    fun copy(): ChessGame {
        val clone = ChessGame()
        board.copyInto(clone.board)
        clone.turn = turn
        clone.whiteCastleK = whiteCastleK
        clone.whiteCastleQ = whiteCastleQ
        clone.blackCastleK = blackCastleK
        clone.blackCastleQ = blackCastleQ
        clone.epSquare = epSquare
        clone.halfMoveClock = halfMoveClock
        clone.fullMoveNumber = fullMoveNumber
        clone.moveHistory.clear()
        clone.moveHistory.addAll(moveHistory)
        clone.repetitionCounts.clear()
        clone.repetitionCounts.putAll(repetitionCounts)
        return clone
    }

    fun pieceAt(square: Square): ChessPiece? = board[square.index]

    fun pieceAt(algebraic: String): ChessPiece? =
        Square.fromAlgebraic(algebraic)?.let { board[it.index] }

    fun boardSnapshot(): List<ChessPiece?> = board.toList()

    fun history(): List<ChessMove> = moveHistory.toList()

    fun lastMove(): ChessMove? = moveHistory.lastOrNull()

    fun findKing(color: PieceColor): Square? {
        for (i in 0 until 64) {
            val p = board[i]
            if (p != null && p.color == color && p.type == PieceType.KING) {
                return Square(i)
            }
        }
        return null
    }

    fun loadFen(fen: String): Boolean {
        val parts = fen.trim().split(Regex("\\s+"))
        if (parts.size < 4) return false

        val rows = parts[0].split('/')
        if (rows.size != 8) return false

        val tempBoard = arrayOfNulls<ChessPiece>(64)
        for (r in 0 until 8) {
            var f = 0
            for (ch in rows[r]) {
                if (ch in '1'..'8') {
                    f += ch - '0'
                } else {
                    val type = PieceType.fromFenChar(ch) ?: return false
                    val color = if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    if (f >= 8) return false
                    tempBoard[r * 8 + f] = ChessPiece(color, type)
                    f++
                }
            }
            if (f != 8) return false
        }

        val nextTurn = when (parts[1]) {
            "w" -> PieceColor.WHITE
            "b" -> PieceColor.BLACK
            else -> return false
        }

        val castling = parts[2]
        val wK = castling.contains('K')
        val wQ = castling.contains('Q')
        val bK = castling.contains('k')
        val bQ = castling.contains('q')
        if (castling != "-" && !castling.all { it in "KQkq" }) return false

        val ep = if (parts[3] == "-") {
            null
        } else {
            Square.fromAlgebraic(parts[3]) ?: return false
        }

        val half = parts.getOrNull(4)?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val full = parts.getOrNull(5)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

        tempBoard.copyInto(board)
        turn = nextTurn
        whiteCastleK = wK
        whiteCastleQ = wQ
        blackCastleK = bK
        blackCastleQ = bQ
        epSquare = ep
        halfMoveClock = half
        fullMoveNumber = full

        moveHistory.clear()
        undoStack.clear()
        repetitionCounts.clear()
        pgnHeaders.clear()

        val key = positionKey()
        repetitionCounts[key] = 1
        return true
    }

    fun fen(): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            var empty = 0
            for (f in 0 until 8) {
                val p = board[r * 8 + f]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    val ch = if (p.color == PieceColor.WHITE) {
                        p.type.fenChar.uppercaseChar()
                    } else {
                        p.type.fenChar
                    }
                    sb.append(ch)
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }
        sb.append(' ').append(turn.fenChar).append(' ')
        val castling = buildString {
            if (whiteCastleK) append('K')
            if (whiteCastleQ) append('Q')
            if (blackCastleK) append('k')
            if (blackCastleQ) append('q')
        }.ifEmpty { "-" }
        sb.append(castling).append(' ')
        sb.append(epSquare?.toAlgebraic() ?: "-").append(' ')
        sb.append(halfMoveClock).append(' ').append(fullMoveNumber)
        return sb.toString()
    }

    private fun positionKey(): String {
        val full = fen()
        val parts = full.split(' ')
        return "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]}"
    }

    fun inCheck(): Boolean {
        val kingSq = findKing(turn) ?: return false
        return isSquareAttacked(kingSq, turn.opposite())
    }

    fun legalMoves(square: Square? = null): List<ChessMove> {
        val pseudo = generatePseudoLegalMoves(square)
        val legal = ArrayList<ChessMove>(pseudo.size)
        for (move in pseudo) {
            if (isLegalAfterPseudoMove(move)) {
                legal.add(move)
            }
        }
        return legal
    }

    /**
     * Generates legal moves with SAN notation computed (used for UI & move history).
     */
    fun legalMovesWithSan(square: Square? = null): List<ChessMove> {
        val allLegal = legalMoves(null)
        val filtered = if (square == null) allLegal else allLegal.filter { it.from == square }
        return filtered.map { enrichWithSan(it, allLegal) }
    }

    fun makeMove(from: Square, to: Square, promotion: PieceType? = null): ChessMove? {
        val candidates = legalMoves(from).filter { it.to == to }
        if (candidates.isEmpty()) return null
        val chosen = if (candidates.size == 1) {
            candidates.first()
        } else {
            val promo = promotion ?: PieceType.QUEEN
            candidates.firstOrNull { it.promotion == promo } ?: candidates.first()
        }
        val allLegal = legalMoves(null)
        val enriched = enrichWithSan(chosen, allLegal)
        applyMoveInternal(enriched)
        return enriched
    }

    fun makeMoveUci(uci: String): ChessMove? {
        if (uci.length < 4) return null
        val from = Square.fromAlgebraic(uci.substring(0, 2)) ?: return null
        val to = Square.fromAlgebraic(uci.substring(2, 4)) ?: return null
        val promo = if (uci.length >= 5) PieceType.fromFenChar(uci[4]) else null
        return makeMove(from, to, promo)
    }

    fun makeMoveSan(sanToken: String): ChessMove? {
        val clean = normalizeSan(sanToken)
        val allLegal = legalMovesWithSan(null)
        val found = allLegal.firstOrNull {
            normalizeSan(it.san) == clean
        } ?: return null
        applyMoveInternal(found)
        return found
    }

    private fun normalizeSan(raw: String): String {
        return raw.trim()
            .replace("0-0-0", "O-O-O")
            .replace("0-0", "O-O")
            .trimEnd('+', '#', '!', '?')
    }

    fun makeMoveFast(move: ChessMove) {
        applyMoveInternal(move, trackHistory = true)
    }

    private fun applyMoveInternal(move: ChessMove, trackHistory: Boolean = true) {
        if (trackHistory) {
            undoStack.add(
                GameStateSnapshot(
                    board = board.copyOf(),
                    turn = turn,
                    whiteCastleK = whiteCastleK,
                    whiteCastleQ = whiteCastleQ,
                    blackCastleK = blackCastleK,
                    blackCastleQ = blackCastleQ,
                    epSquare = epSquare,
                    halfMoveClock = halfMoveClock,
                    fullMoveNumber = fullMoveNumber,
                    positionKey = positionKey()
                )
            )
            moveHistory.add(move)
        }

        // Move piece
        board[move.from.index] = null
        if (move.isEnPassant) {
            val capturedPawnSq = Square.of(move.to.file, move.from.rank)
            board[capturedPawnSq.index] = null
        }

        val placedPiece = if (move.promotion != null) {
            ChessPiece(move.piece.color, move.promotion)
        } else {
            move.piece
        }
        board[move.to.index] = placedPiece

        // Castling rook move
        if (move.isCastleKingside) {
            val r = move.from.rank
            board[Square.of(5, r).index] = board[Square.of(7, r).index]
            board[Square.of(7, r).index] = null
        } else if (move.isCastleQueenside) {
            val r = move.from.rank
            board[Square.of(3, r).index] = board[Square.of(0, r).index]
            board[Square.of(0, r).index] = null
        }

        // Update castling rights
        if (move.piece.type == PieceType.KING) {
            if (move.piece.color == PieceColor.WHITE) {
                whiteCastleK = false
                whiteCastleQ = false
            } else {
                blackCastleK = false
                blackCastleQ = false
            }
        }
        updateRookCastlingRights(move.from)
        updateRookCastlingRights(move.to)

        // Update en passant target
        epSquare = if (move.piece.type == PieceType.PAWN && abs(move.from.rank - move.to.rank) == 2) {
            Square.of(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else {
            null
        }

        // Update clocks
        if (move.piece.type == PieceType.PAWN || move.isCapture) {
            halfMoveClock = 0
        } else {
            halfMoveClock++
        }
        if (turn == PieceColor.BLACK) {
            fullMoveNumber++
        }

        turn = turn.opposite()

        if (trackHistory) {
            val key = positionKey()
            repetitionCounts[key] = (repetitionCounts[key] ?: 0) + 1
        }
    }

    private fun updateRookCastlingRights(sq: Square) {
        when (sq.index) {
            63 -> whiteCastleK = false // h1
            56 -> whiteCastleQ = false // a1
            7 -> blackCastleK = false  // h8
            0 -> blackCastleQ = false  // a8
        }
    }

    fun undo(): ChessMove? {
        if (undoStack.isEmpty() || moveHistory.isEmpty()) return null
        val currentKey = positionKey()
        val count = repetitionCounts[currentKey] ?: 0
        if (count <= 1) repetitionCounts.remove(currentKey) else repetitionCounts[currentKey] = count - 1

        val snap = undoStack.removeAt(undoStack.lastIndex)
        snap.board.copyInto(board)
        turn = snap.turn
        whiteCastleK = snap.whiteCastleK
        whiteCastleQ = snap.whiteCastleQ
        blackCastleK = snap.blackCastleK
        blackCastleQ = snap.blackCastleQ
        epSquare = snap.epSquare
        halfMoveClock = snap.halfMoveClock
        fullMoveNumber = snap.fullMoveNumber
        return moveHistory.removeAt(moveHistory.lastIndex)
    }

    fun inCheckmate(): Boolean = inCheck() && legalMoves().isEmpty()

    fun inStalemate(): Boolean = !inCheck() && legalMoves().isEmpty()

    fun inThreefoldRepetition(): Boolean = repetitionCounts.values.any { it >= 3 }

    fun insufficientMaterial(): Boolean {
        val pieces = mutableListOf<Pair<Square, ChessPiece>>()
        for (i in 0 until 64) {
            val p = board[i] ?: continue
            if (p.type != PieceType.KING) pieces.add(Square(i) to p)
        }
        if (pieces.isEmpty()) return true
        if (pieces.size == 1) {
            val t = pieces[0].second.type
            return t == PieceType.KNIGHT || t == PieceType.BISHOP
        }
        if (pieces.size == 2) {
            val p1 = pieces[0]
            val p2 = pieces[1]
            if (p1.second.type == PieceType.BISHOP && p2.second.type == PieceType.BISHOP &&
                p1.second.color != p2.second.color
            ) {
                val color1 = (p1.first.file + p1.first.rank) % 2
                val color2 = (p2.first.file + p2.first.rank) % 2
                return color1 == color2
            }
        }
        return false
    }

    fun inDraw(): Boolean =
        inStalemate() || halfMoveClock >= 100 || inThreefoldRepetition() || insufficientMaterial()

    fun isGameOver(): Boolean = inCheckmate() || inDraw()

    fun pgn(): String {
        val sb = StringBuilder()
        if (pgnHeaders.isNotEmpty()) {
            for ((k, v) in pgnHeaders) {
                sb.append("[$k \"$v\"]\n")
            }
            sb.append("\n")
        }
        if (moveHistory.isEmpty()) {
            if (sb.isEmpty()) {
                return "[Event \"Chess-Vision Stockfish UCI Analysis\"]\n\n*"
            }
            sb.append("*")
            return sb.toString()
        }
        for (i in moveHistory.indices) {
            if (i % 2 == 0) {
                sb.append("${i / 2 + 1}. ")
            }
            sb.append(moveHistory[i].san)
            if (i < moveHistory.lastIndex) sb.append(" ")
        }
        val result = when {
            inCheckmate() -> if (turn == PieceColor.BLACK) "1-0" else "0-1"
            inDraw() -> "1/2-1/2"
            else -> "*"
        }
        if (result != "*") sb.append(" ").append(result)
        return sb.toString()
    }

    fun loadPgn(rawPgn: String): Boolean {
        val text = rawPgn.trim()
        if (text.isEmpty()) return false

        val headers = linkedMapOf<String, String>()
        val headerRegex = Regex("""\[([A-Za-z0-9_]+)\s+"([^"]*)"\]""")
        for (match in headerRegex.findAll(text)) {
            headers[match.groupValues[1]] = match.groupValues[2]
        }

        // Remove headers, comments, variations, and NAGs
        var body = text.replace(headerRegex, " ")
        body = body.replace(Regex("""\{[^}]*\}"""), " ")
        body = body.replace(Regex(""";[^\n]*"""), " ")

        // Strip nested parentheses variations
        val noVars = StringBuilder()
        var depth = 0
        for (ch in body) {
            when (ch) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                else -> if (depth == 0) noVars.append(ch)
            }
        }

        val tokens = noVars.toString()
            .replace(Regex("""\$\d+"""), " ")
            .replace(Regex("""\d+\.+"""), " ")
            .trim()
            .split(Regex("""\s+"""))
            .filter { token ->
                token.isNotEmpty() &&
                    token != "1-0" && token != "0-1" &&
                    token != "1/2-1/2" && token != "*"
            }

        if (tokens.isEmpty()) return false

        val tempGame = ChessGame(headers["FEN"] ?: START_FEN)
        for (token in tokens) {
            if (tempGame.makeMoveSan(token) == null) {
                return false
            }
        }

        // Commit loaded state
        loadFen(headers["FEN"] ?: START_FEN)
        pgnHeaders.putAll(headers)
        for (token in tokens) {
            makeMoveSan(token)
        }
        return true
    }

    private fun enrichWithSan(move: ChessMove, allLegal: List<ChessMove>): ChessMove {
        val baseSan = when {
            move.isCastleKingside -> "O-O"
            move.isCastleQueenside -> "O-O-O"
            move.piece.type == PieceType.PAWN -> buildString {
                if (move.isCapture) {
                    append(move.from.fileChar).append('x')
                }
                append(move.to.toAlgebraic())
                if (move.promotion != null) {
                    append('=').append(move.promotion.sanChar)
                }
            }
            else -> buildString {
                append(move.piece.type.sanChar)
                val sameTypePeers = allLegal.filter {
                    it.from != move.from &&
                        it.to == move.to &&
                        it.piece.type == move.piece.type &&
                        it.piece.color == move.piece.color
                }
                if (sameTypePeers.isNotEmpty()) {
                    val sameFile = sameTypePeers.any { it.from.file == move.from.file }
                    val sameRank = sameTypePeers.any { it.from.rank == move.from.rank }
                    if (!sameFile) {
                        append(move.from.fileChar)
                    } else if (!sameRank) {
                        append(move.from.rankChar)
                    } else {
                        append(move.from.fileChar).append(move.from.rankChar)
                    }
                }
                if (move.isCapture) append('x')
                append(move.to.toAlgebraic())
            }
        }

        applyMoveInternal(move, trackHistory = true)
        val check = inCheck()
        val mate = check && legalMoves().isEmpty()
        undo()

        val suffix = when {
            mate -> "#"
            check -> "+"
            else -> ""
        }
        return move.copy(
            san = baseSan + suffix,
            isCheck = check,
            isCheckmate = mate
        )
    }

    private fun isLegalAfterPseudoMove(move: ChessMove): Boolean {
        applyMoveInternal(move, trackHistory = true)
        val moverColor = turn.opposite()
        val kingSq = findKing(moverColor)
        val safe = kingSq != null && !isSquareAttacked(kingSq, turn)
        undo()
        return safe
    }

    private fun generatePseudoLegalMoves(squareFilter: Square?): List<ChessMove> {
        val moves = ArrayList<ChessMove>(40)
        val startIdx = squareFilter?.index ?: 0
        val endIdx = squareFilter?.index ?: 63

        for (i in startIdx..endIdx) {
            val piece = board[i] ?: continue
            if (piece.color != turn) continue
            val from = Square(i)
            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(from, piece, moves)
                PieceType.KNIGHT -> generateJumpMoves(from, piece, KNIGHT_OFFSETS, moves)
                PieceType.BISHOP -> generateSlideMoves(from, piece, BISHOP_DIRS, moves)
                PieceType.ROOK -> generateSlideMoves(from, piece, ROOK_DIRS, moves)
                PieceType.QUEEN -> generateSlideMoves(from, piece, QUEEN_DIRS, moves)
                PieceType.KING -> generateKingMoves(from, piece, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Square, piece: ChessPiece, out: MutableList<ChessMove>) {
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRank = if (piece.color == PieceColor.WHITE) 6 else 1
        val promoRank = if (piece.color == PieceColor.WHITE) 0 else 7

        val oneRank = from.rank + dir
        if (oneRank in 0..7) {
            val oneSq = Square.of(from.file, oneRank)
            if (board[oneSq.index] == null) {
                addPawnAdvance(from, oneSq, piece, null, false, oneRank == promoRank, out)
                if (from.rank == startRank) {
                    val twoRank = from.rank + dir * 2
                    val twoSq = Square.of(from.file, twoRank)
                    if (board[twoSq.index] == null) {
                        out.add(ChessMove(from, twoSq, piece))
                    }
                }
            }

            // Captures
            for (df in intArrayOf(-1, 1)) {
                val cf = from.file + df
                if (cf !in 0..7) continue
                val targetSq = Square.of(cf, oneRank)
                val targetPiece = board[targetSq.index]
                if (targetPiece != null && targetPiece.color != piece.color) {
                    addPawnAdvance(from, targetSq, piece, targetPiece, false, oneRank == promoRank, out)
                } else if (epSquare == targetSq) {
                    val epVictimSq = Square.of(cf, from.rank)
                    val epVictim = board[epVictimSq.index]
                    if (epVictim != null && epVictim.color != piece.color && epVictim.type == PieceType.PAWN) {
                        out.add(
                            ChessMove(
                                from = from,
                                to = targetSq,
                                piece = piece,
                                captured = epVictim,
                                isEnPassant = true
                            )
                        )
                    }
                }
            }
        }
    }

    private fun addPawnAdvance(
        from: Square,
        to: Square,
        piece: ChessPiece,
        captured: ChessPiece?,
        isEnPassant: Boolean,
        isPromotion: Boolean,
        out: MutableList<ChessMove>
    ) {
        if (isPromotion) {
            for (promo in PROMO_TYPES) {
                out.add(
                    ChessMove(
                        from = from,
                        to = to,
                        piece = piece,
                        captured = captured,
                        promotion = promo,
                        isEnPassant = isEnPassant
                    )
                )
            }
        } else {
            out.add(
                ChessMove(
                    from = from,
                    to = to,
                    piece = piece,
                    captured = captured,
                    isEnPassant = isEnPassant
                )
            )
        }
    }

    private fun generateJumpMoves(
        from: Square,
        piece: ChessPiece,
        offsets: Array<IntArray>,
        out: MutableList<ChessMove>
    ) {
        for (off in offsets) {
            val nf = from.file + off[0]
            val nr = from.rank + off[1]
            if (nf !in 0..7 || nr !in 0..7) continue
            val to = Square.of(nf, nr)
            val target = board[to.index]
            if (target == null || target.color != piece.color) {
                out.add(ChessMove(from, to, piece, captured = target))
            }
        }
    }

    private fun generateSlideMoves(
        from: Square,
        piece: ChessPiece,
        dirs: Array<IntArray>,
        out: MutableList<ChessMove>
    ) {
        for (d in dirs) {
            var nf = from.file + d[0]
            var nr = from.rank + d[1]
            while (nf in 0..7 && nr in 0..7) {
                val to = Square.of(nf, nr)
                val target = board[to.index]
                if (target == null) {
                    out.add(ChessMove(from, to, piece))
                } else {
                    if (target.color != piece.color) {
                        out.add(ChessMove(from, to, piece, captured = target))
                    }
                    break
                }
                nf += d[0]
                nr += d[1]
            }
        }
    }

    private fun generateKingMoves(from: Square, piece: ChessPiece, out: MutableList<ChessMove>) {
        generateJumpMoves(from, piece, QUEEN_DIRS, out)

        val homeRank = if (piece.color == PieceColor.WHITE) 7 else 0
        if (from.file == 4 && from.rank == homeRank && !isSquareAttacked(from, piece.color.opposite())) {
            val canK = if (piece.color == PieceColor.WHITE) whiteCastleK else blackCastleK
            val canQ = if (piece.color == PieceColor.WHITE) whiteCastleQ else blackCastleQ

            if (canK) {
                val f1 = Square.of(5, homeRank)
                val g1 = Square.of(6, homeRank)
                val h1 = Square.of(7, homeRank)
                val rook = board[h1.index]
                if (rook?.type == PieceType.ROOK && rook.color == piece.color &&
                    board[f1.index] == null && board[g1.index] == null &&
                    !isSquareAttacked(f1, piece.color.opposite()) &&
                    !isSquareAttacked(g1, piece.color.opposite())
                ) {
                    out.add(ChessMove(from, g1, piece, isCastleKingside = true))
                }
            }
            if (canQ) {
                val d1 = Square.of(3, homeRank)
                val c1 = Square.of(2, homeRank)
                val b1 = Square.of(1, homeRank)
                val a1 = Square.of(0, homeRank)
                val rook = board[a1.index]
                if (rook?.type == PieceType.ROOK && rook.color == piece.color &&
                    board[d1.index] == null && board[c1.index] == null && board[b1.index] == null &&
                    !isSquareAttacked(d1, piece.color.opposite()) &&
                    !isSquareAttacked(c1, piece.color.opposite())
                ) {
                    out.add(ChessMove(from, c1, piece, isCastleQueenside = true))
                }
            }
        }
    }

    fun isSquareAttacked(sq: Square, byColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        val pawnRank = sq.rank + pawnDir
        if (pawnRank in 0..7) {
            for (df in intArrayOf(-1, 1)) {
                val pf = sq.file + df
                if (pf in 0..7) {
                    val p = board[Square.of(pf, pawnRank).index]
                    if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
                }
            }
        }

        // Knight attacks
        for (off in KNIGHT_OFFSETS) {
            val nf = sq.file + off[0]
            val nr = sq.rank + off[1]
            if (nf in 0..7 && nr in 0..7) {
                val p = board[Square.of(nf, nr).index]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for (off in QUEEN_DIRS) {
            val kf = sq.file + off[0]
            val kr = sq.rank + off[1]
            if (kf in 0..7 && kr in 0..7) {
                val p = board[Square.of(kf, kr).index]
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }

        // Bishop / Queen diagonal attacks
        for (d in BISHOP_DIRS) {
            var nf = sq.file + d[0]
            var nr = sq.rank + d[1]
            while (nf in 0..7 && nr in 0..7) {
                val p = board[Square.of(nf, nr).index]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nf += d[0]
                nr += d[1]
            }
        }

        // Rook / Queen orthogonal attacks
        for (d in ROOK_DIRS) {
            var nf = sq.file + d[0]
            var nr = sq.rank + d[1]
            while (nf in 0..7 && nr in 0..7) {
                val p = board[Square.of(nf, nr).index]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                nf += d[0]
                nr += d[1]
            }
        }

        return false
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        private val PROMO_TYPES = arrayOf(
            PieceType.QUEEN,
            PieceType.ROOK,
            PieceType.BISHOP,
            PieceType.KNIGHT
        )

        private val KNIGHT_OFFSETS = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1)
        )

        private val BISHOP_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(1, 1)
        )

        private val ROOK_DIRS = arrayOf(
            intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(1, 0)
        )

        private val QUEEN_DIRS = arrayOf(
            intArrayOf(-1, -1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(1, 1),
            intArrayOf(0, -1), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(1, 0)
        )
    }
}
