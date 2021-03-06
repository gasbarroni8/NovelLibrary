package io.github.gmathi.novellibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import java.util.*
import kotlin.system.exitProcess


class LocaleManager {

    companion object {

        private const val untranslatable = 26
        private const val falsePositive = 67

        private lateinit var stringResourceIds: List<Int>

        private lateinit var stringResourcesEnglish: List<String>

        private val translations: HashMap<String, Int> = HashMap()

        @Synchronized
        fun translated(context: Context, language: String = "en"): Int {
            if (language == SYSTEM_DEFAULT)
                return -1
            if (!translations.containsKey(language)) {
                if (!::stringResourceIds.isInitialized) {
                    val strings = R.string()
                    stringResourceIds = R.string::class.java.fields
                            .filter { it.type.toString() == "int" }
                            .map { it.getInt(strings) }
                }

                if (!::stringResourcesEnglish.isInitialized) {
                    val resources = getResourcesLocale(context) ?: return -1
                    stringResourcesEnglish = stringResourceIds.map { resources.getString(it) }
                }

                if (language == "en")
                    translations["en"] = stringResourcesEnglish.size - untranslatable - falsePositive
                else {
                    val resources = getResourcesLocale(context, language) ?: return -1
                    val stringResources = stringResourceIds.map { resources.getString(it) }
                            .filter { !stringResourcesEnglish.contains(it) }
                    translations[language] = stringResources.size
                }
            }
            return translations[language] ?: -1
        }

        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        private fun getResourcesLocale(context: Context, language: String = "en"): Resources? {
            if (language == SYSTEM_DEFAULT)
                return null
            val locale = Locale(language)
            return if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config).resources
            } else null
        }

        private fun getLanguage(): String {
            return try {
                dataCenter.language
            } catch (e: KotlinNullPointerException) {
                SYSTEM_DEFAULT
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        @Suppress("DEPRECATION")
        fun updateContextLocale(context: Context, language: String = getLanguage()): Context {
            if (language == SYSTEM_DEFAULT)
                return context
            val config = Configuration(context.resources.configuration)
            val locale = Locale(language)
            Locale.setDefault(locale)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                config.setLocale(locale)
                context.createConfigurationContext(config)
            } else {
                val res: Resources = context.resources
                if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
                    config.setLocale(locale)
                } else {
                    config.locale = locale
                }
                res.updateConfiguration(config, res.displayMetrics)
                context
            }
        }

        fun changeLocale(context: Context, language: String) {
            if (dataCenter.language != language) {
                dataCenter.language = language
                val intent = context.packageManager
                        .getLaunchIntentForPackage(context.packageName)!!
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                if (context is AppCompatActivity)
                    context.finish()
                exitProcess(0)
            }
        }
    }
}