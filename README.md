# Gradle Jarsigner

A gradle plugin that enforces signatures on remote dependencies. Inspired by https://github.com/signalapp/gradle-witness.

## Limitations

Only `jarsigner` signatures are supported. PGP ones aren't.

## Usage

This gradle plugin simply allows the author of a project to pin signatures on a subset of dependencies. Because not all jar/aar files are signed, just use it on packages that you know are signed (Mozilla's GeckoView is an example). Enforcing the signatures on a subset of dependencies ensures a dependency on a 3rd-party host hasn't been compromised. This also allows a dependency to be pinned to the latest, without a maintainer having to manually edit the checksum (which can have been compromised beforehand)


```gradle
dependencies {
    compile 'some-group:some-lib:1.0.0'
    compile 'some-group:some-other-lib:1.0.1'
    compile 'unenforced-group:unenforced-lib:2.0.0'
    compile 'a-third-group:a-third-lib:3.0.0'
}

signatureVerification {
    certificates = [
        publisher1: 'project/path/to/publisher1/cert.pem',
        publisher2: 'project/path/to/publisher2/cert.pem',
    ]
    dependencies = [
        'some-group:some-lib:publisher1',
        'some-group:another-lib:publisher1',
        'a-third-group:a-third-lib:publisher2',
    ]
}
```

The top-level `dependencies` definition is the same, but `gradle-jarsigner` allows one to also specify a `signatureVerification` definition as well. `signatureVerification` must include `certificates` and `dependencies`. `certificates` maps an alias (used in `dependencies`) and relative paths to the certificate (in `pem` format). `dependencies` then defines `group:name:alias`

At this point, running `gradle build` will first verify that all of the listed dependencies are signed by the corresponding certificates.  If there's a mismatch, the build is aborted.

## Gradle Jarsigner

Unfortunately, it doesn't make sense to publish `gradle-jarsigner` as an artifact, since that creates a bootstrapping problem. To use `gradle-jarsigner`, the jar needs to be built and included in your project:

```bash
git clone https://github.com/mozilla-releng/gradle-jarsigner.git
cd gradle-jarsigner
gradle build
cp build/libs/gradle-jarsigner.jar /path/to/your/project/libs/gradle-jarsigner.jar
```

Then in your project's `build.gradle`, the buildscript needs to add a `gradle-jarsigner` dependency.
It might look something like:

```gradle
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.+'
        classpath files('libs/gradle-jarsigner.jar')
    }
}

apply plugin: 'jarsigner'
```

From then on, running a standard `gradle build` will verify the signatures of the targeted dependencies.
