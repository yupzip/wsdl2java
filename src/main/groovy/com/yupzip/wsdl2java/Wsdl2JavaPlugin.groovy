package com.yupzip.wsdl2java


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

class Wsdl2JavaPlugin implements Plugin<Project> {

    public static final String IMPLEMENTATION = "implementation"
    public static final String WSDL2JAVA = "wsdl2java"
    public static final String WSDL2JAVA_TASK = "wsdl2javaTask"

    public static final String CXF_VERSION = "4.2.0"
    public static final String CXF_PLUGIN_VERSION = "4.2.0"
    public static final String CXF_TOOLS_VERSION = "4.2.1"
    public static final String JAXB2_NAMESPACE_PREFIX_VERSION = "2.0"
    public static final String JAXB2_BASICS_VERSION = "3.0.0"

    void apply(Project project) {
        project.apply(plugin: "java")
        def extension = project.extensions.create(WSDL2JAVA, Wsdl2JavaPluginExtension.class)
        extension.cxfVersion.convention(CXF_VERSION)
        extension.cxfPluginVersion.convention(CXF_PLUGIN_VERSION)
        extension.cxfToolsVersion.convention(CXF_TOOLS_VERSION)
        extension.jaxb2NamespacePrefixVersion.convention(JAXB2_NAMESPACE_PREFIX_VERSION)
        extension.jaxb2BasicsVersion.convention(JAXB2_BASICS_VERSION)
        extension.generatedWsdlDir.convention(Wsdl2JavaTask.DEFAULT_GENERATED_WSDL_DIR)

        // Add new configuration for our plugin and add required dependencies to it.
        def wsdl2javaConfiguration = project.configurations.maybeCreate(WSDL2JAVA)
        wsdl2javaConfiguration.withDependencies {
            String cxfVer = extension.cxfVersion.get()
            String cxfPluginVer = extension.cxfPluginVersion.get()
            String cxfToolsVer = extension.cxfToolsVersion.get()
            String jaxb2NsPrefixVer = extension.jaxb2NamespacePrefixVersion.get()
            String jaxb2BasicsVer = extension.jaxb2BasicsVersion.get()
            it.add(project.dependencies.create("org.apache.cxf.xjc-utils:cxf-xjc-runtime:${cxfVer}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:${cxfToolsVer}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws:${cxfToolsVer}"))
            it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-ts:${cxfPluginVer}"))
            it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-boolean:${cxfPluginVer}"))
            it.add(project.dependencies.create("org.jvnet.jaxb2_commons:jaxb2-namespace-prefix:${jaxb2NsPrefixVer}"))
            it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics:${jaxb2BasicsVer}"))
            it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${jaxb2BasicsVer}"))
        }

        project.configurations.named(IMPLEMENTATION).configure { implementationConfig ->
            implementationConfig.withDependencies { deps ->
                if (!deps.any { dep -> dep.name == 'cxf-xjc-runtime' }) {
                    project.dependencies.add(IMPLEMENTATION, "org.apache.cxf.xjc-utils:cxf-xjc-runtime:${extension.cxfVersion.get()}")
                }
                if (!deps.any { dep -> dep.name == 'jaxb2-basics-runtime' }) {
                    project.dependencies.add(IMPLEMENTATION, "codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${extension.jaxb2BasicsVersion.get()}")
                }
            }
        }

        def wsdl2JavaTask = project.tasks.register(WSDL2JAVA_TASK, Wsdl2JavaTask.class) { task ->
            task.group = "Wsdl2Java"
            task.description = "Generate java source code from WSDL files."
            task.classpath = wsdl2javaConfiguration
            task.extension = extension
        }

        // Register the generated directory as a Java source directory so consumers
        // don't have to add `sourceSets.main.java.srcDirs "..."` manually. Resolved
        // lazily — `generatedWsdlDir` may be set by the user after plugin apply.
        def mainSourceSet = project.extensions.getByType(SourceSetContainer).named("main").get()
        mainSourceSet.java.srcDir(project.provider {
            extension.generatedWsdlDir.getOrElse(Wsdl2JavaTask.DEFAULT_GENERATED_WSDL_DIR)
        })

        project.tasks.named("compileJava").configure {
            it.dependsOn wsdl2JavaTask
        }

        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            project.tasks.named("compileKotlin").configure {
                it.dependsOn wsdl2JavaTask
            }
        }
    }
}
