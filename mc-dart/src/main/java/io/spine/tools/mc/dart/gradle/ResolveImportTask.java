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

import com.google.common.flogger.FluentLogger;
import io.spine.tools.dart.fs.DartFile;
import io.spine.tools.fs.ExternalModules;
import io.spine.tools.gradle.task.GradleTask;
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

final class ResolveImportTask {

    private static final FluentLogger log = FluentLogger.forEnclosingClass();

    private ResolveImportTask() {
    }

    static void createTasksIn(Project project) {
        McDartOptions options = getMcDart(project);
        createTask(project, options, false);
        createTask(project, options, true);
    }

    private static void createTask(Project project, McDartOptions options, boolean tests) {
        ExternalModules modules = options.modules();
        Action<Task> action = new ReplaceImportAction(options.getLibDir(), modules);
        GradleTask.newBuilder(tests ? resolveTestImports : resolveImports, action)
                .insertAfterTask(copyGeneratedDart)
                .insertBeforeTask(assemble)
                .applyNowTo(project);
    }

    private static final class ReplaceImportAction implements Action<Task> {

        private final DirectoryProperty libDir;
        private final ExternalModules modules;

        private ReplaceImportAction(DirectoryProperty dir, ExternalModules modules) {
            this.libDir = dir;
            this.modules = modules;
        }

        @Override
        public void execute(Task task) {
            FileTree generatedFiles = libDir.getAsFileTree();
            generatedFiles.forEach(this::resolveImports);
        }

        private void resolveImports(File sourceFile) {
            log.atFine().log("Resolving imports in the file `%s`.", sourceFile);
            Path libPath = libDir.getAsFile()
                                 .map(File::toPath)
                                 .get();
            DartFile file = DartFile.read(sourceFile.toPath());
            file.resolveImports(libPath, modules);
        }
    }
}
