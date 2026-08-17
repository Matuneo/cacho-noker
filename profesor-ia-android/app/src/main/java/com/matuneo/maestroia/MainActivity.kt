package com.matuneo.maestroia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matuneo.maestroia.data.CourseContent
import com.matuneo.maestroia.data.GuidedProject
import com.matuneo.maestroia.data.StudyModule
import com.matuneo.maestroia.ui.Amber
import com.matuneo.maestroia.ui.Cyan
import com.matuneo.maestroia.ui.Green
import com.matuneo.maestroia.ui.Night
import com.matuneo.maestroia.ui.NightSoft
import com.matuneo.maestroia.ui.Panel
import com.matuneo.maestroia.ui.ProfesorIATheme
import com.matuneo.maestroia.ui.TextMain
import com.matuneo.maestroia.ui.TextMuted
import com.matuneo.maestroia.ui.Violet

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfesorIATheme {
                val vm: AppViewModel = viewModel()
                ProfesorIAApp(vm)
            }
        }
    }
}

private data class NavItem(val tab: AppTab, val label: String, val icon: ImageVector)

@Composable
private fun ProfesorIAApp(vm: AppViewModel) {
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importModel)
    }
    val navigation = remember {
        listOf(
            NavItem(AppTab.HOME, "Inicio", Icons.Rounded.Home),
            NavItem(AppTab.ROUTES, "Rutas", Icons.Rounded.School),
            NavItem(AppTab.TUTOR, "Tutor", Icons.Rounded.SmartToy),
            NavItem(AppTab.LAB, "Código", Icons.Rounded.Code),
            NavItem(AppTab.PROJECTS, "Proyectos", Icons.Rounded.RocketLaunch)
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Night) {
        Scaffold(
            containerColor = Night,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = { AppHeader(vm) },
            bottomBar = {
                NavigationBar(containerColor = NightSoft, modifier = Modifier.navigationBarsPadding()) {
                    navigation.forEach { item ->
                        NavigationBarItem(
                            selected = vm.currentTab == item.tab,
                            onClick = { vm.currentTab = item.tab },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label, fontSize = 10.sp, maxLines = 1) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (vm.currentTab) {
                    AppTab.HOME -> HomeScreen(vm, onImport = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) })
                    AppTab.ROUTES -> RoutesScreen(vm)
                    AppTab.TUTOR -> TutorScreen(vm, onImport = { importLauncher.launch(arrayOf("application/octet-stream", "*/*")) })
                    AppTab.LAB -> LabScreen(vm)
                    AppTab.PROJECTS -> ProjectsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(vm: AppViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Night.copy(alpha = 0.97f)).statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Cyan, Violet))),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Code, null, tint = Night) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("PROFESOR IA", fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = TextMain)
            Text("PYTHON + ANDROID STUDIO", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.6.sp)
        }
        Row(
            Modifier.clip(CircleShape).background(if (vm.modelReady) Green.copy(alpha = 0.14f) else Panel).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (vm.modelReady) Green else Amber))
            Spacer(Modifier.width(6.dp))
            Text(if (vm.modelReady) "IA LOCAL" else "CURSO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (vm.modelReady) Green else Amber)
        }
    }
}

@Composable
private fun HomeScreen(vm: AppViewModel, onImport: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 14.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HeroCard(vm) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("16", "Módulos", Icons.Rounded.School, Cyan, Modifier.weight(1f))
                MetricCard("112", "Lecciones", Icons.Rounded.AutoAwesome, Violet, Modifier.weight(1f))
                MetricCard("6", "Proyectos", Icons.Rounded.RocketLaunch, Green, Modifier.weight(1f))
            }
        }
        item { ModelPanel(vm, onImport) }
        item {
            SectionTitle("Continúa aprendiendo", "Tu siguiente paso recomendado")
            Spacer(Modifier.height(10.dp))
            ModulePreview(CourseContent.allModules.firstOrNull { !vm.completedModules.contains(it.id) } ?: CourseContent.python.first()) {
                vm.currentTab = AppTab.ROUTES
            }
        }
        item {
            SectionTitle("Capacidades del profesor", "Aprende haciendo, no memorizando")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeatureRow(Icons.Rounded.SmartToy, Cyan, "Tutor adaptativo", "Explica según tu nivel y analiza código línea por línea.")
                FeatureRow(Icons.Rounded.Terminal, Green, "Python ejecutable", "Ejecuta ejercicios en una zona aislada y muestra el error exacto.")
                FeatureRow(Icons.Rounded.Analytics, Violet, "Diagnóstico profesional", "Revisa arquitectura, rendimiento, seguridad y pruebas.")
                FeatureRow(Icons.Rounded.Lock, Amber, "Privado y local", "Después de instalar el modelo, tus conversaciones no salen del teléfono.")
            }
        }
    }
}

