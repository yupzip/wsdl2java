package com.yupzip.wsdl2java


import org.gradle.api.Plugin
import org.gradle.api.Project

class Wsdl2JavaPlugin implements Plugin<Project> {

    public static final String IMPLEMENTATION = "implementation"
    public static final String WSDL2JAVA = "wsdl2java"
    public static final String WSDL2JAVA_TASK = "wsdl2javaTask"

    public static final String CXF_VERSION = "4.1.2"
    public static final String CXF_PLUGIN_VERSION = "4.1.2"
    public static final String CXF_TOOLS_VERSION = "4.1.2"
    public static final String JAXB2_NAMESPACE_PREFIX_VERSION = "2.0"
    public static final String JAXB2_BASICS_VERSION = "3.0.0"

    void apply(Project project) {
        project.apply(plugin: "java")
        def extension = project.extensions.create(WSDL2JAVA, Wsdl2JavaPluginExtension.class)
        // Add new configuration for our plugin and add required dependencies to it.
        def wsdl2javaConfiguration = project.configurations.maybeCreate(WSDL2JAVA)
        wsdl2javaConfiguration.withDependencies {
            it.add(project.dependencies.create("org.apache.cxf.xjc-utils:cxf-xjc-runtime:${CXF_VERSION}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:${CXF_TOOLS_VERSION}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws:${CXF_TOOLS_VERSION}"))
            it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-ts:${CXF_PLUGIN_VERSION}"))
            it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-boolean:${CXF_PLUGIN_VERSION}"))
            it.add(project.dependencies.create("org.jvnet.jaxb2_commons:jaxb2-namespace-prefix:${JAXB2_NAMESPACE_PREFIX_VERSION}"))
            it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics:${JAXB2_BASICS_VERSION}"))
            it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${JAXB2_BASICS_VERSION}"))
        }

        def implementationConfig = project.configurations.named(IMPLEMENTATION).getOrNull()
        if (implementationConfig != null) {
            if (!implementationConfig.allDependencies.any { dep -> dep.name == 'cxf-xjc-runtime'}) {
                project.dependencies.add(IMPLEMENTATION, "org.apache.cxf.xjc-utils:cxf-xjc-runtime:${CXF_VERSION}")
            }
            if (!implementationConfig.allDependencies.any { dep -> dep.name == 'jaxb2-basics-runtime'}) {
                project.dependencies.add(IMPLEMENTATION, "codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${JAXB2_BASICS_VERSION}")
            }
        }

        def wsdl2JavaTask = project.tasks.register(WSDL2JAVA_TASK, Wsdl2JavaTask.class) { task ->
            task.group = "Wsdl2Java"
            task.description = "Generate java source code from WSDL files."
            task.classpath = wsdl2javaConfiguration
            task.extension = extension
        }

        project.tasks.named("compileJava").configure {
            it.dependsOn wsdl2JavaTask
        }

        if (project.tasks.findByName("compileKotlin") != null) {
            project.tasks.named("compileKotlin").configure {
                it.dependsOn wsdl2JavaTask
            }
        }
    }
}
