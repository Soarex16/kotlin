// DUMP_CALL_GRAPH

fun foo() {
    Test(12)
}

class Test(s: String) {
    constructor(x: Int): this(x.toString()) {

    }
}
