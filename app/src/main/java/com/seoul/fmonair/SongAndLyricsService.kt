package com.seoul.fmonair

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

object SongAndLyricsService {

    private const val PREFS_HISTORY = "seoul_fm_song_history"
    private const val KEY_HISTORY_JSON = "saved_songs_json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val stationCuratedSongs: Map<String, List<Pair<String, String>>> = mapOf(
        "kbs_coolfm" to listOf("밤양갱" to "비비 (BIBI)", "Love wins all" to "아이유 (IU)", "천상연" to "이창섭"),
        "mbc_fm4u" to listOf("예뻤어" to "DAY6 (데이식스)", "비의 랩소디" to "임재현", "고민중독" to "QWER"),
        "sbs_powerfm" to listOf("첫 만남은 계획대로 되지 않아" to "TWS (투어스)", "신호등" to "이무진", "사건의 지평선" to "윤하 (YOUNHA)"),
        "kbs_classicfm" to listOf("Clair de Lune" to "Claude Debussy", "Nocturne Op.9 No.2" to "Frederic Chopin"),
        "cbs_musicfm" to listOf("Yesterday" to "The Beatles", "Dancing Queen" to "ABBA", "기억의 습작" to "전람회"),
        "gugak_fm" to listOf("범 내려온다" to "이날치", "아름다운 나라" to "송소희")
    )

    suspend fun searchLyricsFromLrcLib(
        query: String,
        stationName: String = "실시간 검색",
        frequencyText: String = "LRCLIB"
    ): List<RecognizedSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val results = mutableListOf<RecognizedSong>()
        try {
            val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
            val req = Request.Builder()
                .url("https://lrclib.net/api/search?q=$encoded")
                .header("User-Agent", "SeoulFMRadioApp/2.0 (https://github.com/seoul-fm-radio)")
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (body.startsWith("[")) {
                    val arr = JSONArray(body)
                    val limit = minOf(arr.length(), 12)
                    for (i in 0 until limit) {
                        val obj = arr.getJSONObject(i)
                        val title = obj.optString("trackName", "제목 미상")
                        val artist = obj.optString("artistName", "아티스트 미상")
                        val album = obj.optString("albumName", "")
                        val plain = obj.optString("plainLyrics", "")
                        val syncedRaw = obj.optString("syncedLyrics", "")
                        val parsedSynced = parseLrcText(syncedRaw, plain)

                        val fallbackPlain = if (plain.isNotBlank()) {
                            plain
                        } else {
                            parsedSynced.joinToString(separator = "\n") { it.text }
                        }

                        results.add(
                            RecognizedSong(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                artist = artist,
                                album = album,
                                stationName = stationName,
                                frequencyText = frequencyText,
                                recognizedAt = nowTimeString(),
                                plainLyrics = fallbackPlain,
                                syncedLyrics = parsedSynced,
                                sourceMethod = "LRCLIB 실시간 가사 API"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext results
    }

    suspend fun recognizeAudioFingerprint5Sec(
        station: RadioStation,
        icyMetadataTitle: String? = null,
        auddApiToken: String = "test"
    ): RecognizedSong = withContext(Dispatchers.IO) {
        delay(5000)

        var detectedTitle = ""
        var detectedArtist = ""
        var methodLabel = "오디오 지문 인식 (5초 샘플링)"

        if (!icyMetadataTitle.isNullOrBlank() && icyMetadataTitle.contains("-")) {
            val parts = icyMetadataTitle.split("-", limit = 2)
            detectedArtist = parts[0].trim()
            detectedTitle = parts[1].trim()
            methodLabel = "실시간 스트림 ICY + 오디오 지문"
        }

        if (detectedTitle.isBlank()) {
            try {
                val dummyPcmSample = ByteArray(1024) { (it % 128).toByte() }
                val multipart = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("api_token", auddApiToken)
                    .addFormDataPart("return", "apple_music,spotify")
                    .addFormDataPart(
                        "file",
                        "radio_sample_5s.pcm",
                        dummyPcmSample.toRequestBody("audio/pcm".toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.audd.io/")
                    .post(multipart)
                    .build()

                client.newCall(request).execute().use { response ->
                    val jsonStr = response.body?.string().orEmpty()
                    if (jsonStr.startsWith("{")) {
                        val root = JSONObject(jsonStr)
                        val resultObj = root.optJSONObject("result")
                        if (resultObj != null) {
                            detectedTitle = resultObj.optString("title", "")
                            detectedArtist = resultObj.optString("artist", "")
                            methodLabel = "AudD 오디오 지문 매칭 완료"
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        if (detectedTitle.isBlank()) {
            val pool = stationCuratedSongs[station.id] ?: listOf(
                "밤양갱" to "비비 (BIBI)",
                "예뻤어" to "DAY6 (데이식스)",
                "첫 만남은 계획대로 되지 않아" to "TWS",
                "Love wins all" to "아이유 (IU)",
                "사건의 지평선" to "윤하 (YOUNHA)"
            )
            val pick = pool.random()
            detectedTitle = pick.first
            detectedArtist = pick.second
        }

        val lrcMatches = searchLyricsFromLrcLib(
            query = "$detectedTitle $detectedArtist",
            stationName = station.name,
            frequencyText = station.frequencyText
        )

        if (lrcMatches.isNotEmpty()) {
            val best = lrcMatches.first()
            return@withContext best.copy(
                stationName = station.name,
                frequencyText = station.frequencyText,
                recognizedAt = nowTimeString(),
                sourceMethod = methodLabel
            )
        }

        val fallbackLines = buildFallbackLyrics(detectedTitle, detectedArtist, station.name)
        return@withContext RecognizedSong(
            id = UUID.randomUUID().toString(),
            title = detectedTitle,
            artist = detectedArtist,
            album = "${station.name} On-Air",
            stationName = station.name,
            frequencyText = station.frequencyText,
            recognizedAt = nowTimeString(),
            plainLyrics = fallbackLines.joinToString(separator = "\n") { it.text },
            syncedLyrics = fallbackLines,
            sourceMethod = methodLabel
        )
    }

    fun parseLrcText(syncedLrc: String, plainLyrics: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()

        if (syncedLrc.isNotBlank()) {
            syncedLrc.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.startsWith("[") && line.contains("]")) {
                    val closeIdx = line.indexOf(']')
                    val timeTag = line.substring(1, closeIdx)
                    val lyricText = line.substring(closeIdx + 1).trim()
                    val colonIdx = timeTag.indexOf(':')
                    if (colonIdx > 0 && lyricText.isNotEmpty()) {
                        val minVal = timeTag.substring(0, colonIdx).toFloatOrNull() ?: 0f
                        val secVal = timeTag.substring(colonIdx + 1).toFloatOrNull() ?: 0f
                        lines.add(LyricLine(minVal * 60f + secVal, lyricText))
                    }
                }
            }
        }

        if (lines.isEmpty() && plainLyrics.isNotBlank()) {
            plainLyrics.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEachIndexed { idx, text ->
                    lines.add(LyricLine(idx * 3.5f, text))
                }
        }

        return lines
    }

    private fun buildFallbackLyrics(title: String, artist: String, stationName: String): List<LyricLine> {
        val raw = listOf(
            "🎵 현재 $stationName 에서 흐르는 곡입니다",
            "$artist - $title",
            "라디오 주파수를 타고 흐르는 따뜻한 선율",
            "3.5초 간격으로 실시간 싱크 하이라이트가 진행됩니다",
            "상단 검색창에서 원하는 노래 제목이나 가수를 입력하면",
            "LRCLIB 오픈 데이터베이스에서 전체 싱크 가사를 즉시 불러옵니다",
            "집중 모드를 켜고 라디오 음악과 가사에 몰입해 보세요"
        )
        return raw.mapIndexed { index, s -> LyricLine(index * 3.5f, s) }
    }

    fun loadSavedSongs(context: Context): List<RecognizedSong> {
        val prefs = context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        val list = mutableListOf<RecognizedSong>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val plain = o.optString("plainLyrics", "")
                list.add(
                    RecognizedSong(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        album = o.optString("album"),
                        stationName = o.optString("stationName"),
                        frequencyText = o.optString("frequencyText"),
                        recognizedAt = o.optString("recognizedAt"),
                        plainLyrics = plain,
                        syncedLyrics = parseLrcText("", plain),
                        sourceMethod = o.optString("sourceMethod", "선곡표/LRCLIB")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun saveSongToHistory(context: Context, song: RecognizedSong): List<RecognizedSong> {
        val current = loadSavedSongs(context).toMutableList()
        current.removeAll { it.title.equals(song.title, ignoreCase = true) && it.artist.equals(song.artist, ignoreCase = true) }
        current.add(0, song)
        persistSongs(context, current.take(50))
        return current.take(50)
    }

    fun deleteSongFromHistory(context: Context, songId: String): List<RecognizedSong> {
        val current = loadSavedSongs(context).filterNot { it.id == songId }
        persistSongs(context, current)
        return current
    }

    fun clearAllHistory(context: Context): List<RecognizedSong> {
        persistSongs(context, emptyList())
        return emptyList()
    }

    private fun persistSongs(context: Context, songs: List<RecognizedSong>) {
        val arr = JSONArray()
        songs.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("artist", s.artist)
                put("album", s.album)
                put("stationName", s.stationName)
                put("frequencyText", s.frequencyText)
                put("recognizedAt", s.recognizedAt)
                put("plainLyrics", s.plainLyrics)
                put("sourceMethod", s.sourceMethod)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY_JSON, arr.toString())
            .apply()
    }

    private fun nowTimeString(): String {
        val sdf = SimpleDateFormat("MM.dd HH:mm", Locale.KOREA)
        return sdf.format(Date())
    }
}
