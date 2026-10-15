package com.spamsentinel.app

import java.io.File
import java.io.InputStream

object FileStreamHelper {
    fun getStream(fileName: String): InputStream {
        val classLoader = FileStreamHelper::class.java.classLoader
        classLoader?.getResourceAsStream(fileName)?.let { return it }

        val possiblePaths = listOf(
            "app/src/main/assets/$fileName",
            "src/main/assets/$fileName",
            "../app/src/main/assets/$fileName"
        )
        for (path in possiblePaths) {
            val f = File(path)
            if (f.exists()) {
                return f.inputStream()
            }
        }
        error("Asset file $fileName not found in resources or file paths!")
    }
}
