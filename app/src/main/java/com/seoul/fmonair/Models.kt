package com.seoul.fmonair

enum class StationCategory(val displayName: String) {
    ALL("전체"),
    FAVORITE("즐겨찾기"),
    KBS("KBS"),
    MBC("MBC"),
    SBS("SBS"),
    NEWS_EDU("시사·교육"),
    MUSIC_RELIGION("음악·종교")
}

enum class StreamResolverType {
    STATIC_HLS,
    KBS_API,
    MBC_API,
    SBS_API
}

data class RadioStation(
    val id: String,
    val name: String,
    val broadcaster: String,
    val frequencyMhz: Float,
    val frequencyText: String,
    val category: StationCategory,
    val badgeColorHex: Long,
    val resolverType: StreamResolverType,
    val endpointOrStreamUrl: String,
    val channelCode: String = "",
    val fallbackStreamUrl: String = "",
    val defaultProgram: String = "실시간 온에어 방송",
    val scheduleHours: String = "00:00 ~ 24:00 LIVE",
    val playlistWebUrl: String = "https://www.kbs.co.kr",
    val customStreamUrl: String? = null
) {
    val activeEndpoint: String
        get() = if (!customStreamUrl.isNullOrBlank()) customStreamUrl else endpointOrStreamUrl
}

data class LyricLine(
    val timeSec: Float,
    val text: String
)

data class RecognizedSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "On-Air Radio Single",
    val stationName: String,
    val frequencyText: String,
    val recognizedAt: String,
    val plainLyrics: String = "",
    val syncedLyrics: List<LyricLine> = emptyList(),
    val sourceMethod: String = "선곡표/LRCLIB"
)

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("해제", 0),
    MIN_15("15분", 15),
    MIN_30("30분", 30),
    MIN_45("45분", 45),
    MIN_60("60분", 60),
    MIN_90("90분", 90)
}
