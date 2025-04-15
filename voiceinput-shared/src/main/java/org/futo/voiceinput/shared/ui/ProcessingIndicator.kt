package org.futo.voiceinput.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.futo.voiceinput.shared.R

/**
 * Indicator showing whether voice processing is happening locally or remotely.
 */
@Composable
fun ProcessingIndicator(
    isRemoteProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Indicator dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isRemoteProcessing) Color(0xFF4CAF50) else Color(0xFFFFA000)
                )
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Text label
        Text(
            text = if (isRemoteProcessing) 
                stringResource(R.string.remote_processing_indicator) 
            else 
                stringResource(R.string.local_processing_indicator),
            style = MaterialTheme.typography.bodySmall,
            color = if (isRemoteProcessing) Color(0xFF4CAF50) else Color(0xFFFFA000)
        )
    }
}
