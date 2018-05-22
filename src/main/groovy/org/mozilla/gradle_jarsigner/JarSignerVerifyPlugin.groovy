package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

import java.security.MessageDigest
import java.nio.file.Paths

class JarSignerVerifyPluginExtension {
    List dependencies
    Map certificates
}

@groovy.util.logging.Commons
class JarSignerVerifyPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.extensions.create("signatureVerification", JarSignerVerifyPluginExtension)
        project.afterEvaluate {
            def tempDir = File.createTempDir("gradle-jarsigner", "tmp")
            String tempDirPath = tempDir.toString()
            def keystorePath = Paths.get(tempDirPath, "keystore")

            try {
                Keystore.populate(keystorePath, project.signatureVerification.certificates)
                verifyDependencies(project, keystorePath, project.signatureVerification.dependencies)
            } finally {
                tempDir.deleteDir()
            }
        }
    }


    static verifyDependencies(project, keystorePath, dependencies) {
        dependencies.each { assertion ->
            verifySingleDependency(project, keystorePath, assertion)
        }
    }

    static verifySingleDependency(project, keystorePath, assertion) {
        List  parts  = assertion.tokenize(":")
        String group = parts.get(0)
        String name  = parts.get(1)
        String certificateAlias = parts.get(2)

        log.debug "Verifying the signature of ${group}:${name} against certificate alias '${certificateAlias}'..."

        ResolvedArtifact dependency = project.configurations.compile.resolvedConfiguration.resolvedArtifacts.find {
            return it.name.equals(name) && it.moduleVersion.id.group.equals(group)
        }

        if (dependency == null) {
            throw new InvalidUserDataException("No dependency for integrity assertion found: ${group}:${name}")
        }

        verifyJar(keystorePath, dependency.file, certificateAlias)
        println "Signature of ${group}:${name} succesfully verified against certificate alias '${certificateAlias}'"
    }

    static verifyJar(keystorePath, jarFile, certificateAlias) {
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

    static craftJarsignerVerifyCommand(keystorePath, dependencyPath, certificateAlias) {
        return [
            "jarsigner", "-verbose", "-verify", "-strict", // -strict enforces the alias to match
            "-keystore", keystorePath,
            dependencyPath, certificateAlias
        ]
    }
}
