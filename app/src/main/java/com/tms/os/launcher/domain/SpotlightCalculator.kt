package com.tms.os.launcher.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * The calculator behind Spotlight. It is forgiving about how people type:
 * `10x10`, `10*10`, `10×10`, `39-18`, `39 − 18`, `3,5 + 2`, `1.234,5 / 2`,
 * `200 + 19%`, `19% von 250`, `2^10`, `3²`, `√81`, `sqrt(2)`, `2(3+4)`,
 * `5 mal 3`, `10 geteilt durch 4`, an open bracket or a trailing `=`.
 */
object SpotlightCalculator {

    data class Result(
        /** The expression as understood, in clean typography ("39 − 18"). */
        val expression: String,
        /** Formatted for reading ("1.234,5"). */
        val display: String,
        /** Plain value for the clipboard ("1234,5"). */
        val plain: String
    )

    fun evaluate(input: String): Result? {
        val tokens = tokenize(normalize(input)) ?: return null
        if (tokens.none { it is Tok.Op || it is Tok.Func || it is Tok.Percent || it is Tok.Postfix }) return null
        if (tokens.none { it is Tok.Num || it is Tok.Const }) return null
        // A lone number, signed or not, is not a sum.
        if (tokens.dropWhile { it is Tok.Op }.size < 2) return null
        val value = runCatching { Parser(tokens).parse() }.getOrNull() ?: return null
        if (value.isNaN() || value.isInfinite()) return null
        return Result(pretty(tokens), format(value, grouping = true), format(value, grouping = false))
    }

    /* ------------------------------------------------------------ input */

    private fun normalize(input: String): String {
        var s = input.trim().lowercase(Locale.ROOT).trimEnd('=').trim()
        // Words people type instead of symbols.
        val words = listOf(
            "geteilt durch" to "/", "dividiert durch" to "/", "divided by" to "/", "durch" to "/",
            "multipliziert mit" to "*", "times" to "*", "mal" to "*",
            "plus" to "+", "minus" to "-", "hoch" to "^",
            "wurzel aus" to "sqrt", "wurzel" to "sqrt", "prozent" to "%", "percent" to "%",
            "von" to "*", "of" to "*"
        )
        for ((word, symbol) in words) s = s.replace(Regex("(?<![a-z])${Regex.escape(word)}(?![a-z])"), " $symbol ")
        return s
            .replace('−', '-').replace('–', '-').replace('—', '-').replace('﹣', '-').replace('－', '-')
            .replace('×', '*').replace('✕', '*').replace('✖', '*').replace('·', '*').replace('⋅', '*').replace('∙', '*')
            .replace('÷', '/').replace('∕', '/').replace('⁄', '/').replace(':', '/')
            .replace("²", "^2").replace("³", "^3")
            .replace("√", "sqrt").replace("π", "pi")
    }

    private sealed interface Tok {
        data class Num(val value: Double, val text: String) : Tok
        data class Op(val c: Char) : Tok
        data class Func(val name: String) : Tok
        data class Const(val name: String, val value: Double) : Tok
        data object Open : Tok
        data object Close : Tok
        data object Percent : Tok
        data class Postfix(val c: Char) : Tok
    }

    private val functions = setOf("sqrt", "sin", "cos", "tan", "ln", "log", "abs")

