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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.Copy;

import java.io.File;

import static io.spine.tools.gradle.ProtocPluginName.dart;
import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateProto;
import static io.spine.tools.gradle.task.ProtobufTaskName.generateTestProto;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyTestGeneratedDart;
import static io.spine.tools.mc.dart.gradle.Projects.getMcDart;
import static org.gradle.api.Task.TASK_TYPE;

final class CopyTask {

    private CopyTask() {
    }

    static void createTasksIn(Project project) {
        createCopyTask(project, SourceSetName.main);
        createCopyTask(project, SourceSetName.test);
    }

    private static void createCopyTask(Project project, SourceSetName ssn) {
        McDartOptions options = getMcDart(project);

        McDartTaskName taskName;
        DirectoryProperty targetDir;
        TaskName runAfter;
        if (ssn.equals(SourceSetName.main)) {
            taskName = copyGeneratedDart;
            targetDir = options.getLibDir();
            runAfter = generateProto;
        } else {
            taskName = copyTestGeneratedDart;
            targetDir = options.getTestDir();
            runAfter = generateTestProto;
        }
        Copy task = (Copy) project.task(ImmutableMap.of(TASK_TYPE, Copy.class), taskName.name());
        task.from(options.getGeneratedBaseDir().dir(ssn.getValue() + File.separator + dart.name()));
        task.into(targetDir);
        task.dependsOn(runAfter.name());
        project.getTasks()
               .getByName(assemble.name())
               .dependsOn(taskName.name());
    }
}
