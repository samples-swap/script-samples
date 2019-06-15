def calcDigitSum(String digits) {
    digits.collect {it}.withIndex().collect { String entry, int i ->
        def nextIndex = (i < digits.length() - 1) ? i + 1 : 0
        String nextDigit = digits[nextIndex]
        (entry == nextDigit) ? Integer.parseInt(entry) : 0
    }.sum()
}