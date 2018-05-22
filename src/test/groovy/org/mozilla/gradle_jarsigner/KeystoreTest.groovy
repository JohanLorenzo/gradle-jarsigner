package org.mozilla.gradle_jarsigner

import groovy.mock.interceptor.MockFor
import spock.lang.Specification

class KeystoreTest extends Specification {

    def "populate calls shellOut with the right arguments"() {
        setup:
        def i = 1

        GroovyMock(Utils, global: true)
        Utils.generateRandomPassword(_) >> "deterministicPassword"
        Utils.shellOut(_, _) >> {List command, List stdinParams ->
            assert command == [
                "keytool", "-importcert", "-v", "-noprompt",
                "-file", "/path/to/cert${i}.pem",
                "-alias", "alias${i}",
                "-keystore", "/path/to/keystore"
            ]
            assert stdinParams == ["deterministicPassword", "deterministicPassword"]
            i++
            return [
                exitCode: 0,
                stdOut: "",
                stdErr: "",
            ]
        }

        when:
        Keystore.populate("/path/to/keystore", [
            alias1: "/path/to/cert1.pem",
            alias2: "/path/to/cert2.pem",
            alias3: "/path/to/cert3.pem",
        ])

        then:
        true // assertions done above

    }

    def "craftKeystoreCreationCommand() generates command line"() {
        when:
        def command = Keystore.craftKeystoreCreationCommand(
            "/path/to/keystore", "relative/path/to/certificate", "some-alias"
        )

        then:
        command == [
            "keytool", "-importcert", "-v", "-noprompt",
            "-file", "relative/path/to/certificate",
            "-alias", "some-alias",
            "-keystore", "/path/to/keystore",
        ]
    }
}
