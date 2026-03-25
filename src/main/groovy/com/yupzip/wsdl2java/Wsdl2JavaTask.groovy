package com.yupzip.wsdl2java

import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.*

import java.nio.charset.Charset
import java.security.MessageDigest

@CacheableTask
class Wsdl2JavaTask extends DefaultTask {

    static final DEFAULT_WSDL_DIR = "src/main/resources/wsdl"
    static final DEFAULT_GENERATED_WSDL_DIR = "build/generated/wsdl"

    @InputFiles
    @Classpath
    Configuration classpath

    @Internal
    ClassLoader classLoader

    @Nested
    Wsdl2JavaPluginExtension extension

    @InputDirectory
    @PathSensitive(PathSensitivity.ABSOLUTE)
    File wsdlDir = new File(extension != null ? extension.wsdlDir.getOrElse(DEFAULT_WSDL_DIR) : DEFAULT_WSDL_DIR)

    @Internal
    File generatedWsdlDir

    @OutputDirectory
    File getGeneratedWsdlFile() {
        return new File(extension != null ? extension.generatedWsdlDir.getOrElse(DEFAULT_GENERATED_WSDL_DIR) : DEFAULT_GENERATED_WSDL_DIR)
    }

    @TaskAction
    def wsdl2java() {
        generatedWsdlDir = getGeneratedWsdlFile()
        deleteOutputFolders()
        MessageDigest md5 = MessageDigest.getInstance("MD5")

        File tmpDir = new File(project.layout.buildDirectory.get().asFile, "wsdl2java")
        tmpDir.deleteDir()

        if (classpath == null) {
            classpath = project.configurations.named(Wsdl2JavaPlugin.WSDL2JAVA).get()
        }
        setupClassLoader()
        assert classLoader != null
        extension.wsdlsToGenerate.get().each { args ->
            // Defensively copy the input args, because this might be a immutable implementation.
            def argsCopy = args.collect() as List<Object>

            String wsdlPath = md5.digest(argsCopy[-1].toString().bytes).encodeHex().toString()
            File targetDir = new File(tmpDir, wsdlPath)

            argsCopy.add(argsCopy.size() - 1, '-d')
            argsCopy.add(argsCopy.size() - 1, targetDir.getAbsolutePath())
            String[] wsdl2JavaArgs = new String[argsCopy.size()]
            for (int i = 0; i < argsCopy.size(); i++) {
                wsdl2JavaArgs[i] = argsCopy[i]
            }

            def wsdlToJava = classLoader.loadClass("org.apache.cxf.tools.wsdlto.WSDLToJava").getConstructor().newInstance()
            def toolContext = classLoader.loadClass("org.apache.cxf.tools.common.ToolContext").getConstructor().newInstance()
            wsdlToJava.args = wsdl2JavaArgs
            runWithLocale(extension.locale.getOrElse(Locale.getDefault())) { ->
                try {
                    wsdlToJava.run(toolContext)
                } catch (Exception e) {
                    throw new TaskExecutionException(this, e)
                }
            }
            copyToOutputDir(targetDir)
        }
    }

    protected void setupClassLoader() {
        if (classpath?.files) {
            def urls = classpath.files.collect { it.toURI().toURL() }
            classLoader = new URLClassLoader(urls as URL[], Thread.currentThread().contextClassLoader)
            Thread.currentThread().contextClassLoader = classLoader
        } else {
            classLoader = Thread.currentThread().contextClassLoader
        }
    }

    protected static void runWithLocale(Locale locale, Closure<Void> closure) {
        // save the current default locale – will be set back at the end
        Locale currentDefaultLocale = Locale.getDefault()
        try {
            // set the wanted locale for the generated java classes
            Locale.setDefault(locale)
            closure()
        }
        finally {
            // set the default locale back to the previous default
            Locale.setDefault(currentDefaultLocale)
        }
    }

    protected void deleteOutputFolders() {
        Set<String> packagePaths = findPackagePaths()
        if (packagePaths.isEmpty()) {
            packagePaths.add("") // add root if no package paths
        }
        Set<File> packageTargetDirs = packagePaths.collect { subPath -> new File(generatedWsdlDir, subPath) }
        getLogger().info("Clear target folders {}", packageTargetDirs)
        getProject().delete(packageTargetDirs)
    }

    protected Set<String> findPackagePaths() {
        Set<String> packagePaths = new HashSet<>()
        for (List<String> args : extension.wsdlsToGenerate.get()) {
            int packageArgIdx = args.indexOf("-p")
            int packageIx = packageArgIdx + 1
            if (packageArgIdx != -1 && args.size() >= packageIx) {
                //check if it's wsdl-namespace=package
                String[] maybeWsdlNameSpaceAndPackage = args.get(packageIx).split("=")
                String packageName = maybeWsdlNameSpaceAndPackage.size() == 1 ? maybeWsdlNameSpaceAndPackage[0] : maybeWsdlNameSpaceAndPackage[1]
                String pathPath = packageName.replace(".", "/")
                packagePaths.add(pathPath)
            }
        }
        return packagePaths
    }

