package com.yupzip.wsdl2java

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class ObjectFactoryMergerTest {

    private static final String NL = System.lineSeparator()

    private static String objectFactorySource(String qnameField, String createMethodName) {
        return [
                "package com.example;",
                "",
                "import jakarta.xml.bind.JAXBElement;",
                "import javax.xml.namespace.QName;",
                "",
                "public class ObjectFactory {",
                "",
                "    private final static QName ${qnameField} = new QName(\"urn:test\", \"x\");",
                "",
                "    public ObjectFactory() {",
                "    }",
                "",
                "    public Object ${createMethodName}() {",
                "        return new Object();",
                "    }",
                "",
                "}"
        ].join(NL) + NL
    }

    @Test
    void merge_combinesMethodsFromBothFiles(@TempDir Path tmpDir) {
        File src = tmpDir.resolve("ObjectFactoryA.java").toFile()
        src.text = objectFactorySource("_Zebra_QNAME", "createZebra")

        File dest = tmpDir.resolve("ObjectFactoryB.java").toFile()
        dest.text = objectFactorySource("_Apple_QNAME", "createApple")

        ObjectFactoryMerger.merge(src, dest, "UTF-8")

        String merged = dest.text
        assertTrue(merged.contains("createApple"), "merged file should contain createApple")
        assertTrue(merged.contains("createZebra"), "merged file should contain createZebra")
        assertTrue(merged.contains("_Apple_QNAME"), "merged file should contain _Apple_QNAME")
        assertTrue(merged.contains("_Zebra_QNAME"), "merged file should contain _Zebra_QNAME")
    }

    @Test
    void merge_sortsCreateMethodsAlphabetically(@TempDir Path tmpDir) {
        File src = tmpDir.resolve("ObjectFactoryA.java").toFile()
        src.text = objectFactorySource("_Zebra_QNAME", "createZebra")

        File dest = tmpDir.resolve("ObjectFactoryB.java").toFile()
        dest.text = objectFactorySource("_Apple_QNAME", "createApple")

        ObjectFactoryMerger.merge(src, dest, "UTF-8")

        String merged = dest.text
        int appleIdx = merged.indexOf("createApple")
        int zebraIdx = merged.indexOf("createZebra")
        assertTrue(appleIdx > 0 && zebraIdx > 0)
        assertTrue(appleIdx < zebraIdx, "createApple should appear before createZebra; got apple@${appleIdx}, zebra@${zebraIdx}")
    }

    @Test
    void merge_isIdempotent_secondMergeProducesSameContent(@TempDir Path tmpDir) {
        File src = tmpDir.resolve("ObjectFactoryA.java").toFile()
        src.text = objectFactorySource("_Zebra_QNAME", "createZebra")

        File dest = tmpDir.resolve("ObjectFactoryB.java").toFile()
        dest.text = objectFactorySource("_Apple_QNAME", "createApple")

        ObjectFactoryMerger.merge(src, dest, "UTF-8")
        String afterFirst = dest.text

        ObjectFactoryMerger.merge(src, dest, "UTF-8")
        String afterSecond = dest.text

        assertEquals(afterFirst, afterSecond)
    }

    @Test
    void merge_preservesImportsAcrossBothFiles(@TempDir Path tmpDir) {
        File src = tmpDir.resolve("ObjectFactoryA.java").toFile()
        src.text = [
                "package com.example;",
                "",
                "import jakarta.xml.bind.JAXBElement;",
                "import com.example.special.SpecialClass;",
                "",
                "public class ObjectFactory {",
                "",
                "    public ObjectFactory() {",
                "    }",
                "",
                "    public Object createZebra() {",
                "        return new Object();",
                "    }",
                "",
                "}"
        ].join(NL) + NL

        File dest = tmpDir.resolve("ObjectFactoryB.java").toFile()
        dest.text = objectFactorySource("_Apple_QNAME", "createApple")

        ObjectFactoryMerger.merge(src, dest, "UTF-8")

        String merged = dest.text
        assertTrue(merged.contains("import jakarta.xml.bind.JAXBElement;"))
        assertTrue(merged.contains("import javax.xml.namespace.QName;"))
        assertTrue(merged.contains("import com.example.special.SpecialClass;"))
    }
}
