// IGNORE_BACKEND_K1: ANY
// ^^^^^^^^^^^^^^^^^^^^^^ Unsigned operations will not be supported in the legacy K1 frontend.
// WITH_STDLIB
fun <T> T.id() = this

const val compareTo1 = 1u.toUShort().<!EVALUATED("-1")!>compareTo(2u.toUShort())<!>
const val compareTo2 = 2u.toUShort().<!EVALUATED("0")!>compareTo(2u.toUShort())<!>
const val compareTo3 = 3u.toUShort().<!EVALUATED("1")!>compareTo(2u.toUShort())<!>
const val compareTo4 = 2u.toUShort().<!EVALUATED("0")!>compareTo(2u.toUByte())<!>
const val compareTo5 = 2u.toUShort().<!EVALUATED("0")!>compareTo(2u)<!>
const val compareTo6 = 2u.toUShort().<!EVALUATED("0")!>compareTo(2UL)<!>

const val plus1 = 1u.toUShort().<!EVALUATED("3")!>plus(2u.toUShort())<!>
const val plus2 = 2u.toUShort().<!EVALUATED("4")!>plus(2u.toUShort())<!>
const val plus3 = 3u.toUShort().<!EVALUATED("5")!>plus(2u.toUShort())<!>
const val plus4 = 2u.toUShort().<!EVALUATED("4")!>plus(2u.toUByte())<!>
const val plus5 = 2u.toUShort().<!EVALUATED("4")!>plus(2u)<!>
const val plus6 = 2u.toUShort().<!EVALUATED("4")!>plus(2UL)<!>

const val minus1 = 1u.toUShort().<!EVALUATED("4294967295")!>minus(2u.toUShort())<!>
const val minus2 = 2u.toUShort().<!EVALUATED("0")!>minus(2u.toUShort())<!>
const val minus3 = 3u.toUShort().<!EVALUATED("1")!>minus(2u.toUShort())<!>
const val minus4 = 2u.toUShort().<!EVALUATED("0")!>minus(2u.toUByte())<!>
const val minus5 = 2u.toUShort().<!EVALUATED("0")!>minus(2u)<!>
const val minus6 = 2u.toUShort().<!EVALUATED("0")!>minus(2UL)<!>

const val times1 = 1u.toUShort().<!EVALUATED("2")!>times(2u.toUShort())<!>
const val times2 = 2u.toUShort().<!EVALUATED("4")!>times(2u.toUShort())<!>
const val times3 = 3u.toUShort().<!EVALUATED("6")!>times(2u.toUShort())<!>
const val times4 = 2u.toUShort().<!EVALUATED("4")!>times(2u.toUByte())<!>
const val times5 = 2u.toUShort().<!EVALUATED("4")!>times(2u)<!>
const val times6 = 2u.toUShort().<!EVALUATED("4")!>times(2UL)<!>

const val div1 = 1u.toUShort().<!EVALUATED("0")!>div(2u.toUShort())<!>
const val div2 = 2u.toUShort().<!EVALUATED("1")!>div(2u.toUShort())<!>
const val div3 = 3u.toUShort().<!EVALUATED("1")!>div(2u.toUShort())<!>
const val div4 = 2u.toUShort().<!EVALUATED("1")!>div(2u.toUByte())<!>
const val div5 = 2u.toUShort().<!EVALUATED("1")!>div(2u)<!>
const val div6 = 2u.toUShort().<!EVALUATED("1")!>div(2UL)<!>

const val floorDiv1 = 1u.toUShort().<!EVALUATED("0")!>floorDiv(2u.toUShort())<!>
const val floorDiv2 = 2u.toUShort().<!EVALUATED("1")!>floorDiv(2u.toUShort())<!>
const val floorDiv3 = 3u.toUShort().<!EVALUATED("1")!>floorDiv(2u.toUShort())<!>
const val floorDiv4 = 2u.toUShort().<!EVALUATED("1")!>floorDiv(2u.toUByte())<!>
const val floorDiv5 = 2u.toUShort().<!EVALUATED("1")!>floorDiv(2u)<!>
const val floorDiv6 = 2u.toUShort().<!EVALUATED("1")!>floorDiv(2UL)<!>

const val rem1 = 1u.toUShort().<!EVALUATED("1")!>rem(2u.toUShort())<!>
const val rem2 = 2u.toUShort().<!EVALUATED("0")!>rem(2u.toUShort())<!>
const val rem3 = 3u.toUShort().<!EVALUATED("1")!>rem(2u.toUShort())<!>
const val rem4 = 2u.toUShort().<!EVALUATED("0")!>rem(2u.toUByte())<!>
const val rem5 = 2u.toUShort().<!EVALUATED("0")!>rem(2u)<!>
const val rem6 = 2u.toUShort().<!EVALUATED("0")!>rem(2UL)<!>

