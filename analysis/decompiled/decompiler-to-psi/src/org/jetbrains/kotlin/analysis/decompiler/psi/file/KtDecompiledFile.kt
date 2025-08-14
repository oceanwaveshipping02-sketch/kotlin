/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.file

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.stubs.StubTreeLoader
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.text.DecompiledText
import org.jetbrains.kotlin.analysis.decompiler.psi.text.buildDecompiledText
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue

open class KtDecompiledFile(
    private val provider: KotlinDecompiledFileViewProvider,
    buildDecompiledText: (VirtualFile) -> DecompiledText
) : KtFile(provider, true) {

    override fun getStub(): KotlinFileStub? = stubTree?.root?.let { it as KotlinFileStub }

    override val greenStub: KotlinFileStub?
        get() = greenStubTree?.root?.let { it as KotlinFileStub }

    private val decompiledText = LockedClearableLazyValue(Any()) {
        val vFile = provider.virtualFile
        // A copy of the original file already has decompiled text as a content
        if (vFile is LightVirtualFile) {
            vFile.content.toString()
        } else if (stubBasedDecompilerEnabled) {
            val stubTree = ClsClassFinder.allowMultifileClassPart {
                val stubLoader = StubTreeLoader.getInstance()
                val project = project

                // The default project is not supported in the stub loader
                if (project.isDefault) {
                    stubLoader.build(/* project = */ null,/* vFile = */ vFile,/* psiFile = */ null)
                } else {
                    // Read stub from cache if it is present
                    stubLoader.readOrBuild(/* project = */ project,/* vFile = */ vFile,/* psiFile = */ null)
                }
            }

            val fileStub = stubTree?.root as? KotlinFileStub
            if (fileStub != null) {
                buildDecompiledText(fileStub)
            } else {
                val cause = if (stubTree == null) {
                    "stub tree is not found"
                } else {
                    "non-Kotlin stub tree (${stubTree::class.simpleName})"
                }

                """
                    // Could not decompile the file: $cause
                    // Please report an issue: https://kotl.in/issue
                """.trimIndent()
            }
        } else {
            buildDecompiledText(provider.virtualFile).text
        }
    }

    override fun getText(): String? {
        return decompiledText.get()
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }

}

private val stubBasedDecompilerEnabled: Boolean by lazyPub {
    Registry.`is`("kotlin.analysis.stub.based.decompiler", true)
}