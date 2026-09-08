package com.nivya.ui.cleanup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nivya.ui.common.*
import com.nivya.ui.theme.*

/**
 * Child Clean Up Screen helping child safely clean temporary cache files.
 */
@Composable
fun ChildCleanUpScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    var isCleaning by remember { mutableStateOf(false) }
    var cleanedSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader(title = "Storage Clean Up")

        NivyaCard {
            Text(
                text = "Temporary Cache Files",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1.2 GB of temporary cached data can be safely removed without affecting personal files.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isCleaning = true
                    cleanedSuccess = true
                    isCleaning = false
                },
                enabled = !isCleaning && !cleanedSuccess,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cleanedSuccess) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "1.2 GB Cleaned!")
                } else {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Clean Up 1.2 GB")
                }
            }
        }

        SectionHeader(title = "Safe Cleaning Guide")

        NivyaCard {
            Text(text = "What gets cleaned?", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "✓ App temporary cache and preview thumbnails", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "✓ Unused temporary installation packages", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(text = "✗ Your photos, messages, and game saves are NEVER touched", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
        }
    }
}
