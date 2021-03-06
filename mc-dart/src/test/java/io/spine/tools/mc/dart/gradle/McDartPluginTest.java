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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.tools.gradle.task.TaskName;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.tools.code.SourceSetName.main;
import static io.spine.tools.code.SourceSetName.test;
import static io.spine.tools.gradle.task.BaseTaskName.assemble;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.copyGeneratedDart;
import static io.spine.tools.mc.dart.gradle.McDartTaskName.resolveImports;

@DisplayName("`McDartPlugin` should")
class McDartPluginTest {

    private static @MonotonicNonNull Project project = null;

    @BeforeAll
    static void setUp(@TempDir File dir) {
        project = ProjectBuilder.builder()
                .withName(McDartPluginTest.class.getName())
                .withProjectDir(dir)
                .build();
        project.apply(action -> action.plugin("java"));

        var plugin = new McDartPlugin();
        plugin.apply(project);
    }

    @Nested
    @DisplayName("create tasks")
    class TaskCreation {

        @Test
        @DisplayName("`copyGeneratedDart`")
        void createMainTask() {
            var task = findTask(copyGeneratedDart(main));
            assertThat(task.getDependsOn()).isNotEmpty();

            var assembleTask = findTask(assemble);
            assertThat(assembleTask.getDependsOn()).contains(task.getName());
        }

        @Test
        @DisplayName("`copyTestGeneratedDart`")
        void createTestTask() {
            var task = findTask(copyGeneratedDart(test));
            assertThat(task.getDependsOn()).isNotEmpty();

            var assembleTask = findTask(assemble);
            assertThat(assembleTask.getDependsOn()).contains(task.getName());
        }

        @Test
        @DisplayName("`resolveImports`")
        void createResolveTask() {
            findTask(resolveImports(main));
        }

        @CanIgnoreReturnValue
        private Task findTask(TaskName name) {
            var task = project.getTasks()
                              .findByName(name.name());
            assertThat(task).isNotNull();
            return task;
        }
    }
}
