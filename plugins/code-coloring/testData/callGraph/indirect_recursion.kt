// DUMP_CALL_GRAPH

import org.jetbrains.kotlin.code.coloring.*

fun f1() {
    f2()
    g1()
    g2()
}

@CodeWithIO
fun f2() {
    f3()
}

fun f3() {
    f4()
}

@CodeWithoutIO
fun f4() {
    f1()
}

@CodeWithoutIO
fun g1() {

}

fun g2() {
    h1()
}

@CodeWithoutIO
fun h1() {

}

fun a1() {
    g1()
}

fun b1() {
    a1()
    g1()
    g2()
}

@CodeWithoutIO
fun foo1() {
    foo2()
}

fun foo2() {
    h1()
    foo3()
}

@CodeWithoutIO
fun foo3() {
    foo1()
}

fun main() {
    f1()
    b1()
    foo1()
}
