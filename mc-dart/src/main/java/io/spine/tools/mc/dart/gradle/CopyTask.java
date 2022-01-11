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

import com.google.common.collect.ImmutableMap;
import io.spine.tools.code.SourceSetName;
import io.spine.tools.gradle.task.TaskName;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.Copy;

import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.tools.gradle.ProtocPluginName.dart;
import static io.spine.tools.gradle.project.Projects.getSourceSetNames;
import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateProto;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.Projects.getMcDart;
import static io.spine.tools.mc.dart.gradle.StandardTypes.camelToSnake;
import static org.gradle.api.Task.TASK_TYPE;

/**
 * Creates {@link McDartTaskName#copyGeneratedDart(SourceSetName) copyGeneratedDart} tasks
 * in a project.
 */
final class CopyTask {

    private final Project project;

    private CopyTask(Project project) {
        this.project = project;
    }

    /**
     * Creates {@link McDartTaskName#copyGeneratedDart(SourceSetName) copyGeneratedDart} in
     * the given project for all source sets.
     */
    static void createTasksIn(Project project) {
        checkNotNull(project);
        var factory = new CopyTask(project);
        factory.createTasks();
    }

    private void createTasks() {
        var sourceSetNames = getSourceSetNames(project);
        sourceSetNames.forEach(this::createTask);
    }

    private void createTask(SourceSetName ssn) {
        var taskName = copyGeneratedDart(ssn);
        var task = (Copy) project.task(ImmutableMap.of(TASK_TYPE, Copy.class), taskName.value());

        var sourceDir = sourceDir(ssn);
        task.from(sourceDir);

        var targetDir = targetDir(ssn);
        task.into(targetDir);

        var runAfter = generateProto(ssn);
        task.dependsOn(runAfter.name());

        project.getTasks()
               .getByName(assemble.name())
               .dependsOn(taskName.value());
    }

    private Directory sourceDir(SourceSetName ssn) {
        var options = getMcDart(project);
        var sourceDir = options.getGeneratedBaseDir()
                               .dir(ssn.getValue() + File.separator + dart.name())
                               .get();
        return sourceDir;
    }

    /**
     * Obtains the target directory for the copy operation.
     *
     * <p>If the given source set is {@code main} the {@link McDartOptions#getLibDir()}  lib}
     * directory will be returned.
     *
     * <p>If the source set is {@code test} the {@link  McDartOptions#getTestDir() test}
     * directory will be returned.
     *
     * <p>For a custom source set, the directory named after the {@code snake_case} of
     * the given source set under the root of the project will be returned. E.g. if the given
     * source set is {@code integrationTest} the directory would be {@code integration_test}.
     */
    private Directory targetDir(SourceSetName ssn) {
        var options = getMcDart(project);
        if (ssn.equals(SourceSetName.main)) {
            return options.getLibDir().get();
        }
        if (ssn.equals(SourceSetName.test)) {
            return options.getTestDir().get();
        }

        var ssnSnailCase = camelToSnake(ssn.getValue());
        var customTarget = project.getLayout()
                                  .getProjectDirectory()
                                  .dir(ssnSnailCase);
        return customTarget;
    }
}
