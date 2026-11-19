package com.tms.os.launcher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotlightCalculatorTest {
    private fun calc(s: String) = SpotlightCalculator.evaluate(s)?.display

    @Test
    fun `every way of typing the four operations`() {
        assertEquals("100", calc("10x10"))
        assertEquals("100", calc("10 X 10"))
        assertEquals("100", calc("10*10"))
        assertEquals("100", calc("10×10"))
        assertEquals("21", calc("39-18"))
        assertEquals("21", calc("39 − 18"))
        assertEquals("21", calc("39–18"))
        assertEquals("1", calc("10÷10"))
        assertEquals("2,5", calc("10:4"))
        assertEquals("2,5", calc("10/4"))
        assertEquals("11", calc("1+10"))
        assertEquals("15", calc("5 mal 3"))
        assertEquals("2,5", calc("10 geteilt durch 4"))
    }

    @Test
    fun `precedence brackets powers and roots`() {
        assertEquals("14", calc("2+3*4"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("14", calc("2(3+4)"))
        assertEquals("14", calc("2*(3+4"))
        assertEquals("1.024", calc("2^10"))
        assertEquals("9", calc("3²"))
        assertEquals("9", calc("√81"))
        assertEquals("-6", calc("-2*3"))
        assertEquals("120", calc("5!"))
        assertEquals("21", calc("39-18="))
    }

    @Test
    fun `decimal comma decimal point and grouping`() {
        assertEquals("5,5", calc("3,5+2"))
        assertEquals("5,5", calc("3.5+2"))
        assertEquals("617,25", calc("1.234,5/2"))
        assertEquals("0,3", calc("0,1+0,2"))
        assertEquals("1.000.000", calc("1000*1000"))
        assertEquals("1000000", SpotlightCalculator.evaluate("1000*1000")?.plain)
    }

    @Test
    fun `percent behaves like a pocket calculator`() {
        assertEquals("238", calc("200+19%"))
        assertEquals("160", calc("200-20%"))
        assertEquals("47,5", calc("19% von 250"))
        assertEquals("47,5", calc("250*19%"))
        assertEquals("0,5", calc("50%"))
    }

    @Test
    fun `still typing shows the result so far`() {
        assertEquals("21", calc("39-18+"))
        assertNull(calc("39-"))
        assertNull(calc("39"))
    }

    @Test
    fun `things that are not sums stay untouched`() {
        assertNull(calc("xbox"))
        assertNull(calc("13.11.2024"))
        assertNull(calc("0176-1234567"))
        assertNull(calc("max"))
        assertNull(calc("-5"))
        assertNull(calc("10/0"))
    }

    @Test
    fun `the expression is shown in clean typography`() {
        assertEquals("39 − 18", SpotlightCalculator.evaluate("39-18")?.expression)
        assertEquals("10 × 10", SpotlightCalculator.evaluate("10x10")?.expression)
    }
}
