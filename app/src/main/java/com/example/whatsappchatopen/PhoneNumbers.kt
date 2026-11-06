package com.example.whatsappchatopen

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.ValidationResult
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Phone number parsing and validation, backed by libphonenumber so that every country's
 * own rules (number lengths, national prefixes such as a leading 0, …) are respected.
 */
object PhoneNumbers {

    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()
    private const val UNKNOWN_REGION = "ZZ"

    // wa.me/919876543210 and api.whatsapp.com/send?phone=919876543210
    private val waLink = Regex("""(?:wa\.me/|[?&]phone=)\+?(\d{5,})""", RegexOption.IGNORE_CASE)

    data class Country(val regionCode: String, val name: String, val callingCode: Int) {
        val flag: String = regionCode.uppercase(Locale.ROOT)
            .map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }
            .joinToString("")

        fun matches(query: String): Boolean {
            val q = query.trim()
            if (q.isEmpty()) return true
            return name.contains(q, ignoreCase = true) ||
                regionCode.equals(q, ignoreCase = true) ||
                q.removePrefix("+").let { it.isNotEmpty() && it.all(Char::isDigit) && callingCode.toString().startsWith(it) }
        }
    }

    /** A number split into the country it belongs to and the digits to show in the input box. */
    data class Extracted(val regionCode: String, val nationalNumber: String)

    enum class Status { EMPTY, TOO_SHORT, TOO_LONG, WRONG_LENGTH, UNLIKELY, VALID }

    data class Check(
        val status: Status,
        val digitCount: Int,
        /** Country code + national number, i.e. what goes after https://wa.me/ */
        val waMeNumber: String?,
    )

    fun isSupported(regionCode: String): Boolean = regionCode in util.supportedRegions

    fun country(regionCode: String, locale: Locale = Locale.getDefault()): Country {
        val name = Locale.Builder().setRegion(regionCode).build().getDisplayCountry(locale)
        return Country(regionCode, name.ifBlank { regionCode }, util.getCountryCodeForRegion(regionCode))
    }

    fun countries(locale: Locale = Locale.getDefault()): List<Country> {
        val collator = Collator.getInstance(locale)
        return util.supportedRegions.map { country(it, locale) }.sortedWith(compareBy(collator) { it.name })
    }

    /**
     * Pulls a phone number out of pasted, copied or shared text. A number carrying its own country
     * code ("+44 20…", "0044 20…", "4420…", wa.me links) moves to that country; otherwise the number
     * is read as a local number of [currentRegion].
     *
     * With [strict], only numbers that are valid per libphonenumber's metadata are accepted, so random
     * clipboard content such as OTPs or amounts is not mistaken for a phone number.
     */
    fun extract(text: String, currentRegion: String, strict: Boolean): Extracted? {
        val cleaned = text.trim().let { t -> waLink.find(t)?.let { "+" + it.groupValues[1] } ?: t }
        if (cleaned.none(Char::isDigit)) return null

        val candidates = sequence {
            parseOrNull(cleaned, currentRegion)?.let { yield(it) }
            // Country code written without "+", e.g. "919876543210" or "0049301234567"
            if (!cleaned.contains('+') && cleaned.none(Char::isLetter)) {
                val digits = cleaned.filter(Char::isDigit).removePrefix("00")
                parseOrNull("+$digits", UNKNOWN_REGION)?.let { yield(it) }
            }
            // Longer text that merely contains a number ("call me on +1 650 253 0000 tomorrow")
            if (cleaned.length <= 1000) {
                util.findNumbers(cleaned, currentRegion).firstOrNull()?.let { yield(it.number()) }
            }
        }.toList()

        val number = candidates.firstOrNull(util::isValidNumber)
            ?: if (strict) null else candidates.firstOrNull { util.isPossibleNumber(it) }
        return number?.let { toExtracted(it, currentRegion) }
    }

    fun check(regionCode: String, input: String): Check {
        if (input.none(Char::isDigit)) return Check(Status.EMPTY, 0, null)
        val number = parseOrNull(input, regionCode)
            ?: return Check(Status.TOO_SHORT, input.count(Char::isDigit), null)
        val national = util.getNationalSignificantNumber(number)
        val status = when (util.isPossibleNumberWithReason(number)) {
            ValidationResult.IS_POSSIBLE ->
                if (util.isValidNumber(number)) Status.VALID else Status.UNLIKELY
            ValidationResult.TOO_LONG -> Status.TOO_LONG
            ValidationResult.INVALID_LENGTH -> Status.WRONG_LENGTH
            else -> Status.TOO_SHORT // TOO_SHORT, IS_POSSIBLE_LOCAL_ONLY, INVALID_COUNTRY_CODE
        }
        val openable = status == Status.VALID || status == Status.UNLIKELY
        return Check(status, national.length, if (openable) "${number.countryCode}$national" else null)
    }

    private val lengthCache = ConcurrentHashMap<String, List<Int>>()

    /**
     * The digit counts (without country code) of this country's mobile numbers, which is what
     * WhatsApp numbers almost always are. Falls back to all number types if no mobile data exists.
     */
    fun possibleLengths(regionCode: String): List<Int> = lengthCache.getOrPut(regionCode) {
        val callingCode = util.getCountryCodeForRegion(regionCode)
        fun lengthsFor(type: PhoneNumberUtil.PhoneNumberType) = (2..17).filter { length ->
            val probe = PhoneNumber().setCountryCode(callingCode).setNationalNumber("5".repeat(length).toLong())
            util.isPossibleNumberForTypeWithReason(probe, type) == ValidationResult.IS_POSSIBLE
        }
        lengthsFor(PhoneNumberUtil.PhoneNumberType.MOBILE).ifEmpty { lengthsFor(PhoneNumberUtil.PhoneNumberType.UNKNOWN) }
    }

    /** "10 digits", "5–15 digits", "9 or 10 digits" */
    fun describeLengths(lengths: List<Int>): String = when {
        lengths.isEmpty() -> "a varying number of digits"
        lengths.size == 1 -> "${lengths[0]} digits"
        lengths.size > 2 && lengths.last() - lengths.first() == lengths.size - 1 ->
            "${lengths.first()}–${lengths.last()} digits"
        else -> lengths.dropLast(1).joinToString(", ") + " or ${lengths.last()} digits"
    }

    private fun parseOrNull(text: String, region: String): PhoneNumber? = try {
        util.parse(text, region)
    } catch (_: NumberParseException) {
        null
    }

    private fun toExtracted(number: PhoneNumber, currentRegion: String): Extracted? {
        // Valid numbers resolve to their exact country (e.g. +1 → US or CA); otherwise keep the
        // selected country if it shares the calling code, else fall back to the code's main country.
        val region = util.getRegionCodeForNumber(number)
            ?: if (util.getCountryCodeForRegion(currentRegion) == number.countryCode) currentRegion
            else util.getRegionCodeForCountryCode(number.countryCode)
        // Non-geographic numbers (+800, +882, …) have no country to select.
        if (!isSupported(region)) return null
        return Extracted(region, util.getNationalSignificantNumber(number))
    }
}
