def calcDigitSum2(String digits) {
    digits.collect {it}.withIndex().collect { String entry, int i ->
        int halfway = digits.length() / 2
        def nextIndex = (i + halfway) % digits.length()
        String nextDigit = digits[nextIndex]
        (entry == nextDigit) ? Integer.parseInt(entry) : 0
    }.sum()
}