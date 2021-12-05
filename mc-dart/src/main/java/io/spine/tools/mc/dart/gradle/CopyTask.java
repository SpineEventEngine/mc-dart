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

import com.google.common.collect.ImmutableMap;
import io.spine.tools.gradle.SourceSetName;
import io.spine.tools.gradle.task.TaskName;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.Copy;

import java.io.File;

import static io.spine.tools.gradle.ProtocPluginName.dart;
import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateProto;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateTestProto;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyTestGeneratedDart;
import static io.spine.tools.mc.dart.gradle.Projects.getMcDart;
import static io.spine.tools.mc.dart.gradle.StandardTypes.camelToSnake;
import static org.gradle.api.Task.TASK_TYPE;

final class CopyTask {

    private CopyTask() {
    }

    static void createTasksIn(Project project) {
        createCopyTask(project, SourceSetName.main);
        createCopyTask(project, SourceSetName.test);
    }

    private static void createCopyTask(Project project, SourceSetName ssn) {
        TaskName taskName = taskName(ssn);
        Copy task = (Copy) project.task(ImmutableMap.of(TASK_TYPE, Copy.class), taskName.name());

        Directory sourceDir = sourceDir(project, ssn);
        task.from(sourceDir);

        Directory targetDir = targetDir(project, ssn);
        task.into(targetDir);

        TaskName runAfter = runAfter(ssn);
        task.dependsOn(runAfter.name());

        project.getTasks()
               .getByName(assemble.name())
               .dependsOn(taskName.name());
    }

    private static Directory sourceDir(Project project, SourceSetName ssn) {
        McDartOptions options = getMcDart(project);
        Directory sourceDir =
                options.getGeneratedBaseDir()
                       .dir(ssn.getValue() + File.separator + dart.name())
                       .get();
        return sourceDir;
    }

    private static TaskName taskName(SourceSetName ssn) {
        //TODO:2021-12-05:alexander.yevsyukov: Return calculated here.
        if (ssn.equals(SourceSetName.main)) {
            return copyGeneratedDart;
        } else {
            return copyTestGeneratedDart;
        }
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
    private static Directory targetDir(Project project, SourceSetName ssn) {
        McDartOptions options = getMcDart(project);
        if (ssn.equals(SourceSetName.main)) {
            return options.getLibDir().get();
        }
        if (ssn.equals(SourceSetName.test)) {
            return options.getTestDir().get();
        }

        String ssnSnailCase = camelToSnake(ssn.getValue());
        Directory customTarget = project.getLayout()
                               .getProjectDirectory()
                               .dir(ssnSnailCase);
        return customTarget;
    }

    private static TaskName runAfter(SourceSetName ssn) {
        //TODO:2021-12-05:alexander.yevsyukov: Return calculated here.
        if (ssn.equals(SourceSetName.main)) {
            return generateProto;
        } else {
            return generateTestProto;
        }
    }
}