@Composable
private fun HeroCard(vm: AppViewModel) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF15324E), Color(0xFF20184A), NightSoft))).padding(22.dp)
        ) {
            Text("DOMINA EL CÓDIGO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Cyan, letterSpacing = 1.6.sp)
            Spacer(Modifier.height(8.dp))
            Text("De tu primera variable a aplicaciones Android avanzadas.", fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, color = TextMain)
            Spacer(Modifier.height(10.dp))
            Text("Un camino guiado, laboratorio real y un mentor de IA dentro del teléfono.", color = TextMuted, lineHeight = 21.sp)
            Spacer(Modifier.height(18.dp))
            Text("PROGRESO GENERAL  ${(vm.progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { vm.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = Cyan,
                trackColor = Color.White.copy(alpha = 0.10f)
            )
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = NightSoft)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun ModelPanel(vm: AppViewModel, onImport: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bolt, null, tint = if (vm.modelReady) Green else Cyan)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Motor de IA en el dispositivo", fontWeight = FontWeight.Bold)
                    Text(vm.modelUi.message, color = TextMuted, fontSize = 12.sp)
                }
                if (vm.modelUi.phase == ModelPhase.LOADING) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                if (vm.modelReady) Icon(Icons.Rounded.CheckCircle, null, tint = Green)
            }
            if (vm.modelUi.phase == ModelPhase.DOWNLOADING) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { vm.modelUi.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                    color = Cyan
                )
            }
            if (!vm.modelReady && vm.modelUi.phase != ModelPhase.DOWNLOADING && vm.modelUi.phase != ModelPhase.LOADING) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::downloadRecommendedModel, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Instalar 347 MB")
                    }
                    OutlinedButton(onClick = onImport) {
                        Icon(Icons.Rounded.UploadFile, null, Modifier.size(18.dp)); Spacer(Modifier.width(7.dp)); Text("Importar")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutesScreen(vm: AppViewModel) {
    val modules = if (vm.selectedTrack == "Python") CourseContent.python else CourseContent.android
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Rutas maestras", "Marca cada módulo cuando puedas explicarlo y aplicarlo")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = vm.selectedTrack == "Python", onClick = { vm.selectedTrack = "Python" }, label = { Text("Python") }, leadingIcon = { Icon(Icons.Rounded.Terminal, null, Modifier.size(18.dp)) })
                FilterChip(selected = vm.selectedTrack == "Android", onClick = { vm.selectedTrack = "Android" }, label = { Text("Android Studio") }, leadingIcon = { Icon(Icons.Rounded.Code, null, Modifier.size(18.dp)) })
            }
        }
        items(modules, key = { it.id }) { module ->
            ModuleCard(module, vm.completedModules.contains(module.id)) { vm.toggleModule(module.id) }
        }
    }
}

@Composable
private fun ModuleCard(module: StudyModule, completed: Boolean, onToggle: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = if (completed) Green.copy(alpha = 0.09f) else NightSoft)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (completed) Green.copy(alpha = 0.18f) else Cyan.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Text(module.id.takeLast(2), fontWeight = FontWeight.Black, color = if (completed) Green else Cyan) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(module.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(module.level, color = if (completed) Green else Violet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onToggle) {
                    Icon(Icons.Rounded.CheckCircle, "Completar", tint = if (completed) Green else TextMuted)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(module.description, color = TextMuted, lineHeight = 19.sp, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            module.lessons.forEachIndexed { index, lesson ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (completed) Green else Cyan))
                    Spacer(Modifier.width(9.dp))
                    Text("${index + 1}. $lesson", fontSize = 12.sp, color = TextMain.copy(alpha = 0.88f))
                }
            }
        }
    }
}

