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

package org.gradle.api.internal.changedetection.state


import org.gradle.internal.hash.HashCode
import org.gradle.internal.snapshot.RegularFileSnapshot
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.testing.internal.util.Specification
import org.junit.Rule

class ZipHasherTest extends Specification {

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()

    ZipHasher zipHasher = new ZipHasher(new RuntimeClasspathResourceHasher(), { false } as ResourceFilter)

    def "adding an empty jar inside another jar changes the hashcode"() {
        given:
        def outer = tmpDir.testDirectory.createZip("outer.jar")
        def originalHash = zipHasher.hash(snapshot(outer))

        when:
        def inner = tmpDir.testDirectory.createZip("inner.jar")
        inner.zipTo(outer)
        def newHash = zipHasher.hash(snapshot(outer))

        then:
        originalHash != newHash
    }

    def "relative path of nested zip entries is tracked"() {
        given:
        def outer = tmpDir.testDirectory.createZip("outer.jar")
        def originalHash = zipHasher.hash(snapshot(outer))
        def inner = tmpDir.createDir("inner")
        def foo = inner.createFile("foo") << "Foo"
        def bar = inner.createFile("bar") << "Bar"
        inner.zipTo(outer)

        when:
        foo.text = "Bar"
        bar.text = "Foo"
        def newHash = zipHasher.hash(snapshot(outer))

        then:
        originalHash != newHash
    }

    private static RegularFileSnapshot snapshot(TestFile file) {
        new RegularFileSnapshot(file.path, file.name, HashCode.fromInt(0), 0)
    }
}
