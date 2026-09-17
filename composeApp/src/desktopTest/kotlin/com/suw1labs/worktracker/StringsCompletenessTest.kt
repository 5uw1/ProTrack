package com.suw1labs.worktracker

import com.suw1labs.worktracker.ui.i18n.AppStrings
import com.suw1labs.worktracker.ui.i18n.Translations
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * AppStrings uses lateinit properties (the JVM caps constructors at 255 parameters), so a text
 * missing from one language would only surface as a crash on that screen. This walks every
 * getter for every language instead.
 */
class StringsCompletenessTest {
    @Test
    fun everyLanguageDefinesEveryText() {
        val getters = AppStrings::class.java.methods
            .filter { it.declaringClass == AppStrings::class.java && it.parameterCount == 0 && it.name.startsWith("get") }
        assertTrue(getters.size > 200, "expected the full set of texts, found ${getters.size}")
        for (strings in listOf(Translations.EN, Translations.DE, Translations.FR)) {
            for (getter in getters) {
                val value = try {
                    getter.invoke(strings)
                } catch (e: InvocationTargetException) {
                    fail("${strings.language}: ${getter.name} is not set (${e.cause?.message})")
                }
                if (value is String) assertTrue(value.isNotBlank(), "${strings.language}: ${getter.name} is blank")
            }
        }
    }
}
