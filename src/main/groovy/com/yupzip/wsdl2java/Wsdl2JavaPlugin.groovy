package com.yupzip.wsdl2java


import org.gradle.api.Plugin
import org.gradle.api.Project

class Wsdl2JavaPlugin implements Plugin<Project> {
    public static final String WSDL2JAVA = "wsdl2java"
    public static final String WSDL2JAVA_TASK = "wsdl2javaTask"

    void apply(Project project) {
        project.apply(plugin: "java")

        def extension = project.extensions.create(WSDL2JAVA, Wsdl2JavaPluginExtension.class)
        def cxfVersion = project.provider { extension.cxfVersion }
        def cxfPluginVersion = project.provider { extension.cxfPluginVersion }
        def cxfToolsVersion = project.provider { extension.cxfToolsVersion }
        def namespacePrefixVersion = project.provider { extension.namespacePrefixVersion }
        def jax2bBasicsVersion = project.provider { extension.jax2bBasicsVersion }

        // Add new configuration for our plugin and add required dependencies to it.
        def wsdl2javaConfiguration = project.configurations.maybeCreate(WSDL2JAVA)
        wsdl2javaConfiguration.withDependencies {
            it.add(project.dependencies.create("org.apache.cxf.xjc-utils:cxf-xjc-runtime:${cxfVersion.get()}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb:${cxfToolsVersion.get()}"))
            it.add(project.dependencies.create("org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws:${cxfToolsVersion.get()}"))
            if (project.wsdl2java.wsdlsToGenerate.any { it.contains('-xjc-Xts') }) {
                it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-ts:${cxfPluginVersion.get()}"))
            }
            if (project.wsdl2java.wsdlsToGenerate.any { it.contains('-xjc-Xbg') }) {
                it.add(project.dependencies.create("org.apache.cxf.xjcplugins:cxf-xjc-boolean:${cxfPluginVersion.get()}"))
            }
            if (namespacePrefixVersion.getOrNull() != null) {
                it.add(project.dependencies.create("org.jvnet.jaxb2_commons:jaxb2-namespace-prefix:${namespacePrefixVersion.get()}"))
            }
            if (jax2bBasicsVersion.getOrNull() != null) {
                it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics:${jax2bBasicsVersion.get()}"))
                it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${jax2bBasicsVersion.get()}"))
            }
        }
        project.configurations.named("implementation").any { config ->
            def implementationConfig = config.getOrNull()
            if (implementationConfig != null) {
                if (!implementationConfig.dependencies.any { dep -> dep.name == 'cxf-xjc-runtime'}) {
                    implementationConfig.withDependencies {
                        it.add(project.dependencies.create("org.apache.cxf.xjc-utils:cxf-xjc-runtime:${cxfVersion.get()}"))
                    }
                }
                if (jax2bBasicsVersion.getOrNull() != null && !implementationConfig.dependencies.any { dep -> dep.name == 'jaxb2-basics-runtime'}) {
                    implementationConfig.withDependencies {
                        it.add(project.dependencies.create("codes.rafael.jaxb2_commons:jaxb2-basics-runtime:${jax2bBasicsVersion.get()}"))
                    }
                }
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

        project.sourceSets {
            main.java.srcDirs += extension.generatedWsdlDir
        }
    }
}
