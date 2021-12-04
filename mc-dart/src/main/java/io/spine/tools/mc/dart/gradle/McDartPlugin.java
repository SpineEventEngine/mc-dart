/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.mc.dart.gradle;

import io.spine.logging.Logging;
import io.spine.tools.dart.fs.DartFile;
import io.spine.tools.gradle.task.GradleTask;
import io.spine.tools.mc.gradle.LanguagePlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;

import java.io.File;
import java.nio.file.Path;

import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.resolveImports;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.resolveTestImports;
import static io.spine.tools.mc.dart.gradle.Projects.getMcDart;
import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;

/**
 * A Gradle plugin which configures Protobuf Dart code generation.
 *
 * <p>Generates mapping between Protobuf type URLs and Dart types and reflective
 * descriptors (a.k.a. {@code BuilderInfo}s).
 *
 * @see ProtocConfig
 */
public final class McDartPlugin extends LanguagePlugin implements Logging {

    public McDartPlugin() {
        super(McDartOptions.NAME, getKotlinClass(McDartOptions.class));
    }

    @Override
    public void apply(Project project) {
        super.apply(project);
        ProtocConfig.applyTo(project);
        createTasks(project);
    }

    private void createTasks(Project project) {
        McDartOptions options = getMcDart(project);
        options.createMainCopyTaskIn(project);
        options.createTestCopyTaskIn(project);
        createMainResolveImportTask(project, options);
        createTestResolveImportTask(project, options);
    }

    private void createMainResolveImportTask(Project project, McDartOptions extension) {
        DirectoryProperty rootDir = extension.getGeneratedMainDir();
        doCreateResolveImportsTask(project, extension, rootDir, false);
    }

    private void createTestResolveImportTask(Project project, McDartOptions extension) {
        DirectoryProperty rootDir = extension.getGeneratedTestDir();
        doCreateResolveImportsTask(project, extension, rootDir, true);
    }

    private void doCreateResolveImportsTask(Project project,
                                            McDartOptions extension,
                                            DirectoryProperty rootDir,
                                            boolean tests) {
        Action<Task> action = task -> {
            FileTree generatedFiles = rootDir.getAsFileTree();
            generatedFiles.forEach(file -> resolveImports(file, extension));
        };
        GradleTask.newBuilder(tests ? resolveTestImports : resolveImports, action)
                .insertAfterTask(copyGeneratedDart)
                .insertBeforeTask(assemble)
                .applyNowTo(project);
    }

    private void resolveImports(File sourceFile, McDartOptions extension) {
        _debug().log("Resolving imports in the file `%s`.", sourceFile);
        DartFile file = DartFile.read(sourceFile.toPath());
        Path libPath = extension.getLibDir()
                                .getAsFile()
                                .map(File::toPath)
                                .get();
        file.resolveImports(libPath, extension.modules());
    }
}
