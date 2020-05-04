package com.ggstudios.tools.datafixer

import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HttpClient {

    private val memcache = HashMap<String, String>()

    @Throws(IOException::class, JSONException::class)
    fun makeRequestRaw(rawUrl: String, retries: Int): String? {
        if (memcache.containsKey(rawUrl)) {
            //Log.d(TAG, "Request cached for: " + rawUrl);
            return memcache[rawUrl]
        }
        val url = URL(rawUrl)

        System.setProperty("http.agent", "")

        //System.out.println(url.toString());

        val con = url.openConnection() as HttpURLConnection
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")

        // optional default is GET
        con.requestMethod = "GET"

        val responseCode = con.responseCode
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        when {
            responseCode == HttpURLConnection.HTTP_OK -> {
                val result = BufferedReader(InputStreamReader(con.inputStream)).use { it.readText() }
                memcache[rawUrl] = result
                return result
            }
            responseCode / 100 == 5 -> {
                System.err.println("Server error: $responseCode for url: $url")
                return if (retries <= 2) {
                    // retry...
                    System.err.println("Sleeping for a bit and then retrying...")

                    try {
                        Thread.sleep((1000 * retries).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    makeRequestRaw(rawUrl, retries + 1)
                } else {
                    System.err.println("Retry limit exceeded... failing...")
                    null
                }
            }
            responseCode == 429 -> { // rate limit hit
                // technically all of the static endpoints we call shouldn't be rate
                val timeToWait = Integer.valueOf(con.headerFields["Retry-After"]?.get(0))

                System.err.println("Rate limit exceeded: $responseCode for url: $url. Waiting: $timeToWait")
                System.err.println(con.headerFields)
                RuntimeException().printStackTrace()


                throw RuntimeException()
            }
            else -> {
                System.err.println("Unexpected response code: $responseCode for url: $url")
                return null
            }
        }
    }
}
