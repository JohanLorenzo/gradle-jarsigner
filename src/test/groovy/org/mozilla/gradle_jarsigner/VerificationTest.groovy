package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException

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

        when:
        Verification.verifyJar("/path/to/keystore", "/path/to/file.jar", "some-alias")

        then:
        true // assertions done above

    }

    def "verifyJar() throws error when signature can't be validated"() {
        setup:
        GroovyMock(Utils, global: true)
        Utils.shellOut(_) >> {_ -> [
                exitCode: code,
                stdOut: out,
                stdErr: "",
            ]
        }

        when:
        Verification.verifyJar("/path/to/keystore", "/path/to/file.jar", "some-alias")

        then:
        thrown InvalidUserDataException

        where:
        code << [32, 0, 0]
        out << ["jar verified, with signer errors", "jar is unsigned", "no manifest" ]
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
