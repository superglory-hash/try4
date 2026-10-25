package com.seoul.fmonair

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkGlassColorScheme = darkColorScheme(
                primary = Color(0xFF38BDF8),
                secondary = Color(0xFF818CF8),
                background = Color(0xFF0B1120),
                surface = Color(0xFF111827),
                onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFE2E8F0)
            )
            MaterialTheme(colorScheme = darkGlassColorScheme) {
                SeoulRadioAppScreen()
            }
        }
    }
}

@Composable
fun SeoulRadioAppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var stations by remember { mutableStateOf(StationRepository.loadStations(context)) }
    var favoriteIds by remember { mutableStateOf(StationRepository.getFavoriteIds(context)) }

    val currentStation by RadioPlaybackService.currentStationFlow.collectAsState()
    val isPlaying by RadioPlaybackService.isPlayingFlow.collectAsState()
    val streamStatus by RadioPlaybackService.streamStatusFlow.collectAsState()
    val liveIcyTitle by RadioPlaybackService.liveMetadataTitleFlow.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf(StationCategory.ALL) }
    var channelSearchQuery by remember { mutableStateOf("") }

    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVol)
    }

    var selectedSleepOption by remember { mutableStateOf(SleepTimerOption.OFF) }
    var remainingSleepSeconds by remember { mutableIntStateOf(0) }

    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showCustomUrlModal by remember { mutableStateOf(false) }
    var showApkGuideModal by remember { mutableStateOf(false) }

    val defaultLyricLines = remember {
        listOf(
            "떠나는 길에 네가 내게 말했지",
            "너는 바라는 게 너무나 많아",
            "잠깐이라도 널 안 바라보면",
            "머리에 불이 나버린다니까",
            "나는 흐르려는 눈물을 참고",
            "하려던 얘길 어렵게 누르고",
            "그래 미안해라는 한마디로",
            "너랑 나눈 날들 마무리했었지",
            "달디달고 달디달고 달디단 밤양갱 밤양갱",
            "내가 먹고 싶었던 건 달디단 밤양갱 밤양갱이야"
        )
    }

    var currentSong by remember {
        mutableStateOf(
            RecognizedSong(
                id = "init_song",
                title = "밤양갱",
                artist = "비비 (BIBI)",
                album = "밤양갱 - Single",
                stationName = "KBS 쿨FM",
                frequencyText = "89.1 MHz",
                recognizedAt = "실시간 ON-AIR",
                plainLyrics = defaultLyricLines.joinToString(separator = "\n"),
                syncedLyrics = defaultLyricLines.mapIndexed { idx, line -> LyricLine(idx * 3.5f, line) },
                sourceMethod = "선곡표 & LRCLIB"
            )
        )
    }
    var isRecognizingAudio by remember { mutableStateOf(false) }
    var lyricsSearchQuery by remember { mutableStateOf("") }
    var lrcSearchResults by remember { mutableStateOf<List<RecognizedSong>>(emptyList()) }
    var isSearchingLyrics by remember { mutableStateOf(false) }
    var isLyricsFocusMode by remember { mutableStateOf(false) }
    var activeLyricLineIndex by remember { mutableIntStateOf(0) }

    var savedSongsHistory by remember {
        mutableStateOf<List<RecognizedSong>>(SongAndLyricsService.loadSavedSongs(context))
    }

    LaunchedEffect(currentSong.id, isPlaying) {
        activeLyricLineIndex = 0
        while (true) {
            delay(3500)
            if (currentSong.syncedLyrics.isNotEmpty()) {
                activeLyricLineIndex = (activeLyricLineIndex + 1) % currentSong.syncedLyrics.size
            }
        }
    }

    LaunchedEffect(remainingSleepSeconds) {
        if (remainingSleepSeconds > 0) {
            delay(1000)
            remainingSleepSeconds -= 1
            if (remainingSleepSeconds == 0) {
                selectedSleepOption = SleepTimerOption.OFF
                if (isPlaying) {
                    val stopIntent = Intent(context, RadioPlaybackService::class.java).apply {
                        action = RadioPlaybackService.ACTION_TOGGLE_PLAY
                    }
                    context.startService(stopIntent)
                }
                Toast.makeText(context, "취침예약 시간이 종료되어 라디오를 자동으로 정지했습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun playStation(station: RadioStation) {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_PLAY_STATION
            putExtra(RadioPlaybackService.EXTRA_STATION_ID, station.id)
        }
        context.startService(intent)
    }

    fun triggerAudioRecognition() {
        if (isRecognizingAudio) return
        isRecognizingAudio = true
        coroutineScope.launch {
            val recognized = SongAndLyricsService.recognizeAudioFingerprint5Sec(
                station = currentStation,
                icyMetadataTitle = liveIcyTitle
            )
            currentSong = recognized
            savedSongsHistory = SongAndLyricsService.saveSongToHistory(context, recognized)
            isRecognizingAudio = false
            selectedTabIndex = 1
            Toast.makeText(
                context,
                "곡 인식 완료: ${recognized.artist} - ${recognized.title}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B1120)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1120), Color(0xFF111827), Color(0xFF0F172A))
                    )
                )
                .padding(innerPadding)
        ) {
            TopHeaderBar(
                remainingSleepSeconds = remainingSleepSeconds,
                onOpenSleepModal = { showSleepTimerModal = true },
                onOpenCustomUrlModal = { showCustomUrlModal = true },
                onOpenApkGuideModal = { showApkGuideModal = true }
            )

            if (!isLyricsFocusMode || selectedTabIndex != 1) {
                NowPlayingHeroCard(
                    station = currentStation,
                    isPlaying = isPlaying,
                    streamStatus = streamStatus,
                    isFavorite = favoriteIds.contains(currentStation.id),
                    volumeLevel = volumeLevel,
                    isRecognizingAudio = isRecognizingAudio,
                    onTogglePlay = {
                        val intent = Intent(context, RadioPlaybackService::class.java).apply {
                            action = RadioPlaybackService.ACTION_TOGGLE_PLAY
                        }
                        context.startService(intent)
                    },
                    onPrevChannel = {
                        val intent = Intent(context, RadioPlaybackService::class.java).apply {
                            action = RadioPlaybackService.ACTION_PREV_STATION
                        }
                        context.startService(intent)
                    },
                    onNextChannel = {
                        val intent = Intent(context, RadioPlaybackService::class.java).apply {
                            action = RadioPlaybackService.ACTION_NEXT_STATION
                        }
                        context.startService(intent)
                    },
                    onToggleFavorite = {
                        favoriteIds = StationRepository.toggleFavorite(context, currentStation.id)
                    },
                    onVolumeChange = { newVol ->
                        volumeLevel = newVol
                        val targetVol = (newVol * maxVol).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    onJumpToLyrics = {
                        selectedTabIndex = 1
                    },
                    onRecognizeAudio5Sec = {
                        triggerAudioRecognition()
                    }
                )
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF111827).copy(alpha = 0.85f),
                contentColor = Color(0xFF38BDF8),
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color(0xFF38BDF8)
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            "수도권 채널 (${stations.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Radio, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            "실시간 가사·인식",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            "노래 보관함 (${savedSongsHistory.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> ChannelsTabContent(
                        stations = stations,
                        currentStation = currentStation,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        selectedCategory = selectedCategory,
                        searchQuery = channelSearchQuery,
                        onSelectCategory = { selectedCategory = it },
                        onSearchQueryChange = { channelSearchQuery = it },
                        onSelectStation = { playStation(it) },
                        onToggleFavorite = { st ->
                            favoriteIds = StationRepository.toggleFavorite(context, st.id)
                        }
                    )

                    1 -> RealtimeLyricsTabContent(
                        currentStation = currentStation,
                        currentSong = currentSong,
                        activeLyricIndex = activeLyricLineIndex,
                        isFocusMode = isLyricsFocusMode,
                        isRecognizingAudio = isRecognizingAudio,
                        searchQuery = lyricsSearchQuery,
                        isSearching = isSearchingLyrics,
                        searchResults = lrcSearchResults,
                        onToggleFocusMode = { isLyricsFocusMode = !isLyricsFocusMode },
                        onRecognize5SecAudio = { triggerAudioRecognition() },
                        onSearchQueryChange = { lyricsSearchQuery = it },
                        onExecuteLrcSearch = {
                            if (lyricsSearchQuery.isNotBlank()) {
                                isSearchingLyrics = true
                                coroutineScope.launch {
                                    lrcSearchResults = SongAndLyricsService.searchLyricsFromLrcLib(
                                        query = lyricsSearchQuery,
                                        stationName = currentStation.name,
                                        frequencyText = currentStation.frequencyText
                                    )
                                    isSearchingLyrics = false
                                }
                            }
                        },
                        onSelectSearchedSong = { chosen ->
                            currentSong = chosen
                            savedSongsHistory = SongAndLyricsService.saveSongToHistory(context, chosen)
                            lrcSearchResults = emptyList()
                        }
                    )

                    2 -> SavedSongsHistoryTabContent(
                        savedSongs = savedSongsHistory,
                        onClickSong = { song ->
                            currentSong = song
                            selectedTabIndex = 1
                        },
                        onDeleteSong = { id ->
                            savedSongsHistory = SongAndLyricsService.deleteSongFromHistory(context, id)
                        },
                        onClearAll = {
                            savedSongsHistory = SongAndLyricsService.clearAllHistory(context)
                        }
                    )
                }
            }
        }
    }

    if (showSleepTimerModal) {
        SleepTimerDialog(
            currentOption = selectedSleepOption,
            remainingSeconds = remainingSleepSeconds,
            onSelectOption = { option ->
                selectedSleepOption = option
                remainingSleepSeconds = option.minutes * 60
                showSleepTimerModal = false
            },
            onDismiss = { showSleepTimerModal = false }
        )
    }

    if (showCustomUrlModal) {
        CustomStreamUrlDialog(
            station = currentStation,
            onSaveCustomUrl = { newUrl ->
                StationRepository.saveCustomStreamUrl(context, currentStation.id, newUrl)
                stations = StationRepository.loadStations(context)
                val updated = stations.find { it.id == currentStation.id } ?: currentStation
                playStation(updated)
                showCustomUrlModal = false
                Toast.makeText(context, "${currentStation.name} 스트림 URL 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showCustomUrlModal = false }
        )
    }

    if (showApkGuideModal) {
        ApkBuildGuideDialog(onDismiss = { showApkGuideModal = false })
    }
}

@Composable
fun TopHeaderBar(
    remainingSleepSeconds: Int,
    onOpenSleepModal: () -> Unit,
    onOpenCustomUrlModal: () -> Unit,
    onOpenApkGuideModal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "수도권 온에어 FM 라디오",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (remainingSleepSeconds > 0) Color(0xFF1D4ED8) else Color(0xFF1E293B),
                modifier = Modifier
                    .clickable { onOpenSleepModal() }
                    .padding(end = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = "취침예약",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (remainingSleepSeconds > 0) {
                            String.format("%02d:%02d", remainingSleepSeconds / 60, remainingSleepSeconds % 60)
                        } else {
                            "취침예약"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onOpenCustomUrlModal,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "커스텀 스트림 수정",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onOpenApkGuideModal,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = "APK 빌드 가이드",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun NowPlayingHeroCard(
    station: RadioStation,
    isPlaying: Boolean,
    streamStatus: String,
    isFavorite: Boolean,
    volumeLevel: Float,
    isRecognizingAudio: Boolean,
    onTogglePlay: () -> Unit,
    onPrevChannel: () -> Unit,
    onNextChannel: () -> Unit,
    onToggleFavorite: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onJumpToLyrics: () -> Unit,
    onRecognizeAudio5Sec: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(22.dp)
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(station.badgeColorHex),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = station.broadcaster,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = station.scheduleHours,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        if (!station.customStreamUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[커스텀 URL]",
                                color = Color(0xFFFACC15),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.1f", station.frequencyMhz),
                            color = Color(0xFF38BDF8),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MHz",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = station.name,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = station.defaultProgram,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "상태: $streamStatus",
                        color = if (isPlaying) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    FiveBarAudioEqualizer(isPlaying = isPlaying)
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "즐겨찾기 토글",
                            tint = if (isFavorite) Color(0xFFFACC15) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnalogFmFrequencyDial(currentFrequencyMhz = station.frequencyMhz)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevChannel,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "이전 채널", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF06B6D4))
                                )
                            )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "재생/일시정지",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = onNextChannel,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "다음 채널", tint = Color.White)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onRecognizeAudio5Sec,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(50)
                    ) {
                        if (isRecognizingAudio) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("5초 샘플링...", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        } else {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BGM 인식", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = onJumpToLyrics,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            Icons.Default.Lyrics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("현재 곡 가사", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "볼륨",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = volumeLevel,
                    onValueChange = onVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(22.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF0F172A)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${(volumeLevel * 100).toInt()}%",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun AnalogFmFrequencyDial(currentFrequencyMhz: Float) {
    val clamped = currentFrequencyMhz.coerceIn(88.0f, 108.0f)
    val fraction by animateFloatAsState(
        targetValue = (clamped - 88.0f) / 20.0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "dialPointer"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF090D16))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("88", "92", "96", "100", "104", "108 MHz").forEach { label ->
                Text(
                    text = label,
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            val width = size.width
            val height = size.height
            val tickCount = 40

            for (i in 0..tickCount) {
                val x = (i.toFloat() / tickCount) * width
                val isMajor = i % 4 == 0
                val tickTop = if (isMajor) 2f else height * 0.35f
                drawLine(
                    color = if (isMajor) Color(0xFF94A3B8) else Color(0xFF475569),
                    start = Offset(x, tickTop),
                    end = Offset(x, height - 2f),
                    strokeWidth = if (isMajor) 2f else 1f
                )
            }

            val needleX = fraction * width
            drawLine(
                color = Color(0xFFEF4444),
                start = Offset(needleX, 0f),
                end = Offset(needleX, height),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFFACC15),
                radius = 5f,
                center = Offset(needleX, height / 2f)
            )
        }
    }
}

@Composable
fun FiveBarAudioEqualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "eqTransition")
    val durations = listOf(380, 520, 310, 460, 350)

    Row(
        modifier = Modifier
            .height(24.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        durations.forEachIndexed { idx, dur ->
            val animRatio by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = dur, delayMillis = idx * 70),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$idx"
            )
            val barFraction = if (isPlaying) animRatio else 0.18f
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((22 * barFraction).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) Color(0xFF38BDF8) else Color(0xFF475569)
                    )
            )
        }
    }
}

