package com.prototype.gradusp.ui.components.dialogs

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.prototype.gradusp.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

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
    onLectureSelected: (Lecture, Classroom) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Lecture>>(emptyList()) }
    var selectedLecture by remember { mutableStateOf<Lecture?>(null) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Define dialog state
    var dialogState by remember { mutableStateOf(DialogState.SEARCH) }

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
                            .heightIn(max = 500.dp)
                    ) {
                        Text(
                            text = "Adicionar Matéria",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Buscar por código ou nome da matéria") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            coroutineScope.launch {
                                                isSearching = true
                                                searchError = null
                                                try {
                                                    // Create a parser instance directly
                                                    val parser = UspParser(context)

                                                    // If it looks like a lecture code (e.g., MAC0110)
                                                    if (searchQuery.matches(Regex("[A-Z]{3}\\d{4}"))) {
                                                        val lecture = parser.fetchLecture(searchQuery)
                                                        searchResults = listOfNotNull(lecture)
                                                    } else {
                                                        // Simplified search - in a real app, implement a more robust search
                                                        searchError = "Por favor, digite o código exato da disciplina (ex: MAC0110)"
                                                        searchResults = emptyList()
                                                    }

                                                    if (searchResults.isEmpty() && searchError == null) {
                                                        searchError = "Nenhuma matéria encontrada com esse termo"
                                                    }
                                                } catch (e: Exception) {
                                                    searchError = "Erro ao buscar matérias: ${e.message}"
                                                    Log.e("AddLectureDialog", "Search error", e)
                                                } finally {
                                                    isSearching = false
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Buscar"
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isSearching) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        searchError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search results
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(searchResults) { lecture ->
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
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "${lecture.unit} - ${lecture.department}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Créditos: ${lecture.lectureCredits} (aula) + ${lecture.workCredits} (trabalho)",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
                            .heightIn(max = 500.dp)
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
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        Text(
                                            text = "Turma ${classroom.code}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )

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
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "Professores: ${classroom.teachers.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall
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