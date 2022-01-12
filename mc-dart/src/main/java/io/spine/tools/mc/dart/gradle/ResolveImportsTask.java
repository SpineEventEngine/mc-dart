/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.common.flogger.FluentLogger;
import io.spine.tools.dart.fs.DartFile;
import io.spine.tools.fs.ExternalModules;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.gradle.task.GradleTask;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.gradle.project.Projects.getSourceSetNames;
import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.resolveImports;
import static io.spine.tools.mc.dart.gradle.Projects.getMcDart;

/**
 * Creates {@link McDartTaskName#resolveImports(SourceSetName) resolveImports} tasks in a project.
 */
final class ResolveImportsTask {

    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    private final Project project;

    private ResolveImportsTask(Project project) {
        this.project = project;
    }

    /**
     * Creates {@link McDartTaskName#resolveImports(SourceSetName) resolveImports} tasks
     * for all source sets in the given project.
     */
    static void createTasksIn(Project project) {
        checkNotNull(project);
        var factory = new ResolveImportsTask(project);
        factory.createTasks();
    }

    private void createTasks() {
        var sourceSetNames = getSourceSetNames(project);
        sourceSetNames.forEach(this::createTask);
    }

    private void createTask(SourceSetName ssn) {
        var action = createAction();
        var taskName = resolveImports(ssn);
        var copyTaskName = copyGeneratedDart(ssn);
        GradleTask.newBuilder(taskName, action)
                .insertAfterTask(copyTaskName)
                .insertBeforeTask(assemble)
                .applyNowTo(project);
    }

    private Action<Task> createAction() {
        var options = getMcDart(project);
        var libDir = options.getLibDir();
        var libPath = libDir.getAsFile()
                            .map(File::toPath)
                            .get();
        var generatedFiles = libDir.getAsFileTree().getFiles();
        var modules = options.modules();
        Action<Task> action = new ResolveImportsAction(libPath, generatedFiles, modules);
        return action;
    }

    /**
     * {@linkplain DartFile#resolveImports(Path, ExternalModules) Replaces} imports in
     * the given generated files taking into account the path to {@code lib} files and
     * external modules.
     */
    private static final class ResolveImportsAction implements Action<Task> {

        private final Path libPath;
        private final Set<File> generatedFiles;
        private final ExternalModules modules;

        private ResolveImportsAction(Path libPath,
                                     Set<File> generatedFiles,
                                     ExternalModules modules) {
            this.libPath = libPath;
            this.generatedFiles = generatedFiles;
            this.modules = modules;
        }

        @Override
        public void execute(Task task) {
            generatedFiles.forEach(this::resolveImports);
        }

        private void resolveImports(File sourceFile) {
            log.atFine().log("Resolving imports in the file `%s`.", sourceFile);
            var file = DartFile.read(sourceFile.toPath());
            file.resolveImports(libPath, modules);
        }
    }
}
