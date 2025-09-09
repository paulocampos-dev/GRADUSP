package com.prototype.gradusp.data.parser

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException

/**
 * Base interface for all USP data crawlers.
 * Provides common functionality and error handling.
 */
interface BaseCrawler {
    val client: OkHttpClient
    val tag: String

    /**
     * Fetches a document from the given URL with proper error handling
     */
    suspend fun fetchDocument(url: String): Document? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(tag, "HTTP error ${response.code} for URL: $url")
                    return null
                }
                Jsoup.parse(response.body?.string() ?: "")
            }
        } catch (e: IOException) {
            Log.e(tag, "Network error fetching document from $url", e)
            null
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error fetching document from $url", e)
            null
        }
    }

    /**
     * Safely extracts text from an element, returning empty string if null
     */
    fun safeText(element: org.jsoup.nodes.Element?): String {
        return element?.text()?.trim() ?: ""
    }

    /**
     * Safely extracts attribute value from an element
     */
    fun safeAttr(element: org.jsoup.nodes.Element?, attribute: String): String {
        return element?.attr(attribute)?.trim() ?: ""
    }
}

/**
 * Result wrapper for crawler operations
 */
sealed class CrawlerResult<out T> {
    data class Success<T>(val data: T) : CrawlerResult<T>()
    data class Error(val message: String, val cause: Exception? = null) : CrawlerResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> default
    }

    fun <R> map(transform: (T) -> R): CrawlerResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    fun onSuccess(action: (T) -> Unit): CrawlerResult<T> {
        if (this is Success) action(data)
        return this
    }

    fun onError(action: (String, Exception?) -> Unit): CrawlerResult<T> {
        if (this is Error) action(message, cause)
        return this
    }
}
