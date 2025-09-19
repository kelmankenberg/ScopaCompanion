package com.example.scopacompanion

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scopacompanion.ui.theme.PrimaryBlue
import com.example.scopacompanion.ui.theme.PrimaryDarkGray
import com.example.scopacompanion.ui.theme.PrimaryDarkYellow
import com.example.scopacompanion.ui.theme.PrimaryGreen
import com.example.scopacompanion.ui.theme.PrimaryPurple
import com.example.scopacompanion.ui.theme.PrimaryRed
import com.example.scopacompanion.ui.theme.ScopaCompanionTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


// =================================================================================
// --- 1. Data Models ---
// =================================================================================

data class Player(val id: Int, var name: String)
data class Team(val id: Int, var name: String, val players: List<Player>)
data class RoundResult(val roundNumber: Int, val details: List<RoundDetail>)
data class RoundDetail(val entityId: Int, val entityName: String, val points: Int, val breakdown: String)

enum class ThemeSetting {
    LIGHT, DARK, SYSTEM
}

enum class UserPrimaryColor {
    PURPLE, RED, GREEN, BLUE, DARK_YELLOW, DARK_GRAY
}

data class GameState(
    val players: List<Player> = listOf(Player(1, "Player 1"), Player(2, "Player 2")),
    val teams: List<Team> = emptyList(),
    val targetScore: Int = 11,
    val isGameStarted: Boolean = false,
    val isTeamsMode: Boolean = false,
    val scores: Map<Int, Int> = emptyMap(),
    val roundHistory: List<RoundResult> = emptyList(),
    val isNapolaEnabled: Boolean = false,
    val isReBelloEnabled: Boolean = false,
    val winner: String? = null
)

data class RoundInput(
    val scopaCounts: Map<Int, Int>,
    val carteWinner: Int?,
    val denariWinner: Int?,
    val settebelloWinner: Int?,
    val primieraWinner: Int?,
    val napolaWinner: Int?,
    val reBelloWinner: Int?
)


// =================================================================================
// --- 2. Core ViewModel ---
// =================================================================================

