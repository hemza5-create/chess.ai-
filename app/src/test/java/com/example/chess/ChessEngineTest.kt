package com.example.chess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

    @Test
    fun startingPositionHasTwentyLegalMoves() {
        val game = ChessGame()
        assertEquals(20, game.legalMoves().size)
        assertEquals(ChessGame.START_FEN, game.fen())
    }

    @Test
    fun scholarsMateIsDetectedAsCheckmate() {
        val game = ChessGame()
        assertTrue(game.loadPgn("1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7#"))
        assertTrue(game.inCheck())
        assertTrue(game.inCheckmate())
        assertTrue(game.isGameOver())
    }

    @Test
    fun enPassantCaptureWorksCorrectly() {
        val game = ChessGame()
        assertNotNull(game.makeMoveSan("e4"))
        assertNotNull(game.makeMoveSan("a6"))
        assertNotNull(game.makeMoveSan("e5"))
        assertNotNull(game.makeMoveSan("d5"))
        val epMove = game.makeMoveSan("exd6")
        assertNotNull(epMove)
        assertTrue(epMove!!.isEnPassant)
        assertEquals(null, game.pieceAt("d5"))
        assertEquals(PieceType.PAWN, game.pieceAt("d6")?.type)
    }

    @Test
    fun castlingKingsideAndUndoRestoresState() {
        val game = ChessGame()
        assertTrue(game.loadPgn("1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5"))
        val fenBefore = game.fen()
        val castle = game.makeMoveSan("O-O")
        assertNotNull(castle)
        assertTrue(castle!!.isCastleKingside)
        assertEquals(PieceType.KING, game.pieceAt("g1")?.type)
        assertEquals(PieceType.ROOK, game.pieceAt("f1")?.type)

        game.undo()
        assertEquals(fenBefore, game.fen())
    }

    @Test
    fun engineFindsMateInTacticalPuzzle() {
        val scholarsPuzzle = ChessEngine.TACTICAL_PUZZLES.first()
        val analysis = ChessEngine.computeMultiPv(scholarsPuzzle.fen, depth = 2)
        assertTrue(analysis.lines.isNotEmpty())
        assertEquals(scholarsPuzzle.solutionUci, analysis.lines.first().uci)
        assertTrue(analysis.scoreCp >= 90000)
    }

    @Test
    fun invalidFenIsRejected() {
        val game = ChessGame()
        assertFalse(game.loadFen("invalid/fen/string"))
        assertEquals(ChessGame.START_FEN, game.fen())
    }
}
