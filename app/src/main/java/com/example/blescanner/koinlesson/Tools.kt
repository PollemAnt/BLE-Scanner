package com.example.blescanner.koinlesson

import android.content.Context

class JsonParser {
    fun parse(rawJson: String): List<String> {
        // Udajemy parsowanie
        return rawJson.split(",")
    }
}

class FileReader(private val context: Context, private val parser: JsonParser) {
    fun readFakeFile(): List<String> {
        val fakeRaw = "one,two,three"
        return parser.parse(fakeRaw)
    }
}