class GameViewModel : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _showAddRoundDialog = MutableStateFlow(false)
    val showAddRoundDialog: StateFlow<Boolean> = _showAddRoundDialog.asStateFlow()

    private val _showCancelGameDialog = MutableStateFlow(false)
    val showCancelGameDialog: StateFlow<Boolean> = _showCancelGameDialog.asStateFlow()

    private val _showRulesScreen = MutableStateFlow(false)
    val showRulesScreen: StateFlow<Boolean> = _showRulesScreen.asStateFlow()

    private val _showSettingsScreen = MutableStateFlow(false)
    val showSettingsScreen: StateFlow<Boolean> = _showSettingsScreen.asStateFlow()

    private val _showAboutScreen = MutableStateFlow(false)
    val showAboutScreen: StateFlow<Boolean> = _showAboutScreen.asStateFlow()

    private val _currentThemeSetting = MutableStateFlow(ThemeSetting.SYSTEM)
    val currentThemeSetting: StateFlow<ThemeSetting> = _currentThemeSetting.asStateFlow()

    private val _currentPrimaryColor = MutableStateFlow(UserPrimaryColor.PURPLE) // Default to Purple
    val currentPrimaryColor: StateFlow<UserPrimaryColor> = _currentPrimaryColor.asStateFlow()

    fun openAddRoundDialog() { _showAddRoundDialog.value = true }
    fun closeAddRoundDialog() { _showAddRoundDialog.value = false }

    fun openCancelGameDialog() { _showCancelGameDialog.value = true }
    fun closeCancelGameDialog() { _showCancelGameDialog.value = false }

    fun openRulesScreen() {
        _showRulesScreen.value = true
        _showSettingsScreen.value = false
        _showAboutScreen.value = false
    }
    fun closeRulesScreen() { _showRulesScreen.value = false }

    fun openSettingsScreen() {
        _showSettingsScreen.value = true
        _showRulesScreen.value = false
        _showAboutScreen.value = false
    }
    fun closeSettingsScreen() { _showSettingsScreen.value = false }

    fun openAboutScreen() {
        _showAboutScreen.value = true
        _showRulesScreen.value = false
        _showSettingsScreen.value = false
    }
    fun closeAboutScreen() { _showAboutScreen.value = false }

    fun setTheme(theme: ThemeSetting) {
        _currentThemeSetting.value = theme
    }

    fun setPrimaryColor(color: UserPrimaryColor) {
        _currentPrimaryColor.value = color
    }

    fun submitRound(input: RoundInput) {
        _gameState.update { currentState ->
            val newScores = currentState.scores.toMutableMap()
            val roundDetails = mutableListOf<RoundDetail>()
            val entities = if (currentState.isTeamsMode) currentState.teams else currentState.players
            val entityMap = entities.associateBy({ if (it is Player) it.id else (it as Team).id }, { if (it is Player) it.name else (it as Team).name })

            entityMap.keys.forEach { id ->
                var roundPoints = 0
                val breakdown = mutableListOf<String>()

                val scopas = input.scopaCounts[id] ?: 0
                if (scopas > 0) { roundPoints += scopas; breakdown.add("$scopas Scope") }
                if (input.carteWinner == id) { roundPoints++; breakdown.add("Carte") }
                if (input.denariWinner == id) { roundPoints++; breakdown.add("Denari") }
                if (input.settebelloWinner == id) { roundPoints++; breakdown.add("Settebello") }
                if (input.primieraWinner == id) { roundPoints++; breakdown.add("Primiera") }
                if (currentState.isReBelloEnabled && input.reBelloWinner == id) { roundPoints++; breakdown.add("Re Bello") }
                if (currentState.isNapolaEnabled && input.napolaWinner == id) { roundPoints += 3; breakdown.add("Napola") }

                if (roundPoints > 0) {
                    newScores[id] = (newScores[id] ?: 0) + roundPoints
                    roundDetails.add(RoundDetail(entityId = id, entityName = entityMap[id] ?: "", points = roundPoints, breakdown = breakdown.joinToString(", ")))
                }
            }

            val newRoundResult = RoundResult(roundNumber = currentState.roundHistory.size + 1, details = roundDetails)
            var winnerName: String? = null
            for ((id, score) in newScores) {
                if (score >= currentState.targetScore) {
                    winnerName = entityMap[id]; break
                }
            }
            currentState.copy(scores = newScores, roundHistory = listOf(newRoundResult) + currentState.roundHistory, winner = winnerName)
        }
        closeAddRoundDialog()
    }

    fun newGame() { 
        _gameState.value = GameState()
        closeRulesScreen()
        closeSettingsScreen()
        closeAboutScreen()
        closeCancelGameDialog() // Ensure cancel dialog is closed if new game is started from there
    }
    fun toggleNapola(enabled: Boolean) { _gameState.update { it.copy(isNapolaEnabled = enabled) } }
    fun toggleReBello(enabled: Boolean) { _gameState.update { it.copy(isReBelloEnabled = enabled) } }

    fun setNumberOfPlayers(count: Int) {
        _gameState.update { it.copy(players = (1..count).map { Player(id = it, name = "Player $it") }, teams = emptyList(), isTeamsMode = false) }
    }

    fun setTeamsMode(enabled: Boolean) {
        if (enabled) {
            _gameState.update { it.copy(isTeamsMode = true, players = emptyList(), teams = listOf(Team(1, "Team 1", listOf(Player(1, "Player 1"), Player(3, "Player 3"))), Team(2, "Team 2", listOf(Player(2, "Player 2"), Player(4, "Player 4"))))) }
        } else {
            if (_gameState.value.players.isEmpty()) { 
                 setNumberOfPlayers(4) 
            }
            else {
                 _gameState.update { it.copy(isTeamsMode = false) }
            }
        }
    }

    fun updatePlayerName(playerId: Int, name: String) { _gameState.update { it.copy(players = it.players.map { p -> if (p.id == playerId) p.copy(name = name) else p }) } }
    fun updateTeamName(teamId: Int, name: String) { _gameState.update { it.copy(teams = it.teams.map { t -> if (t.id == teamId) t.copy(name = name) else t }) } }
    fun setTargetScore(score: String) { _gameState.update { it.copy(targetScore = score.toIntOrNull() ?: 11) } }
    fun startGame() {
        _gameState.update { currentState ->
            val initialScores = if (currentState.isTeamsMode) currentState.teams.associate { it.id to 0 } else currentState.players.associate { it.id to 0 }
            currentState.copy(isGameStarted = true, scores = initialScores, winner = null, roundHistory = emptyList()) 
        }
        closeRulesScreen() 
        closeSettingsScreen()
        closeAboutScreen()
    }

    fun onHowToPlayClicked() { 
        openRulesScreen()
    }
    fun onSettingsClicked() { 
        openSettingsScreen()
    }
    fun onAboutClicked() { 
        openAboutScreen()
    }
}


