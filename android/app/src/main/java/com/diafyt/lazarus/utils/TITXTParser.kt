package com.diafyt.lazarus.utils

/**
 * Parse the TI-TXT format for describing binary images.
 *
 * This uses a state machine (with the state kept in the `state` variable)
 * and transition functions (stored in the `transitionMatrix` variable)
 * which are selected by matching regular expressions on the token at hand.
 *
 * This raises TITXTParser.ParseException on malformed input.
 */
class TITXTParser {
    private var state = TITXTParserStates.INITIAL
    private var result = HashMap<Int, ByteArray>()
    private var address: Int? = null
    private var chunk: ArrayList<Byte>? = null
    private val transitionMatrix = mapOf(
        Pair(TITXTParserStates.INITIAL, mapOf(
            Pair(Regex("^@([0-9a-fA-F]+)$"), ::newSection),
            Pair(Regex("^q$"), ::done))),
        Pair(TITXTParserStates.SECTION, mapOf(
            Pair(Regex("^@([0-9a-fA-F]+)$"), ::newSection),
            Pair(Regex("^[0-9a-fA-F]{2}$"), ::newByte),
            Pair(Regex("^q$"), ::done))),
        Pair(TITXTParserStates.DONE, mapOf()))

    enum class TITXTParserStates {
        INITIAL, SECTION, DONE
    }

    class ParseException(message: String) : RuntimeException(message)

    /**
     * Reset this instance so it is ready to parse a new document.
     */
    private fun initialize() {
        state = TITXTParserStates.INITIAL
        result = HashMap()
        address = null
        chunk = null
    }

    private fun maySaveSection() {
        address?.let { a ->
            chunk?.let {c ->
                result[a] = c.toByteArray()
                address = null
                chunk = null
            }
        }
    }

    // ([TITXTParserStates.INITIAL, TITXTParserStates.SECTION], "@([0-9a-fA-F]+)")
    private fun newSection(match: MatchResult) {
        maySaveSection()
        address = match.groupValues[1].toInt( 16)
        chunk = ArrayList()
        state = TITXTParserStates.SECTION
    }

    // ([TITXTParserStates.SECTION], "[0-9a-fA-F]{2}")
    private fun newByte(match: MatchResult) {
        chunk?.add(match.groupValues[0].toInt(16).toByte())
    }

    // ([TITXTParserStates.INITIAL, TITXTParserStates.SECTION], "q")
    private fun done(@Suppress("UNUSED_PARAMETER") match: MatchResult) {
        maySaveSection()
        state = TITXTParserStates.DONE
    }

    private fun lex(input: String): List<String>{
        return input.split(Regex("\\s+"))
    }

    private fun parse(tokens: List<String>): Map<Int, ByteArray> {
        for (token in tokens) {
            if (token.isNotEmpty()) {
                var hasMatched = false
                for (regex in transitionMatrix[state]!!.keys) {
                    val match = regex.find(token)
                    if (match != null) {
                        hasMatched = true
                        transitionMatrix[state]!![regex]!!(match)
                        break
                    }
                }
                if (!hasMatched) {
                    throw ParseException("Parsing failed at token '$token'")
                }
            }
        }
        if (state != TITXTParserStates.DONE) {
            throw ParseException("Input was not correctly terminated")
        }
        return result
    }

    fun process(input: String): Map<Int, ByteArray> {
        initialize()
        return parse(lex(input))
    }
}