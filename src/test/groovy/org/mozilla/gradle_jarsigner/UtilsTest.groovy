package org.mozilla.gradle_jarsigner

import spock.lang.Specification

import java.nio.file.Paths

class UtilsTest extends Specification {

    def "generateRandomPassword() generates unique enough strings"() {
        when:
        def password1 = Utils.generateRandomPassword(32)
        def password2 = Utils.generateRandomPassword(32)

        then:
        password1 != password2
    }

    def "generateRandomPassword() generates length-based strings"() {
        when:
        def password1 = Utils.generateRandomPassword(32)
        def password2 = Utils.generateRandomPassword(16)

        then:
        password1.size() == 32
        password2.size() == 16
    }

    def "shellOut() outputs stdOut"() {
        when:
        def processData = Utils.shellOut(["echo", "hello world", "!"])

        then:
        processData.exitCode == 0
        processData.stdOut == "hello world !\n"
        processData.stdErr == ""
    }

    def "shellOut() returns bad error code"() {
        when:
        def processData = Utils.shellOut(["false"])

        then:
        processData.exitCode == 1
        processData.stdOut == ""
        processData.stdErr == ""
    }

    def "shellOut() allows stdinParams"() {
        when:
        def tempDir = File.createTempDir()
        def keystorePath = Paths.get(tempDir.toString(), "keystore")
        def processData = Utils.shellOut(["keytool", "-genkey", "-keystore", keystorePath], [
            'somepassword',
            'somepassword',
            'Some Name',
            'Some Org Unit',
            'Some Org',
            'Some City',
            'Some State',
            'US',
            'yes',
            'somepassword',
        ])

        then:
        processData.exitCode == 0
        processData.stdOut == ""
        processData.stdErr.contains("Is CN=Some Name, OU=Some Org Unit, O=Some Org, L=Some City, ST=Some State, C=US correct?")
        cleanup:
        tempDir.deleteDir()
    }
}
