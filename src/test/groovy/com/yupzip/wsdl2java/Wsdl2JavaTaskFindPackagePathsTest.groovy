package com.yupzip.wsdl2java

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class Wsdl2JavaTaskFindPackagePathsTest {

    private static Wsdl2JavaTask taskWithWsdls(List<List<String>> wsdlsToGenerate) {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply('com.yupzip.wsdl2java')
        def extension = project.extensions.getByType(Wsdl2JavaPluginExtension)
        extension.wsdlsToGenerate.set(wsdlsToGenerate)
        return (Wsdl2JavaTask) project.tasks.named(Wsdl2JavaPlugin.WSDL2JAVA_TASK).get()
    }

    @Test
    void findPackagePaths_extractsPackageFromMinusP() {
        def task = taskWithWsdls([
                ['-xjc', '-p', 'com.example.foo', 'src/main/resources/wsdl/foo.wsdl']
        ])

        assertEquals(['com/example/foo'] as Set, task.findPackagePaths())
    }

    @Test
    void findPackagePaths_handlesMultipleWsdls() {
        def task = taskWithWsdls([
                ['-xjc', '-p', 'com.example.foo', 'a.wsdl'],
                ['-p', 'com.example.bar', 'b.wsdl']
        ])

        assertEquals(['com/example/foo', 'com/example/bar'] as Set, task.findPackagePaths())
    }

    @Test
    void findPackagePaths_handlesNamespaceEqualsPackageMapping() {
        def task = taskWithWsdls([
                ['-xjc', '-p', 'http://example.com/ns=com.example.foo', 'foo.wsdl']
        ])

        assertEquals(['com/example/foo'] as Set, task.findPackagePaths())
    }

    @Test
    void findPackagePaths_returnsEmptyWhenNoMinusPGiven() {
        def task = taskWithWsdls([
                ['src/main/resources/wsdl/foo.wsdl']
        ])

        assertTrue(task.findPackagePaths().isEmpty())
    }

    @Test
    void findPackagePaths_doesNotThrowWhenMinusPIsLastArg() {
        // Regression for the off-by-one bug: previously `args.size() >= packageIx`
        // permitted args.get(packageIx) when packageIx == args.size(), raising IOOBE.
        def task = taskWithWsdls([
                ['foo.wsdl', '-p']
        ])

        // Must not throw; with no value after -p we expect an empty result.
        assertTrue(task.findPackagePaths().isEmpty())
    }
}
