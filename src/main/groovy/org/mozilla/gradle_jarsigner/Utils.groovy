class Utils {
    static String ALPHABET = (('A'..'Z')+('a'..'z')+('0'..'9')).join()

    static generateRandomPassword(length) {
        return new Random().with {
            (1..length).collect { ALPHABET[nextInt(ALPHABET.length())] }.join()
        }
    }
}
