// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1109
package foo


fun box(): String {
    val a: Int? = null

    try {
        if ((a!! + 3) == 3) return "fail1"
    }
    catch (e: NullPointerException) {
        return "OK"
    }
    return "fail2"
}