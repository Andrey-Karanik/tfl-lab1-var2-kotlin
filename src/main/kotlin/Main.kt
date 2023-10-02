import java.io.File
import java.io.PrintWriter

data class Coefficient(var name: String) {
    override fun toString(): String {
        return name
    }
}

/**
 * w^n*an + ... + a0
 * coefficients = (an, ... , a0)
 */
class Ordinal(val coefficients: MutableList<Coefficient>) {

    companion object {

        fun getProduct(ordinal1: Ordinal, ordinal2: Ordinal) : Ordinal {
            val list = mutableListOf<Coefficient>()
            for (i in 0..ordinal2.coefficients.size - 2) {
                list.add(ordinal2.coefficients[i].copy())
            }
            list.add(Coefficient(ordinal1.coefficients[0].toString()
                    + "*" + ordinal2.coefficients[ordinal2.coefficients.size - 1].toString()))
            list.add(ordinal1.coefficients[1].copy())

            return Ordinal(list)
        }

        fun getSum(ordinal1: Ordinal, ordinal2: Ordinal) : Ordinal {
            val list = mutableListOf<Coefficient>()

            if (ordinal1.coefficients.size > ordinal2.coefficients.size) {
                var k = 0
                for (i in 0..<ordinal1.coefficients.size) {
                    if (ordinal1.coefficients.size - i > ordinal2.coefficients.size) {
                        list.add(ordinal1.coefficients[i].copy())
                    } else {
                        if (i == ordinal1.coefficients.size - 1) {
                            list.add(ordinal2.coefficients[k].copy())
                            continue
                        }
                        list.add(Coefficient(ordinal1.coefficients[i].name + "+" + ordinal2.coefficients[k].name))
                        k++
                    }
                }
            }

            if (ordinal2.coefficients.size > ordinal1.coefficients.size) {
                var k = 0
                for (i in 0..<ordinal2.coefficients.size) {
                    if (ordinal2.coefficients.size - i > ordinal1.coefficients.size) {
                        list.add(ordinal2.coefficients[i].copy())
                    } else {
                        if (i == ordinal2.coefficients.size - 1) {
                            list.add(ordinal2.coefficients[i].copy())
                            continue
                        }
                        list.add(Coefficient(ordinal1.coefficients[k].name + "+" + ordinal2.coefficients[i].name))
                        k++
                    }
                }
            }

            return Ordinal(list)
        }

    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (isZero()) {
            return "0"
        }
        for (i in coefficients.size - 1 downTo 0) {
            if (i == 0) {
                builder.append(coefficients[coefficients.size - 1].toString())
            } else {
                builder.append("w^").append(i).append("*").append(coefficients[coefficients.size - 1 - i].toString()).append(" + ")
            }
        }
        return builder.toString()
    }

    fun isZero(): Boolean {
        return coefficients.isEmpty()
    }
}

/**
 * f(x) = ordinal1 * x + ordinal0
 */
class LinearOrdinalFunction(var ordinal1: Ordinal, var ordinal0: Ordinal) {

    companion object {

        fun buildByCoefficientPrefix(prefix: String) : LinearOrdinalFunction {
            return LinearOrdinalFunction(
                Ordinal(mutableListOf(Coefficient(prefix + "11"), Coefficient(prefix + "10"))),
                Ordinal(mutableListOf(Coefficient(prefix + "01"), Coefficient(prefix + "00"))))
        }

        fun compose(function1: LinearOrdinalFunction, function2: LinearOrdinalFunction) : LinearOrdinalFunction {
            val ord1 = Ordinal.getProduct(function1.ordinal1, function2.ordinal1)
            val ord2 = Ordinal.getProduct(function1.ordinal1, function2.ordinal0)
            val ord3 = Ordinal.getSum(ord2, function1.ordinal0)

            return LinearOrdinalFunction(ord1, ord3)
        }
    }

    override fun toString(): String {
        val builder = StringBuilder()
        if (!ordinal1.isZero()) {
            builder.append("(").append(ordinal1).append(")x")
            if (!ordinal0.isZero()) {
                builder.append(" + ").append(ordinal0)
            }
        } else if(!ordinal0.isZero()) {
            builder.append(ordinal0)
        } else {
            return "0"
        }
        return builder.toString()
    }
}

