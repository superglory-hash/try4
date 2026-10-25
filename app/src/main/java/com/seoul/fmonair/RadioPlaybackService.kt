package com.seoul.fmonair

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaLibraryService() {

    companion object {
        private const val ROOT_ID = "seoul_fm_root"
        const val ACTION_PLAY_STATION = "com.seoul.fmonair.ACTION_PLAY_STATION"
        const val ACTION_NEXT_STATION = "com.seoul.fmonair.ACTION_NEXT_STATION"
        const val ACTION_PREV_STATION = "com.seoul.fmonair.ACTION_PREV_STATION"
        const val ACTION_TOGGLE_PLAY = "com.seoul.fmonair.ACTION_TOGGLE_PLAY"
        const val EXTRA_STATION_ID = "extra_station_id"

        private val _currentStationFlow = MutableStateFlow(StationRepository.initialStations[2])
        val currentStationFlow: StateFlow<RadioStation> = _currentStationFlow

        private val _isPlayingFlow = MutableStateFlow(false)
        val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow

        private val _streamStatusFlow = MutableStateFlow("대기 중")
        val streamStatusFlow: StateFlow<String> = _streamStatusFlow

        private val _liveMetadataTitleFlow = MutableStateFlow<String?>(null)
        val liveMetadataTitleFlow: StateFlow<String?> = _liveMetadataTitleFlow
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
                if (isPlaying) {
                    _streamStatusFlow.value = "ON-AIR 실시간 스트리밍 중"
                } else if (exoPlayer.playbackState == Player.STATE_IDLE || !exoPlayer.playWhenReady) {
                    _streamStatusFlow.value = "일시정지됨"
                }
                RadioWidgetReceiver.notifyWidgetUpdate(this@RadioPlaybackService)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> _streamStatusFlow.value = "스트림 토큰 해석 및 버퍼링 중..."
                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying) _streamStatusFlow.value = "ON-AIR 실시간 스트리밍 중"
                    }
                    Player.STATE_ENDED, Player.STATE_IDLE -> {}
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                val title = mediaMetadata.title?.toString()
                if (!title.isNullOrBlank() && title != _currentStationFlow.value.name) {
                    _liveMetadataTitleFlow.value = title
                }
            }
        })

        player = exoPlayer

        val openAppIntent = Intent(this, MainActivity::class.java)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, LibrarySessionCallback())
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val stations = StationRepository.loadStations(this)

        when (intent?.action) {
            ACTION_PLAY_STATION -> {
                val stationId = intent.getStringExtra(EXTRA_STATION_ID)
                val target = stations.find { it.id == stationId } ?: _currentStationFlow.value
                resolveAndPlayStation(target)
            }
            ACTION_NEXT_STATION -> {
                val idx = stations.indexOfFirst { it.id == _currentStationFlow.value.id }
                val next = stations[(idx + 1).mod(stations.size)]
                resolveAndPlayStation(next)
            }
            ACTION_PREV_STATION -> {
                val idx = stations.indexOfFirst { it.id == _currentStationFlow.value.id }
                val prev = stations[(idx - 1 + stations.size).mod(stations.size)]
                resolveAndPlayStation(prev)
            }
            ACTION_TOGGLE_PLAY -> {
                val p = player
                if (p != null) {
                    if (p.isPlaying) {
                        p.pause()
                    } else {
                        if (p.currentMediaItem == null) {
                            resolveAndPlayStation(_currentStationFlow.value)
                        } else {
                            p.play()
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun resolveAndPlayStation(station: RadioStation) {
        _currentStationFlow.value = station
        _streamStatusFlow.value = "${station.broadcaster} 온에어 주소 해석 중..."
        RadioWidgetReceiver.notifyWidgetUpdate(this)

        serviceScope.launch {
            val resolvedUrl = StationRepository.resolvePlayableStreamUrl(station)
            val metadata = MediaMetadata.Builder()
                .setTitle("${station.name} (${station.frequencyText})")
                .setArtist(station.defaultProgram)
                .setStation(station.name)
                .setSubtitle(station.frequencyText)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id)
                .setUri(Uri.parse(resolvedUrl))
                .setMediaMetadata(metadata)
                .build()

            player?.apply {
                setMediaItem(mediaItem)
                prepare()
                play()
            }
        }
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootMetadata = MediaMetadata.Builder()
                .setTitle("수도권 온에어 FM 라디오")
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()

            val rootItem = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(rootMetadata)
                .build()

            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val stations = StationRepository.loadStations(this@RadioPlaybackService)
            val items = stations.map { station ->
                val meta = MediaMetadata.Builder()
                    .setTitle("${station.name} (${station.frequencyText})")
                    .setArtist("${station.broadcaster} · ${station.defaultProgram}")
                    .setStation(station.name)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()

                MediaItem.Builder()
                    .setMediaId(station.id)
                    .setMediaMetadata(meta)
                    .build()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val future = SettableFuture.create<MutableList<MediaItem>>()
            val stations = StationRepository.loadStations(this@RadioPlaybackService)

            serviceScope.launch {
                val resolvedList = mutableListOf<MediaItem>()
                for (reqItem in mediaItems) {
                    val station = stations.find { it.id == reqItem.mediaId } ?: stations.first()
                    _currentStationFlow.value = station
                    val streamUrl = StationRepository.resolvePlayableStreamUrl(station)
                    val meta = MediaMetadata.Builder()
                        .setTitle("${station.name} (${station.frequencyText})")
                        .setArtist(station.defaultProgram)
                        .setStation(station.name)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()

                    resolvedList.add(
                        reqItem.buildUpon()
                            .setUri(streamUrl)
                            .setMediaMetadata(meta)
                            .build()
                    )
                }
                future.set(resolvedList)
            }
            return future
        }
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
