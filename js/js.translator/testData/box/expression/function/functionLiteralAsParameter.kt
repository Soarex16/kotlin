// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
package foo

fun apply(f: (Int) -> Int, t: Int): Int {
    return f(t)
}


fun box(): String {
    return if (apply({ a: Int -> a + 5 }, 3) == 8) return "OK" else "fail"
}