package com.ggstudios.tools.datafixer

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class Message(
        val id: String,
        val type: Long,
        val content: String,
        val channelId: String,
        val author: Author,
        val attachments: List<Attachment>,
        val embeds: List<Embed>,
        val mentions: List<Mention>,
        val pinned: Boolean,
        val mentionEveryone: Boolean,
        val tts: Boolean,
        val timestamp: String,
        val editedTimestamp: String?,
        val flags: Long,

        @Transient
        var ts: LocalDateTime
)

class Mention(
        val id: String,
        val username: String,
        val avatar: String,
        val discriminator: String
)

class Embed(
        val type: String,
        val url: String,
        val title: String
// class is incomplete...
/*
				"type": "gifv",
				"url": "https://tenor.com/view/small-ken-jeong-community-too-small-to-read-read-gif-5494204",
				"title": "Too small to read - Small",
				"provider": {
					"name": "Tenor",
					"url": "https://tenor.co"
				},
				"thumbnail": {
					"url": "https://media.tenor.co/images/3287bc26c9cd4f83aa0ab6ecebb3ece5/raw",
					"proxy_url": "https://images-ext-2.discordapp.net/external/EnG3IxJpjsCcQ3vmyFxcQ49jBRPlDJWEcqt-Foh8Fqg/https/media.tenor.co/images/3287bc26c9cd4f83aa0ab6ecebb3ece5/raw",
					"width": 498,
					"height": 350
				},
				"video": {
					"url": "https://media.tenor.co/videos/a552983cc3f24ad0c7f8f27f1ef998b6/mp4",
					"width": 498,
					"height": 350
				}*/
)

class Author(
        val id: String,
        val username: String,
        val avatar: String,
        val discriminator: String
)

class Attachment(
        val id: String,
        val filename: String,
        val size: Long,
        val url: String,
        val proxyUrl: String,
        val width: Long,
        val height: Long
)

class RandLol {

    val fileTag = "ssreal"
    val fileStartIndex = 0

    fun dothing() {
        doStage2()
    }

    fun readFileAsLinesUsingBufferedReader(fileName: String): List<String>
            = File(fileName).bufferedReader().readLines()