const val mod1 = 1u.toUShort().<!EVALUATED("1")!>mod(2u.toUShort())<!>
const val mod2 = 2u.toUShort().<!EVALUATED("0")!>mod(2u.toUShort())<!>
const val mod3 = 3u.toUShort().<!EVALUATED("1")!>mod(2u.toUShort())<!>
const val mod4 = 2u.toUShort().<!EVALUATED("0")!>mod(2u.toUByte())<!>
const val mod5 = 2u.toUShort().<!EVALUATED("0")!>mod(2u)<!>
const val mod6 = 2u.toUShort().<!EVALUATED("0")!>mod(2UL)<!>

const val and1 = 1u.toUShort().<!EVALUATED("0")!>and(2u.toUShort())<!>
const val and2 = 2u.toUShort().<!EVALUATED("2")!>and(2u.toUShort())<!>
const val and3 = 3u.toUShort().<!EVALUATED("2")!>and(2u.toUShort())<!>
const val and4 = 12u.toUShort().<!EVALUATED("8")!>and(10u.toUShort())<!>

const val or1 = 1u.toUShort().<!EVALUATED("3")!>or(2u.toUShort())<!>
const val or2 = 2u.toUShort().<!EVALUATED("2")!>or(2u.toUShort())<!>
const val or3 = 3u.toUShort().<!EVALUATED("3")!>or(2u.toUShort())<!>
const val or4 = 12u.toUShort().<!EVALUATED("14")!>or(10u.toUShort())<!>

const val xor1 = 1u.toUShort().<!EVALUATED("3")!>xor(2u.toUShort())<!>
const val xor2 = 2u.toUShort().<!EVALUATED("0")!>xor(2u.toUShort())<!>
const val xor3 = 3u.toUShort().<!EVALUATED("1")!>xor(2u.toUShort())<!>
const val xor4 = 12u.toUShort().<!EVALUATED("6")!>xor(10u.toUShort())<!>

const val inv1 = 0u.toUShort().<!EVALUATED("65535")!>inv()<!>
const val inv2 = 1u.toUShort().<!EVALUATED("65534")!>inv()<!>

const val convert1 = 1u.toUShort().<!EVALUATED("1")!>toUByte()<!>
const val convert2 = 1u.toUShort().<!EVALUATED("1")!>toUShort()<!>
const val convert3 = 1u.toUShort().<!EVALUATED("1")!>toUInt()<!>
const val convert4 = 1u.toUShort().<!EVALUATED("1")!>toULong()<!>
const val convert5 = 1u.toUShort().<!EVALUATED("1.0")!>toFloat()<!>
const val convert6 = 1u.toUShort().<!EVALUATED("1.0")!>toDouble()<!>
const val convert7 = 1u.toUShort().<!EVALUATED("1")!>toByte()<!>
const val convert8 = 1u.toUShort().<!EVALUATED("1")!>toShort()<!>
const val convert9 = 1u.toUShort().<!EVALUATED("1")!>toInt()<!>
const val convert10 = 1u.toUShort().<!EVALUATED("1")!>toLong()<!>

// TODO: Enable once conversion extension functions are supported (requires bootstrapped compiler)
// const val convert11 = 1.toByte().toUShort()
// const val convert12 = 1.toShort().toUShort()
// const val convert13 = 1.toUShort()
// const val convert14 = 1L.toUShort()

const val equals1 = <!EVALUATED("false")!>1u.toUShort() == 2u.toUShort()<!>
const val equals2 = <!EVALUATED("true")!>2u.toUShort() == 2u.toUShort()<!>
const val equals3 = <!EVALUATED("false")!>3u.toUShort() == 2u.toUShort()<!>
const val equals4 = <!EVALUATED("false")!>4u.toUShort() == 2u.toUShort()<!>

const val toString1 = 1u.toUShort().<!EVALUATED("1")!>toString()<!>
const val toString2 = 2u.toUShort().<!EVALUATED("2")!>toString()<!>

const val limits1 = <!EVALUATED("65535")!>65534u.toUShort()+1u.toUShort()<!>
const val limits2 = <!EVALUATED("65536")!>65535u.toUShort()+1u.toUShort()<!>
const val limits3 = <!EVALUATED("4294967295")!>0u.toUShort()-1u.toUShort()<!>

