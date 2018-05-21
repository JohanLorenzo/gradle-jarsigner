package org.mozilla.gradle_jarsigner

import spock.lang.Specification

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
        def processData = Utils.shellOut(["echo", "hello world"])

        then:
        processData.exitCode == 0
        processData.stdOut == "hello world\n"
        processData.stdErr == ""
    }

    def "shellOut() outputs stdErr"() {
        when:
        def processData = Utils.shellOut(["cat", "non_existing-file"])

        then:
        processData.exitCode == 1
        processData.stdOut == ""
        processData.stdErr == "cat: non_existing-file: No such file or directory\n"
    }
}