class Operation(var name: String) {
    var operation1: Operation? = null
    var operation2: Operation? = null
    var data1: String = ""
    var data2: String = ""

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("($name ")
        if (operation1 == null) {
            builder.append(data1)
        } else {
            builder.append("$operation1")
        }
        builder.append(" ")
        if (operation2 == null) {
            builder.append(data2)
        } else {
            builder.append("$operation2")
        }
        builder.append(")")
        return builder.toString()
    }
}

fun main(args: Array<String>) {
    val stringRules: MutableList<Pair<String, String>> = mutableListOf()
    File("input.txt").forEachLine {
        val parts = it.split("->")
        val pair = Pair(parts[0].replace(" ", ""), parts[1].replace(" ", ""))
        var exists = false
        for (rule in stringRules) {
            if (rule.first == pair.first && rule.second == pair.second) {
                exists = true
                break
            }
        }
        if (!exists) {
            stringRules.add(pair)
        }
    }

    val rules: MutableList<Pair<LinearOrdinalFunction, LinearOrdinalFunction>> = mutableListOf()
    val coefficientNameSet: MutableSet<String> = mutableSetOf()

    for (stringRule in stringRules) {
        rules.add(
            Pair(generateLOF(stringRule.first, coefficientNameSet),
                generateLOF(stringRule.second, coefficientNameSet)
            )
        )
    }

    writeToSmtFile(rules, coefficientNameSet)
}

fun generateLOF(string: String, coefficientNameSet: MutableSet<String>) : LinearOrdinalFunction {
    var result = LinearOrdinalFunction.buildByCoefficientPrefix(string[string.length - 1].toString())
    for (i in 0..1) {
        coefficientNameSet.add(result.ordinal0.coefficients[i].name)
        coefficientNameSet.add(result.ordinal1.coefficients[i].name)
    }
    if (string.length > 1) {
        for (i in string.length - 2 downTo 0) {
            val function = LinearOrdinalFunction.buildByCoefficientPrefix(string[i].toString())
            for (j in 0..1) {
                coefficientNameSet.add(function.ordinal0.coefficients[j].name)
                coefficientNameSet.add(function.ordinal1.coefficients[j].name)
            }
            result = LinearOrdinalFunction.compose(function, result)
        }
    }

    return result
}

fun writeToSmtFile(rules: MutableList<Pair<LinearOrdinalFunction, LinearOrdinalFunction>>, coefficientNameSet: MutableSet<String>) {
    val file = File("output.smt")
    PrintWriter(file, Charsets.UTF_8).use {
        it.println("(set-logic QF_NIA)")
        for (name in coefficientNameSet) {
            smtDeclareConst(it, name)
            val operation = Operation(">=")
            operation.data1 = name
            operation.data2 = "0"
            smtAssert(it, operation)
        }
        for (rule in rules) {

            if (rule.first.ordinal0.coefficients.size < rule.second.ordinal0.coefficients.size) {
                extendOrdinal(rule.first.ordinal0,  rule.second.ordinal0.coefficients.size - rule.first.ordinal0.coefficients.size)
                extendOrdinal(rule.first.ordinal1,  rule.second.ordinal1.coefficients.size - rule.first.ordinal1.coefficients.size)
            } else {
                extendOrdinal(rule.second.ordinal0,  rule.first.ordinal0.coefficients.size - rule.second.ordinal0.coefficients.size)
                extendOrdinal(rule.second.ordinal1,  rule.first.ordinal1.coefficients.size - rule.second.ordinal1.coefficients.size)
            }

            val root = Operation("or")
            val andOperation1 = Operation("and")
            val andOperation2 = Operation("and")

            root.operation1 = andOperation1
            root.operation2 = andOperation2

            var orOperation11 = Operation("or")
            var orOperation12 = Operation("or")
            var orOperation22 = Operation("or")

            andOperation1.operation1 = orOperation11
            andOperation1.operation2 = orOperation12
            andOperation2.operation2 = orOperation22

            var operation = orOperation11

            val n = rule.first.ordinal1.coefficients.size

            for (i in 0..<n) {
                when (i) {
                    0 -> {
                        operation.operation1 = asAndOperation(">", "=", rule.first.ordinal1, rule.second.ordinal1, 0)
                    }
                    1 -> {
                        operation.operation2 = asAndOperation(">", "=", rule.first.ordinal1, rule.second.ordinal1, 1)
                    }
                    else -> {
                        val op = operation.operation2
                        operation.operation2 = Operation("or")
                        operation = operation.operation2!!
                        operation.operation1 = op
                        operation.operation2 = asAndOperation(">", "=", rule.first.ordinal1, rule.second.ordinal1, i)
                    }
                }
            }

            operation = orOperation12

            for (i in 0..<n) {
                when (i) {
                    0 -> {
                        operation.operation1 = asAndOperation(">=", "=", rule.first.ordinal0, rule.second.ordinal0, 0)
                    }
                    1 -> {
                        operation.operation2 = asAndOperation(">=", "=", rule.first.ordinal0, rule.second.ordinal0, 1)
                    }
                    else -> {
                        val op = operation.operation2
                        operation.operation2 = Operation("or")
                        operation = operation.operation2!!
                        operation.operation1 = op
                        operation.operation2 = asAndOperation(">=", "=", rule.first.ordinal0, rule.second.ordinal0, i)
                    }
                }
            }

            andOperation2.operation1 = asAndOperation("=", "=", rule.first.ordinal1, rule.second.ordinal1, 0)

            operation = orOperation22

            for (i in 0..<n) {
                when (i) {
                    0 -> {
                        operation.operation1 = asAndOperation(">", "=", rule.first.ordinal0, rule.second.ordinal0, 0)
                    }
                    1 -> {
                        operation.operation2 = asAndOperation(">", "=", rule.first.ordinal0, rule.second.ordinal0, 1)
                    }
                    else -> {
                        val op = operation.operation2
                        operation.operation2 = Operation("or")
                        operation = operation.operation2!!
                        operation.operation1 = op
                        operation.operation2 = asAndOperation(">", "=", rule.first.ordinal0, rule.second.ordinal0, i)
                    }
                }
            }

            smtAssert(it, root)
        }
        it.println("(check-sat)")
        it.println("(get-model)")
    }
}

