package com.getakyra.app.utils

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

object ConfigReader {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun loadConfigFromAssets(context: Context, fileName: String): ConfigMigrator.StoreConfig? {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            val jsonString = reader.readText()
            reader.close()
            jsonParser.decodeFromString<ConfigMigrator.StoreConfig>(jsonString)
        } catch (e: Exception) {
            android.util.Log.e("CONFIG_READER", "Failed to load config file: $fileName", e)
            null
        }
    }
}
