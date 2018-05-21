class Utils {
    static String ALPHABET = (('A'..'Z')+('a'..'z')+('0'..'9')).join()

    static generateRandomPassword(length) {
        return new Random().with {
            (1..length).collect { ALPHABET[nextInt(ALPHABET.length())] }.join()
        }
    }

    static shellOut(command, stdinParams=[]) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        println "Running command: ${command}"
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
        println "out> $sout err> $serr"
    }
}
