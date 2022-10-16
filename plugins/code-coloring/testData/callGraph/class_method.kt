// DUMP_CALL_GRAPH

fun foo() {
    Test().testFun()
}

class Test {
    fun testFun() {}
}
