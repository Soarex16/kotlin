/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.coloring.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link GenerateNewCompilerTests.kt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/worker-context-code-coloring/testData/diagnostics")
@TestDataPath("$PROJECT_ROOT")
public class ContextCodeColoringDiagnosticTestGenerated extends AbstractContextCodeColoringDiagnosticTest {
    @Test
    public void testAllFilesPresentInDiagnostics() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/worker-context-code-coloring/testData/diagnostics"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Nested
    @TestMetadata("plugins/worker-context-code-coloring/testData/diagnostics/checkers")
    @TestDataPath("$PROJECT_ROOT")
    public class Checkers {
        @Test
        public void testAllFilesPresentInCheckers() throws Exception {
            KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/worker-context-code-coloring/testData/diagnostics/checkers"), Pattern.compile("^(.+)\\.kt$"), null, true);
        }

        @Test
        @TestMetadata("multiple_contexts.kt")
        public void testMultiple_contexts() throws Exception {
            runTest("plugins/worker-context-code-coloring/testData/diagnostics/checkers/multiple_contexts.kt");
        }

        @Test
        @TestMetadata("multiple_restrictions.kt")
        public void testMultiple_restrictions() throws Exception {
            runTest("plugins/worker-context-code-coloring/testData/diagnostics/checkers/multiple_restrictions.kt");
        }

        @Test
        @TestMetadata("simple.kt")
        public void testSimple() throws Exception {
            runTest("plugins/worker-context-code-coloring/testData/diagnostics/checkers/simple.kt");
        }
    }
}