    private fun tokenize(s: String): List<Tok>? {
        val out = ArrayList<Tok>()
        var i = 0
        fun endsOperand() = out.lastOrNull().let { it is Tok.Num || it is Tok.Const || it is Tok.Close || it is Tok.Percent || it is Tok.Postfix }
        while (i < s.length) {
            val c = s[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || ((c == '.' || c == ',') && i + 1 < s.length && s[i + 1].isDigit()) -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == ',')) i++
                    // A separator at the very end belongs to the sentence, not the number.
                    while (i > start && (s[i - 1] == '.' || s[i - 1] == ',')) i--
                    val raw = s.substring(start, i)
                    val value = parseNumber(raw) ?: return null
                    if (endsOperand()) out += Tok.Op('*')
                    out += Tok.Num(value, raw)
                    if (i < s.length && (s[i] == '.' || s[i] == ',')) i++
                }
                c == 'x' && endsOperand() && s.drop(i + 1).trimStart().firstOrNull().let { it != null && (it.isDigit() || it == '(' || it == '-' || it == '.' || it == ',') } -> {
                    out += Tok.Op('*'); i++
                }
                c.isLetter() -> {
                    val start = i
                    while (i < s.length && s[i].isLetter()) i++
                    val word = s.substring(start, i)
                    if (endsOperand()) out += Tok.Op('*')
                    out += when (word) {
                        in functions -> Tok.Func(word)
                        "pi" -> Tok.Const("π", Math.PI)
                        else -> return null
                    }
                }
                c == '(' -> { if (endsOperand()) out += Tok.Op('*'); out += Tok.Open; i++ }
                c == ')' -> { out += Tok.Close; i++ }
                c == '%' -> { out += Tok.Percent; i++ }
                c == '!' -> { out += Tok.Postfix('!'); i++ }
                c in "+-*/^" -> { out += Tok.Op(c); i++ }
                else -> return null
            }
        }
        // Still typing: a dangling operator or an open bracket is no reason to show nothing.
        while (out.lastOrNull().let { it is Tok.Op || it is Tok.Open || it is Tok.Func }) out.removeAt(out.size - 1)
        repeat(out.count { it is Tok.Open } - out.count { it is Tok.Close }) { out += Tok.Close }
        return out
    }

    /** "3,5", "3.5", "1.234,56", "1,234.56", "1.234.567". Phone-like numbers ("0176…") are not numbers. */
    private fun parseNumber(raw: String): Double? {
        val dots = raw.count { it == '.' }
        val commas = raw.count { it == ',' }
        val cleaned = when {
            dots > 0 && commas > 0 -> {
                val decimal = if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) ',' else '.'
                val grouping = if (decimal == ',') '.' else ','
                if (raw.count { it == decimal } > 1) return null
                raw.replace(grouping.toString(), "").replace(decimal, '.')
            }
            commas > 1 || dots > 1 -> {
                val sep = if (commas > 1) ',' else '.'
                val parts = raw.split(sep)
                if (parts.first().length !in 1..3 || parts.drop(1).any { it.length != 3 }) return null
                raw.replace(sep.toString(), "")
            }
            commas == 1 -> raw.replace(',', '.')
            else -> raw
        }
        val intPart = cleaned.substringBefore('.')
        if (intPart.length > 1 && intPart.startsWith("0")) return null
        return cleaned.toDoubleOrNull()
    }

    /* ----------------------------------------------------------- parser */

    private class Parser(private val t: List<Tok>) {
        private var i = 0
        private fun peek() = t.getOrNull(i)
        private fun isOp(c: Char) = (peek() as? Tok.Op)?.c == c

        fun parse(): Double {
            val v = expression()
            if (i != t.size) error("unexpected")
            return v
        }

        // a + b%  ->  a + a·b/100, as on every pocket calculator.
        private fun expression(): Double {
            var x = term().first
            while (true) {
                val plus = isOp('+')
                if (!plus && !isOp('-')) return x
                i++
                val (y, percent) = term()
                val delta = if (percent) x * y else y
                x = if (plus) x + delta else x - delta
            }
        }

        /** Value and whether it was a bare percentage ("19%"). */
        private fun term(): Pair<Double, Boolean> {
            var (x, percent) = power()
            while (isOp('*') || isOp('/')) {
                val mul = isOp('*'); i++
                val y = power().first
                x = if (mul) x * y else x / y
                percent = false
            }
            return x to percent
        }

        private fun power(): Pair<Double, Boolean> {
            val (base, percent) = unary()
            if (isOp('^')) { i++; return base.pow(power().first) to false }
            return base to percent
        }

        private fun unary(): Pair<Double, Boolean> {
            if (isOp('-')) { i++; val (v, p) = unary(); return -v to p }
            if (isOp('+')) { i++; return unary() }
            return postfix()
        }

        private fun postfix(): Pair<Double, Boolean> {
            var x = primary()
            var percent = false
            while (true) {
                when (val p = peek()) {
                    Tok.Percent -> { i++; x /= 100.0; percent = true }
                    is Tok.Postfix -> { i++; x = factorial(x); percent = false; if (p.c != '!') error("postfix") }
                    else -> return x to percent
                }
            }
        }

        private fun primary(): Double = when (val p = peek()) {
            is Tok.Num -> { i++; p.value }
            is Tok.Const -> { i++; p.value }
            Tok.Open -> { i++; val v = expression(); if (peek() == Tok.Close) i++ else error(")"); v }
            is Tok.Func -> {
                i++
                val arg = unary().first
                when (p.name) {
                    "sqrt" -> sqrt(arg)
                    "sin" -> sin(arg)
                    "cos" -> cos(arg)
                    "tan" -> tan(arg)
                    "ln" -> ln(arg)
                    "log" -> log10(arg)
                    else -> abs(arg)
                }
            }
            else -> error("operand")
        }

        private fun factorial(x: Double): Double {
            if (x < 0 || x > 170 || x != Math.floor(x)) error("factorial")
            var r = 1.0
            for (k in 2..x.toInt()) r *= k
            return r
        }
    }

    /* ----------------------------------------------------------- output */

    private fun pretty(tokens: List<Tok>): String = buildString {
        var prev: Tok? = null
        for (tok in tokens) {
            when (tok) {
                is Tok.Num -> append(tok.text)
                is Tok.Const -> append(tok.name)
                is Tok.Func -> append(if (tok.name == "sqrt") "√" else tok.name)
                Tok.Open -> append('(')
                Tok.Close -> append(')')
                Tok.Percent -> append('%')
                is Tok.Postfix -> append(tok.c)
                is Tok.Op -> {
                    val unary = tok.c in "+-" && (prev == null || prev is Tok.Op || prev == Tok.Open || prev is Tok.Func)
                    val symbol = when (tok.c) { '-' -> '−'; '*' -> '×'; '/' -> '÷'; else -> tok.c }
                    if (unary || tok.c == '^') append(symbol) else append(' ').append(symbol).append(' ')
                }
            }
            prev = tok
        }
    }

    private fun format(value: Double, grouping: Boolean): String {
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val magnitude = abs(value)
        if (magnitude != 0.0 && (magnitude >= 1e15 || magnitude < 1e-9)) {
            return DecimalFormat("0.######E0", symbols).format(value).replace("E", " × 10^")
        }
        // Ten significant digits hide binary noise (0.1 + 0.2) without losing real precision.
        val rounded = BigDecimal(value).round(MathContext(10, RoundingMode.HALF_UP))
        val format = DecimalFormat(if (grouping) "#,##0.##########" else "0.##########", symbols)
        val text = format.format(rounded)
        return if (text == "-0") "0" else text
    }
}
