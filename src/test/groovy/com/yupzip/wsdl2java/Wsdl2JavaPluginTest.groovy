package com.yupzip.wsdl2java

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertTrue

class Wsdl2JavaPluginTest {

    private static Project appliedProject() {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply('com.yupzip.wsdl2java')
        return project
    }

    /**
     * `withDependencies { … }` callbacks fire lazily when the configuration is
     * resolved. ProjectBuilder never resolves, so trigger them by hand.
     */
    private static void realizeDependencies(Configuration config) {
        ((ConfigurationInternal) config).runDependencyActions()
    }

    @Test
    void apply_appliesJavaPlugin() {
        Project project = appliedProject()
        assertTrue(project.plugins.hasPlugin('java'))
    }

    @Test
    void apply_registersWsdl2JavaTask() {
        Project project = appliedProject()
        assertTrue(project.tasks.names.contains(Wsdl2JavaPlugin.WSDL2JAVA_TASK))
    }

    @Test
    void apply_createsWsdl2JavaConfiguration() {
        Project project = appliedProject()
        assertTrue(project.configurations.names.contains(Wsdl2JavaPlugin.WSDL2JAVA))
    }

    @Test
    void apply_extensionConventionsMatchPluginConstants() {
        Project project = appliedProject()
        def extension = project.extensions.getByType(Wsdl2JavaPluginExtension)

        assertEquals(Wsdl2JavaPlugin.CXF_VERSION, extension.cxfVersion.get())
        assertEquals(Wsdl2JavaPlugin.CXF_PLUGIN_VERSION, extension.cxfPluginVersion.get())
        assertEquals(Wsdl2JavaPlugin.CXF_TOOLS_VERSION, extension.cxfToolsVersion.get())
        assertEquals(Wsdl2JavaPlugin.JAXB2_NAMESPACE_PREFIX_VERSION, extension.jaxb2NamespacePrefixVersion.get())
        assertEquals(Wsdl2JavaPlugin.JAXB2_BASICS_VERSION, extension.jaxb2BasicsVersion.get())
    }

    @Test
    void apply_wsdl2JavaConfigurationUsesDefaultVersions() {
        Project project = appliedProject()
        def config = project.configurations.named(Wsdl2JavaPlugin.WSDL2JAVA).get()
        realizeDependencies(config)

        def cxfRuntime = config.allDependencies.find { it.name == 'cxf-xjc-runtime' }
        def cxfTools = config.allDependencies.find { it.name == 'cxf-tools-wsdlto-databinding-jaxb' }
        def jaxb2Basics = config.allDependencies.find { it.name == 'jaxb2-basics' }

        assertNotNull(cxfRuntime)
        assertEquals(Wsdl2JavaPlugin.CXF_VERSION, cxfRuntime.version)
        assertNotNull(cxfTools)
        assertEquals(Wsdl2JavaPlugin.CXF_TOOLS_VERSION, cxfTools.version)
        assertNotNull(jaxb2Basics)
        assertEquals(Wsdl2JavaPlugin.JAXB2_BASICS_VERSION, jaxb2Basics.version)
    }

    @Test
    void apply_extensionVersionOverridePropagatesToConfiguration() {
        Project project = appliedProject()
        def extension = project.extensions.getByType(Wsdl2JavaPluginExtension)
        extension.cxfToolsVersion.set("9.9.9")
        extension.jaxb2BasicsVersion.set("8.8.8")

        def config = project.configurations.named(Wsdl2JavaPlugin.WSDL2JAVA).get()
        realizeDependencies(config)
        def cxfTools = config.allDependencies.find { it.name == 'cxf-tools-wsdlto-databinding-jaxb' }
        def jaxb2BasicsRuntime = config.allDependencies.find { it.name == 'jaxb2-basics-runtime' }

        assertEquals("9.9.9", cxfTools.version)
        assertEquals("8.8.8", jaxb2BasicsRuntime.version)
    }

    @Test
    void apply_autoInjectsCxfAndJaxb2RuntimeIntoImplementation() {
        Project project = appliedProject()
        def impl = project.configurations.named('implementation').get()
        realizeDependencies(impl)

        def cxfRuntime = impl.allDependencies.find { it.name == 'cxf-xjc-runtime' }
        def jaxb2BasicsRuntime = impl.allDependencies.find { it.name == 'jaxb2-basics-runtime' }

        assertNotNull(cxfRuntime, "cxf-xjc-runtime should be auto-injected into implementation")
        assertEquals(Wsdl2JavaPlugin.CXF_VERSION, cxfRuntime.version)
        assertNotNull(jaxb2BasicsRuntime, "jaxb2-basics-runtime should be auto-injected into implementation")
        assertEquals(Wsdl2JavaPlugin.JAXB2_BASICS_VERSION, jaxb2BasicsRuntime.version)
    }

    @Test
    void apply_doesNotDuplicateImplementationDependencyAlreadyDeclared() {
        Project project = ProjectBuilder.builder().build()
        project.plugins.apply('java')
        project.dependencies.add('implementation', 'org.apache.cxf.xjc-utils:cxf-xjc-runtime:1.2.3')
        project.plugins.apply('com.yupzip.wsdl2java')

        def impl = project.configurations.named('implementation').get()
        realizeDependencies(impl)
        def matches = impl.allDependencies.findAll { it.name == 'cxf-xjc-runtime' }

        assertEquals(1, matches.size(), "user-declared cxf-xjc-runtime must not be duplicated")
        assertEquals("1.2.3", matches[0].version)
    }
}
