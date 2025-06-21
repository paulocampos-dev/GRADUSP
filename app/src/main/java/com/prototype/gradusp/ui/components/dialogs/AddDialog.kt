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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.prototype.gradusp.data.model.Event
import com.prototype.gradusp.data.model.EventOccurrence
import com.prototype.gradusp.data.model.Lecture
import com.prototype.gradusp.data.model.eventColors
import com.prototype.gradusp.data.parser.UspParser
import com.prototype.gradusp.data.repository.UspDataRepository
import com.prototype.gradusp.ui.theme.GRADUSPTheme
import com.prototype.gradusp.utils.DateTimeUtils
import com.prototype.gradusp.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onEventAdded: (Event) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY) }
    var startTime by remember { mutableStateOf(LocalTime.of(8, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var color by remember { mutableStateOf(eventColors[0]) }

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
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Adicionar Evento",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Location input
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Local (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Day of week selector
                Text(
                    text = "Dia da Semana",
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        FilterChip(
                            selected = dayOfWeek == day,
                            onClick = { dayOfWeek = day },
                            label = {
                                Text(
                                    DateTimeUtils.getShortDayName(day)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Horário Início",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // In a real app, implement a time picker
                        // This is a simplified version
                        OutlinedTextField(
                            value = startTime.toString(),
                            onValueChange = { /* Implement time parsing */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Horário Fim",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedTextField(
                            value = endTime.toString(),
                            onValueChange = { /* Implement time parsing */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color selector
                Text(
                    text = "Cor",
                    style = MaterialTheme.typography.bodyLarge
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(eventColors.size) { eventColor ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(eventColors[eventColor])
                                .border(
                                    width = if (color == eventColors[eventColor]) 3.dp else 1.dp,
                                    color = if (color == eventColors[eventColor])
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable { color = eventColors[eventColor] }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // Create event and add it
                            val eventOccurrence = EventOccurrence(
                                day = dayOfWeek,
                                startTime = startTime,
                                endTime = endTime
                            )

                            val event = Event(
                                title = title,
                                occurrences = listOf(eventOccurrence),
                                color = color,
                                location = location.takeIf { it.isNotBlank() }
                            )

                            onEventAdded(event)
                        },
                        enabled = title.isNotBlank() && endTime > startTime
                    ) {
                        Text("Adicionar")
                    }
                }
            }
        }
    }
}

// Define dialog state
enum class DialogState { SEARCH, SELECT_CLASSROOM }

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AddLectureDialog(
    onDismiss: () -> Unit,
    onLectureSelected: (Lecture, Classroom) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var allLectures by remember { mutableStateOf<List<Lecture>>(emptyList()) }
    var filteredLectures by remember { mutableStateOf<List<Lecture>>(emptyList()) }
    var selectedLecture by remember { mutableStateOf<Lecture?>(null) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var campusUnits by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uspDataRepository = remember { UspDataRepository(context, viewModel.userPreferencesRepository) }

    // Define dialog state
    var dialogState by remember { mutableStateOf(DialogState.SEARCH) }

    // Filter campus units
    val uniqueCampuses = remember(campusUnits) {
        campusUnits.keys.toList()
    }

    // Load all cached lectures
    LaunchedEffect(true) {
        coroutineScope.launch {
            try {
                isLoading = true

                // Load campus units
                campusUnits = uspDataRepository.getCampusUnits()

                // Load all lectures from cache
                val lecturesDirectory = File(context.filesDir, "lectures")
                if (lecturesDirectory.exists()) {
                    val lectureFiles = withContext(Dispatchers.IO) {
                        lecturesDirectory.listFiles { file -> file.extension == "json" }
                    } ?: emptyArray()

                    val loadedLectures = lectureFiles.mapNotNull { file ->
                        withContext(Dispatchers.IO) {
                            try {
                                uspDataRepository.getLecture(file.nameWithoutExtension)
                            } catch (e: Exception) {
                                Log.e("AddLectureDialog", "Error loading lecture: ${file.name}", e)
                                null
                            }
                        }
                    }

                    allLectures = loadedLectures
                    filteredLectures = loadedLectures
                } else {
                    searchError = "Nenhuma matéria encontrada. Por favor, atualize os dados da USP na tela de configurações."
                }
            } catch (e: Exception) {
                searchError = "Erro ao carregar matérias: ${e.message}"
                Log.e("AddLectureDialog", "Loading error", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Update filtered lectures when search query changes
    LaunchedEffect(searchQuery, selectedFilter, allLectures) {
        if (allLectures.isNotEmpty()) {
            filteredLectures = if (searchQuery.isBlank() && selectedFilter == null) {
                allLectures
            } else {
                allLectures.filter { lecture ->
                    val matchesFilter = selectedFilter == null || lecture.campus == selectedFilter
                    val matchesSearch = searchQuery.isBlank() || lecture.matchesSearch(searchQuery)

                    matchesFilter && matchesSearch
                }
            }
        }
    }

    // Function to check if a lecture matches the search query (fuzzy search)
    fun Lecture.matchesSearch(query: String): Boolean {
        val normalizedQuery = query.lowercase(Locale.getDefault()).trim()

        // Empty query matches everything
        if (normalizedQuery.isEmpty()) return true

        // Split the query into words for multi-word matching
        val queryWords = normalizedQuery.split(Regex("\\s+"))

        // Check each field for matches
        return queryWords.all { word ->
            code.lowercase().contains(word) ||
                    name.lowercase().contains(word) ||
                    unit.lowercase().contains(word) ||
                    department.lowercase().contains(word) ||
                    campus.lowercase().contains(word) ||

                    // Check for partial matches in lecture code
                    // MAC0110 would match "mac", "110", "m11", etc.
                    code.lowercase().windowed(word.length, 1, true)
                        .any { it == word } ||

                    // Match acronyms (e.g. "CD" would match "Calculo Diferencial")
                    (word.length > 1 && word.all { it.isLetter() } &&
                            name.split(Regex("\\s+")).filter { it.isNotEmpty() }
                                .map { it.first().lowercase() }
                                .joinToString("").contains(word))
        }
    }

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
                DialogState.SEARCH -> {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .heightIn(max = 600.dp)
                    ) {
                        Text(
                            text = "Adicionar Matéria",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Search field with better styling
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar por código, nome ou unidade") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ícone de busca"
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpar busca"
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Campus filter chips
                        if (uniqueCampuses.isNotEmpty()) {
                            Text(
                                text = "Filtrar por Campus:",
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
                                        selected = selectedFilter == null,
                                        onClick = { selectedFilter = null },
                                        label = { Text("Todos") }
                                    )
                                }

                                items(uniqueCampuses) { campus ->
                                    FilterChip(
                                        selected = selectedFilter == campus,
                                        onClick = { selectedFilter = campus },
                                        label = { Text(campus) }
                                    )
                                }
                            }
                        }

                        if (isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Carregando matérias...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        searchError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Results count
                        if (!isLoading && filteredLectures.isNotEmpty()) {
                            Text(
                                text = "${filteredLectures.size} matérias encontradas",
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
                            items(filteredLectures) { lecture ->
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
                                                dialogState = DialogState.SELECT_CLASSROOM
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
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cancel button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }

                DialogState.SELECT_CLASSROOM -> {
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
                                onClick = { dialogState = DialogState.SEARCH }
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
            }
        }
    }
}

@Preview(name = "Add Event Dialog")
@Composable
fun AddEventDialogPreview() {
    GRADUSPTheme {
        AddEventDialog(
            onDismiss = {},
            onEventAdded = {}
        )
    }
}
