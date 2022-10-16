// support lib

// meta-annotation to create color markers
// we use integers as colors because they form a lattice
@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class CodeColor(val color: Int)

// user lib

@CodeColor(42) // some unique color
@Target(AnnotationTarget.FUNCTION)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class UnitTestCode

// user code
abstract class Test {
    @UnitTestCode
    abstract fun run()
}

fun runTest(@UnitTestCode test: () -> Unit) {
    try {
        test()
    } catch (e : Throwable) {
        // report fail
    } finally {
        // clean up
    }
}