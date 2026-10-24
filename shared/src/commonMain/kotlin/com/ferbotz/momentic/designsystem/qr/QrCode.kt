package com.ferbotz.momentic.designsystem.qr

import kotlin.math.abs

/** How much of a QR code can be lost (a logo over the middle, a scuff) and still read: about 7 / 15 / 25 / 30 %. */
enum class QrErrorCorrection(internal val formatBits: Int) {
    Low(1),
    Medium(0),
    Quartile(3),
    High(2),
}

/**
 * A QR code (ISO/IEC 18004): [size] × [size] modules, `true` for dark. Encodes text as UTF-8 in byte
 * mode — what a link needs — choosing the smallest version from 1 to 10 that fits (up to 271 bytes at
 * Low, 119 at High) and the mask with the lowest penalty. The algorithm follows the standard as laid out
 * in Project Nayuki's reference generator.
 */
class QrCode private constructor(val version: Int, private val modules: Array<BooleanArray>) {

    val size: Int get() = modules.size

    operator fun get(x: Int, y: Int): Boolean = modules[y][x]

    companion object {
        private const val MAX_VERSION = 10

        /** The code for [text], or null when it's too long for version 10 at [errorCorrection]. */
        fun encode(text: String, errorCorrection: QrErrorCorrection = QrErrorCorrection.Quartile): QrCode? {
            val data = text.encodeToByteArray()
            val version = (1..MAX_VERSION).firstOrNull { version ->
                val capacityBits = dataCodewords(version, errorCorrection) * 8
                4 + countBits(version) + data.size * 8 <= capacityBits
            } ?: return null

            // Mode, length, the bytes; then the terminator and padding up to the data capacity.
            val bits = BitBuffer()
            bits.append(0b0100, 4)
            bits.append(data.size, countBits(version))
            data.forEach { bits.append(it.toInt() and 0xFF, 8) }
            val capacityBits = dataCodewords(version, errorCorrection) * 8
            bits.append(0, minOf(4, capacityBits - bits.length))
            bits.append(0, (8 - bits.length % 8) % 8)
            var pad = 0xEC
            while (bits.length < capacityBits) {
                bits.append(pad, 8)
                pad = pad xor (0xEC xor 0x11)
            }

            val codewords = withErrorCorrection(bits.toBytes(), version, errorCorrection)
            val grid = Grid(version)
            grid.drawFunctionPatterns(errorCorrection)
            grid.drawCodewords(codewords)
            // The mask with the lowest penalty (each tried, then undone; masking is its own inverse).
            val best = (0 until 8).minBy { mask ->
                grid.applyMask(mask)
                grid.drawFormatBits(errorCorrection, mask)
                val penalty = grid.penalty()
                grid.applyMask(mask)
                penalty
            }
            grid.applyMask(best)
            grid.drawFormatBits(errorCorrection, best)
            return QrCode(version, grid.modules)
        }

        /** The character-count field's length in byte mode. */
        private fun countBits(version: Int): Int = if (version <= 9) 8 else 16

        // Indexed by version (0 unused), for versions 1–10.
        private val ECC_PER_BLOCK = mapOf(
            QrErrorCorrection.Low to intArrayOf(0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18),
            QrErrorCorrection.Medium to intArrayOf(0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26),
            QrErrorCorrection.Quartile to intArrayOf(0, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24),
            QrErrorCorrection.High to intArrayOf(0, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28),
        )
        private val BLOCKS = mapOf(
            QrErrorCorrection.Low to intArrayOf(0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4),
            QrErrorCorrection.Medium to intArrayOf(0, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5),
            QrErrorCorrection.Quartile to intArrayOf(0, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8),
            QrErrorCorrection.High to intArrayOf(0, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8),
        )

        /** Modules left for data and error correction once the function patterns are drawn. */
        internal fun rawDataModules(version: Int): Int {
            var result = (16 * version + 128) * version + 64
            if (version >= 2) {
                val alignments = version / 7 + 2
                result -= (25 * alignments - 10) * alignments - 55
                if (version >= 7) result -= 36
            }
            return result
        }

        internal fun dataCodewords(version: Int, ecc: QrErrorCorrection): Int =
            rawDataModules(version) / 8 - ECC_PER_BLOCK.getValue(ecc)[version] * BLOCKS.getValue(ecc)[version]

        /** Splits the data into blocks, adds each block's Reed–Solomon codewords, and interleaves them. */
        private fun withErrorCorrection(data: ByteArray, version: Int, ecc: QrErrorCorrection): ByteArray {
            val blocks = BLOCKS.getValue(ecc)[version]
            val eccLength = ECC_PER_BLOCK.getValue(ecc)[version]
            val raw = rawDataModules(version) / 8
            val shortBlocks = blocks - raw % blocks
            val shortLength = raw / blocks
            val divisor = reedSolomonDivisor(eccLength)
            var taken = 0
            val all = List(blocks) { index ->
                val length = shortLength - eccLength + if (index < shortBlocks) 0 else 1
                val chunk = data.copyOfRange(taken, taken + length)
                taken += length
                val block = chunk.copyOf(shortLength + 1)
                reedSolomonRemainder(chunk, divisor).copyInto(block, block.size - eccLength)
                block
            }
            val result = ByteArray(raw)
            var k = 0
            for (i in 0 until shortLength + 1) {
                for (j in 0 until blocks) {
                    // Short blocks have no codeword at the end of their data part.
                    if (i != shortLength - eccLength || j >= shortBlocks) result[k++] = all[j][i]
                }
            }
            return result
        }

        private fun reedSolomonDivisor(degree: Int): ByteArray {
            val result = ByteArray(degree)
            result[degree - 1] = 1
            var root = 1
            repeat(degree) {
                for (j in result.indices) {
                    result[j] = multiply(result[j].toInt() and 0xFF, root).toByte()
                    if (j + 1 < result.size) result[j] = (result[j].toInt() xor result[j + 1].toInt()).toByte()
                }
                root = multiply(root, 0x02)
            }
            return result
        }

        private fun reedSolomonRemainder(data: ByteArray, divisor: ByteArray): ByteArray {
            val result = ByteArray(divisor.size)
            for (b in data) {
                val factor = (b.toInt() xor result[0].toInt()) and 0xFF
                result.copyInto(result, 0, 1, result.size)
                result[result.size - 1] = 0
                for (i in result.indices) {
                    result[i] = (result[i].toInt() xor multiply(divisor[i].toInt() and 0xFF, factor)).toByte()
                }
            }
            return result
        }

        /** Multiplication in GF(2⁸) modulo x⁸ + x⁴ + x³ + x² + 1. */
        private fun multiply(x: Int, y: Int): Int {
            var z = 0
            for (i in 7 downTo 0) {
                z = (z shl 1) xor ((z ushr 7) * 0x11D)
                z = z xor (((y ushr i) and 1) * x)
            }
            return z
        }
    }
}

