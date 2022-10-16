// DUMP_CALL_GRAPH

fun foo() {
    val t: A = C()

}

abstract class A {
    abstract fun foo(): Int
}

class B : A() {
    override fun foo(): Int {
        return 42
    }
}

class C : A() {
    override fun foo(): Int {
        return 100
    }
}