// =================================================================================
// --- 3. UI (Composable Functions) ---
// =================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues()) 
            .padding(16.dp) 
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Style, contentDescription = "Scopa Icon", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Scopa Companion", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.weight(1f)) 
            Box { 
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("How to Play") },
                        onClick = {
                            gameViewModel.onHowToPlayClicked()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.HelpOutline, "How to Play")}
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            gameViewModel.onSettingsClicked()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Settings, "Settings")}
                    )
                    DropdownMenuItem(
                        text = { Text("About") },
                        onClick = {
                            gameViewModel.onAboutClicked()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Info, "About")}
                    )
                }
            }
        }

        Card(elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Number of Players", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(2, 3, 4).forEach { count ->
                        val isSelected = gameState.players.size == count && !gameState.isTeamsMode
                        Button(
                            onClick = { gameViewModel.setNumberOfPlayers(count) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                                 else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                               else MaterialTheme.colorScheme.onSurface // Adjusted content color
                            )
                        ) {
                            Text("$count")
                        }
                    }
                }
                if (gameState.players.size == 4 || gameState.isTeamsMode) {
                    RuleVariationSwitch("2v2 Teams Mode", gameState.isTeamsMode, Icons.Default.Groups) { isEnabled ->
                        gameViewModel.setTeamsMode(isEnabled)
                    }
                }
            }
        }

        Card(elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gameState.isTeamsMode) {
                    Text("Team Names", style = MaterialTheme.typography.titleMedium)
                    gameState.teams.forEach { team -> OutlinedTextField(value = team.name, onValueChange = { gameViewModel.updateTeamName(team.id, it) }, label = { Text("Team ${team.id} Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Group, null) }) }
                } else {
                    Text("Player Names", style = MaterialTheme.typography.titleMedium)
                    gameState.players.forEach { player -> OutlinedTextField(value = player.name, onValueChange = { gameViewModel.updatePlayerName(player.id, it) }, label = { Text("Player ${player.id} Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null) }) }
                }
            }
        }

        OutlinedTextField(
            value = gameState.targetScore.toString(),
            onValueChange = { gameViewModel.setTargetScore(it) },
            label = { Text("Target Score") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Flag, null) }
        )

        Button(
            onClick = { gameViewModel.startGame() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Start Game")
            Spacer(Modifier.width(8.dp))
            Text("Start Game", fontSize = 18.sp)
        }
    }
}

@Composable
fun RuleVariationSwitch(label: String, isChecked: Boolean, icon: ImageVector, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Icon(icon, null, modifier = Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.secondary)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = isChecked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardScreen(gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()
    val showAddRoundDialog by gameViewModel.showAddRoundDialog.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scoreboard") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("How to Play") },
                            onClick = {
                                gameViewModel.onHowToPlayClicked()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.HelpOutline, "How to Play")}
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                gameViewModel.onSettingsClicked()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Settings, "Settings")}
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                gameViewModel.onAboutClicked()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Info, "About")}
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (gameState.winner == null) { // Only show action buttons if game is ongoing
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextButton(
                        onClick = { gameViewModel.openCancelGameDialog() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Cancel Game")
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel Game")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { gameViewModel.openAddRoundDialog() },
                        enabled = gameState.winner == null
                    ) {
                        Icon(Icons.Default.Add, "Add Round")
                        Spacer(Modifier.width(4.dp))
                        Text("New Round")
                    }
                }
            }
        }
    ) { scaffoldPaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "First to ${gameState.targetScore} wins!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("Player/Team", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text("Score", modifier = Modifier.width(100.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.titleMedium)
                    }
                    Divider()
                    val entitiesToDisplay = if (gameState.isTeamsMode) gameState.teams else gameState.players
                    if (entitiesToDisplay.isEmpty() && gameState.scores.isEmpty()) {
                        Text("No scores yet.", modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.CenterHorizontally))
                    } else if (gameState.isTeamsMode) {
                        gameState.teams.forEach { ScoreRow(it.name, gameState.scores[it.id] ?: 0) }
                    } else {
                        gameState.players.forEach { ScoreRow(it.name, gameState.scores[it.id] ?: 0) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Round History", style = MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (gameState.roundHistory.isEmpty()) {
                    item { Text("No rounds played yet.", modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                }
                items(gameState.roundHistory) { round -> RoundHistoryCard(round) }
            }
        }
    }
    if (showAddRoundDialog) {
        AddRoundDialog(
            gameState = gameState,
            onDismiss = { gameViewModel.closeAddRoundDialog() },
            onSubmit = { gameViewModel.submitRound(it) }
        )
    }
}

