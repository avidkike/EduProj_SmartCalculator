package calculator

import java.math.BigDecimal
import kotlin.math.pow
import kotlin.system.exitProcess


fun main() {
    var variableList = mutableListOf<Pair<String, String>>()
    init@ while (true) {
        when (val input = readln().trim()) {
            "/exit" -> println("Bye!").also { exitProcess(0) }
            "/help" -> println("The program calculates the sum of numbers")
            "" -> continue
            else -> {
                if (input.first() == '/') {
                    println("Unknown command")
                    continue
                }

                if (Regex("^[a-zA-Z]+\$").matches(input)) {
                    if (variableList.none { it.first == input }) {
                        println("Unknown variable")
                    } else {
                        println(variableList.findValue(input))
                    }
                    continue
                }

                val equation = input.split("=").map { it.trim() }
                if (equation.size == 2) {
                    if (!Regex("^[a-zA-Z]+\$").matches(equation.component1())) {
                        println("Invalid identifier")
                        continue
                    }
                    if (Regex("^[a-zA-Z]+\$").matches(equation.component2())) {
                        if (variableList.none { it.first == equation.component2() }) {
                            println("Invalid assignment")
                        } else //adding equations to varList
                            variableList = variableList.filter { it.first != equation.component1() }.toMutableList()
                                .also { it.add(equation.component1() to equation.component2()) }
                    } else {
                        try {
                            variableList = variableList.filter { it.first != equation.component1() }.toMutableList()
                                .also { it.add(equation.component1() to equation.component2().toBigDecimal().toString()) }
                        } catch (e: NumberFormatException) {
                            println("Invalid identifier")
                        }
                    }
                    continue
                } else if (equation.size > 2) {
                    println("Invalid assignment")
                    continue
                }

                var bracket = 0
                val expression = mutableListOf<String>()

                var length = 0

                while (length != input.length) {
                    when (val c = input[length]) {
                        ' ' -> {
                            length++
                        }
                        '(' -> {
                            length++
                            bracket++
                            expression.add(c.toString())
                        }
                        ')' -> {
                            length++
                            bracket--
                            expression.add(c.toString())
                        }
                        in listOf('*', '+', '-', '/', '^') -> {
                            var newSign = ""
                            for (sc in input.subSequence(length, input.length)) {
                                if (sc in listOf('*', '+', '-', '/', '^')) {
                                    length++
                                    newSign += sc
                                    if (newSign.length >= 2 &&
                                        (newSign[newSign.lastIndex-1] != sc || sc in listOf('*', '/', '^') )) {
                                        println("Invalid expression")
                                        continue@init
                                    }
                                } else break
                            }
                            expression.add(newSign).also { newSign = "" }
                        }
                        else -> {
                            var newVar = ""
                            for (sc in input.subSequence(length, input.length)) {
                                if (Regex("[0-9a-zA-Z]").matches(sc.toString())) {
                                    length++
                                    newVar += sc
                                } else break
                            }
                            expression.add(newVar).also { newVar = "" }
                        }
                    }
                }

                if (bracket != 0) {
                    println("Invalid expression")
                    continue
                }

                val expressionConverted = expression.map {
                    if (it.all { c -> c == '+' }) "+"
                    else if (it.all { c -> c == '-' } && it.length % 2 == 0) "+"
                    else if (it.all { c -> c == '-' } && it.length % 2 != 0) "-"
                    else it
                }

                //-----INFIX -> POSTFIX
                val result = mutableListOf<String>()
                val stack = mutableListOf<String>()

                for ((i, sign) in expressionConverted.withIndex()) {
                    if (Regex("[A-Za-z0-9]+").matches(sign)) {
                        result.add(variableList.findValue(sign).toString())
                    } else {
                        while (stack.isNotEmpty() && stack[stack.lastIndex] != "(") {
                            if (stack[stack.lastIndex].higherThan(sign))
                                result.add(stack[stack.lastIndex]).also { stack.removeLast() }
                            else break
                        }
                        stack.add(sign)
                    }
                    if (stack.size >= 2) {
                        val lastTwo = stack.subList(stack.size - 2, stack.size)
                        if (lastTwo.component1() == "(" && lastTwo.component2() == ")") {
                            stack.removeLast()
                            stack.removeLast()
                        }
                    }
                    if (i == expressionConverted.size - 1) {

                        val stackForSort = stack.map { st ->
                            st to priorities.find { it.second == st }?.first
                        }.sortedBy { it.second }.map { it.first }

                        result.addAll(stackForSort).also { stack.clear() }
                    }
                }

                //-----POSTFIX -> INFIX
                for (s in result) {
                    if (Regex("-?[0-9]+(\\.[0-9]+)?").matches(s)) {
                        stack.add(s)
                    } else {

                        val lastTwo = try {
                            stack.subList(stack.size-2, stack.size)
                        } catch (e: NumberFormatException) {
                            listOf(stack[0], "0")
                        }

                        val postfixResul = when (s) {
                            "+" -> lastTwo.component1().toBigDecimal() + lastTwo.component2().toBigDecimal()
                            "-" -> lastTwo.component1().toBigDecimal() - lastTwo.component2().toBigDecimal()
                            "*" -> lastTwo.component1().toBigDecimal() * lastTwo.component2().toBigDecimal()
                            "^" -> lastTwo.component1().toDouble().pow(lastTwo.component2().toDouble()).toBigDecimal()
                            else -> lastTwo.component1().toBigDecimal() / lastTwo.component2().toBigDecimal()
                        }
                        repeat(2) { stack.removeLast() }
                        stack.add(postfixResul.toString())
                    }
                }
                println(stack[0])
            }
        }
    }
}

fun List<Pair<String, String>>.findValue(variable: String) : BigDecimal {
    return try {
        variable.toBigDecimal()
    } catch (e: NumberFormatException) {
        val nextVar = this.find { it.first == variable }!!.second
        try {
            nextVar.toBigDecimal()
        } catch (e: NumberFormatException) {
            this.findValue(nextVar)
        }
    }
}

fun String.higherThan(sigh: String) =
    priorities.find { it.second == this }!!.first <= priorities.find { it.second == sigh }!!.first

val priorities =
    listOf(Pair(0, "("), Pair(1, "^"), Pair(2, "*"), Pair(2, "/"), Pair(3, "+"), Pair(3, "-"), Pair(4, ")"))