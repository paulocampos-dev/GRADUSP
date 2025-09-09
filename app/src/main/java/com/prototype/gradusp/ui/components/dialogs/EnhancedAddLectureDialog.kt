package com.prototype.gradusp.ui.components.dialogs

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.prototype.gradusp.data.model.Classroom
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.parser.SearchService
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.utils.DateTimeUtils
import com.prototype.gradusp.viewmodel.SearchViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

// Define dialog state
enum class EnhancedDialogState { SEARCH, SELECT_CLASSROOM, SUGGESTIONS }

// Enhanced search filters UI
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SearchFiltersSection(
    filters: SearchService.SearchFilters,
    onFiltersChange: (SearchService.SearchFilters) -> Unit,
    campusUnits: Map<String, List<String>>
) {
    var showAdvancedFilters by remember { mutableStateOf(false) }

    Column {
        // Campus filter
        if (campusUnits.isNotEmpty()) {
            Text(
                text = "Campus:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filters.campus == null,
                        onClick = { onFiltersChange(filters.copy(campus = null)) },
                        label = { Text("Todos") }
                    )
                }

                items(campusUnits.keys.toList()) { campus ->
                    FilterChip(
                        selected = filters.campus == campus,
                        onClick = { onFiltersChange(filters.copy(campus = campus)) },
                        label = { Text(campus) }
                    )
                }
            }
        }

        // Show advanced filters toggle
        if (showAdvancedFilters) {
            Spacer(modifier = Modifier.height(8.dp))

            // Day of week filter
            Text(
                text = "Dia da Semana:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = filters.dayOfWeek == null,
                        onClick = { onFiltersChange(filters.copy(dayOfWeek = null)) },
                        label = { Text("Qualquer") }
                    )
                }

                items(DayOfWeek.values()) { day ->
                    FilterChip(
                        selected = filters.dayOfWeek == day,
                        onClick = { onFiltersChange(filters.copy(dayOfWeek = day)) },
                        label = { Text(DateTimeUtils.getShortDayName(day)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time preference (morning/afternoon)
            Text(
                text = "Horário Preferido:",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filters.timeRange?.start == LocalTime.of(8, 0),
                    onClick = {
                        onFiltersChange(filters.copy(
                            timeRange = LocalTime.of(8, 0)..LocalTime.of(12, 0)
                        ))
                    },
                    label = { Text("Manhã") }
                )

                FilterChip(
                    selected = filters.timeRange?.start == LocalTime.of(13, 0),
                    onClick = {
                        onFiltersChange(filters.copy(
                            timeRange = LocalTime.of(13, 0)..LocalTime.of(18, 0)
                        ))
                    },
                    label = { Text("Tarde") }
                )

                FilterChip(
                    selected = filters.timeRange == null,
                    onClick = { onFiltersChange(filters.copy(timeRange = null)) },
                    label = { Text("Qualquer") }
                )
            }
        }

        // Toggle advanced filters
        TextButton(
            onClick = { showAdvancedFilters = !showAdvancedFilters },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(if (showAdvancedFilters) "Menos filtros" else "Mais filtros")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EnhancedAddLectureDialog(
    onDismiss: () -> Unit,
    onLectureSelected: (Lecture, Classroom) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val campusUnits by viewModel.campusUnits.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val cachedDataAvailable by viewModel.cachedDataAvailable.collectAsState()

    var selectedLecture by remember { mutableStateOf<Lecture?>(null) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var dialogState by remember { mutableStateOf(EnhancedDialogState.SEARCH) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            when (dialogState) {
                EnhancedDialogState.SEARCH -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 600.dp)
                    ) {
                        Text(
                            text = "Buscar Matéria",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Enhanced search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            label = { Text("Código, nome ou unidade") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ícone de busca"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpar busca"
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Connectivity status
                        if (!isOnline || isOfflineMode) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (cachedDataAvailable) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close, // Could be a wifi icon
                                        contentDescription = "Status de conexão",
                                        tint = if (cachedDataAvailable) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (cachedDataAvailable) {
                                                "Modo offline - Dados em cache disponíveis"
                                            } else {
                                                "Sem conexão - Nenhum dado em cache"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = viewModel.getConnectionStatusMessage(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Search filters
                        SearchFiltersSection(
                            filters = selectedFilters,
                            onFiltersChange = { viewModel.updateFilters(it) },
                            campusUnits = campusUnits
                        )

                        // Clear filters button
                        if (selectedFilters != SearchService.SearchFilters()) {
                            TextButton(
                                onClick = { viewModel.clearFilters() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Limpar filtros")
                            }
                        }

                        // Loading indicator
                        if (isSearching) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Buscando...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Error message
                        searchError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Search suggestions
                        if (searchSuggestions.isNotEmpty() && searchQuery.isNotBlank()) {
                            Text(
                                text = "Sugestões:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                            ) {
                                items(searchSuggestions) { suggestion ->
                                    Text(
                                        text = suggestion,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectSuggestion(suggestion) }
                                            .padding(8.dp),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Results count
                        if (!isSearching && searchResults.isNotEmpty()) {
                            Text(
                                text = "${searchResults.size} matérias encontradas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search results
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(searchResults) { searchResult ->
                                val lecture = searchResult.item
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedLecture = lecture

                                            // If there's only one classroom, select it automatically
                                            if (lecture.classrooms.size == 1) {
                                                selectedClassroom = lecture.classrooms[0]
                                                onLectureSelected(lecture, lecture.classrooms[0])
                                            } else {
                                                dialogState = EnhancedDialogState.SELECT_CLASSROOM
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "${lecture.code} - ${lecture.name}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = lecture.unit,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Text(
                                                    text = lecture.department,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(
                                                    text = lecture.campus,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                Text(
                                                    text = "${lecture.classrooms.size} turmas",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        // Relevance score indicator
                                        if (searchResult.relevanceScore > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = "Relevância: ${searchResult.relevanceScore}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onDismiss
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }

                EnhancedDialogState.SELECT_CLASSROOM -> {
                    // Classroom selection UI (similar to original)
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 600.dp)
                    ) {
                        Text(
                            text = "Selecionar Turma",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        selectedLecture?.let { lecture ->
                            Text(
                                text = "${lecture.code} - ${lecture.name}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = lecture.unit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Classroom count
                        Text(
                            text = "${selectedLecture?.classrooms?.size ?: 0} turmas disponíveis",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(selectedLecture?.classrooms ?: emptyList()) { classroom ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            selectedClassroom = classroom
                                            selectedLecture?.let { lecture ->
                                                onLectureSelected(lecture, classroom)
                                            }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Turma ${classroom.code}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            if (classroom.startDate.isNotBlank() && classroom.endDate.isNotBlank()) {
                                                Text(
                                                    text = "${classroom.startDate} - ${classroom.endDate}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        classroom.schedules.forEach { schedule ->
                                            val dayOfWeek = try {
                                                DateTimeUtils.convertDayStringToDayOfWeek(schedule.day)
                                                val dayName = DateTimeUtils.getDayName(DateTimeUtils.convertDayStringToDayOfWeek(schedule.day))
                                                "$dayName ${schedule.startTime} - ${schedule.endTime}"
                                            } catch (e: Exception) {
                                                "${schedule.day} ${schedule.startTime} - ${schedule.endTime}"
                                            }

                                            Text(
                                                text = dayOfWeek,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        if (classroom.teachers.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Professor${if (classroom.teachers.size > 1) "es" else ""}:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )

                                            classroom.teachers.forEach { teacher ->
                                                Text(
                                                    text = "• $teacher",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        if (classroom.observations.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "Observações:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Text(
                                                text = classroom.observations,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { dialogState = EnhancedDialogState.SEARCH }
                            ) {
                                Text("Voltar")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onDismiss
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                }

                EnhancedDialogState.SUGGESTIONS -> {
                    // This state is not used in the current implementation
                    // Could be used for advanced suggestions in the future
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(name = "Enhanced Add Lecture Dialog")
@Composable
fun EnhancedAddLectureDialogPreview() {
    GRADUSPTheme {
        // Preview would require proper ViewModel setup
        Text("Enhanced Add Lecture Dialog Preview")
    }
}
