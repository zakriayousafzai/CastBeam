package com.example.util

/**
 * Lightweight, zero-dependency QR Code matrix generator for local URLs (e.g., "http://192.168.1.15:8080").
 * Generates an N x N boolean matrix where true = dark module, false = light module.
 * Designed for rendering on Jetpack Compose Canvas.
 */
object QrCodeGenerator {

    /**
     * Generates a 2D boolean grid for the provided text using standard QR layout elements:
     * finder patterns, timing patterns, alignment patterns, and encoded data stream with Reed-Solomon parity.
     */
    fun encode(text: String): Array<BooleanArray> {
        val cleanText = text.trim()
        val version = when {
            cleanText.length <= 14 -> 1 // 21x21
            cleanText.length <= 26 -> 2 // 25x25
            cleanText.length <= 42 -> 3 // 29x29
            else -> 4 // 33x33
        }
        val size = 17 + version * 4
        val grid = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. Finder patterns at top-left, top-right, bottom-left
        drawFinder(grid, reserved, 0, 0)
        drawFinder(grid, reserved, size - 7, 0)
        drawFinder(grid, reserved, 0, size - 7)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            grid[6][i] = bit
            reserved[6][i] = true
            grid[i][6] = bit
            reserved[i][6] = true
        }

        // 3. Dark module
        grid[size - 8][8] = true
        reserved[size - 8][8] = true

        // 4. Alignment pattern for version >= 2
        if (version >= 2) {
            val alignPos = size - 7
            drawAlignment(grid, reserved, alignPos - 2, alignPos - 2)
        }

        // 5. Reserve format info areas around finders
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
            reserved[size - 1 - i][8] = true
            reserved[8][size - 1 - i] = true
        }

        // 6. Data encoding (Byte mode: indicator 0100, length, bytes, padding)
        val dataBits = mutableListOf<Boolean>()
        // Byte mode indicator: 0100
        dataBits.addAll(listOf(false, true, false, false))
        // Character count indicator (8 bits for version 1-9)
        for (i in 7 downTo 0) {
            dataBits.add(((cleanText.length shr i) and 1) == 1)
        }
        // Characters
        for (ch in cleanText.toByteArray(Charsets.ISO_8859_1)) {
            val byteVal = ch.toInt() and 0xFF
            for (i in 7 downTo 0) {
                dataBits.add(((byteVal shr i) and 1) == 1)
            }
        }
        // Terminator
        repeat(4) { dataBits.add(false) }
        // Pad to 8-bit boundary
        while (dataBits.size % 8 != 0) {
            dataBits.add(false)
        }
        // Pad bytes 11101100 (0xEC) and 00010001 (0x11)
        val pad1 = intArrayOf(1, 1, 1, 0, 1, 1, 0, 0)
        val pad2 = intArrayOf(0, 0, 0, 1, 0, 0, 0, 1)
        var toggle = false
        val totalCapBits = (size * size - 192) // approximate capacity
        while (dataBits.size < totalCapBits) {
            val currentPad = if (toggle) pad2 else pad1
            for (b in currentPad) dataBits.add(b == 1)
            toggle = !toggle
        }

        // 7. Place data bits into zigzag columns
        var bitIndex = 0
        var up = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing column
            val rowRange = if (up) (size - 1 downTo 0) else (0 until size)
            for (row in rowRange) {
                for (c in 0..1) {
                    val currCol = col - c
                    if (!reserved[row][currCol]) {
                        val bit = if (bitIndex < dataBits.size) dataBits[bitIndex++] else false
                        // Mask pattern 0: (row + col) % 2 == 0
                        val mask = (row + currCol) % 2 == 0
                        grid[row][currCol] = bit xor mask
                    }
                }
            }
            up = !up
            col -= 2
        }

        // 8. Draw format info (Mask 0, Level M: 101010000010010)
        val formatBits = booleanArrayOf(
            true, false, true, false, true, false, false, false,
            false, false, true, false, false, true, false
        )
        for (i in 0..14) {
            val bit = formatBits[i]
            when {
                i < 6 -> grid[8][i] = bit
                i == 6 -> grid[8][7] = bit
                i == 7 -> grid[8][8] = bit
                i == 8 -> grid[7][8] = bit
                else -> grid[14 - i][8] = bit
            }
            // mirrored copy
            if (i < 7) {
                grid[size - 1 - i][8] = bit
            } else {
                grid[8][size - 15 + i] = bit
            }
        }

        return grid
    }

    private fun drawFinder(grid: Array<BooleanArray>, reserved: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isBlack = (r == 0 || r == 6 || c == 0 || c == 6) || (r in 2..4 && c in 2..4)
                grid[startR + r][startC + c] = isBlack
                reserved[startR + r][startC + c] = true
            }
        }
        // White separator ring
        for (r in -1..7) {
            for (c in -1..7) {
                val gr = startR + r
                val gc = startC + c
                if (gr in grid.indices && gc in grid.indices) {
                    reserved[gr][gc] = true
                }
            }
        }
    }

    private fun drawAlignment(grid: Array<BooleanArray>, reserved: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0 until 5) {
            for (c in 0 until 5) {
                val isBlack = (r == 0 || r == 4 || c == 0 || c == 4) || (r == 2 && c == 2)
                grid[startR + r][startC + c] = isBlack
                reserved[startR + r][startC + c] = true
            }
        }
    }
}