private class BitBuffer {
    private val bits = mutableListOf<Boolean>()
    val length: Int get() = bits.size

    fun append(value: Int, count: Int) {
        for (i in count - 1 downTo 0) bits += (value ushr i) and 1 == 1
    }

    fun toBytes(): ByteArray = ByteArray(bits.size / 8) { index ->
        var byte = 0
        for (bit in 0 until 8) byte = (byte shl 1) or if (bits[index * 8 + bit]) 1 else 0
        byte.toByte()
    }
}

/** The module grid being drawn, with which modules belong to function patterns (never masked). */
private class Grid(private val version: Int) {
    val size = version * 4 + 17
    val modules = Array(size) { BooleanArray(size) }
    private val isFunction = Array(size) { BooleanArray(size) }

    private fun set(x: Int, y: Int, dark: Boolean) {
        modules[y][x] = dark
        isFunction[y][x] = true
    }

    fun drawFunctionPatterns(ecc: QrErrorCorrection) {
        for (i in 0 until size) {
            set(6, i, i % 2 == 0)
            set(i, 6, i % 2 == 0)
        }
        finder(3, 3)
        finder(size - 4, 3)
        finder(3, size - 4)
        val positions = alignmentPositions()
        val last = positions.lastIndex
        for (i in positions.indices) {
            for (j in positions.indices) {
                // Not over the three finders.
                if ((i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)) continue
                alignment(positions[i], positions[j])
            }
        }
        drawFormatBits(ecc, 0)
        drawVersion()
    }

