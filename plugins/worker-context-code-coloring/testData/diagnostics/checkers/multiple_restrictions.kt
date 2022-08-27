import org.jetbrains.kotlin.code.coloring.RestrictedContext

@RestrictedContext([ "kotlin" ])
object FooCtx

@RestrictedContext([ "kotlin" ])
object BarCtx

context(FooCtx, BarCtx)
fun foo_bar() {
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>foo()<!>
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>bar()<!>
    both_ctx()
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>empty_ctx()<!>
}

context(FooCtx)
fun foo() {
    with(BarCtx) {
        both_ctx()
    }
}

context(BarCtx)
fun bar() { }

context(FooCtx, BarCtx)
fun both_ctx() { }

fun empty_ctx() { }
