/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TargetBackend;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link GenerateNewCompilerTests.kt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers")
@TestDataPath("$PROJECT_ROOT")
public class SerializationFirBlackBoxTestGenerated extends AbstractSerializationFirBlackBoxTest {
    @Test
    public void testAllFilesPresentInFirMembers() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers"), Pattern.compile("^(.+)\\.kt$"), null, TargetBackend.JVM_IR, true);
    }

    @Test
    @TestMetadata("classWithCompanionObject.kt")
    public void testClassWithCompanionObject() throws Exception {
        runTest("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers/classWithCompanionObject.kt");
    }

    @Test
    @TestMetadata("classWithGenericParameters.kt")
    public void testClassWithGenericParameters() throws Exception {
        runTest("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers/classWithGenericParameters.kt");
    }

    @Test
    @TestMetadata("defaultProperties.kt")
    public void testDefaultProperties() throws Exception {
        runTest("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers/defaultProperties.kt");
    }

    @Test
    @TestMetadata("multipleProperties.kt")
    public void testMultipleProperties() throws Exception {
        runTest("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers/multipleProperties.kt");
    }

    @Test
    @TestMetadata("privatePropertiesSerialization.kt")
    public void testPrivatePropertiesSerialization() throws Exception {
        runTest("plugins/kotlin-serialization/kotlin-serialization-compiler/testData/firMembers/privatePropertiesSerialization.kt");
    }
}
