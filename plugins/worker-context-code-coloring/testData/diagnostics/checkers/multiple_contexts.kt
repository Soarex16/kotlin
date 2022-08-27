import org.jetbrains.kotlin.code.coloring.RestrictedContext

interface SimpleCtx

@RestrictedContext
interface RestrictedCtx

context(SimpleCtx, RestrictedCtx)
fun main() {
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>simple()<!>
    restricted()
    both_ctx()
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>empty_ctx()<!>
}

context(SimpleCtx)
fun simple() { }

context(RestrictedCtx)
fun restricted() { }

context(SimpleCtx, RestrictedCtx)
fun both_ctx() { }

fun empty_ctx() { }