@Composable
fun ScoreRow(name: String, score: Int) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = score.toString(),
            modifier = Modifier.width(100.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        )
    }
    Divider()
}

@Composable
fun RoundHistoryCard(round: RoundResult) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("Round ${round.roundNumber}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            if (round.details.isEmpty()) {
                Text("No points scored this round.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else {
                round.details.forEach { detail ->
                    Row { Text("${detail.entityName}: ${detail.points} points", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium) }
                    Text("(${detail.breakdown})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRoundDialog(gameState: GameState, onDismiss: () -> Unit, onSubmit: (RoundInput) -> Unit) {
    val entities = remember(gameState.isTeamsMode, gameState.players, gameState.teams) {
        if (gameState.isTeamsMode) gameState.teams.map { it.id to it.name }
        else gameState.players.map { it.id to it.name }
    }
    var scopaCounts by remember(entities) { mutableStateOf(entities.associate { it.first to 0 }) }
    var carteWinner by remember { mutableStateOf<Int?>(null) }
    var denariWinner by remember { mutableStateOf<Int?>(null) }
    var settebelloWinner by remember { mutableStateOf<Int?>(null) }
    var primieraWinner by remember { mutableStateOf<Int?>(null) }
    var napolaWinner by remember { mutableStateOf<Int?>(null) }
    var reBelloWinner by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(elevation = CardDefaults.cardElevation(8.dp)) {
            Column(Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState())) {
                Text("Add Round Result", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

                if (entities.isEmpty()) {
                    Text("No players or teams configured to assign points.", modifier = Modifier.padding(bottom=16.dp))
                } else {
                    PointCategorySelector("Carte", entities, carteWinner) { carteWinner = it }
                    PointCategorySelector("Denari", entities, denariWinner) { denariWinner = it }
                    PointCategorySelector("Settebello", entities, settebelloWinner) { settebelloWinner = it }
                    PointCategorySelector("Primiera", entities, primieraWinner) { primieraWinner = it }
                    if (gameState.isReBelloEnabled) { PointCategorySelector("Re Bello", entities, reBelloWinner) { reBelloWinner = it } }
                    if (gameState.isNapolaEnabled) { PointCategorySelector("Napola (3 pts)", entities, napolaWinner) { napolaWinner = it } }

                    Divider(Modifier.padding(vertical = 8.dp))
                    Text("Scopa", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    entities.forEach { (id, name) ->
                        ScopaCounter(name = name, count = scopaCounts[id] ?: 0) { newCount ->
                            scopaCounts = scopaCounts.toMutableMap().apply { this[id] = newCount }
                        }
                    }
                }

                Row(Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (entities.isNotEmpty()) {
                                onSubmit(RoundInput(scopaCounts, carteWinner, denariWinner, settebelloWinner, primieraWinner, napolaWinner, reBelloWinner))
                            } else {
                                onDismiss()
                            }
                        }
                    ) { Text("Submit") }
                }
            }
        }
    }
}

@Composable
private fun PointCategorySelector(categoryName: String, entities: List<Pair<Int, String>>, selectedId: Int?, onSelect: (Int?) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(categoryName, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (entities.size <= 2) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = if (entities.size == 1) Arrangement.Start else Arrangement.SpaceAround
            ) {
                entities.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        RadioButton(selected = selectedId == id, onClick = { onSelect(if (selectedId == id) null else id) })
                        Text(name, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            Column {
                entities.forEach { (id, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)) {
                        RadioButton(selected = selectedId == id, onClick = { onSelect(if (selectedId == id) null else id) })
                        Text(name, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopaCounter(name: String, count: Int, onCountChange: (Int) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (count > 0) onCountChange(count - 1) }, modifier=Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("-", style = MaterialTheme.typography.bodyLarge) }
            Text(text = "$count", modifier = Modifier
                .width(40.dp)
                .padding(horizontal = 4.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = { onCountChange(count + 1) }, modifier=Modifier.size(40.dp), contentPadding = PaddingValues(0.dp)) { Text("+", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Composable
fun GameOverDialog(winnerName: String, onNewGame: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissing by click outside */ },
        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Winner Trophy", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.tertiary) },
        title = { Text("Game Over!", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("$winnerName wins the game!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = onNewGame) { Text("Start New Game") } }
    )
}

@Composable
fun CancelGameConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Game?") },
        text = { Text("Are you sure you want to cancel the current game? All progress will be lost.") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    // onDismiss() // onConfirm in this case (gameViewModel.newGame()) already closes it.
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(gameViewModel: GameViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How to Play Scopa") },
                navigationIcon = {
                    IconButton(onClick = { gameViewModel.closeRulesScreen() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Rules")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Traditional Scopa Rules", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

            RuleSection("The Deck", "Scopa is played with a standard Italian 40-card deck. This deck is divided into four suits: Coins (Denari), Cups (Coppe), Swords (Spade), and Clubs (Bastoni). Each suit has cards numbered 1 through 7, plus three face cards: Knave (Fante - worth 8), Knight (Cavallo - worth 9), and King (Re - worth 10).")

            RuleSection("Objective", "The main objective is to score points by capturing cards from the table. Points are awarded at the end of each round for various achievements, such as capturing the most cards, the most Coins, the Seven of Coins (Settebello), and having the best Primiera (Prime).")

            RuleSection("The Deal",
                "To start, one player is chosen as the dealer. The dealer shuffles the deck and deals three cards to each player, one at a time, face down. After dealing to the players, the dealer places four cards face up on the table.\n\n" +
                "If the four table cards include three or four Kings, the deal is considered invalid. The cards are re-shuffled, and the dealer deals again. This is because it's impossible to make a Scopa if three or four Kings are on the table at the start of a hand.\n\n" +
                "The player to the dealer\'s right plays first. Play proceeds counter-clockwise.")

            RuleSection("Gameplay",
                "On a player\'s turn, they play one card from their hand face up on the table. This card can be used to capture other cards.\n\n" +
                "Capturing Cards:\n" +
                "- Single Card Capture: If the card played matches the value of a single card on the table, the player captures both their played card and the table card. For example, if a player plays a 7 and there is a 7 on the table, they capture both.\n" +
                "- Multiple Card Capture: If the card played matches the sum of the values of two or more cards on the table, the player captures their played card and the combination of table cards. For example, if a player plays a 7, and the table has a 3 and a 4, they can capture the 3 and 4. If there is also a 7 on the table, the player must choose to capture either the single 7 or the combination (3+4). They cannot capture both with a single card unless it is part of a Scopa (see below). When multiple captures are possible, a player must capture a single card if that option is available.\n\n" +
                "Scopa:\n" +
                "A player achieves a \"Scopa\" (sweep) if they capture all the cards currently on the table. For each Scopa achieved, the player (or their team) scores 1 point. When a Scopa occurs, the captured cards are placed face up in the player\'s capture pile to mark the Scopa, usually with one card sticking out.\n\n" +
                "No Capture:\n" +
                "If a player cannot make a capture, they must play one card from their hand and leave it face up on the table alongside the other cards.\n\n" +
                "End of Round:\n" +
                "After all players have played their three cards, the dealer deals three more cards to each player. This continues until all cards from the deck have been dealt and played. After the last card has been played, any remaining cards on the table are awarded to the player who made the last capture. This does NOT count as a Scopa unless it actually clears the table on the last play.")

            RuleSection("Scoring",
                "At the end of each round (when all 40 cards have been played), points are calculated based on the cards each player/team has captured:\n" +
                "- Carte (Most Cards): 1 point is awarded to the player/team that captured the most cards. If there is a tie, no point is awarded.\n" +
                "- Denari (Most Coins/Diamonds): 1 point is awarded to the player/team that captured the most cards of the Coins (or Diamonds) suit. If there is a tie, no point is awarded.\n" +
                "- Settebello (Seven of Coins/Diamonds): 1 point is awarded to the player/team that captured the Seven of Coins (or Diamonds). This card is often called \"il Bello\" (the beautiful one).\n" +
                "- Primiera (Prime): 1 point is awarded for the best Primiera. The Primiera is calculated by each player/team selecting their best card from each of the four suits, based on a separate point scale (Seven = 21 points, Six = 18, Ace = 16, Five = 15, Four = 14, Three = 13, Two = 12, Face cards = 10). The player/team with the highest total Primiera score wins this point. If a player/team does not have at least one card from each suit, they cannot claim the Primiera. If both tie, no point is awarded.\n" +
                "- Scopa: 1 point is awarded for each Scopa achieved during the round.")

            RuleSection("Ending the Game", "The game typically ends when a player or team reaches a predetermined score (e.g., 11, 16, or 21 points). The first player/team to reach this score wins. If multiple players/teams reach the target score in the same round, the one with the highest score wins. If tied, another round is played.")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(gameViewModel: GameViewModel) {
    val gameState by gameViewModel.gameState.collectAsState()
    val currentTheme by gameViewModel.currentThemeSetting.collectAsState()
    val currentPrimaryColor by gameViewModel.currentPrimaryColor.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { gameViewModel.closeSettingsScreen() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Theme Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Column(Modifier.selectableGroup()) {
                ThemeSetting.values().forEach { theme ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == currentTheme),
                                onClick = { gameViewModel.setTheme(theme) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null // null recommended for accessibility with selectable parent
                        )
                        Text(
                            text = theme.name.toLowerCase().capitalize(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Primary Color", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserPrimaryColor.values().forEach { colorOption ->
                    val actualColor = when (colorOption) {
                        UserPrimaryColor.PURPLE -> PrimaryPurple
                        UserPrimaryColor.RED -> PrimaryRed
                        UserPrimaryColor.GREEN -> PrimaryGreen
                        UserPrimaryColor.BLUE -> PrimaryBlue
                        UserPrimaryColor.DARK_YELLOW -> PrimaryDarkYellow
                        UserPrimaryColor.DARK_GRAY -> PrimaryDarkGray
                    }
                    val isSelected = colorOption == currentPrimaryColor

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(actualColor)
                            .clickable { gameViewModel.setPrimaryColor(colorOption) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "${colorOption.name.replace("_", " ").toLowerCase().capitalize()} selected",
                                tint = Color.White // Explicitly White for better contrast on all colors
                            )
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Rule Variations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            RuleVariationSwitch("Re Bello (King of Coins)", gameState.isReBelloEnabled, Icons.Default.Diamond) { gameViewModel.toggleReBello(it) }
            RuleVariationSwitch("Napola (Coin Sequence)", gameState.isNapolaEnabled, Icons.Default.Filter3) { gameViewModel.toggleNapola(it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(gameViewModel: GameViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Scopa Companion") },
                navigationIcon = {
                    IconButton(onClick = { gameViewModel.closeAboutScreen() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close About Screen")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Style, // Or a custom app icon if you have one
                contentDescription = "App Icon",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scopa Companion",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Version 1.0.0", // You can make this dynamic later if needed
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp)) 
            Text(
                text = "Scopa Companion helps you keep score for the traditional Italian card game Scopa. Easily track player scores, round history, and common rule variations like Re Bello and Napola.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, 
                modifier = Modifier.padding(horizontal = 16.dp) 
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Created by Kel Mankenberg",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "© 2025", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RuleSection(title: String, text: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}


// =================================================================================
// --- 4. Main Activity & App Entry Point ---
// =================================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val gameViewModel: GameViewModel = viewModel()
            val themeSetting by gameViewModel.currentThemeSetting.collectAsState()
            val selectedPrimaryColorEnum by gameViewModel.currentPrimaryColor.collectAsState()

            val userSelectedColor = when (selectedPrimaryColorEnum) {
                UserPrimaryColor.PURPLE -> PrimaryPurple
                UserPrimaryColor.RED -> PrimaryRed
                UserPrimaryColor.GREEN -> PrimaryGreen
                UserPrimaryColor.BLUE -> PrimaryBlue
                UserPrimaryColor.DARK_YELLOW -> PrimaryDarkYellow
                UserPrimaryColor.DARK_GRAY -> PrimaryDarkGray
            }

            ScopaCompanionTheme(
                themeSetting = themeSetting,
                userSelectedPrimaryColor = userSelectedColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(gameViewModel)
                }
            }
        }
    }
}

@Composable
fun App(gameViewModel: GameViewModel = viewModel()) {
    val gameState by gameViewModel.gameState.collectAsState()
    val showRules by gameViewModel.showRulesScreen.collectAsState()
    val showSettings by gameViewModel.showSettingsScreen.collectAsState()
    val showAbout by gameViewModel.showAboutScreen.collectAsState()
    val showCancelGameDialog by gameViewModel.showCancelGameDialog.collectAsState()

    if (showAbout) {
        AboutScreen(gameViewModel = gameViewModel)
    } else if (showSettings) {
        SettingsScreen(gameViewModel = gameViewModel)
    } else if (showRules) {
        RulesScreen(gameViewModel = gameViewModel)
    } else if (gameState.isGameStarted) {
        ScoreboardScreen(gameViewModel)
    } else {
        GameSetupScreen(gameViewModel)
    }

    gameState.winner?.let { winnerName ->
        GameOverDialog(winnerName = winnerName, onNewGame = { gameViewModel.newGame() })
    }

    if (showCancelGameDialog) {
        CancelGameConfirmationDialog(
            onConfirm = { gameViewModel.newGame() }, // newGame() now also handles closing the dialog
            onDismiss = { gameViewModel.closeCancelGameDialog() }
        )
    }
}
