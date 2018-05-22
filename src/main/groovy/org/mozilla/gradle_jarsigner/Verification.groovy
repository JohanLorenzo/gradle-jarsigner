package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.ResolvedArtifact

@groovy.util.logging.Commons
class Verification {
    static verifyDependencies(keystorePath, resolvedArtifacts, dependencies) {
        // XXX Using a closure prevents this function to be unit tested
        // See https://github.com/spockframework/spock/issues/88
        dependencies.each { signedArtifactAssertion ->
            verifySingleDependency(keystorePath, resolvedArtifacts, signedArtifactAssertion)
        }
    }

    private static verifySingleDependency(keystorePath, resolvedArtifacts, signedArtifactAssertion) {
        List  parts  = signedArtifactAssertion.tokenize(":")
        String group = parts.get(0)
        String name  = parts.get(1)
        String certificateAlias = parts.get(2)

        log.debug "Verifying the signature of ${group}:${name} against certificate alias '${certificateAlias}'..."

        def dependency = resolvedArtifacts.find {
            return it.name.equals(name) && it.moduleVersion.id.group.equals(group)
        }

        if (dependency == null) {
            throw new InvalidUserDataException("No dependency for integrity assertion found: ${group}:${name}")
        }

        verifyJar(keystorePath, dependency.file, certificateAlias)
        println "Signature of ${group}:${name} succesfully verified against certificate alias '${certificateAlias}'"
    }

    private static verifyJar(keystorePath, jarFile, certificateAlias) {
        def command = craftJarsignerVerifyCommand(keystorePath, jarFile, certificateAlias)
        def processData = Utils.shellOut(command)

        processData.with {
            if (exitCode != 0) {
                throw new InvalidUserDataException("Wrong signature found on ${jarFile}. out> $stdOut err> $stdErr")
            }
            if (stdOut.contains("jar is unsigned") || stdOut.contains("no manifest")) {
                throw new InvalidUserDataException("Unsigned jar: ${jarFile}")
            }
        }
    }

    private static craftJarsignerVerifyCommand(keystorePath, dependencyPath, certificateAlias) {
        return [
            "jarsigner", "-verbose", "-verify", "-strict", // -strict enforces the alias to match
            "-keystore", keystorePath,
            dependencyPath, certificateAlias
        ]
    }
}
