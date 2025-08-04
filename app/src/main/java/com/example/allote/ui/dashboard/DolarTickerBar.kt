package com.example.allote.ui.dashboard

import android.text.TextUtils
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.allote.data.DolarValue
import com.example.allote.ui.main.DolarInfo

@Composable
fun DolarTickerBar(
    dolarInfo: DolarInfo,
) {
    if (dolarInfo.isLoading || dolarInfo.error != null || dolarInfo.dolarBlue == null || dolarInfo.dolarOficial == null) {
        Box(modifier = Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            if (dolarInfo.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else if (dolarInfo.error != null) {
                Text(
                    text = dolarInfo.error,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .wrapContentHeight()
                )
            }
        }
        return
    }

    val tickerText = buildTickerString(dolarInfo.dolarOficial, dolarInfo.dolarBlue)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth().height(36.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AppCompatTextView(context).apply {
                    ellipsize = TextUtils.TruncateAt.MARQUEE
                    marqueeRepeatLimit = -1
                    isSingleLine = true
                    textSize = 14f
                    setTextColor(textColor.toArgb())
                }
            },
            update = { view ->
                view.text = tickerText
                view.isSelected = true
            }
        )
    }
}

private fun buildTickerString(oficial: DolarValue, blue: DolarValue): String {
    val separator = "   -   "
    val dot = " • "
    val oficialText = "Dólar Oficial${dot}Compra: $${oficial.valueBuy}${dot}Venta: $${oficial.valueSell}"
    val blueText = "Dólar Blue${dot}Compra: $${blue.valueBuy}${dot}Venta: $${blue.valueSell}"

    return "$oficialText$separator$blueText        "
}