    protected void copyToOutputDir(File srcDir) {
        int srcPathLength = srcDir.getAbsolutePath().size() + 1

        srcDir.eachFileRecurse(FileType.FILES) { file ->
            String relPath = file.getAbsolutePath().substring(srcPathLength)
            File target = new File(generatedWsdlDir, relPath)
            switchToEncoding(file)
            if (extension.stabilizeAndMergeObjectFactory.getOrElse(false)) {
                mergeAndStabilizeObjectFactory(file, target)
            } else {
                project.ant.copy(file: file, tofile: target)
            }
        }
    }

    protected void switchToEncoding(File file) {
        String lineEnding = extension.lineEnding.getOrElse(LineEnding.PLATFORM_NATIVE).value
        List<String> lines = file.getText().split(lineEnding)
        file.delete()
        if (extension.stabilize.getOrElse(false)) {
            stripCommentDates(lines)
            stabilizeCommentLinks(lines)
            stabilizeXmlElementRef(lines)
            stabilizeXmlSeeAlso(lines)
        }
        String text = lines.join(lineEnding) + lineEnding  // want empty line last
        file.withWriter(extension.encoding.getOrElse(Charset.defaultCharset().name())) { w -> w.write(text) }
    }

    static void stripCommentDates(List<String> lines) {
        String prevLine = ""
        for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
            String l = lix.next()
            if (prevLine.contains("This class was generated") && l.startsWith(" * 201")) {
                lix.remove()
            }
            prevLine = l
        }
    }

    static void stabilizeCommentLinks(List<String> lines) {
        for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
            String l = lix.next()
            if (l.contains("* {@link")) {
                int start = lix.previousIndex()
                while (lix.hasNext()) {
                    l = lix.next()
                    if (!l.contains("* {@link")) {
                        int end = lix.previousIndex()
                        List<String> subList = lines.subList(start, end)
                        Collections.sort(subList)
                        break
                    }
                }
            }
        }
    }

    static void stabilizeXmlSeeAlso(List<String> lines) {
        String seeAlsoStart = "@XmlSeeAlso({"
        String seeAlsoEnd = "})"
        for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
            String l = lix.next()

            if (l.startsWith(seeAlsoStart) && l.endsWith(seeAlsoEnd)) {
                List<String> classes = l.replace(seeAlsoStart, "").replace(seeAlsoEnd, "").split(",").collect { it.trim() }
                String sortedClasses = seeAlsoStart + classes.sort().join(", ") + seeAlsoEnd
                lix.set(sortedClasses)
            }
        }
    }

    static void stabilizeXmlElementRef(List<String> lines) {
        String prevLine = ""
        for (ListIterator<String> lix = lines.listIterator(); lix.hasNext();) {
            String l = lix.next()
            if (l.contains("@XmlElementRef") && prevLine.contains("@XmlElementRefs")) {
                int start = lix.previousIndex()
                while (lix.hasNext()) {
                    l = lix.next()
                    if (!l.contains("@XmlElementRef")) {
                        int end = lix.previousIndex()
                        List<String> subList = lines.subList(start, end)
                        Collections.sort(subList)
                        // Fix ,-separation of lines
                        for (ListIterator<String> subLix = subList.listIterator(); subLix.hasNext();) {
                            String line = subLix.next()
                            line = line.replaceFirst(',$', "")
                            if (subLix.hasNext()) {
                                line = line + ","
                            }
                            subLix.set(line)
                        }
                        break
                    }
                }
            }
            prevLine = l
        }
    }

    protected void mergeAndStabilizeObjectFactory(File src, File target) {
        if (!target.exists()) {
            target.getParentFile().mkdirs()
            project.ant.copy(file: src, tofile: target)
            stabilizeObjFacWithItself(target)
        } else {
            stabilizeObjFacWithTarget(src, target)
        }
    }

    protected void stabilizeObjFacWithItself(File target) {
        if (isObjectFactory(target)) {
            getLogger().info(" stabilize ${target}")
            ObjectFactoryMerger.merge(target, target, extension.encoding.getOrElse(Charset.defaultCharset().name()))
        }
    }

    protected stabilizeObjFacWithTarget(File src, File target) {
        def encoding = extension.encoding.getOrElse(Charset.defaultCharset().name())
        if (isObjectFactory(src) && src.getText(encoding) != target.getText(encoding)) {
            getLogger().info(" merge     ${target}")
            ObjectFactoryMerger.merge(src, target, encoding)
        }
    }

    protected static boolean isObjectFactory(File f) {
        return "ObjectFactory.java".equals(f.getName())
    }
}
