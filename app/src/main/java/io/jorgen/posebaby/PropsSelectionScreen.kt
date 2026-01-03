package io.jorgen.posebaby

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PropsSelectionScreen(
    onGenerate: (List<String>, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customPropText by remember { mutableStateOf("") }
    
    val commonProps = listOf(
        "鲜花", "书本", "雨伞", 
        "扇子", "吉他", "咖啡",
        "帽子", "气球", "椅子"
    )
    
    val selectedProps = remember { mutableStateListOf<String>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(24.dp), tint = Color.White)
            }
            Text(
                "添加道具",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Common Props Selection
        Text("常用道具", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonProps.forEach { prop ->
                FilterChip(
                    selected = selectedProps.contains(prop),
                    onClick = {
                        if (selectedProps.contains(prop)) {
                            selectedProps.remove(prop)
                        } else {
                            // Single selection logic? Or multiple? User implied multiple "list".
                            // But usually props shouldn't be too many. Let's allow multiple.
                            selectedProps.add(prop)
                        }
                    },
                    label = { Text(prop, color = Color.White) },
                    leadingIcon = if (selectedProps.contains(prop)) {
                        { Icon(Icons.Default.Check, null, tint = Color.White) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE91E63),
                        containerColor = Color(0xFF1A1A2E)
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Custom Text Prop
        Text("自定义道具", color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = customPropText,
            onValueChange = { customPropText = it },
            placeholder = { Text("例如：红色围巾", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFE91E63),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Prop Image removed
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Generate Button
        Button(
            onClick = { onGenerate(selectedProps.toList(), customPropText) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
        ) {
            Text("生成图片", style = MaterialTheme.typography.titleMedium)
        }
    }
}
