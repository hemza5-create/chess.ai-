package com.example.chess

enum class PieceColor {
    WHITE,
    BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE

    val fenChar: Char get() = if (this == WHITE) 'w' else 'b'
}

enum class PieceType(
    val fenChar: Char,
    val sanChar: Char,
    val valueCp: Int
) {
    PAWN('p', ' ', 100),
    KNIGHT('n', 'N', 320),
    BISHOP('b', 'B', 330),
    ROOK('r', 'R', 500),
    QUEEN('q', 'Q', 900),
    KING('k', 'K', 20000);

    companion object {
        fun fromFenChar(ch: Char): PieceType? = when (ch.lowercaseChar()) {
            'p' -> PAWN
            'n' -> KNIGHT
            'b' -> BISHOP
            'r' -> ROOK
            'q' -> QUEEN
            'k' -> KING
            else -> null
        }
    }
}

data class ChessPiece(
    val color: PieceColor,
    val type: PieceType
) {
    val key: String get() = "${color.fenChar}${type.fenChar.uppercaseChar()}"
}

/**
 * Square index 0..63 where:
 * rank = index / 8 (0 = rank '8', 7 = rank '1')
 * file = index % 8 (0 = file 'a', 7 = file 'h')
 */
@JvmInline
value class Square(val index: Int) {
    val rank: Int get() = index / 8
    val file: Int get() = index % 8

    val fileChar: Char get() = ('a'.code + file).toChar()
    val rankChar: Char get() = ('8'.code - rank).toChar()

    fun toAlgebraic(): String = "$fileChar$rankChar"

    companion object {
        fun of(file: Int, rank: Int): Square = Square(rank * 8 + file)

        fun fromAlgebraic(sq: String): Square? {
            if (sq.length != 2) return null
            val f = sq[0].lowercaseChar() - 'a'
            val r = '8' - sq[1]
            if (f !in 0..7 || r !in 0..7) return null
            return Square(r * 8 + f)
        }
    }
}

data class ChessMove(
    val from: Square,
    val to: Square,
    val piece: ChessPiece,
    val captured: ChessPiece? = null,
    val promotion: PieceType? = null,
    val isEnPassant: Boolean = false,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false,
    val san: String = "",
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false
) {
    val uci: String
        get() = from.toAlgebraic() + to.toAlgebraic() + (promotion?.fenChar?.toString() ?: "")

    val isCapture: Boolean get() = captured != null || isEnPassant
}

enum class AppMode {
    ANALYSIS,
    VS_ENGINE,
    PUZZLE
}

enum class AppLang {
    AR,
    EN
}

data class PvLine(
    val move: ChessMove,
    val uci: String,
    val san: String,
    val scoreCp: Int,
    val pvText: String
)

data class EngineAnalysisResult(
    val scoreCp: Int,
    val lines: List<PvLine>,
    val evaluatedNodes: Int
)

data class TacticalPuzzle(
    val id: String,
    val fen: String,
    val solutionUci: String,
    val rating: String,
    val descriptionAr: String,
    val descriptionEn: String
)

data class OpeningEntry(
    val movesPrefix: String,
    val nameAr: String,
    val nameEn: String
)
