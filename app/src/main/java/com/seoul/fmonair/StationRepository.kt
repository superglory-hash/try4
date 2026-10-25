package com.seoul.fmonair

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object StationRepository {

    private const val PREFS_NAME = "seoul_fm_radio_prefs"
    private const val KEY_FAVORITES = "favorite_station_ids"
    private const val KEY_CUSTOM_URL_PREFIX = "custom_url_"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val initialStations: List<RadioStation> = listOf(
        RadioStation(
            id = "kbs_1radio",
            name = "KBS 1라디오",
            broadcaster = "KBS",
            frequencyMhz = 97.3f,
            frequencyText = "97.3 MHz",
            category = StationCategory.KBS,
            badgeColorHex = 0xFF2563EB,
            resolverType = StreamResolverType.KBS_API,
            endpointOrStreamUrl = "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/21",
            channelCode = "21",
            defaultProgram = "KBS 뉴스 & 시사 포커스",
            scheduleHours = "24시간 시사/뉴스 LIVE",
            playlistWebUrl = "https://program.kbs.co.kr/1radio/radio/main.html"
        ),
        RadioStation(
            id = "kbs_happyfm",
            name = "KBS 해피FM",
            broadcaster = "KBS",
            frequencyMhz = 106.1f,
            frequencyText = "106.1 MHz",
            category = StationCategory.KBS,
            badgeColorHex = 0xFF3B82F6,
            resolverType = StreamResolverType.KBS_API,
            endpointOrStreamUrl = "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/22",
            channelCode = "22",
            defaultProgram = "행복한 대중음악 채널 해피FM",
            scheduleHours = "05:00 ~ 03:00 대중가요",
            playlistWebUrl = "https://program.kbs.co.kr/2radio/radio/main.html"
        ),
        RadioStation(
            id = "kbs_coolfm",
            name = "KBS 쿨FM",
            broadcaster = "KBS",
            frequencyMhz = 89.1f,
            frequencyText = "89.1 MHz",
            category = StationCategory.KBS,
            badgeColorHex = 0xFF06B6D4,
            resolverType = StreamResolverType.KBS_API,
            endpointOrStreamUrl = "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/25",
            channelCode = "25",
            defaultProgram = "가요광장 / 볼륨을 높여요",
            scheduleHours = "24시간 K-POP & 팝",
            playlistWebUrl = "https://program.kbs.co.kr/2fm/radio/main.html"
        ),
        RadioStation(
            id = "kbs_classicfm",
            name = "KBS 클래식FM",
            broadcaster = "KBS",
            frequencyMhz = 93.1f,
            frequencyText = "93.1 MHz",
            category = StationCategory.KBS,
            badgeColorHex = 0xFF6366F1,
            resolverType = StreamResolverType.KBS_API,
            endpointOrStreamUrl = "https://cfpwwwapi.kbs.co.kr/api/v1/landing/live/channel_code/24",
            channelCode = "24",
            defaultProgram = "출발 FM과 함께 / 세상의 모든 음악",
            scheduleHours = "24시간 정통 클래식",
            playlistWebUrl = "https://program.kbs.co.kr/1fm/radio/main.html"
        ),
        RadioStation(
            id = "mbc_sfm",
            name = "MBC 표준FM",
            broadcaster = "MBC",
            frequencyMhz = 95.9f,
            frequencyText = "95.9 MHz",
            category = StationCategory.MBC,
            badgeColorHex = 0xFF8B5CF6,
            resolverType = StreamResolverType.MBC_API,
            endpointOrStreamUrl = "https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel=sfm",
            channelCode = "sfm",
            defaultProgram = "시선집중 / 여성시대 / 별이 빛나는 밤에",
            scheduleHours = "24시간 종합 시사·오락",
            playlistWebUrl = "https://www.imbc.com/broad/radio/"
        ),
        RadioStation(
            id = "mbc_fm4u",
            name = "MBC FM4U",
            broadcaster = "MBC",
            frequencyMhz = 91.9f,
            frequencyText = "91.9 MHz",
            category = StationCategory.MBC,
            badgeColorHex = 0xFFA855F7,
            resolverType = StreamResolverType.MBC_API,
            endpointOrStreamUrl = "https://sminiplay.imbc.com/aacplay.ashx?agent=webapp&channel=mfm",
            channelCode = "mfm",
            defaultProgram = "굿모닝FM / 정오의 희망곡 / 배철수의 음악캠프",
            scheduleHours = "24시간 논스톱 뮤직",
            playlistWebUrl = "https://www.imbc.com/broad/radio/"
        ),
        RadioStation(
            id = "sbs_powerfm",
            name = "SBS 파워FM",
            broadcaster = "SBS",
            frequencyMhz = 107.7f,
            frequencyText = "107.7 MHz",
            category = StationCategory.SBS,
            badgeColorHex = 0xFFF97316,
            resolverType = StreamResolverType.SBS_API,
            endpointOrStreamUrl = "https://apis.sbs.co.kr/play-api/1.0/livestream/powerpc/powerfm?protocol=hls&ssl=Y",
            channelCode = "powerfm",
            defaultProgram = "김영철의 파워FM / 두시탈출 컬투쇼",
            scheduleHours = "24시간 뮤직 & 엔터",
            playlistWebUrl = "https://www.sbs.co.kr/radio"
        ),
        RadioStation(
            id = "sbs_lovefm",
            name = "SBS 러브FM",
            broadcaster = "SBS",
            frequencyMhz = 103.5f,
            frequencyText = "103.5 MHz",
            category = StationCategory.SBS,
            badgeColorHex = 0xFFEA580C,
            resolverType = StreamResolverType.SBS_API,
            endpointOrStreamUrl = "https://apis.sbs.co.kr/play-api/1.0/livestream/lovepc/lovefm?protocol=hls&ssl=Y",
            channelCode = "lovefm",
            defaultProgram = "이숙영의 러브FM / 뜨거우면 지상렬",
            scheduleHours = "24시간 시사 & 7080 가요",
            playlistWebUrl = "https://www.sbs.co.kr/radio"
        ),
        RadioStation(
            id = "ebs_fm",
            name = "EBS FM",
            broadcaster = "EBS",
            frequencyMhz = 104.5f,
            frequencyText = "104.5 MHz",
            category = StationCategory.NEWS_EDU,
            badgeColorHex = 0xFF10B981,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://ebsonair.ebs.co.kr/fmradiofamilypc/familypc1m/playlist.m3u8",
            defaultProgram = "모닝 스페셜 / 세상의 모든 지혜",
            scheduleHours = "05:00 ~ 02:00 어학·교양",
            playlistWebUrl = "https://www.ebs.co.kr/radio/home"
        ),
        RadioStation(
            id = "tbs_fm",
            name = "TBS FM",
            broadcaster = "TBS",
            frequencyMhz = 95.1f,
            frequencyText = "95.1 MHz",
            category = StationCategory.NEWS_EDU,
            badgeColorHex = 0xFF0EA5E9,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://cdnfm.tbs.seoul.kr/tbs/_definst_/tbs_fm_web_360.smil/playlist.m3u8",
            defaultProgram = "수도권 교통정보 & 시민 라디오",
            scheduleHours = "24시간 수도권 교통·음악",
            playlistWebUrl = "https://tbs.seoul.kr"
        ),
        RadioStation(
            id = "tbs_efm",
            name = "TBS eFM",
            broadcaster = "TBS",
            frequencyMhz = 101.3f,
            frequencyText = "101.3 MHz",
            category = StationCategory.NEWS_EDU,
            badgeColorHex = 0xFF0284C7,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://cdnefm.tbs.seoul.kr/tbs/_definst_/tbs_efm_web_360.smil/playlist.m3u8",
            defaultProgram = "Seoul Foreign Language Radio",
            scheduleHours = "24시간 영어·외국어 방송",
            playlistWebUrl = "https://tbs.seoul.kr/efm"
        ),
        RadioStation(
            id = "ytn_radio",
            name = "YTN 라디오",
            broadcaster = "YTN",
            frequencyMhz = 94.5f,
            frequencyText = "94.5 MHz",
            category = StationCategory.NEWS_EDU,
            badgeColorHex = 0xFFEF4444,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://radiolive.ytn.co.kr/radio/_definst_/20211118_fmlive/playlist.m3u8",
            defaultProgram = "YTN 24시간 뉴스 FM",
            scheduleHours = "24시간 실시간 속보·시사",
            playlistWebUrl = "https://radio.ytn.co.kr"
        ),
        RadioStation(
            id = "gugak_fm",
            name = "국악방송 FM",
            broadcaster = "국악",
            frequencyMhz = 99.1f,
            frequencyText = "99.1 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFFD97706,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://mgugaklive.nowcdn.co.kr/gugakradio/gugakradio.stream/playlist.m3u8",
            defaultProgram = "창호에 드리는 햇살 / 우리 소리",
            scheduleHours = "24시간 전통·퓨전 국악",
            playlistWebUrl = "https://www.igbf.kr"
        ),
        RadioStation(
            id = "cbs_sfm",
            name = "CBS 표준FM",
            broadcaster = "CBS",
            frequencyMhz = 98.1f,
            frequencyText = "98.1 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFF14B8A6,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://maac.cbs.co.kr/cbs981/_definst_/cbs981.stream/playlist.m3u8",
            defaultProgram = "김현정의 뉴스쇼 / 한판승부",
            scheduleHours = "24시간 시사 & 교양",
            playlistWebUrl = "https://www.cbs.co.kr/radio"
        ),
        RadioStation(
            id = "cbs_musicfm",
            name = "CBS 음악FM",
            broadcaster = "CBS",
            frequencyMhz = 93.9f,
            frequencyText = "93.9 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFF0D9488,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://maac.cbs.co.kr/cbs939/_definst_/cbs939.stream/playlist.m3u8",
            defaultProgram = "아름다운 당신에게 / 배미향의 저녁스케치",
            scheduleHours = "24시간 아름다운 팝 & 가요",
            playlistWebUrl = "https://www.cbs.co.kr/radio"
        ),
        RadioStation(
            id = "febc_fm",
            name = "FEBC 극동방송",
            broadcaster = "극동",
            frequencyMhz = 106.9f,
            frequencyText = "106.9 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFFEC4899,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://mlive1.febc.net/live/seoulfm/playlist.m3u8",
            defaultProgram = "사랑의 뜰안 / 찬양과 말씀",
            scheduleHours = "24시간 복음·찬양 방송",
            playlistWebUrl = "https://seoul.febc.net"
        ),
        RadioStation(
            id = "bbs_fm",
            name = "BBS 불교방송",
            broadcaster = "불교",
            frequencyMhz = 101.9f,
            frequencyText = "101.9 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFFF59E0B,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://bbslive.nowcdn.co.kr/bbsradio/bbsradio.stream/playlist.m3u8",
            defaultProgram = "아침저널 / 무명을 밝히고",
            scheduleHours = "04:00 ~ 02:00 명상·힐링·음악",
            playlistWebUrl = "https://www.bbsi.co.kr"
        ),
        RadioStation(
            id = "cpbc_fm",
            name = "CPBC 가톨릭평화방송",
            broadcaster = "평화",
            frequencyMhz = 105.3f,
            frequencyText = "105.3 MHz",
            category = StationCategory.MUSIC_RELIGION,
            badgeColorHex = 0xFF64748B,
            resolverType = StreamResolverType.STATIC_HLS,
            endpointOrStreamUrl = "https://onair.cpbc.co.kr/live/radio/playlist.m3u8",
            defaultProgram = "열린세상 오늘 / 음악이 있는 저녁풍경",
            scheduleHours = "05:00 ~ 02:00 평화·생활음악",
            playlistWebUrl = "https://www.cpbc.co.kr"
        )
    )

    fun loadStations(context: Context): List<RadioStation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return initialStations.map { station ->
            val customUrl = prefs.getString(KEY_CUSTOM_URL_PREFIX + station.id, null)
            station.copy(customStreamUrl = customUrl)
        }
    }

    fun saveCustomStreamUrl(context: Context, stationId: String, customUrl: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (customUrl.isNullOrBlank()) {
            prefs.edit().remove(KEY_CUSTOM_URL_PREFIX + stationId).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_URL_PREFIX + stationId, customUrl.trim()).apply()
        }
    }

    fun getFavoriteIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_FAVORITES, setOf("kbs_coolfm", "mbc_fm4u", "sbs_powerfm")) ?: emptySet()
    }

    fun toggleFavorite(context: Context, stationId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getFavoriteIds(context).toMutableSet()
        if (current.contains(stationId)) {
            current.remove(stationId)
        } else {
            current.add(stationId)
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        return current
    }

    suspend fun resolvePlayableStreamUrl(station: RadioStation): String = withContext(Dispatchers.IO) {
        if (!station.customStreamUrl.isNullOrBlank()) {
            return@withContext station.customStreamUrl.trim()
        }

        val endpoint = station.endpointOrStreamUrl
        if (station.resolverType == StreamResolverType.STATIC_HLS) {
            return@withContext endpoint
        }

        try {
            val request = Request.Builder()
                .url(endpoint)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .header("Accept", "*/*")
                .header("Referer", when (station.resolverType) {
                    StreamResolverType.KBS_API -> "https://onair.kbs.co.kr/"
                    StreamResolverType.MBC_API -> "https://mini.imbc.com/"
                    StreamResolverType.SBS_API -> "https://www.sbs.co.kr/"
                    else -> endpoint
                })
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyText = response.body?.string()?.trim().orEmpty()
                if (bodyText.isNotEmpty()) {
                    when (station.resolverType) {
                        StreamResolverType.KBS_API -> {
                            val json = JSONObject(bodyText)
                            val items = json.optJSONArray("channel_item")
                            if (items != null && items.length() > 0) {
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    val serviceUrl = item.optString("service_url", "")
                                    val mediaType = item.optString("media_type", "")
                                    if (serviceUrl.contains(".m3u8") && (mediaType == "audio" || i == 0)) {
                                        return@withContext serviceUrl
                                    }
                                }
                                val firstUrl = items.getJSONObject(0).optString("service_url", "")
                                if (firstUrl.startsWith("http")) return@withContext firstUrl
                            }
                        }
                        StreamResolverType.MBC_API, StreamResolverType.SBS_API -> {
                            val firstToken = bodyText.lineSequence()
                                .map { it.trim() }
                                .firstOrNull { it.startsWith("http") }
                            if (firstToken != null) {
                                return@withContext firstToken
                            }
                            val startIdx = bodyText.indexOf("http")
                            if (startIdx >= 0) {
                                val sub = bodyText.substring(startIdx)
                                val extracted = sub.takeWhile { ch ->
                                    !ch.isWhitespace() && ch !in "\"'<>" 
                                }
                                if (extracted.startsWith("http")) {
                                    return@withContext extracted
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val corsProxies = listOf(
            "https://api.allorigins.win/raw?url=",
            "https://api.codetabs.com/v1/proxy?quest="
        )
        for (proxy in corsProxies) {
            try {
                val encodedUrl = java.net.URLEncoder.encode(endpoint, "UTF-8")
                val proxyReq = Request.Builder().url(proxy + encodedUrl).build()
                httpClient.newCall(proxyReq).execute().use { resp ->
                    val text = resp.body?.string()?.trim().orEmpty().replace("\\/", "/")
                    val startIdx = text.indexOf("http")
                    if (startIdx >= 0) {
                        val sub = text.substring(startIdx)
                        val candidate = sub.takeWhile { ch ->
                            !ch.isWhitespace() && ch !in "\"'<>" 
                        }
                        if (candidate.contains(".m3u8")) {
                            return@withContext candidate
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return@withContext endpoint
    }
}