// STOP_EVALUATION_CHECKS
fun box(): String {
    if (compareTo1.id() != -1)   return "Fail compareTo1"
    if (compareTo2.id() != 0)    return "Fail compareTo2"
    if (compareTo3.id() != 1)    return "Fail compareTo3"
    if (compareTo4.id() != 0)    return "Fail compareTo4"
    if (compareTo5.id() != 0)    return "Fail compareTo5"
    if (compareTo6.id() != 0)    return "Fail compareTo6"

    if (plus1.id() != 3u)    return "Fail plus1"
    if (plus2.id() != 4u)    return "Fail plus2"
    if (plus3.id() != 5u)    return "Fail plus3"
    if (plus4.id() != 4u)    return "Fail plus4"
    if (plus5.id() != 4u)    return "Fail plus5"
    if (plus6.id() != 4UL)   return "Fail plus6"

    if (minus1.id() != 4294967295u)     return "Fail minus1"
    if (minus2.id() != 0u)              return "Fail minus2"
    if (minus3.id() != 1u)              return "Fail minus3"
    if (minus4.id() != 0u)              return "Fail minus4"
    if (minus5.id() != 0u)              return "Fail minus5"
    if (minus6.id() != 0UL)             return "Fail minus6"

    if (times1.id() != 2u)        return "Fail times1"
    if (times2.id() != 4u)        return "Fail times2"
    if (times3.id() != 6u)        return "Fail times3"
    if (times4.id() != 4u)        return "Fail times4"
    if (times5.id() != 4u)        return "Fail times5"
    if (times6.id() != 4UL)       return "Fail times6"

    if (div1.id() != 0u)          return "Fail div1"
    if (div2.id() != 1u)          return "Fail div2"
    if (div3.id() != 1u)          return "Fail div3"
    if (div4.id() != 1u)          return "Fail div4"
    if (div5.id() != 1u)          return "Fail div5"
    if (div6.id() != 1uL)         return "Fail div6"

    if ( floorDiv1.id() != 0u)          return "Fail  floorDiv1"
    if ( floorDiv2.id() != 1u)          return "Fail  floorDiv2"
    if ( floorDiv3.id() != 1u)          return "Fail  floorDiv3"
    if ( floorDiv4.id() != 1u)          return "Fail  floorDiv4"
    if ( floorDiv5.id() != 1u)          return "Fail  floorDiv5"
    if ( floorDiv6.id() != 1uL)         return "Fail  floorDiv6"

    if (rem1.id() != 1u)      return "Fail rem1"
    if (rem2.id() != 0u)      return "Fail rem2"
    if (rem3.id() != 1u)      return "Fail rem3"
    if (rem4.id() != 0u)      return "Fail rem4"
    if (rem5.id() != 0u)      return "Fail rem5"
    if (rem6.id() != 0UL)     return "Fail rem6"

    if (mod1.id() !=  1u.toUShort())    return "Fail mod1"
    if (mod2.id() !=  0u.toUShort())    return "Fail mod2"
    if (mod3.id() !=  1u.toUShort())    return "Fail mod3"
    if (mod4.id() != 0u.toUByte())      return "Fail mod4"
    if (mod5.id() != 0u)                return "Fail mod5"
    if (mod6.id() != 0UL)               return "Fail mod6"

    if (and1.id() !=  0u.toUShort())      return "Fail and1"
    if (and2.id() !=  2u.toUShort())      return "Fail and2"
    if (and3.id() !=  2u.toUShort())      return "Fail and3"
    if (and4.id() !=  8u.toUShort())      return "Fail and4"

    if (or1.id() !=  3u.toUShort())      return "Fail or1"
    if (or2.id() !=  2u.toUShort())      return "Fail or2"
    if (or3.id() !=  3u.toUShort())      return "Fail or3"
    if (or4.id() !=  14u.toUShort())     return "Fail or4"

    if (xor1.id() !=  3u.toUShort())      return "Fail xor1"
    if (xor2.id() !=  0u.toUShort())      return "Fail xor2"
    if (xor3.id() !=  1u.toUShort())      return "Fail xor3"
    if (xor4.id() !=  6u.toUShort())      return "Fail xor4"

    if (inv1.id() !=  65535u.toUShort())      return "Fail inv1"
    if (inv2.id() !=  65534u.toUShort())      return "Fail inv2"

    if (convert1.id() != 1u.toUByte())   return "Fail convert1"
    if (convert2.id() != 1u.toUShort())  return "Fail convert2"
    if (convert3.id() != 1u)             return "Fail convert3"
    if (convert4.id() != 1UL)            return "Fail convert4"
    if (convert7.id() != 1.toByte())     return "Fail convert7"
    if (convert8.id() != 1.toShort())    return "Fail convert8"
    if (convert9.id() != 1)              return "Fail convert9"
    if (convert10.id() != 1L)            return "Fail convert10"
    // if (convert11.id() != 1u.toUShort())  return "Fail convert11"
    // if (convert12.id() != 1u.toUShort())  return "Fail convert12"
    // if (convert13.id() != 1u.toUShort())  return "Fail convert13"
    // if (convert14.id() != 1u.toUShort())  return "Fail convert14"

    if (equals1.id() != false)   return "Fail equals1"
    if (equals2.id() != true)    return "Fail equals2"
    if (equals3.id() != false)   return "Fail equals3"
    if (equals4.id() != false)   return "Fail equals4"

    if (toString1.id() != "1")   return "Fail toString1"
    if (toString2.id() != "2")   return "Fail toString2"

    if (limits1.id() != 65535u)         return "Fail limits1"
    if (limits2.id() != 65536u)         return "Fail limits2"
    if (limits3.id() != 4294967295u)    return "Fail limits3"

    return "OK"
}
