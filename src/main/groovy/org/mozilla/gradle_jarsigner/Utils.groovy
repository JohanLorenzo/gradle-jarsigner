package org.mozilla.gradle_jarsigner

@groovy.util.logging.Commons
class Utils {
    static String ALPHABET = (('A'..'Z')+('a'..'z')+('0'..'9')).join()

    static generateRandomPassword(length) {
        return new Random().with {
            (1..length).collect { ALPHABET[nextInt(ALPHABET.length())] }.join()
        }
    }

    // shellOut doesn't allow strings commands to safeguard against spaces
    static shellOut(List command, List stdinParams=[]) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        log.debug "Running command: ${command}"
        def proc = command.execute()

        if (!stdinParams.empty) {
            OutputStream stdin = proc.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            stdinParams.each{ param ->
                writer.write("${param}\n");
                writer.flush();
            }
            writer.close();
        }

        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(10000)

        return [
            exitCode: proc.exitValue(),
            stdOut: sout.toString(),
            stdErr: serr.toString(),
        ]
    }
}
