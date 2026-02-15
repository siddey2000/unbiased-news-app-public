package com.unbiased.news.ui.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.unbiased.news.domain.model.Article
import com.unbiased.news.domain.model.Topic
import com.unbiased.news.domain.repository.SyncResult
import com.unbiased.news.ui.components.NewsCard
import com.unbiased.news.ui.components.TopicChip
import com.unbiased.news.ui.components.openInChromeTab
import com.unbiased.news.ui.theme.UnbiasedNewsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val topics = viewModel.topics

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle sync result with Snackbar
    LaunchedEffect(syncResult) {
        when (val result = syncResult) {
            is SyncResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = "Synced ${result.articlesFetched} articles (${result.articlesFiltered} filtered)",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSyncResult()
            }
            is SyncResult.Cached -> {
                snackbarHostState.showSnackbar(
                    message = "Using cached data: ${result.articlesCount} articles",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearSyncResult()
            }
            is SyncResult.Error -> {
                if (result.hasCachedData) {
                    snackbarHostState.showSnackbar(
                        message = "Offline: ${result.message}",
                        duration = SnackbarDuration.Short
                    )
                }
                viewModel.clearSyncResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Unbiased News",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Topic chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(topics) { topic ->
                    TopicChip(
                        topic = topic,
                        isSelected = topic.id == selectedTopic.id,
                        onToggle = { viewModel.selectTopic(it) }
                    )
                }
            }

            // Sync status banner
            when (val result = syncResult) {
                is SyncResult.Success -> {
                    SyncStatusBanner(
                        message = "${result.articlesFetched} articles synced",
                        type = SyncStatusType.SUCCESS
                    )
                }
                is SyncResult.Cached -> {
                    SyncStatusBanner(
                        message = "Cached: ${result.articlesCount} articles",
                        type = SyncStatusType.CACHED
                    )
                }
                is SyncResult.Error -> {
                    if (result.hasCachedData) {
                        SyncStatusBanner(
                            message = "Offline mode",
                            type = SyncStatusType.WARNING
                        )
                    }
                }
                null -> {}
            }

            // Error message
            error?.let { errorMessage ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Content
            if (isLoading && articles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (articles.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No articles yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the refresh button to fetch news",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Fetch News")
                        }
                    }
                }
            } else {
                // Articles list
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        NewsCard(
                            article = article,
                            onClick = { url ->
                                openInChromeTab(context, url)
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class SyncStatusType {
    SUCCESS,
    CACHED,
    WARNING
}

@Composable
private fun SyncStatusBanner(
    message: String,
    type: SyncStatusType,
    modifier: Modifier = Modifier
) {
    val (icon, backgroundColor, contentColor) = when (type) {
        SyncStatusType.SUCCESS -> Triple(
            Icons.Default.Check,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        SyncStatusType.CACHED -> Triple(
            Icons.Default.CloudDownload,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        SyncStatusType.WARNING -> Triple(
            Icons.Default.Warning,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    UnbiasedNewsTheme {
        HomeScreen(
            onNavigateToSettings = {}
        )
    }
}
