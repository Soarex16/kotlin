import org.jetbrains.kotlin.code.coloring.RestrictedContext
import org.w3c.dom.Window
import kotlinx.browser.window

@RestrictedContext
interface WorkerScope {
    @Deprecated("", level = DeprecationLevel.ERROR)
    // такая форма записи плоха тем, что мы выбираем имя переменной так,
    // чтобы оно перекрывало собой "kotlinx.browser.window"
    val window: Window
}

context(WorkerScope)
fun workerFun() {
    safe() // ok
    unsafe() // not ok
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
