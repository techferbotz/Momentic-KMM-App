package com.ferbotz.momentic

import com.ferbotz.momentic.designsystem.qr.QrCode
import com.ferbotz.momentic.designsystem.qr.QrErrorCorrection
import com.ferbotz.momentic.platform.percentEncode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The encoder's output was checked against an outside decoder (Apple's CoreImage QR detector) for
 * versions 1–10, every correction level, UTF-8 text and the centre mark; these keep it that way.
 */
class QrCodeTest {

    @Test
    fun helloAtLowMatchesTheCodeAnOutsideDecoderRead() {
        val expected = listOf(
            "#######..#.##.#######",
            "#.....#..###..#.....#",
            "#.###.#.##.##.#.###.#",
            "#.###.#..#.#..#.###.#",
            "#.###.#...#.#.#.###.#",
            "#.....#.....#.#.....#",
            "#######.#.#.#.#######",
            "........##.##........",
            "###.########.##...#..",
            "...#....#.....#....#.",
            "#.#...#...#.#...#####",
            "##..#...#.#...#....#.",
            "#.#..##..##.#.#.#.#..",
            "........##.#.#.#..##.",
            "#######.#..#.###..###",
            "#.....#.######.##....",
            "#.###.#.#..#.###..###",
            "#.###.#...#...##..##.",
            "#.###.#.###.#...#.#.#",
            "#.....#.##....#.#..#.",
            "#######.##..#.##..###",
        )
        val code = assertNotNull(QrCode.encode("HELLO", QrErrorCorrection.Low))
        assertEquals(expected, rows(code))
    }

    @Test
    fun aMomentsLinkFitsVersionFourAtQuartile() {
        val code = assertNotNull(QrCode.encode("https://momentic.ferbotz.com/aK3x9Qm2", QrErrorCorrection.Quartile))
        assertEquals(4, code.version)
        assertEquals(33, code.size)
    }

    @Test
    fun theFindersTimingAndDarkModuleAreWhereReadersLook() {
        val code = assertNotNull(QrCode.encode("https://momentic.ferbotz.com/aK3x9Qm2"))
        val finder = listOf("#######", "#.....#", "#.###.#", "#.###.#", "#.###.#", "#.....#", "#######")
        for ((left, top) in listOf(0 to 0, code.size - 7 to 0, 0 to code.size - 7)) {
            assertEquals(finder, (0 until 7).map { y -> (0 until 7).joinToString("") { x -> if (code[left + x, top + y]) "#" else "." } })
        }
        for (i in 8 until code.size - 8) {
            assertEquals(i % 2 == 0, code[i, 6], "timing row at $i")
            assertEquals(i % 2 == 0, code[6, i], "timing column at $i")
        }
        assertTrue(code[8, code.size - 8], "the always-dark module")
    }

    @Test
    fun versionTenIsTheLimit() {
        assertEquals(10, QrCode.encode("z".repeat(271), QrErrorCorrection.Low)?.version)
        assertNull(QrCode.encode("z".repeat(272), QrErrorCorrection.Low))
        assertEquals(10, QrCode.encode("w".repeat(119), QrErrorCorrection.High)?.version)
        assertNull(QrCode.encode("w".repeat(120), QrErrorCorrection.High))
    }

    @Test
    fun shareTextIsEncodedForAUrl() {
        assertEquals("Hi%20Divya%21%20https%3A%2F%2Fm.co%2Fa%3Fb%3Dc%26d", percentEncode("Hi Divya! https://m.co/a?b=c&d"))
        assertEquals("%E0%A4%98%E0%A4%B0-._~", percentEncode("घर-._~"))
    }

    private fun rows(code: QrCode): List<String> =
        (0 until code.size).map { y -> (0 until code.size).joinToString("") { x -> if (code[x, y]) "#" else "." } }
}
