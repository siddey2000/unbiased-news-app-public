package com.unbiased.news.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.unbiased.news.domain.model.Topic
import com.unbiased.news.ui.theme.UnbiasedNewsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicChip(
    topic: Topic,
    isSelected: Boolean,
    onToggle: (Topic) -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = { onToggle(topic) },
        label = {
            Text(
                text = topic.name,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier.padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Preview(showBackground = true)
@Composable
fun TopicChipPreview() {
    UnbiasedNewsTheme {
        TopicChip(
            topic = Topic("tech", "Technology"),
            isSelected = true,
            onToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TopicChipUnselectedPreview() {
    UnbiasedNewsTheme {
        TopicChip(
            topic = Topic("politics", "Politics"),
            isSelected = false,
            onToggle = {}
        )
    }
}
