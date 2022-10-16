// DUMP_CALL_GRAPH

//FILE: file1.kt
fun foo() {
    bar()
}

fun bar() {
    baz()
    sum(1, 2)
}

//FILE: file2.kt
fun baz() { }

fun sum(a: Int, b: Int) = a + b