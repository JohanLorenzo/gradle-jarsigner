package org.mozilla.gradle_jarsigner

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

import java.security.MessageDigest
import java.nio.file.Paths

class JarSignerVerifyPluginExtension {
    List dependencies
}

class JarSignerVerifyPlugin implements Plugin<Project> {

    static String DEFAULT_ALIAS = 'non-important-alias'

    static populateKeystore(certificatePath, tempDirPath) {
        String command = craftKeystoreCreationCommand(certificatePath, tempDirPath)
        def password = Utils.generateRandomPassword(32)
        Utils.shellOut(command, [password, password])
    }

    static getKeyStorePath(keystoreFolder) {
        return Paths.get(keystoreFolder, "keystore")
    }

    static craftKeystoreCreationCommand(certificatePath, keystoreFolder) {
        String keystorePath = getKeyStorePath(keystoreFolder)
        return """keytool -importcert -v -noprompt \
-file ${certificatePath} \
-alias ${DEFAULT_ALIAS} \
-keystore ${keystorePath}"""
    }

    static craftJarsignerVerifyCommand(keystoreFolder, dependencyPath) {
        String keystorePath = getKeyStorePath(keystoreFolder)
        // -strict enforces the alias to match
        return """jarsigner -verify -strict -verbose \
-keystore ${keystorePath} \
${dependencyPath} \
${DEFAULT_ALIAS}"""
    }

    static verifyJar(keystoreFolder, jarFile) {
        def command = craftJarsignerVerifyCommand(keystoreFolder, jarFile)
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

    void apply(Project project) {
        project.extensions.create("signatureVerification", JarSignerVerifyPluginExtension)
        project.afterEvaluate {
            project.signatureVerification.dependencies.each {
                assertion ->
                    List  parts  = assertion.tokenize(":")
                    String group = parts.get(0)
                    String name  = parts.get(1)
                    String certificatePath = parts.get(2)

                    ResolvedArtifact dependency = project.configurations.compile.resolvedConfiguration.resolvedArtifacts.find {
                        return it.name.equals(name) && it.moduleVersion.id.group.equals(group)
                    }

                    println "Verifying the signature of ${group}:${name} against certificate '${certificatePath}'"

                    if (dependency == null) {
                        throw new InvalidUserDataException("No dependency for integrity assertion found: ${group}:${name}")
                    }

                    def tempDir = File.createTempDir("gradle-jarsigner", "tmp")
                    try {
                        String tempDirPath = tempDir.toString()
                        populateKeystore(certificatePath, tempDirPath)
                        verifyJar(tempDirPath, dependency.file)
                    } finally {
                        tempDir.deleteDir()
                    }
            }
        }
    }
}
