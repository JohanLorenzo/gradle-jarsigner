package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException

import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

import spock.lang.Specification

class VerificationTest extends Specification {

    def "verifySingleDependency() calls verifyJar() with the right arguments"() {
        setup:
            GroovySpy(Verification, global: true)
            1 * Verification.verifyJar(_, _, _) >> { String keystorePath, String jarFile, String certificateAlias ->
                assert keystorePath == "/path/to/keystore"
                assert jarFile == "/path/to/file.jar"
                assert certificateAlias == "some-cert-alias"
            }

            def resolvedArtifacts = [
                [
                    file: "/path/to/file.jar",
                    moduleVersion: [id: [group: "some-group"]],
                    name: "some-name",
                ],
            ]

        when:
            Verification.verifySingleDependency("/path/to/keystore", resolvedArtifacts, "some-group:some-name:some-cert-alias")

        then:
            true // assertions done above
    }

    def "verifySingleDependency() throws error when dependency is not part of the compile list"() {
        setup:
            def resolvedArtifacts = [
                [
                    file: "/path/to/another-file.jar",
                    moduleVersion: [id: [group: "another-group"]],
                    name: "another-name",
                ],
            ]

        when:
            Verification.verifySingleDependency("/path/to/keystore", resolvedArtifacts, "some-group:some-name:some-cert-alias")

        then:
            thrown(InvalidUserDataException)
    }


    def "verifyJar() calls shellOut with the right arguments"() {
        setup:
        GroovyMock(Utils, global: true)
        Utils.shellOut(_) >> {arguments ->
            assert arguments[0] == [
                "jarsigner", "-verbose", "-verify", "-strict",
                "-keystore", "/path/to/keystore",
                "/path/to/file.jar", "some-alias"
            ]
            return [
                exitCode: 0,
                stdOut: "",
                stdErr: "",
            ]
        }
        GroovySpy(Verification, global: true)
        Verification.verifyMetaInfFilesArePresent(_) >> {}

        when:
        Verification.verifyJar("/path/to/keystore", "/path/to/file.jar", "some-alias")

        then:
        true // assertions done above

    }

    def "verifyJar() throws error when signature can't be validated"() {
        setup:
        GroovyMock(Utils, global: true)
        Utils.shellOut(_) >> {_ -> [
                exitCode: 32,   // Real error code given by jarsigner in this context
                stdOut: "",
                stdErr: "",
            ]
        }
        GroovySpy(Verification, global: true)
        Verification.verifyMetaInfFilesArePresent(_) >> {}

        when:
        Verification.verifyJar("/path/to/keystore", "/path/to/file.jar", "some-alias")

        then:
        thrown InvalidUserDataException
    }

    def "verifyMetaInfFilesArePresent() passes when all files are provided in jar file"() {
        setup:
        def zipFile = generateSimpleJarFile([
            "META-INF/MANIFEST.MF": "Manifest-Version: 1.0",
            "META-INF/ONEALIAS.SF": "Signature-Version: 1.0",
        ])

        when:
        Verification.verifyMetaInfFilesArePresent(zipFile.toPath())

        then:
        true // Function under test should just run and not throw an erro

        cleanup:
        zipFile.delete()
    }

    static generateSimpleJarFile(Map entries) {
        def zipFile = File.createTempFile("some", ".jar")
        def out = new ZipOutputStream(new FileOutputStream(zipFile))

        entries.each { String name, String content ->
            def entry = new ZipEntry(name)
            out.putNextEntry(entry)

            byte[] data = content.getBytes()
            out.write(data, 0, data.length)
            out.closeEntry()
        }
        out.close()

        return zipFile
    }

    def "verifyMetaInfFilesArePresent() throws error when some files are missing"() {
        setup:
        def zipFile = generateSimpleJarFile([zipEntryFileName: zipEntryContent])

        when:
        Verification.verifyMetaInfFilesArePresent(zipFile.toPath())

        then:
        thrown InvalidUserDataException

        cleanup:
        zipFile.delete()

        where:
        zipEntryFileName << ["META-INF/MANIFEST.MF", "META-INF/ONEALIAS.SF", "META-INF/SOMEALIA.SF"]
        zipEntryContent << ["Manifest-Version: 1.0", "Signature-Version: 1.0", "Signature-Version: 1.0"]
    }

    def "craftJarsignerVerifyCommand() generates command line"() {
        when:
        def command = Verification.craftJarsignerVerifyCommand(
            "/path/to/keystore", "/path/to/file.jar", "some-alias"
        )

        then:
        command == [
            "jarsigner", "-verbose", "-verify", "-strict",
            "-keystore", "/path/to/keystore",
            "/path/to/file.jar", "some-alias"
        ]
    }
}
