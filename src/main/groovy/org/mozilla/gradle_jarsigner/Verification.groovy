package org.mozilla.gradle_jarsigner

import java.util.zip.ZipFile

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

    private static verifyJar(keystorePath, jarPath, certificateAlias) {
        // XXX In the case there is no manifest, jarsigner returns 0 even even though the jar is unsigned.
        // That's why we have to manually ensure the manifest exists
        verifyMetaInfFilesArePresent(jarPath)

        def command = craftJarsignerVerifyCommand(keystorePath, jarPath, certificateAlias)
        def processData = Utils.shellOut(command)

        processData.with {
            if (exitCode != 0) {
                throw new InvalidUserDataException("Wrong signature found on ${jarFile}. out> ${stdOut} err> ${stdErr}")
            }
        }
    }

    private static verifyMetaInfFilesArePresent(jarPath) {
        def jarFileLocation = jarPath.toString()
        def zipFile = new ZipFile(new File(jarFileLocation))

        def manifest = zipFile.entries().find { it.name == "META-INF/MANIFEST.MF" }
        if (manifest == null) {
            throw new InvalidUserDataException("Missing \"META-INF/MANIFEST.MF\" in jar: ${jarFileLocation}")
        }

        def signatureFile = zipFile.entries().find { it.name.startsWith("META-INF/") && it.name.endsWith(".SF") }
        if (signatureFile == null) {
            throw new InvalidUserDataException("Missing signature file in jar: ${jarFileLocation}")
        }
    }

    private static craftJarsignerVerifyCommand(keystorePath, dependencyPath, certificateAlias) {
        return [
            "jarsigner", "-verbose", "-verify",
            // -strict raises warnings as errors (and changes the exit code).
            // For instance, it enforces the alias to match
            "-strict",
            "-keystore", keystorePath,
            dependencyPath, certificateAlias
        ]
    }
}
