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
import io.spine.tools.gradle.task.TaskName;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

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
        createTask(project, false);
        createTask(project, true);
    }

    private static void createTask(Project project, boolean tests) {
        Action<Task> action = createAction(project);
        TaskName taskName = tests ? resolveTestImports : resolveImports;
        //TODO:2021-12-05:alexander.yevsyukov: Shouldn't it depend on source set?
        TaskName copyTaskName = copyGeneratedDart;
        GradleTask.newBuilder(taskName, action)
                .insertAfterTask(copyTaskName)
                .insertBeforeTask(assemble)
                .applyNowTo(project);
    }

    private static Action<Task> createAction(Project project) {
        McDartOptions options = getMcDart(project);
        DirectoryProperty libDir = options.getLibDir();
        Path libPath = libDir.getAsFile()
                             .map(File::toPath)
                             .get();
        Set<File> generatedFiles = libDir.getAsFileTree()
                                         .getFiles();
        ExternalModules modules = options.modules();
        Action<Task> action = new ReplaceImportAction(libPath, generatedFiles, modules);
        return action;
    }

    private static final class ReplaceImportAction implements Action<Task> {

        private final Path libPath;
        private final Set<File> generatedFiles;
        private final ExternalModules modules;

        private ReplaceImportAction(Path libPath,
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
            DartFile file = DartFile.read(sourceFile.toPath());
            file.resolveImports(libPath, modules);
        }
    }
}