fun smtDeclareConst(writer: PrintWriter, name: String) {
    writer.println("(declare-const $name Int)")
}

fun smtAssert(writer: PrintWriter, operation: Operation) {
    writer.println("(assert $operation)")
}

fun asAndOperation(firstOperationName: String, otherOperationName: String, ordinal1: Ordinal, ordinal2: Ordinal, startIndex: Int): Operation {
    val n = ordinal1.coefficients.size
    var op = Operation(firstOperationName)
    val name1 = ordinal1.coefficients[n - 1 - startIndex].name
    val name2 = ordinal2.coefficients[n - 1 - startIndex].name

    if (name1.contains("*")) {
        op.operation1 = transformNameToOperation(name1)
    } else {
        op.data1 = name1
    }

    if (name2.contains("*")) {
        op.operation2 = transformNameToOperation(name2)
    } else {
        op.data2 = name2
    }

    if (startIndex == n - 1) {
        return op
    }

    val root = Operation("and")
    root.operation1 = op
    var operation = root

    for (i in startIndex+1..<n) {
        op = Operation(otherOperationName)
        val opName1 = ordinal1.coefficients[n - 1 - i].name
        val opName2 = ordinal2.coefficients[n - 1 - i].name

        if (opName1.contains("*")) {
            op.operation1 = transformNameToOperation(opName1)
        } else {
            op.data1 = opName1
        }

        if (opName2.contains("*")) {
            op.operation2 = transformNameToOperation(opName2)
        } else {
            op.data2 = opName2
        }

        if (operation.operation2 == null) {
            operation.operation2 = op
        } else {
            val temp = operation.operation2
            operation.operation2 = Operation("and")
            operation = operation.operation2!!
            operation.operation1 = temp
            operation.operation2 = op
        }
    }
    return root
}

fun extendOrdinal(ordinal: Ordinal, count: Int) {
    for (i in 0..<count) {
        ordinal.coefficients.add(0, Coefficient("0"))
    }
}

fun transformNameToOperation(name: String) : Operation {
    val operation = Operation("*")
    val list = name.split("*")
    operation.data1 = list.first
    if (list.last.contains("+")) {
        val list2 = list.last.split("+")
        val op = Operation("+")
        op.data1 = list2.first
        op.data2 = list2.last
        operation.operation2 = op
    } else {
        operation.data2 = list.last
    }

    return operation
}