    fun doStage2() {
        println("Reading in messages...")

        val allMessages = ArrayList<Message>()

        for (i in 0..17) {
            val texts = readFileAsLinesUsingBufferedReader("${fileTag}-${i}.txt")
            if (texts.size != 1) throw RuntimeException("Unexpected input")

            val text = texts.first()

            val gson = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()

            val messages = gson.fromJson<ArrayList<Message>>(text, object : TypeToken<ArrayList<Message>>() {}.type)
            allMessages.addAll(messages)
        }
        println("Done reading messages...")
        println("Processing...")

        //process messages
        allMessages.forEach {
            it.ts = LocalDateTime.parse(it.timestamp, DateTimeFormatter.ISO_DATE_TIME)
        }

        // calculate messages sent each month, each day

        val idToAuthor = HashMap<String, Author>()

        class Stats(
                val label: String,
                val personCount: HashMap<String, Int> = HashMap<String, Int>(),
                var totalCount: Int = 0,
                val hourlyStats: HashMap<Int, Int> = HashMap<Int, Int>(),
                var longestMessage: Message? = null
        ) {
            fun addMessage(m: Message) {
                if (!idToAuthor.containsKey(m.author.id)) {
                    idToAuthor[m.author.id] = m.author
                }
                val count = personCount.getOrElse(m.author.id) { 0 }
                personCount[m.author.id] = count + 1
                totalCount++

                val hstat = hourlyStats.getOrElse(m.ts.hour) { 0 }
                hourlyStats[m.ts.hour] = hstat + 1

                if (m.content.length > longestMessage?.content?.length ?: 0) {
                    longestMessage = m
                }
            }

            fun getLoudMouth(): String {
                return personCount.entries.map { it }.sortedByDescending { it.value }.first().let {
                    val author = idToAuthor[it.key]
                    if (author != null) {
                        "${author.username}#${author.discriminator}"
                    } else {
                        "UNKNOWN"
                    } + " (${it.value})"
                }
            }

            override fun toString(): String {
                return StringBuilder().apply {
                    append("\n")
                    append("${label}\n")
                    append("####\n")
                    append("####\n")
                    append("Top 10 actives\n")
                    append("##############\n")
                    personCount.entries.map { it }.sortedByDescending { it.value }/*.take(10)*/.forEach {
                        val author = idToAuthor[it.key]
                        val authorStr = if (author != null) {
                            "${author.username}#${author.discriminator}"
                        } else {
                            "UNKNOWN"
                        }
                        append(authorStr + " (${it.value})\n")
                    }
                    append("##############\n")
                    append("Total\n")
                    append("#####\n")
                    append("${totalCount}\n")
                    append("#####\n")
                    append("Hourly\n")
                    hourlyStats.entries.sortedBy { it.key }.forEach {
                        append("${it.key},${it.value}\n")
                    }

                    append("Longest message by: " + longestMessage?.author?.username)
                    append(longestMessage?.content)
                }.toString()
            }
        }

        var lastYear = -1
        var lastMonth = -1
        var lastDayOfYear = -1

        val totalStats = Stats("total")
        val monthlyStats = ArrayList<Stats>()
        val dailyStats = ArrayList<Stats>()
        val mentioned = hashSetOf<String>()

        allMessages.forEach {
            if (lastYear != it.ts.year) {
                lastDayOfYear = -1
                lastMonth = -1
                lastYear = it.ts.year
            }
            if (lastMonth != it.ts.monthValue) {
                monthlyStats.add(Stats("${it.ts.month}-${it.ts.year}"))
                lastMonth = it.ts.monthValue
            }
            if (lastDayOfYear != it.ts.dayOfYear) {
                dailyStats.add(Stats("${it.ts.dayOfYear}-${it.ts.year}"))
                lastDayOfYear = it.ts.dayOfYear
            }

            totalStats.addMessage(it)
            monthlyStats.last().addMessage(it)
            dailyStats.last().addMessage(it)

            it.mentions.forEach {
                mentioned.add("${it.username}#${it.discriminator}")
            }
        }
        println("Done!")

//        println("Monthly")
//        monthlyStats.forEach {
//            println("${it.label},${it.totalCount},${it.getLoudMouth()}")
//        }
//
//        println("Daily")
//        dailyStats.forEach {
//            println("${it.label},${it.totalCount},${it.getLoudMouth()}")
//        }

        println("All")
        println(totalStats.toString())

        totalStats.personCount.entries.map { it }.forEach {
            val author = idToAuthor[it.key]
            val authorStr = if (author != null) {
                "${author.username}#${author.discriminator}"
            } else {
                "UNKNOWN"
            }
            mentioned.remove(authorStr)
        }
        val other = buildString {
            mentioned.forEach {
                append(it)
                append("\n")
            }
        }

        val writer = FileWriter("stats.txt")
        writer.write(totalStats.toString() + "\n\nMenionted but did not send a message:\n" + other)
        writer.flush()
        writer.close()
    }

    fun doStage1() {
        // downloads all messages from a particular channel and saves them
        val gson = GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()

        val allMessages = ArrayList<Message>()

        var lastMessageId = "669740256936984586"

        val threshold = 100000
        var count = 0L
        var files = 0

        while(true) {
            val result = makeReq("https://discordapp.com/api/v6/channels/271486833223794694/messages?limit=100&before=${lastMessageId}")

            val messages = gson.fromJson<ArrayList<Message>>(result, object : TypeToken<ArrayList<Message>>() {}.type)
            allMessages.addAll(messages)

            count += messages.size

            lastMessageId = messages.last().id

            if (messages.size < 100) {
                break
            }

            println("Sleeping... Messages: ${count}")

            if (allMessages.size > threshold) {
                writeMessages(allMessages, (fileStartIndex + files).toString())
                files++

                allMessages.clear()
            }
        }
    }

    private fun writeMessages(allMessages: ArrayList<Message>, tag: String) {
        println("Writing messages...")
        val gson = GsonBuilder().create()

        val writer = FileWriter("${fileTag}-${tag}.txt")
        writer.append(gson.toJson(allMessages))
        writer.flush()
        writer.close()
        println("Done writing messages...")
    }

    fun makeReq(url: String): String {
        val con = URL(url).openConnection() as HttpURLConnection
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2")
        con.setRequestProperty("authorization", "NTExNjkyOTI3NDg4MjI5Mzg3.DzmW2Q.UDaWdQF7HkyGXGq9_Gh1QY-QJnk")

        // optional default is GET
        con.requestMethod = "GET"

        val responseCode = con.responseCode
        //System.out.println("\nSending 'GET' request to URL : " + url);
        //System.out.println("Response Code : " + responseCode);

        when {
            responseCode == HttpURLConnection.HTTP_OK -> {
                val result = BufferedReader(InputStreamReader(con.inputStream)).use { it.readText() }
                return result
            }
            else -> throw RuntimeException("asdf")
        }
    }
}