package org.mozilla.gradle_jarsigner

import spock.lang.Specification

class JarSignerVerifyPluginTest extends Specification {

    def "setUpAndVerifyDependencies() delegates calls and cleans up temporary keystore dir"() {
        setup:
        def i = 1

        GroovyMock(Keystore, global: true)
        GroovyMock(Verification, global: true)
        def certificatesData = [
            alias1: "/path/to/cert1.pem",
            alias2: "/path/to/cert2.pem",
            alias3: "/path/to/cert3.pem",
        ]

        def keystoreFolder
        def keystorePath

        Keystore.populate(_, _) >> {arguments ->
            keystorePath = arguments[0].toString()
            def keystoreFile = new File(keystorePath);
            keystoreFolder = new File(keystoreFile.getParent())
            assert keystoreFolder.toString().contains("gradle-jarsigner")
            assert keystoreFolder.exists()
            assert keystoreFolder.isDirectory()

            assert arguments[1] == certificatesData
        }

        def resolvedArtifactsData = [
            [
                file: "/path/to/file.jar",
                moduleVersion: [id: [group: "some-group"]],
                name: "some-name",
            ],
        ]
        def dependenciesData = [
            "some-group:some-name:some-cert-alias",
        ]
        Verification.verifyDependencies(_, _, _) >> { arguments ->
            assert arguments[0].toString() == keystorePath
        }

        def signatureVerification = [certificates: certificatesData, dependencies: dependenciesData]

        when:
        JarSignerVerifyPlugin.setUpAndVerifyDependencies(resolvedArtifactsData, signatureVerification)

        then:
        assert !keystoreFolder.exists()
    }
}
