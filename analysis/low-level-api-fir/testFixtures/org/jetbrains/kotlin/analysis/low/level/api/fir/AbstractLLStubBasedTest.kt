/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.util.Disposer
import com.intellij.psi.AbstractFileViewProvider
import com.intellij.psi.impl.PsiManagerEx
import org.jetbrains.kotlin.analysis.low.level.api.fir.AbstractLLStubBasedTest.Companion.computeAstLoadingAware
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderLazyBodiesByStubTest
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * This test case allows testing differences between stub-based and AST-based files.
 *
 * The entry points for tests are [doStubBasedPart] and [doAstBasedPart].
 *
 * @see withAstLoadingAssertion
 * @see computeAstLoadingAware
 * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
 * @see com.intellij.psi.impl.PsiManagerEx.isAssertOnFileLoading
 */
abstract class AbstractLLStubBasedTest<StubBasedOutput> : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    protected object Directives : SimpleDirectivesContainer() {
        /**
         * This directive has to be in sync with [AbstractRawFirBuilderLazyBodiesByStubTest]
         * as [AbstractLLAnnotationArgumentsCalculatorTest] is supposed to work as an extension to the stub test.
         */
        val IGNORE_TREE_ACCESS by stringDirective("Disables the test. The YT issue number has to be provided")

        val INCONSISTENT_DECLARATIONS by directive("Indicates that stub-based and AST-based have a different number of declarations")
    }

    override fun doTestByMainFile(mainFile: KtFile, mainModule: KtTestModule, testServices: TestServices) {
        if (Directives.IGNORE_TREE_ACCESS in testServices.moduleStructure.allDirectives) return

        val viewProvider = mainFile.viewProvider as AbstractFileViewProvider
        val myPhysicalField = AbstractFileViewProvider::class.java.getDeclaredField("myPhysical").apply { isAccessible = true }

        // A hack to enable AST loading assertion and allow stub requests
        myPhysicalField.set(viewProvider, true)

        mainFile.setTreeElementPointer(null)
        testServices.assertions.assertNotNull(mainFile.stub) { "Stub should be present for unloaded file" }

        val output = withAstLoadingAssertion(mainFile) {
            withResolutionFacade(mainFile) { facade ->
                context(facade) {
                    doStubBasedPart(mainFile, mainModule, testServices)
                }
            }
        }

        // We need to clear caches to ensure that we will not load the file from the cache
        clearCaches(mainFile.project)
        mainFile.calcTreeElement()
        testServices.assertions.assertTrue(mainFile.stub == null) { "Stub shouldn't be present for loaded file" }

        withResolutionFacade(mainFile) { facade ->
            context(facade) {
                doAstBasedPart(output, mainFile, mainModule, testServices)
            }
        }
    }

    protected fun <T> withAstLoadingAssertion(file: KtFile, action: () -> T): T {
        val virtualFile = file.virtualFile
        val disposable = Disposer.newDisposable("AST loading assertion")
        (file.manager as PsiManagerEx).setAssertOnFileLoadingFilter({ it == virtualFile }, disposable)
        return try {
            action()
        } finally {
            Disposer.dispose(disposable)
        }
    }

    context(facade: LLResolutionFacade)
    abstract fun doStubBasedPart(
        stubBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    ): StubBasedOutput

    /** @param stubBasedOutput the output of [doStubBasedPart] */
    context(facade: LLResolutionFacade)
    abstract fun doAstBasedPart(
        stubBasedOutput: StubBasedOutput,
        astBasedFile: KtFile,
        mainModule: KtTestModule,
        testServices: TestServices,
    )

    companion object {
        /**
         * Runs the given [action] and returns its result.
         * `null` is returned in case the ast loading attempt.
         */
        fun <T : Any> computeAstLoadingAware(action: () -> T): T? {
            val result = runCatching {
                action()
            }

            result.exceptionOrNull()?.let { exception ->
                if (exception.message?.startsWith("Access to tree elements not allowed for") != true) {
                    throw exception
                }
            }

            return result.getOrNull()
        }
    }
}
