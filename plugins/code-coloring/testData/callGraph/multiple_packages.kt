// DUMP_CALL_GRAPH

//FILE: file2.kt
package bar;

fun bar() { }

//FILE: file1.kt
package foo;

import bar;

fun foo() {
    bar()
    bar.bar()
}

fun bar() { }
