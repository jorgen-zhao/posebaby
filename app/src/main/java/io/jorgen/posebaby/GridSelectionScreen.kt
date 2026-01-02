package io.jorgen.posebaby

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Grid selection screen for choosing layout before image generation
 */
@Composable
fun GridSelectionScreen(
    onGridSelected: (MainViewModel.GridOption) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedOption by remember { mutableStateOf<MainViewModel.GridOption?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "选择图片布局",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "生成多个不同姿势的参考图",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Grid options in 2x2 layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GridOptionCard(
                option = MainViewModel.GridOption.SINGLE,
                isSelected = selectedOption == MainViewModel.GridOption.SINGLE,
                onClick = { selectedOption = MainViewModel.GridOption.SINGLE }
            )
            GridOptionCard(
                option = MainViewModel.GridOption.HORIZONTAL_2,
                isSelected = selectedOption == MainViewModel.GridOption.HORIZONTAL_2,
                onClick = { selectedOption = MainViewModel.GridOption.HORIZONTAL_2 }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GridOptionCard(
                option = MainViewModel.GridOption.GRID_4,
                isSelected = selectedOption == MainViewModel.GridOption.GRID_4,
                onClick = { selectedOption = MainViewModel.GridOption.GRID_4 }
            )
            GridOptionCard(
                option = MainViewModel.GridOption.GRID_9,
                isSelected = selectedOption == MainViewModel.GridOption.GRID_9,
                onClick = { selectedOption = MainViewModel.GridOption.GRID_9 }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
            
            Button(
                onClick = { selectedOption?.let { onGridSelected(it) } },
                enabled = selectedOption != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("确认生成")
            }
        }
    }
}

@Composable
private fun GridOptionCard(
    option: MainViewModel.GridOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFE91E63) else Color.Gray
    val bgColor = if (isSelected) Color(0xFF1A1A2E) else Color(0xFF0A0A14)
    
    Column(
        modifier = Modifier
            .size(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Draw mini grid preview
        Box(
            modifier = Modifier
                .size(60.dp)
                .padding(4.dp)
        ) {
            // Draw grid cells
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (row in 0 until option.rows) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (col in 0 until option.cols) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        Color.White.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}