@Composable
private fun TutorScreen(vm: AppViewModel, onImport: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(vm.messages.size, vm.messages.lastOrNull()?.text) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp).imePadding()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tutor personal", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(if (vm.modelReady) "Modelo local activo" else "Modo curso · instala la IA para respuestas personalizadas", color = if (vm.modelReady) Green else Amber, fontSize = 11.sp)
            }
            IconButton(onClick = vm::clearConversation) { Icon(Icons.Rounded.DeleteSweep, "Reiniciar") }
        }
        if (!vm.modelReady) {
            ModelPanel(vm, onImport)
            Spacer(Modifier.height(8.dp))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            items(QUICK_PROMPTS) { prompt ->
                OutlinedButton(onClick = { vm.sendQuestion(prompt) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(prompt, maxLines = 1, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
        ) {
            items(vm.messages, key = { it.id }) { message -> ChatBubble(message) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = vm.chatInput,
                onValueChange = { vm.chatInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Pregunta, pega un error o un fragmento de código…") },
                maxLines = 5,
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { vm.sendQuestion() },
                modifier = Modifier.size(52.dp).clip(CircleShape).background(Cyan)
            ) { Icon(Icons.Rounded.Send, "Enviar", tint = Night) }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.user) Arrangement.End else Arrangement.Start) {
        Card(
            modifier = Modifier.fillMaxWidth(if (message.user) 0.86f else 0.96f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = if (message.user) Color(0xFF153D52) else NightSoft)
        ) {
            SelectionContainer {
                Text(message.text, modifier = Modifier.padding(14.dp), color = TextMain, lineHeight = 20.sp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LabScreen(vm: AppViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp, 10.dp, 14.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Laboratorio de código", "Ejecuta Python o analiza Python, Kotlin, Java, Gradle y XML") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(listOf("Python", "Kotlin", "Java", "Gradle", "XML")) { language ->
                    FilterChip(selected = vm.selectedLanguage == language, onClick = { vm.selectedLanguage = language }, label = { Text(language) })
                }
            }
        }
        item {
            OutlinedTextField(
                value = vm.code,
                onValueChange = { vm.code = it },
                modifier = Modifier.fillMaxWidth().height(310.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 19.sp),
                label = { Text("Código ${vm.selectedLanguage}") },
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = vm::runPython,
                    enabled = vm.selectedLanguage == "Python" && !vm.busyLab,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Night)
                ) { Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Ejecutar") }
                Button(onClick = vm::analyzeCode, enabled = !vm.busyLab, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Analytics, null); Spacer(Modifier.width(6.dp)); Text("Analizar")
                }
            }
        }
        if (vm.busyLab) item {
            Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Procesando…", color = TextMuted)
            }
        }
        vm.execution?.let { result ->
            item {
                ResultCard(
                    title = if (result.success) "Ejecución correcta" else "Error de ejecución",
                    body = buildString {
                        if (result.output.isNotBlank()) appendLine(result.output.trim())
                        if (result.error.isNotBlank()) append(if (result.line > 0) "Línea ${result.line}: ${result.error}" else result.error)
                    },
                    success = result.success
                )
            }
        }
        if (vm.codeAnalysis.isNotBlank()) item { ResultCard("Revisión del profesor", vm.codeAnalysis, vm.modelReady) }
        item {
            Text("SEGURIDAD DEL LABORATORIO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Amber, letterSpacing = 1.sp)
            Text("La ejecución limita tiempo, tamaño y funciones peligrosas. No permite importar módulos ni acceder a archivos del teléfono.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ResultCard(title: String, body: String, success: Boolean) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (success) Green.copy(alpha = 0.08f) else Color(0xFF351C2A))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (success) Green else Color(0xFFFF7B8B))
            Spacer(Modifier.height(8.dp))
            SelectionContainer { Text(body, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 18.sp) }
        }
    }
}

@Composable
private fun ProjectsScreen(vm: AppViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 12.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Proyectos de portafolio", "Construye productos completos con hitos verificables") }
        items(CourseContent.projects) { project -> ProjectCard(project) { vm.currentTab = AppTab.TUTOR; vm.sendQuestion("Guíame paso a paso para construir: ${project.title}. Mi nivel es ${project.level}.") } }
    }
}

@Composable
private fun ProjectCard(project: GuidedProject, onStart: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NightSoft)) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.RocketLaunch, null, tint = Violet)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(project.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("${project.level} · ${project.technologies}", color = Cyan, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(project.outcome, color = TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Text(project.milestones.joinToString("  →  "), color = TextMain.copy(alpha = 0.86f), fontSize = 11.sp)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Empezar con el tutor") }
        }
    }
}

@Composable
private fun ModulePreview(module: StudyModule, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = NightSoft)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (module.track == "Python") Icons.Rounded.Terminal else Icons.Rounded.Code, null, tint = Cyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(module.title, fontWeight = FontWeight.Bold)
                Text("${module.track} · ${module.level} · ${module.lessons.size} lecciones", color = TextMuted, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.PlayArrow, null, tint = Cyan)
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, color: Color, title: String, detail: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = NightSoft)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detail, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black, color = TextMain)
    Text(subtitle, color = TextMuted, fontSize = 12.sp)
}

private val QUICK_PROMPTS = listOf(
    "Enséñame Python desde cero",
    "Explícame MVVM con un ejemplo",
    "¿Cómo depuro un crash?",
    "Créame un reto de algoritmos",
    "Analiza una app Android compleja"
)
