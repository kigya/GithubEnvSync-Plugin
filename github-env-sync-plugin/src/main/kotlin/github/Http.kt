package github

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

private const val CONNECT_TIMEOUT_MILLIS = 15_000

private const val READ_TIMEOUT_MILLIS = 30_000

internal object Http {

    fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
        }

        return connection.useResponse()
    }

    fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
    ): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
        }

        connection.outputStream.use {
            it.write(body.toByteArray(StandardCharsets.UTF_8))
        }

        return connection.useResponse()
    }

    private fun HttpURLConnection.useResponse(): HttpResponse {
        val stream = try {
            inputStream
        } catch (_: Exception) {
            errorStream
        }

        val text = stream?.readUtf8().orEmpty()
        return HttpResponse(code = responseCode, body = text)
    }

    private fun InputStream.readUtf8(): String = BufferedReader(
        /* input reader */ InputStreamReader(
            /* input stream */ this,
            /* charset */ StandardCharsets.UTF_8,
        ),
    ).use(BufferedReader::readText)
}
