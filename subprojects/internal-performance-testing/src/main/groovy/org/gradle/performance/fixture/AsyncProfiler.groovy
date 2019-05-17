/*
 * Copyright 2019 the original author or authors.
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
import groovy.transform.PackageScope
import org.apache.commons.io.FileUtils
import org.gradle.internal.concurrent.Stoppable

/**
 * Profiles performance test scenarios using async-profiler (https://github.com/jvm-profiling-tools/async-profiler).
 *
 * TODO support pause/resume so we can exclude clean tasks from measurement
 */
@CompileStatic
@PackageScope
class AsyncProfiler extends Profiler implements Stoppable {
    static final String ASYNC_PROFILER_HOME = "ASYNC_PROFILER_HOME";

    private final File logDirectory
    private final PidInstrumentation pid
    private final AsyncProfilerFlameGraphGenerator flameGraphGenerator
    private int sequence

    AsyncProfiler(File targetDir) {
        logDirectory = targetDir
        flameGraphGenerator = new AsyncProfilerFlameGraphGenerator()
        pid = new PidInstrumentation()
    }

    @Override
    List<String> getAdditionalJvmOpts(BuildExperimentSpec spec) {
        def outputFolder = getOutputFolder(spec)
        getJvmOpts(!useDaemon(spec), outputFolder)
    }

    private static List<String> getJvmOpts(boolean startRecordingImmediately, File outputFolder) {
        List<String> opts = ["-agentpath:${getAsyncProfilerHome()}/build/libasyncProfiler.so"] as List<String>
        if (startRecordingImmediately) {
            opts += ["=start,file=${new File(outputFolder, "stacks${sequence++}.txt")},event=cpu,collapsed,ann"] as List<String>
        }
        opts
    }

    private static File getAsyncProfilerHome() {
        String homePath = System.getenv(ASYNC_PROFILER_HOME);
        File profilerHome = homePath != null ? new File(homePath) : null;
        if (profilerHome != null && !profilerHome.isDirectory()) {
            throw new IllegalStateException(ASYNC_PROFILER_HOME + " is not a directory.");
        }
        if (profilerHome == null) {
            profilerHome = AsyncProfilerDownload.defaultHome();
        }
        if (profilerHome == null) {
            throw new IllegalStateException("Async profiler not supported on " + System.getProperty("os.name"));
        }
        return profilerHome;
    }

    @Override
    String getJvmOptsForUseInBuild(String recordingsDirectoryRelativePath) {
        ""
    }

    @Override
    List<String> getAdditionalGradleArgs(BuildExperimentSpec spec) {
        pid.gradleArgs
    }

    private File getOutputFolder(BuildExperimentSpec spec) {
        def fileSafeName = spec.displayName.replaceAll('[^a-zA-Z0-9.-]', '-').replaceAll('-+', '-')
        def baseDir = new File(logDirectory, fileSafeName)
        def outputDir = new File(baseDir, "async-profiler-recordings")
        outputDir.mkdirs()
        return outputDir
    }

    void start(BuildExperimentSpec spec) {
        // Remove any profiles created during warmup
        // TODO Should not run warmup runs with the profiler enabled for no daemon cases – https://github.com/gradle/gradle/issues/9458
        FileUtils.cleanDirectory(getOutputFolder(spec))
        if (useDaemon(spec)) {
            [
                getProfilerScript().getAbsolutePath(),
                "start",
                "-e", "cpu",
                pid
            ].execute().waitForProcessOutput(System.out, System.err)
        }
    }

    private static File getProfilerScript() {
        new File(asyncProfilerHome, "profiler.sh")
    }

    void stop(BuildExperimentSpec spec) {
        def outputFolder = getOutputFolder(spec)
        if (useDaemon(spec)) {
            [
                getProfilerScript().getAbsolutePath(),
                "stop",
                "-o", "collapsed",
                "-a",
                "-f", new File(outputFolder, "stacks.txt").getAbsolutePath(),
                pid
            ].execute().waitForProcessOutput(System.out, System.err)
        }
        flameGraphGenerator.generateGraphs(outputFolder)
    }

    @Override
    void stop() {
        flameGraphGenerator.generateDifferentialGraphs(logDirectory)
    }

    private static boolean useDaemon(BuildExperimentSpec spec) {
        spec.displayInfo.daemon
    }
}
