package org.mozilla.gradle_jarsigner

import org.gradle.api.Plugin
import org.gradle.api.Project

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
                Verification.verifyDependencies(
                    keystorePath,
                    project.configurations.compile.resolvedConfiguration.resolvedArtifacts,
                    project.signatureVerification.dependencies
                )
            } finally {
                tempDir.deleteDir()
            }
        }
    }
}
