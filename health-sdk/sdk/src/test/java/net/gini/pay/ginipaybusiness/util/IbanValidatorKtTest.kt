package net.gini.pay.ginipaybusiness.util

import org.junit.Test

class IbanValidatorKtTest {

    @Test
    fun testValidIban() {
        assert(isValidIban("GB33BUKB20201555555555"))
        assert(isValidIban("GB94BARC10201530093459"))
        assert(!isValidIban("GB94BARC20201530093459"))
        assert(!isValidIban("GB96BARC202015300934591"))
        assert(isValidIban("BE71 0961 2345 6769"))
        assert(isValidIban("BR15 0000 0000 0000 1093 2840 814 P2"))
        assert(isValidIban("FR76 3000 6000 0112 3456 7890 189"))
        assert(isValidIban("DE91 1000 0000 0123 4567 89"))
        assert(isValidIban("GR96 0810 0010 0000 0123 4567 890"))
        assert(isValidIban("MU43 BOMM 0101 1234 5678 9101 000 MUR"))
        assert(isValidIban("PK70 BANK 0000 1234 5678 9000"))
        assert(isValidIban("PL10 1050 0099 7603 1234 5678 9123"))
        assert(isValidIban("RO09 BCYP 0000 0012 3456 7890"))
        assert(isValidIban("SA44 2000 0001 2345 6789 1234"))
        assert(isValidIban("ES79 2100 0813 6101 2345 6789"))
        assert(isValidIban("CH56 0483 5012 3456 7800 9"))
        assert(isValidIban("GB98 MIDL 0700 9312 3456 78"))
    }
}