# wsdl2java Gradle plugin

[![Build](https://github.com/yupzip/wsdl2java/actions/workflows/build.yml/badge.svg)](https://github.com/yupzip/wsdl2java/actions/workflows/build.yml)
[![Coverage Status](https://coveralls.io/repos/github/yupzip/wsdl2java/badge.svg?branch=master)](https://coveralls.io/github/yupzip/wsdl2java?branch=master)

Gradle plugin for generating Java classes from WSDL files, using [Apache CXF](https://cxf.apache.org/) under the hood.

Maintained fork of the deprecated [nilsmagnus/wsdl2java](https://github.com/nilsmagnus/wsdl2java), updated for modern Gradle and current CXF / JAXB versions.

## Requirements

- Gradle 7+ (built and tested against Gradle 9.5.1)
- JDK 17 or newer

## Usage

### Apply the plugin

Groovy:

```groovy
plugins {
    id 'java'
    id 'com.yupzip.wsdl2java' version '4.1.0'
}
```

Kotlin:

```kotlin
plugins {
    id("java")
    id("com.yupzip.wsdl2java") version "4.1.0"
}
```

### Configure

```groovy
wsdl2java {
    wsdlDir = "src/main/resources/wsdl"
    wsdlsToGenerate = [
        ['src/main/resources/wsdl/firstwsdl.wsdl'],
        ['-xjc', '-b', 'bindingfile.xml', 'src/main/resources/wsdl/secondwsdl.wsdl']
    ]
    locale = Locale.GERMANY
}
```

The plugin automatically adds `generatedWsdlDir` (default: `build/generated/wsdl`) to `sourceSets.main.java.srcDirs`, so the generated classes compile without any further setup.

## Tasks

| Name            | Description                                                                                                                  |
|-----------------|------------------------------------------------------------------------------------------------------------------------------|
| `wsdl2javaTask` | Generates Java sources from WSDL files. Wired as a dependency of `compileJava` (and of `compileKotlin` when Kotlin applies). |

## Plugin options

### Generation

| Option                           | Type                 | Default                   | Description                                                                                                                                                                                  |
|----------------------------------|----------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `wsdlDir`                        | `String`             | `src/main/resources/wsdl` | Source directory of WSDL files. Used for the incremental-build input check — changes here invalidate the cached task output.                                                                 |
| `wsdlsToGenerate`                | `List<List<String>>` | _required_                | 2D list of CXF `wsdl-to-java` argument vectors. The **last** element of each inner list must be the WSDL path. See the [CXF reference](http://cxf.apache.org/docs/wsdl-to-java.html).        |
| `generatedWsdlDir`               | `String`             | `build/generated/wsdl`    | Destination directory for generated sources.                                                                                                                                                 |
| `locale`                         | `Locale`             | `Locale.getDefault()`     | Locale used while CXF generates JavaDoc. Set explicitly for reproducible output across machines.                                                                                             |
| `encoding`                       | `String`             | platform default          | Encoding for the generated source files (e.g. `UTF-8`, `EUC-JP`).                                                                                                                            |
| `lineEnding`                     | `LineEnding`         | `PLATFORM_NATIVE`         | Line endings in the generated files. One of `PLATFORM_NATIVE`, `WINDOWS` (`\r\n`), `UNIX` (`\n`), `MAC_CLASSIC` (`\r`).                                                                      |
| `stabilize`                      | `boolean`            | `false`                   | Post-process generated sources to produce diff-friendly output: strip generation timestamps and stable-sort `@XmlSeeAlso`, `@XmlElementRef`, and consecutive `{@link …}` JavaDoc lines.       |
| `stabilizeAndMergeObjectFactory` | `boolean`            | `false`                   | When multiple WSDLs target the same Java package, merge their generated `ObjectFactory` classes into a single, stably ordered file instead of letting the last write win.                    |

### Dependency versions

The plugin resolves CXF tooling against a fixed set of versions by default. Each is overridable via the extension:

| Option                          | Default | Affects                                                                                                       |
|---------------------------------|---------|---------------------------------------------------------------------------------------------------------------|
| `cxfVersion`                    | `4.2.0` | `org.apache.cxf.xjc-utils:cxf-xjc-runtime`                                                                    |
| `cxfPluginVersion`              | `4.2.0` | `org.apache.cxf.xjcplugins:cxf-xjc-ts`, `org.apache.cxf.xjcplugins:cxf-xjc-boolean`                           |
| `cxfToolsVersion`               | `4.2.1` | `org.apache.cxf:cxf-tools-wsdlto-databinding-jaxb`, `org.apache.cxf:cxf-tools-wsdlto-frontend-jaxws`          |
| `jaxb2NamespacePrefixVersion`   | `2.0`   | `org.jvnet.jaxb2_commons:jaxb2-namespace-prefix`                                                              |
| `jaxb2BasicsVersion`            | `3.0.0` | `codes.rafael.jaxb2_commons:jaxb2-basics`, `codes.rafael.jaxb2_commons:jaxb2-basics-runtime`                  |

Example:

```groovy
wsdl2java {
    cxfVersion       = "4.1.2"
    cxfPluginVersion = "4.1.2"
    cxfToolsVersion  = "4.2.0"
    // …rest of config
}
```

## Auto-injected runtime dependencies

So that generated code compiles and runs without extra setup, the plugin adds the following to your `implementation` configuration if they aren't already declared:

- `org.apache.cxf.xjc-utils:cxf-xjc-runtime` (at `cxfVersion`)
- `codes.rafael.jaxb2_commons:jaxb2-basics-runtime` (at `jaxb2BasicsVersion`)

If you already declare these explicitly (any version), the plugin leaves them alone.

## Complete example — Spring Boot 4 with the Jakarta namespace

```groovy
plugins {
    id "java"
    id "org.springframework.boot" version "4.0.6"
    id "io.spring.dependency-management" version "1.7.0"
    id "com.yupzip.wsdl2java" version "4.1.0"
}

bootJar {
    duplicatesStrategy(DuplicatesStrategy.WARN)
}

compileJava {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    options.compilerArgs << '-parameters'
}

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
             '-b', "$projectDir/src/main/resources/wsdl/wsdlBindings.xml",
             '-b', "$projectDir/src/main/resources/wsdl/wsdlTypeDefBindings.xjb",
             '-wsdlLocation', 'classPath:wsdl/myWsdl.wsdl',
             '-p', 'my.package',
             '-autoNameResolution',
             '-verbose',
             "$projectDir/src/main/resources/wsdl/myWsdl.wsdl"
            ],
            ['-xjc',
             '-xjc-Xnamespace-prefix',
             '-b', "$projectDir/src/main/resources/wsdl/wsdlBindings2.xml",
             '-b', "$projectDir/src/main/resources/wsdl/wsdlTypeDefBindings2.xjb",
             '-wsdlLocation', 'classPath:wsdl/myWsdl2.wsdl',
             '-p', 'my.package',
             '-autoNameResolution',
             '-verbose',
             "$projectDir/src/main/resources/wsdl/myWsdl2.wsdl"
            ]
    ]
    generatedWsdlDir = "src/generated-sources/java"
}
```

### Multi-module projects

Prefer `$projectDir` over absolute paths in your build file, as shown above. This keeps the build portable across developer machines and CI agents.

## Changelog

- **4.1.0**
  - Dependency versions are configurable again via extension properties (`cxfVersion`, `cxfPluginVersion`, `cxfToolsVersion`, `jaxb2NamespacePrefixVersion`, `jaxb2BasicsVersion`).
  - Default version bumps: CXF 4.1.2 → 4.2.0, CXF tools 4.2.0 → 4.2.1, CXF xjc plugins 4.1.2 → 4.2.0. Consumers on Jakarta EE 10 stacks can pin the previous versions via the new extension properties.
  - `generatedWsdlDir` is now automatically registered on `sourceSets.main.java.srcDirs` (it doesn't have to be explicitly defined in the project's `build.gradle`).
  - Bug fixes:
    - `wsdlDir` extension setting is now actually honored by the task's up-to-date input check (was silently pinned to the default).
    - Input directory uses `PathSensitivity.RELATIVE`, so the cacheable task can reuse outputs across different checkout paths and CI agents.
    - `stripCommentDates` (under `stabilize`) matches any year (previously only stripped dates starting with `201…`, so the option was a no-op for anything generated from 2020 onwards)
    - `findPackagePaths` no longer throws `IndexOutOfBoundsException` when `-p` is the last argument in a `wsdlsToGenerate` entry.
    - The thread context classloader is restored after task execution, preventing CXF/JAXB classes from being pinned across Gradle daemon invocations.
  - Unit tests covering plugin functionality, plus CI build and Coveralls coverage reporting.
- **4.0.0**
  - Built and tested against Gradle 9.4.1; requires JDK 17+.
  - Default upgrades: CXF 4.1.2, CXF tools 4.2.0, jaxb2-namespace-prefix 2.0, jaxb2-basics 3.0.0.
  - The previous project-level `cxfVersion` / `cxfPluginVersion` properties are removed.
  - `wsdlDir` and `generatedWsdlDir` are now plain `String` properties (previously `File`).
  - The `includeJava8XmlDependencies` extension property is removed. It used to gate the auto-injection of a `javax.*` XML dependencies on Java 9+. With the move to the Jakarta namespace it is no longer applicable (consumers transitioning from Java EE to Jakarta EE who previously set this to `false` can simply remove the line).
  - Inclusion of generated classes in source sets must now be configured in `build.gradle`:
    ```groovy
    sourceSets.main.java.srcDirs "src/generated-sources/java"
    ```
- **3.0.1** — adaptation for Gradle 9.
- **3.0.0** — breaking change: `cxfVersion` and `cxfPluginVersion` introduced as required project properties.
- Forked from the deprecated [nilsmagnus/wsdl2java](https://github.com/nilsmagnus/wsdl2java) to keep the plugin compatible with Gradle 7+.

## Issues

Please file an issue at https://github.com/yupzip/wsdl2java/issues.

## Contributing

Contributions are welcome.

### Contributors

- Peter Vermes — https://github.com/yupzip
- Nicklas Bondesson — https://github.com/nicklasbondesson
- https://github.com/cstsw
