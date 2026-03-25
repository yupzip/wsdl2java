### Note

* Version 4.0.0
  * Plugin built with Gradle 9.4.1
  * JDK 17+ required
  * CXF Tool version 4.1.2 (cxfVersion property removed)
  * CXF Plugin version 4.1.2 (cxfPlugin property removed)
  * CXF Tools version 4.2.0
  * JAXB2 naming prefix version 2.0 (org.jvnet.jaxb2_commons:jaxb2-namespace-prefix)
  * JAXB2 basic plugins version 3.0.0 (org.jvnet.jaxb2_commons:jaxb2-namespace-prefix)
  * wsdlDir property is now a string
  * generatedWsdlDir property is now a string
  * inclusion of generated classes in source to be configured in build.gradle, e.g.:
    ```groovy
    sourceSets.main.java.srcDirs "src/generated-sources/java"
    ```
* Version 3.0.1 - adaption for Gradle 9
* Version 3.0.0 contains a breaking change: 'cxfVersion' and 'cxfPluginVersion' properties are now required.
* This plugin is forked from deprecated nilsmagnus/wsdl2java to make the plugin compatible with Gradle 7+. 

wsdl2java gradle plugin
=========
Gradle plugin for generating java classes from wsdl using CXF under the hood.

### Issues
If you have any issues with the plugin, please file an issue at github, https://github.com/yupzip/wsdl2java/issues

### Contribution
Contributions are welcome.

#### Contributors
- Peter Vermes , https://github.com/yupzip
- Nicklas Bondesson , https://github.com/nicklasbondesson

### CXF
This plugin uses the apache-cxf tools to do the actual work.

### Tasks

| Name | Description | Dependecy |
| ---- | ----------- | --------- |
| wsdl2javaTask | Generate java source from wsdl-files | CompileJava/CompileKotlin depends on wsdl2java |

## Usage

To use this plugin, you must
- apply the plugin
- set the properties of the plugin

### Applying the plugin

Groovy:

```groovy
plugins {
    id 'java'
    id 'com.yupzip.wsdl2java' version '4.0.0'
}
```

Kotlin:

```kotlin
plugins {
    id("java")
    id("com.yupzip.wsdl2java") version "4.0.0"
}
```

### Plugin options

| Option                         | Default value             | Description                                                                                                                                                                                                                                                                                                                                                  |
|--------------------------------|---------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| wsdlDir                        | src/main/resources        | Define the wsdl files directory to support incremental build. This means that the task will be up-to-date if nothing in this directory has changed.                                                                                                                                                                                                          |
| wsdlsToGenerate                | empty                     | This is the main input to the plugin that defines the wsdls to process. It is a list of arguments where each argument is a list of arguments to process a wsdl-file. The Wsdl-file with full path is the last argument. The array can be supplied with the same options as described for the maven-cxf plugin(http://cxf.apache.org/docs/wsdl-to-java.html). |
| generatedWsdlDir               | build/generated/wsdl      | Destination directory for generated sources. The task will be up-to-date if nothing in this directory changes between builds.                                                                                                                                                                                                                                |
| locale                         | Locale.getDefault()       | The locale for the generated sources – especially the JavaDoc. This might be necessary to prevent differing sources due to several development environments.                                                                                                                                                                                                 |
| encoding                       | platform default encoding | Set the encoding name for generated sources, such as EUC-JP or UTF-8.                                                                                                                                                                                                                                                                                        |
| stabilizeAndMergeObjectFactory | false                     | If multiple WSDLs target the same package, merge their `ObjectFactory` classes.                                                                                                                                                                                                                                                                              |

Example setting of options:

Groovy:

```groovy
wsdl2java {
    wsdlDir = "src/main/resources/myWsdlFiles"
    wsdlsToGenerate = [   //  2d-array of wsdls and cxf-parameters
            ['src/main/resources/wsdl/firstwsdl.wsdl'],
            ['-xjc','-b','bindingfile.xml','src/main/resources/wsdl/secondwsdl.wsdl']
    ]
    locale = Locale.GERMANY
}
```
    
Kotlin:

```kotlin
wsdl2java {
    wsdlDir = "$projectDir/src/main/wsdl"
    wsdlsToGenerate = listOf(
        listOf("$wsdlDir/firstwsdl.wsdl"),
        listOf("-xjc", "-b", "bindingfile.xml", "$wsdlDir/secondwsdl.wsdl")
    )
}
```

## Example gradle configuration for Spring Boot 4+ with jakarta namespace
```groovy
plugins {
    id "java"
    id "org.springframework.boot" version "4.0.4"
    id "io.spring.dependency-management" version "1.7.0"
    id "com.yupzip.wsdl2java" version "4.0.0"
}

bootJar {
    duplicatesStrategy(DuplicatesStrategy.WARN)
}

compileJava {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    options.compilerArgs << '-parameters'
}

sourceSets.main.java.srcDirs "src/generated-sources/java"

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-web-services'
    implementation 'org.springframework.ws:spring-ws-support:4.0.0'
    // your project dependencies

    implementation 'com.sun.xml.bind:jaxb-impl:4.0.6'
    implementation 'com.sun.xml.messaging.saaj:saaj-impl:3.0.4'
    implementation 'com.sun.xml.ws:jaxws-ri:4.0.3'
    
    implementation 'io.swagger.core.v3:swagger-jaxrs2-jakarta:2.2.7'
    
    implementation 'jakarta.xml.bind:jakarta.xml.bind-api:4.0.5'
    implementation 'jakarta.xml.soap:jakarta.xml.soap-api:3.0.2'
    implementation 'jakarta.xml.ws:jakarta.xml.ws-api:4.0.3'
    
    implementation 'org.glassfish.jaxb:jaxb-runtime:4.0.6'
}

configurations {
    wsdl2java
}

wsdl2java {
    wsdlDir = "$projectDir/src/main/resources/wsdl/"
    stabilizeAndMergeObjectFactory = true
    wsdlsToGenerate = [
            ['-xjc',
             '-xjc-Xnamespace-prefix',
             '-b',"$projectDir/src/main/resources/wsdl/wsdlBindings.xml",
             '-b',"$projectDir/src/main/resources/wsdl/wsdlTypeDefBindings.xjb",
             '-wsdlLocation', 'classPath:wsdl/myWsdl.wsdl',
             '-p', 'my.package',
             '-autoNameResolution',
             '-verbose',
             "$projectDir/src/main/resources/wsdl/myWsdl.wsdl"
            ],
            ['-xjc',
             '-xjc-Xnamespace-prefix',
             '-b',"$projectDir/src/main/resources/wsdl/wsdlBindings2.xml",
             '-b',"$projectDir/src/main/resources/wsdl/wsdlTypeDefBindings2.xjb",
             '-wsdlLocation', 'classPath:wsdl/myWsdl2.wsdl',
             '-p', 'my.package',
             '-autoNameResolution',
             '-verbose',
             "$projectDir/src/main/resources/wsdl/myWsdl2.wsdl"]
    ]
    generatedWsdlDir = "src/generated-sources/java"
}
```

### A notice on multi-module projects

Instead of referring to absolute paths in your build-file, try using $projectDir as a prefix to your files and directories. As shown in the "Complete example usage".
