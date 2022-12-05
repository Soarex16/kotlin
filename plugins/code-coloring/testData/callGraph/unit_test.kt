// DUMP_CALL_GRAPH
// WITH_STDLIB

import org.jetbrains.kotlin.code.coloring.CodeWithIO
import org.jetbrains.kotlin.code.coloring.CodeWithoutIO

abstract class Test {
    @CodeWithIO
    abstract fun run()
}

open class TestImpl : Test() {
    override fun run() {
        println("test impl")
    }
}

class TestImplImpl : TestImpl() {
    override fun run() {
        println("test impl impl")
    }
}

abstract class OpaqueTestWrapper: Test()

class TestImplImplWrapper : OpaqueTestWrapper() {
    override fun run() {
        println("test impl impl")
    }
}

// TODO: анализ value параметров
@CodeWithoutIO
fun runTest(test: @CodeWithoutIO () -> Unit) {
    try {
        test()
    } catch (e : Throwable) {
        // report fail
    } finally {
        // clean up
    }
}

@CodeWithoutIO
fun badRunTest(test: () -> Unit) { }

@CodeWithoutIO
fun goodTest() { }

fun main() {
    val impl3 = TestImplImplWrapper()
    val impl2 = TestImplImpl()
    val impl = TestImpl()
    val base: Test = TestImpl()

    // in backend erasure to Function0<Unit>
    val x = @CodeWithIO { Unit }
    runTest(x)

    runTest(::goodTest) // also KFunction0<Unit>

    // doesn't work
    runTest(impl3::run)
    runTest(impl2::run)
    runTest(impl::run)
    runTest(base::run)
}
