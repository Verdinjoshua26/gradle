/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.performance.fixture

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.Nullable

@CompileStatic
@EqualsAndHashCode
class BuildExperimentSpec {

    String displayName
    String projectName
    GradleInvocationSpec invocation
    @Nullable
    Integer warmUpCount
    @Nullable
    Integer invocationCount
    Long sleepAfterWarmUpMillis
    Long sleepAfterTestRoundMillis
    BuildExperimentListener listener

    BuildExperimentSpec(String displayName, String projectName, GradleInvocationSpec invocation, Integer warmUpCount, Integer invocationCount, Long sleepAfterWarmUpMillis, Long sleepAfterTestRoundMillis, BuildExperimentListener listener) {
        this.displayName = displayName
        this.projectName = projectName
        this.invocation = invocation
        this.warmUpCount = warmUpCount
        this.invocationCount = invocationCount
        this.sleepAfterWarmUpMillis = sleepAfterWarmUpMillis
        this.sleepAfterTestRoundMillis = sleepAfterTestRoundMillis
        this.listener = listener
    }

    static Builder builder() {
        new Builder()
    }

    BuildDisplayInfo getDisplayInfo() {
        new BuildDisplayInfo(projectName, displayName, invocation.tasksToRun, invocation.args, invocation.jvmOpts, invocation.useDaemon)
    }

    static class Builder {
        String displayName
        String projectName
        GradleInvocationSpec.Builder invocation = GradleInvocationSpec.builder()
        Integer warmUpCount
        Integer invocationCount
        Long sleepAfterWarmUpMillis = 5000L
        Long sleepAfterTestRoundMillis = 1000L
        BuildExperimentListener listener

        Builder displayName(String displayName) {
            this.displayName = displayName
            this
        }

        Builder projectName(String projectName) {
            this.projectName = projectName
            this
        }

        Builder warmUpCount(Integer warmUpCount) {
            this.warmUpCount = warmUpCount
            this
        }

        Builder invocationCount(Integer invocationCount) {
            this.invocationCount = invocationCount
            this
        }

        Builder invocation(@DelegatesTo(GradleInvocationSpec.Builder) Closure<?> conf) {
            invocation.with(conf)
            this
        }

        Builder sleepAfterWarmUpMillis(Long sleepAfterWarmUpMillis) {
            this.sleepAfterWarmUpMillis = sleepAfterWarmUpMillis
            this
        }

        Builder sleepAfterTestRoundMillis(Long sleepAfterTestRoundMillis) {
            this.sleepAfterTestRoundMillis = sleepAfterTestRoundMillis
            this
        }

        Builder listener(BuildExperimentListener listener) {
            this.listener = listener
            this
        }

        BuildExperimentSpec build() {
            assert projectName != null
            assert displayName != null
            assert invocation != null

            new BuildExperimentSpec(displayName, projectName, invocation.buildInfo(displayName, projectName).build(), warmUpCount, invocationCount, sleepAfterWarmUpMillis, sleepAfterTestRoundMillis, listener)
        }
    }
}