@Composable
fun ChannelsTabContent(
    stations: List<RadioStation>,
    currentStation: RadioStation,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    selectedCategory: StationCategory,
    searchQuery: String,
    onSelectCategory: (StationCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit
) {
    val filteredStations = stations.filter { station ->
        val matchesCategory = when (selectedCategory) {
            StationCategory.ALL -> true
            StationCategory.FAVORITE -> favoriteIds.contains(station.id)
            else -> station.category == selectedCategory
        }
        val q = searchQuery.trim()
        val matchesSearch = q.isEmpty() ||
            station.name.contains(q, ignoreCase = true) ||
            station.frequencyText.contains(q, ignoreCase = true) ||
            station.defaultProgram.contains(q, ignoreCase = true) ||
            station.broadcaster.contains(q, ignoreCase = true)

        matchesCategory && matchesSearch
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StationCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onSelectCategory(category) },
                    label = { Text(category.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFFCBD5E1)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("채널명, 주파수(MHz), 프로그램명 검색...", fontSize = 13.sp, color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF334155),
                focusedContainerColor = Color(0xFF111827),
                unfocusedContainerColor = Color(0xFF111827),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredStations, key = { it.id }) { station ->
                val isSelected = station.id == currentStation.id
                val isFav = favoriteIds.contains(station.id)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) Color(0xFF1E3A8A).copy(alpha = 0.65f) else Color(0xFF1E293B).copy(alpha = 0.65f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSelectStation(station) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(station.badgeColorHex),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = station.broadcaster,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Icon(
                                imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "즐겨찾기",
                                tint = if (isFav) Color(0xFFFACC15) else Color(0xFF64748B),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onToggleFavorite(station) }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = station.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = station.frequencyText,
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = station.defaultProgram,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (station.resolverType) {
                                StreamResolverType.STATIC_HLS -> "고정 HLS (.m3u8)"
                                else -> "실시간 토큰 Resolver"
                            },
                            color = if (isSelected && isPlaying) Color(0xFF4ADE80) else Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeLyricsTabContent(
    currentStation: RadioStation,
    currentSong: RecognizedSong,
    activeLyricIndex: Int,
    isFocusMode: Boolean,
    isRecognizingAudio: Boolean,
    searchQuery: String,
    isSearching: Boolean,
    searchResults: List<RecognizedSong>,
    onToggleFocusMode: () -> Unit,
    onRecognize5SecAudio: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onExecuteLrcSearch: () -> Unit,
    onSelectSearchedSong: (RecognizedSong) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(activeLyricIndex) {
        if (currentSong.syncedLyrics.isNotEmpty() && activeLyricIndex in currentSong.syncedLyrics.indices) {
            listState.animateScrollToItem((activeLyricIndex - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (!isFocusMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("노래 제목·가수 입력 (LRCLIB 실시간 가사 검색)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF111827),
                        unfocusedContainerColor = Color(0xFF111827),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onExecuteLrcSearch,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("가사 검색", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    LazyColumn(contentPadding = PaddingValues(8.dp)) {
                        items(searchResults, key = { it.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSearchedSong(item) }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${item.artist} · ${item.album}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                Text("가사 열기", color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickLinks = listOf(
                    "KBS 콩 선곡표" to "https://program.kbs.co.kr/2fm/radio/main.html",
                    "MBC 미니 선곡표" to "https://www.imbc.com/broad/radio/",
                    "SBS 고릴라 선곡표" to "https://www.sbs.co.kr/radio",
                    "CBS 레인보우 선곡표" to "https://www.cbs.co.kr/radio"
                )
                quickLinks.forEach { (label, url) ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Surface(
            color = Color(0xFF1E293B).copy(alpha = 0.85f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF9333EA)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentSong.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentSong.artist} · ${currentSong.stationName} (${currentSong.frequencyText})",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "인식 방식: ${currentSong.sourceMethod}",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("lyrics", currentSong.plainLyrics))
                                Toast.makeText(context, "가사 전체가 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "가사 복사", tint = Color(0xFFCBD5E1))
                        }

                        TextButton(onClick = onToggleFocusMode) {
                            Text(
                                text = if (isFocusMode) "집중모드 해제" else "집중 모드",
                                color = Color(0xFFFACC15),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF090D16),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(18.dp))
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 28.dp, horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(currentSong.syncedLyrics) { index, line ->
                    val isHighlighted = index == activeLyricIndex
                    Text(
                        text = line.text,
                        color = if (isHighlighted) Color(0xFF38BDF8) else Color(0xFF64748B),
                        fontSize = if (isHighlighted) 19.sp else 15.sp,
                        fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isHighlighted) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSongsHistoryTabContent(
    savedSongs: List<RecognizedSong>,
    onClickSong: (RecognizedSong) -> Unit,
    onDeleteSong: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "라디오에서 찾은 노래 보관함 (${savedSongs.size}곡)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (savedSongs.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("전체 삭제", color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (savedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "아직 저장된 노래 히스토리가 없습니다. 상단 'BGM 인식' 또는 가사 검색을 실행하면 자동 저장됩니다.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(savedSongs, key = { it.id }) { song ->
                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.75f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClickSong(song) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${song.title} - ${song.artist}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${song.stationName} (${song.frequencyText}) · 저장 시각: ${song.recognizedAt}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "클릭하여 싱크 가사 즉시 열람 · ${song.sourceMethod}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { onDeleteSong(song.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentOption: SleepTimerOption,
    remainingSeconds: Int,
    onSelectOption: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("취침예약 타이머 설정", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remainingSeconds > 0) {
                    Text(
                        text = "현재 남은 시간: ${remainingSeconds / 60}분 ${remainingSeconds % 60}초 후 자동 종료",
                        color = Color(0xFF38BDF8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SleepTimerOption.entries.forEach { option ->
                    Button(
                        onClick = { onSelectOption(option) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentOption == option) Color(0xFF2563EB) else Color(0xFF0F172A)
                        )
                    ) {
                        Text(option.label, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Color(0xFF38BDF8))
            }
        }
    )
}

@Composable
fun CustomStreamUrlDialog(
    station: RadioStation,
    onSaveCustomUrl: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(station.customStreamUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("${station.name} 커스텀 스트림 설정", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column {
                Text(
                    text = "기본 탑재된 온에어 주소 대신 직접 원하는 .m3u8 또는 .mp3 스트림 URL을 입력하여 로컬 스토리지에 저장할 수 있습니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "기본 엔드포인트: ${station.endpointOrStreamUrl}",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    placeholder = { Text("https://example.com/live/playlist.m3u8", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569)
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onSaveCustomUrl(null) }) {
                Text("기본값 복원", color = Color(0xFFFACC15))
            }
        },
        confirmButton = {
            Button(onClick = { onSaveCustomUrl(inputUrl) }) {
                Text("저장 후 재생")
            }
        }
    )
}

@Composable
fun ApkBuildGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("GitHub Actions 무설치 APK 자동 빌드 가이드", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "1. GitHub에서 새 리포지토리(New Repository)를 생성합니다.",
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    "2. 압축 해제한 프로젝트 루트의 모든 파일(.github, app, build.gradle.kts, settings.gradle.kts)을 GitHub 웹페이지에 드래그 앤 드롭으로 업로드 후 Commit 합니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Text(
                    "3. 상단 'Actions' 탭을 클릭하면 'Build Seoul FM Radio APK' 워크플로우가 자동으로 Gradle 8.7 Wrapper를 생성하고 assembleDebug 빌드를 수행합니다.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Text(
                    "4. 빌드가 완료되면 Artifacts 영역에서 'Seoul-FM-Radio-APK'를 다운로드해 스마트폰에 설치하세요.",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Color(0xFF38BDF8))
            }
        }
    )
}
