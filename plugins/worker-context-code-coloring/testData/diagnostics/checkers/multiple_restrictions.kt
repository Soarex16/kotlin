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
    // TODO: lambdas and enclosing functions
    with(BarCtx) {
        both_ctx()
        // works only because enclosing function is ''/with'', not foo
        // at the same time with call should remove restriction from outer scopes
    }
}

context(BarCtx)
fun bar() { }

context(FooCtx, BarCtx)
fun both_ctx() { }

fun empty_ctx() { }
