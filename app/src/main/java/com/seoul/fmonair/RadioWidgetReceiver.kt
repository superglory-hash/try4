package com.seoul.fmonair

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RadioWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SeoulRadioGlanceWidget()

    companion object {
        fun notifyWidgetUpdate(context: Context) {
            CoroutineScope(Dispatchers.Main).launch {
                val manager = GlanceAppWidgetManager(context)
                val ids = manager.getGlanceIds(SeoulRadioGlanceWidget::class.java)
                ids.forEach { id ->
                    SeoulRadioGlanceWidget().update(context, id)
                }
            }
        }
    }
}

class SeoulRadioGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val station = RadioPlaybackService.currentStationFlow.value
        val isPlaying = RadioPlaybackService.isPlayingFlow.value

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF0F172A)))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ON-AIR 수도권 FM",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = station.frequencyText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFACC15)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = station.name,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Text(
                text = station.defaultProgram,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF94A3B8)),
                    fontSize = 12.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(10.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏮ 이전 채널",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE2E8F0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0xFF1E293B)))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<PrevStationWidgetAction>())
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = if (isPlaying) "⏸ 일시정지" else "▶ 즉시 재생",
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0xFF2563EB)))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<TogglePlayWidgetAction>())
                )

                Spacer(modifier = GlanceModifier.width(8.dp))

                Text(
                    text = "다음 채널 ⏭",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFE2E8F0)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0xFF1E293B)))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable(actionRunCallback<NextStationWidgetAction>())
                )
            }
        }
    }
}

class PrevStationWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_PREV_STATION
        }
        context.startService(intent)
    }
}

class TogglePlayWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_TOGGLE_PLAY
        }
        context.startService(intent)
    }
}

class NextStationWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_NEXT_STATION
        }
        context.startService(intent)
    }
}
