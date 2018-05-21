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

            try {
                populateKeystore(tempDirPath, project.signatureVerification.certificates)
                verifyDependencies(project, tempDirPath, project.signatureVerification.dependencies)
            } finally {
                tempDir.deleteDir()
            }
        }
    }

    static populateKeystore(keystoreFolder, certificatePathPerAlias) {
        // Password is not needed to read the certificates
        def password = Utils.generateRandomPassword(32)
        certificatePathPerAlias.each { alias, certificatePath ->
            String command = craftKeystoreCreationCommand(keystoreFolder, certificatePath, alias)
            def processData = Utils.shellOut(command, [password, password])
            if (processData.exitCode != 0) {
                throw new InvalidUserDataException("Cannot insert certificate ${certificatePath}. out> $stdOut err> $stdErr")
            }
        }
    }

    static craftKeystoreCreationCommand(keystoreFolder, certificatePath, certificateAlias) {
        String keystorePath = getKeyStorePath(keystoreFolder)
        return """keytool -importcert -v -noprompt \
-file ${certificatePath} \
-alias ${certificateAlias} \
-keystore ${keystorePath}"""
    }

    static getKeyStorePath(keystoreFolder) {
        return Paths.get(keystoreFolder, "keystore")
    }

    static verifyDependencies(project, tempDirPath, dependencies) {
        dependencies.each { assertion ->
            verifySingleDependency(project, tempDirPath, assertion)
        }
    }

    static verifySingleDependency(project, keystoreFolder, assertion) {
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

        verifyJar(keystoreFolder, dependency.file, certificateAlias)
        println "Signature of ${group}:${name} verified against certificate alias '${certificateAlias}'"
    }

    static verifyJar(keystoreFolder, jarFile, certificateAlias) {
        def command = craftJarsignerVerifyCommand(keystoreFolder, jarFile, certificateAlias)
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

    static craftJarsignerVerifyCommand(keystoreFolder, dependencyPath, certificateAlias) {
        String keystorePath = getKeyStorePath(keystoreFolder)
        // -strict enforces the alias to match
        return """jarsigner -verify -strict -verbose \
-keystore ${keystorePath} \
${dependencyPath} \
${certificateAlias}"""
    }
}
