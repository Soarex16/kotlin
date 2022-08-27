// WITH_STDLIB
import org.jetbrains.kotlin.code.coloring.RestrictedContext
import org.w3c.dom.Window
import kotlinx.browser.window

@RestrictedContext([ "kotlin" ])
interface WorkerScope {
    @Deprecated("", level = DeprecationLevel.ERROR)
    // такая форма записи плоха тем, что мы выбираем имя переменной так,
    // чтобы оно перекрывало собой "kotlinx.browser.window"
    val window: Window
}

context(WorkerScope)
fun workerFun() {
    safe() // ok
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>unsafe()<!> // not ok
    <!ILLEGAL_RESTRICTED_FUNCTION_CALL!>safeWithoutMark()<!> // not ok
    val x = listOf(1, 2, 3)
    val y = x.map { it + 1 }
}

context(WorkerScope)
fun safe(): Int {
    return 42
}

// С точки зрения вычислительной семантики тут все ок,
// но с учетом синтаксиса контекстов выглядит странно
fun WorkerScope.safeExtension(): Int {
    return 42
}

fun safeWithoutMark(): Int {
    return 42
}

fun unsafe(): Int {
    return window.outerHeight
}

fun main() {
    // Usual code
    with(object : WorkerScope {
        @Deprecated("", level = DeprecationLevel.ERROR)
        override val window: Window
            get() = TODO("Not yet implemented")
    }) {
        workerFun()
    }
}
