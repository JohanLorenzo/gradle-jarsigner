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

    static shellOut(command) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        println "Running command: ${command}"
        def proc = command.execute()
        OutputStream stdin = proc.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
        // TODO Make pass random
        writer.write("nonimportantpass\n");
        writer.flush();
        writer.write("nonimportantpass\n");
        writer.flush();
        writer.close();
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(10000)
        println "out> $sout err> $serr"
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
        return """jarsigner -verify -verbose \
-keystore ${keystorePath} \
${dependencyPath} \
${DEFAULT_ALIAS}"""
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
                        throw new InvalidUserDataException("No dependency for integrity assertion found: " + group + ":" + name)
                    }

                    String tempDir = File.createTempDir("gradle-jarsigner", "tmp")
                    String command = craftKeystoreCreationCommand(certificatePath, tempDir)
                    shellOut(command)
                    command = craftJarsignerVerifyCommand(tempDir, dependency.file)
                    shellOut(command)
            }
        }
    }
}
