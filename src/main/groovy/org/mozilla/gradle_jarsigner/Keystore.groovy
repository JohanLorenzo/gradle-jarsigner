package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException

class Keystore {

    static populate(keystorePath, certificatePathPerAlias) {
        // Password is not needed to read the certificates
        def password = Utils.generateRandomPassword(32)
        certificatePathPerAlias.each { alias, certificatePath ->
            def command = craftKeystoreCreationCommand(keystorePath, certificatePath, alias)
            def processData = Utils.shellOut(command, [password, password])
            processData.with {
                if (exitCode != 0) {
                    throw new InvalidUserDataException("Cannot insert certificate ${certificatePath}. out> $stdOut err> $stdErr")
                }
            }
        }
    }

    private static craftKeystoreCreationCommand(keystorePath, certificatePath, certificateAlias) {
        return [
            "keytool", "-importcert", "-v", // There is no -verbose option
            "-noprompt", // -noprompt doesn't ask to trust this cert
            "-file", certificatePath,
            "-alias", certificateAlias,
            "-keystore", keystorePath,
        ]
    }
}