    private fun finder(cx: Int, cy: Int) {
        for (dy in -4..4) {
            for (dx in -4..4) {
                val distance = maxOf(abs(dx), abs(dy))
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until size && y in 0 until size) set(x, y, distance != 2 && distance != 4)
            }
        }
    }

    private fun alignment(cx: Int, cy: Int) {
        for (dy in -2..2) for (dx in -2..2) set(cx + dx, cy + dy, maxOf(abs(dx), abs(dy)) != 1)
    }

    private fun alignmentPositions(): IntArray {
        if (version == 1) return IntArray(0)
        val count = version / 7 + 2
        val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
        val result = IntArray(count)
        result[0] = 6
        var position = size - 7
        for (i in count - 1 downTo 1) {
            result[i] = position
            position -= step
        }
        return result
    }

    fun drawFormatBits(ecc: QrErrorCorrection, mask: Int) {
        val data = ecc.formatBits shl 3 or mask
        var remainder = data
        repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
        val bits = (data shl 10 or remainder) xor 0x5412
        fun bit(i: Int) = (bits ushr i) and 1 != 0
        for (i in 0..5) set(8, i, bit(i))
        set(8, 7, bit(6))
        set(8, 8, bit(7))
        set(7, 8, bit(8))
        for (i in 9 until 15) set(14 - i, 8, bit(i))
        for (i in 0 until 8) set(size - 1 - i, 8, bit(i))
        for (i in 8 until 15) set(8, size - 15 + i, bit(i))
        set(8, size - 8, true)
    }

    private fun drawVersion() {
        if (version < 7) return
        var remainder = version
        repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * 0x1F25) }
        val bits = version.toLong() shl 12 or remainder.toLong()
        for (i in 0 until 18) {
            val dark = (bits ushr i) and 1L != 0L
            val a = size - 11 + i % 3
            val b = i / 3
            set(a, b, dark)
            set(b, a, dark)
        }
    }

    /** Lays the codewords out in the standard zigzag, two columns at a time, skipping the timing column. */
    fun drawCodewords(data: ByteArray) {
        var i = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vertical in 0 until size) {
                for (j in 0..1) {
                    val x = right - j
                    val upward = (right + 1) and 2 == 0
                    val y = if (upward) size - 1 - vertical else vertical
                    if (!isFunction[y][x] && i < data.size * 8) {
                        modules[y][x] = (data[i ushr 3].toInt() ushr (7 - (i and 7))) and 1 != 0
                        i++
                    }
                }
            }
            right -= 2
        }
    }

    fun applyMask(mask: Int) {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (isFunction[y][x]) continue
                val invert = when (mask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (x / 3 + y / 2) % 2 == 0
                    5 -> x * y % 2 + x * y % 3 == 0
                    6 -> (x * y % 2 + x * y % 3) % 2 == 0
                    else -> ((x + y) % 2 + x * y % 3) % 2 == 0
                }
                if (invert) modules[y][x] = !modules[y][x]
            }
        }
    }

    /** The standard's four penalty rules: runs, 2 × 2 blocks, finder look-alikes, and dark/light balance. */
    fun penalty(): Int {
        var result = 0
        for (y in 0 until size) result += linePenalty { modules[y][it] }
        for (x in 0 until size) result += linePenalty { modules[it][x] }
        for (y in 0 until size - 1) {
            for (x in 0 until size - 1) {
                val color = modules[y][x]
                if (color == modules[y][x + 1] && color == modules[y + 1][x] && color == modules[y + 1][x + 1]) result += 3
            }
        }
        val dark = modules.sumOf { row -> row.count { it } }
        val total = size * size
        val k = (abs(dark * 20 - total * 10) + total - 1) / total - 1
        result += k * 10
        return result
    }

    private fun linePenalty(at: (Int) -> Boolean): Int {
        var result = 0
        var runColor = false
        var runLength = 0
        val history = IntArray(7)
        for (i in 0 until size) {
            if (at(i) == runColor) {
                runLength++
                if (runLength == 5) result += 3 else if (runLength > 5) result++
            } else {
                addHistory(runLength, history)
                if (!runColor) result += finderLike(history) * 40
                runColor = at(i)
                runLength = 1
            }
        }
        // Close the line as if a light border followed it.
        if (runColor) {
            addHistory(runLength, history)
            runLength = 0
        }
        addHistory(runLength + size, history)
        result += finderLike(history) * 40
        return result
    }

    private fun addHistory(length: Int, history: IntArray) {
        // A light border before the first run.
        val run = if (history[0] == 0) length + size else length
        history.copyInto(history, 1, 0, history.size - 1)
        history[0] = run
    }

    private fun finderLike(history: IntArray): Int {
        val n = history[1]
        val core = n > 0 && history[2] == n && history[3] == n * 3 && history[4] == n && history[5] == n
        return (if (core && history[0] >= n * 4 && history[6] >= n) 1 else 0) + (if (core && history[6] >= n * 4 && history[0] >= n) 1 else 0)
    }
}
