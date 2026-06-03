package com.yupzip.wsdl2java

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

interface Wsdl2JavaPluginExtension {

    @Input
    Property<String> getWsdlDir()

    @Input
    ListProperty<List<String>> getWsdlsToGenerate()

    @Optional
    @Input
    Property<Locale> getLocale()

    @Optional
    @Input
    Property<String> getEncoding()

    @Optional
    @Input
    Property<LineEnding> getLineEnding()

    @Optional
    @Input
    Property<Boolean> getStabilize()

    @Optional
    @Input
    Property<Boolean> getStabilizeAndMergeObjectFactory()

    @Optional
    @Input
    Property<String> getGeneratedWsdlDir()

    @Optional
    @Input
    Property<String> getCxfVersion()

    @Optional
    @Input
    Property<String> getCxfPluginVersion()

    @Optional
    @Input
    Property<String> getCxfToolsVersion()

    @Optional
    @Input
    Property<String> getJaxb2NamespacePrefixVersion()

    @Optional
    @Input
    Property<String> getJaxb2BasicsVersion()

}
