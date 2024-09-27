package com.example.party_bot

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.party_bot.ui.theme.Party_botTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Party_botTheme {
                // Установим NavHost и NavController
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(navController = navController, startDestination = "main_screen", modifier = Modifier.padding(innerPadding)) {
                        composable("main_screen") { MainScreen(navController = navController, context = this@MainActivity) }
                        composable("users_screen") { UsersScreen(context = this@MainActivity) }
                        composable("create_event_screen") { CreateEventScreen(context = this@MainActivity, navController = navController) }
                        composable("archive_screen") { ArchiveScreen(context = this@MainActivity) }
                    }

                }
                }
        }
        }
    }

var chatIdGlobal: String? = null
var username: String? = null

// Функция для получения пути к файлу
fun getFile(context: Context): File {
    return File(context.filesDir, "user_data.txt")
}

// Функция для сохранения данных в файл
fun saveUserToFile(context: Context, name: String, tg: String) {
    val file = getFile(context)
    file.appendText("$name:$tg:\r\n")  // Убедитесь, что \n добавляется после каждой записи
}

// Функция для чтения данных из файла
fun readUsersFromFile(context: Context): List<String> {
    val file = File(context.filesDir, "user_data.txt")
    return if (file.exists()) {
        file.readLines().filter { it.isNotBlank() } // Убираем пустые строки
    } else {
        emptyList()
    }
}

@Composable
fun MainScreen(navController: NavController, context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Контекстные меню
        MenuSection(navController = navController, context = context)
    }
}

@Composable
fun MenuSection(navController: NavController, context: Context) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Меню Пользователи
        ContextMenu(
            title = "Пользователи",
            menuItems = listOf("Просмотр", "Добавить"),
            onItemClicked = { item ->
                when (item) {
                    "Просмотр" -> navController.navigate("users_screen")
                    "Добавить" -> showDialog = true
                }
            }
        )

        // Меню Событие
        ContextMenu(
            title = "Событие",
            menuItems = listOf("Создать", "Архив"),
            onItemClicked = { item ->
                when (item) {
                    "Создать" -> navController.navigate("create_event_screen")
                    "Архив" -> navController.navigate("archive_screen")
                }
            }
        )

        if (showDialog) {
            AddUserDialog(onDismiss = { showDialog = false }, context = context)
        }
    }
}

@Composable
fun AddUserDialog(onDismiss: () -> Unit, context: Context) {
    var name by remember { mutableStateOf("") }
    var tg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                // Логика для сохранения данных в файл
                saveUserToFile(context, name, tg)
                onDismiss()
            }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Добавить пользователя") },
        text = {
            Column {
                // Поле ввода для Имени
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Поле ввода для Тг
                OutlinedTextField(
                    value = tg,
                    onValueChange = { tg = it },
                    label = { Text("Тг") }
                )
            }
        }
    )
}

@Composable
fun ContextMenu(title: String, menuItems: List<String>, onItemClicked: (String) -> Unit = {}) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        // Кнопка для отображения меню
        Button(onClick = { expanded = true }) {
            Text(text = title)
        }

        // Выпадающее меню
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onItemClicked(item) // Передаем название выбранного элемента
                    },
                    text = {
                        Text(text = item)
                    }
                )
            }
        }
    }
}

// Экран "Пользователи"
@Composable
fun UsersScreen(context: Context) {
    // Получаем список пользователей
    var users = remember { mutableStateListOf(*readUsersFromFile(context).toTypedArray()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Пользователи",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        users.forEachIndexed { index, user ->
            val parts = user.split(":")
            val name = parts[0]
            val tg = parts[1]
            val chatId = if (parts.size > 2) parts[2] else "Не установлен"

            // Отображаем запись с контекстным меню и кнопкой обновления Chat ID
            UserRow(
                name = name,
                tg = tg,
                chatId = chatId,
                onEdit = { updatedName, updatedTg ->
                    // Обновляем запись
                    users[index] = "$updatedName:$updatedTg:$chatId"
                    saveUsersToFile(context, users)
                },
                onDelete = {
                    // Удаляем запись
                    users.removeAt(index)
                    saveUsersToFile(context, users)
                },
                onUpdateChatId = {
                    // Запускаем getUpdates для обновления chatId
                    getUpdates(tg) {
                        // После получения chatId обновляем запись
                        users[index] = "$name:$tg:$chatIdGlobal"
                        saveUsersToFile(context, users)
                    }
                }
            )
        }
    }
}

@Composable
fun UserRow(
    name: String,
    tg: String,
    chatId: String?,
    onEdit: (String, String) -> Unit,
    onDelete: () -> Unit,
    onUpdateChatId: () -> Unit
) {
    var isEditDialogOpen by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    var updatedName by remember { mutableStateOf(name) }
    var updatedTg by remember { mutableStateOf(tg) }

    // Строка пользователя
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = tg, style = MaterialTheme.typography.bodyMedium)
            chatId?.let {
                Text(text = "Chat ID: $chatId", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Кнопка редактирования
        IconButton(onClick = { isEditDialogOpen = true }) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Редактировать")
        }

        // Кнопка удаления (с диалогом подтверждения)
        IconButton(onClick = { isDeleteDialogOpen = true }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить")
        }

        // Кнопка добавления chatId
        IconButton(onClick = { onUpdateChatId() }) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить chatId")
        }
    }

    // Диалог для редактирования
    if (isEditDialogOpen) {
        AlertDialog(
            onDismissRequest = { isEditDialogOpen = false },
            title = { Text(text = "Редактировать пользователя") },
            text = {
                Column {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("Имя") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = updatedTg,
                        onValueChange = { updatedTg = it },
                        label = { Text("Тг") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEdit(updatedName, updatedTg) // Обновляем данные пользователя
                    isEditDialogOpen = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(onClick = { isEditDialogOpen = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог подтверждения удаления
    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            title = { Text(text = "Удалить пользователя") },
            text = { Text(text = "Вы уверены, что хотите удалить этого пользователя?") },
            confirmButton = {
                Button(onClick = {
                    onDelete() // Удаляем пользователя
                    isDeleteDialogOpen = false
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                Button(onClick = { isDeleteDialogOpen = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun EditUserDialog(
    name: String,
    tg: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var updatedName by remember { mutableStateOf(name) }
    var updatedTg by remember { mutableStateOf(tg) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onConfirm(updatedName, updatedTg)
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Редактировать пользователя") },
        text = {
            Column {
                OutlinedTextField(
                    value = updatedName,
                    onValueChange = { updatedName = it },
                    label = { Text("Имя") }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = updatedTg,
                    onValueChange = { updatedTg = it },
                    label = { Text("Тг") }
                )
            }
        }
    )
}

// Функция для сохранения обновленных данных в файл
fun saveUsersToFile(context: Context, users: List<String>) {
    val file = File(context.filesDir, "user_data.txt")
    file.printWriter().use { writer ->
        users.forEach { user ->
            writer.println(user)
        }
        // Добавляем пустую строку для отступа
        writer.println()
    }
}

@Composable
@Preview(showBackground = true)
fun UserRowPreview() {
    Party_botTheme {
        UserRow(
            name = "Имя",
            tg = "Тг",
            chatId = "123456789", // Значение для chatId
            onEdit = { _, _ -> },
            onDelete = {},
            onUpdateChatId = {}  // Пустой callback для onUpdateChatId
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    Party_botTheme {
        val navController = rememberNavController()
        val context = LocalContext.current

        MainScreen(
            navController = navController,
            context = context
        )
    }
}

// Экран для создания события
@Composable
fun CreateEventScreen(context: Context, navController: NavController) {
    // Считываем пользователей из файла
    val users = remember { mutableStateListOf(*readUsersFromFile(context).map { it.split(":")[0] }.toTypedArray()) }
    val selectedUsers = remember { mutableStateMapOf<String, Boolean>().apply { users.forEach { this[it] = false } } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        // Список пользователей в виде чекбоксов
        Column {
            users.forEach { user ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedUsers[user] == true,
                        onCheckedChange = { isChecked ->
                            selectedUsers[user] = isChecked
                        }
                    )
                    Text(
                        text = user,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // Кнопка Создать
        Button(
            onClick = {
                // Логика создания файла
                val selectedNames = selectedUsers.filter { it.value }.keys
                if (selectedNames.isNotEmpty()) {
                    val fileName = SimpleDateFormat("MM.dd.yyyy_HH.mm.ss", Locale.getDefault()).format(Date()) + ".txt"
                    val file = File(context.filesDir, fileName)
                    file.writeText(selectedNames.joinToString("\n") { "$it: 0" })
                }
                // Закрываем экран после создания события
                navController.navigateUp()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(text = "Создать")
        }
    }
}

@Composable
fun ArchiveScreen(context: Context) {
    var selectedFile by remember { mutableStateOf<String?>(null) }

    // Получаем список файлов, исключая "user_data.txt" и "profileInstalled", сортируем по дате создания (новые выше)
    val files = remember {
        context.filesDir.listFiles()
            ?.filter { it.name != "user_data.txt" && it.name != "profileInstalled" }
            ?.sortedByDescending { it.lastModified() }  // Сортировка по последней модификации (новые сверху)
            ?.map { it.name } ?: emptyList()
    }

    var eventData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка для показа выпадающего списка файлов
        Button(onClick = { isDropdownExpanded = true }) {
            Text(text = selectedFile ?: "Выберите файл")
        }

        // Выпадающее меню для выбора файла
        DropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            files.forEach { fileName ->
                DropdownMenuItem(
                    text = { Text(fileName) },
                    onClick = {
                        // Сбрасываем старые данные перед загрузкой новых
                        selectedFile = fileName
                        eventData = emptyList()  // Сбрасываем данные предыдущего файла
                        eventData = readEventFileContent(context, fileName) // Загружаем новый файл
                        isDropdownExpanded = false
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Отображение содержимого выбранного файла
        if (eventData.isNotEmpty()) {
            LazyColumn {
                items(eventData) { (name, number) ->
                    getTGInfoByName(context, name) // Получаем TG информацию для имени
                    ClickableNameRow(
                        context = context,
                        fileName = selectedFile!!,
                        name = name,
                        number = number
                    )
                }
            }
        } else {
            Text(text = "Нет данных для отображения", fontSize = 18.sp)
        }
    }
}

@Composable
fun EditableNumberRow(context: Context, fileName: String, name: String, number: Int) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var updatedNumber by remember { mutableStateOf(number.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, modifier = Modifier.weight(1f))

        // Отображаем текущее число
        Text(text = updatedNumber, modifier = Modifier.weight(1f), textAlign = TextAlign.End)

        // Кнопка для редактирования числа
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Number")
        }

        // Кнопка для добавления числа
        IconButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add Number")
        }
    }

    // Диалоговое окно для редактирования числа
    if (showEditDialog) {
        EditNumberDialog(
            initialNumber = updatedNumber,
            onNumberChanged = { newNumber ->
                updatedNumber = newNumber
                showEditDialog = false
                // Обновление числа в файле
                updateNumberInFile(context, fileName, name, newNumber.toInt())
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Диалог для добавления числа
    if (showAddDialog) {
        AddNumberDialog(
            onAdd = { addValue ->
                val newTotal = updatedNumber.toInt() + addValue
                updatedNumber = newTotal.toString()
                showAddDialog = false
                // Обновляем результат в файле
                updateNumberInFile(context, fileName, name, newTotal)
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun EditNumberDialog(initialNumber: String, onNumberChanged: (String) -> Unit, onDismiss: () -> Unit) {
    var number by remember { mutableStateOf(initialNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onNumberChanged(number) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Введите новое число") },
        text = {
            TextField(
                value = number,
                onValueChange = { newNumber ->
                    if (newNumber.all { it.isDigit() }) {
                        number = newNumber
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun AddNumberDialog(onAdd: (Int) -> Unit, onDismiss: () -> Unit) {
    var addValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val valueToAdd = addValue.toIntOrNull()
                if (valueToAdd != null) {
                    onAdd(valueToAdd)
                }
            }) {
                Text("Прибавить")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        title = { Text("Введите сумму для прибавления") },
        text = {
            TextField(
                value = addValue,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        addValue = newValue
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

// Функция для обновления числа в файле
fun updateNumberInFile(context: Context, fileName: String, name: String, newNumber: Int) {
    val file = File(context.filesDir, fileName)
    val lines = file.readLines().toMutableList()

    // Обновляем нужную строку
    val updatedLines = lines.map { line ->
        val parts = line.split(":")
        if (parts[0].trim() == name) {
            "$name: $newNumber"
        } else {
            line
        }
    }

    // Записываем обновленные строки обратно в файл
    file.writeText(updatedLines.joinToString("\n"))
}

@Composable
fun ClickableNameRow(context: Context, fileName: String, name: String, number: Int) {
    var showUserDataDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var updatedNumber by remember { mutableStateOf(number.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Имя становится кликабельным
        Text(
            text = name,
            modifier = Modifier
                .weight(1f)
                .clickable { showUserDataDialog = true } // Открываем диалог при клике на имя
        )

        // Отображаем текущее число
        Text(text = updatedNumber, modifier = Modifier.weight(1f), textAlign = TextAlign.End)

        // Кнопка для редактирования числа
        IconButton(onClick = { showEditDialog = true }) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Number")
        }

        // Кнопка для добавления числа
        IconButton(onClick = { showAddDialog = true }) {
            Icon(Icons.Default.Add, contentDescription = "Add Number")
        }
    }

    // Диалог для редактирования числа
    if (showEditDialog) {
        EditNumberDialog(
            initialNumber = updatedNumber,
            onNumberChanged = { newNumber ->
                updatedNumber = newNumber
                showEditDialog = false
                // Обновление числа в файле
                updateNumberInFile(context, fileName, name, newNumber.toInt())
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // В диалоге для добавления числа
    if (showAddDialog) {
        AddNumberDialog(
            onAdd = { addValue ->
                val newTotal = updatedNumber.toInt() + addValue
                updatedNumber = newTotal.toString()  // Обновляем значение
                showAddDialog = false
                // Обновляем результат в файле
                updateNumberInFile(context, fileName, name, newTotal)
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Если кликаем на имя, показываем диалог с данными пользователя
    if (showUserDataDialog) {
        val tgInfo = getTGInfoByName(context, name)  // Получаем информацию ТГ
        ShowUserDataDialog(
            name = name,
            tg = tgInfo,
            number = updatedNumber.toInt(),  // Убедитесь, что передаете актуальное значение
            context = context,
            onDismiss = { showUserDataDialog = false }
        )
    }
}

@Composable
fun ShowUserDataDialog(name: String, tg: String?, number: Int, context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = {
                // Проверяем наличие Telegram логина и отправляем сообщение
                tg?.let {
                    // Ищем chatId в файле пользователей
                    val chatId = getChatIdByTg(it, context)

                    if (chatId != null) {
                        // Отправляем сообщение через Telegram
                        sendTelegramMessage(chatId, "Ваша сумма: $number")  // Число из окна
                    } else {
                        Log.e("TelegramBot", "Не удалось найти chatId для пользователя с ТГ: $tg")
                    }
                }
                onDismiss()
            }) {
                Text("Отправить сообщение")
            }
        },
        title = { Text("Информация о пользователе") },
        text = {
            Column {
                Text(text = "Имя: $name")
                Text(text = "ТГ: $tg")
                Text(text = "Число: $number")
            }
        }
    )
}

// Функция для получения информации ТГ по имени из файла user_data.txt
fun getTGInfoByName(context: Context, name: String): String? {
    val file = File(context.filesDir, "user_data.txt")
    var tgInfo: String? = null  // Переменная для хранения информации о ТГ
    if (file.exists()) {
        file.forEachLine { line ->
            val parts = line.split(":")
            if (parts.size >= 2) {
                val storedName = parts[0].trim()
                val tg = parts[1].trim()
                if (storedName == name) {
                    tgInfo = tg  // Сохраняем поле ТГ, если имя совпадает
                }
            }
        }
    }
    return tgInfo  // Возвращаем найденное поле ТГ или null, если не найдено
}

// Функция для чтения данных события
fun readEventFileContent(context: Context, fileName: String): List<Pair<String, Int>> {
    val file = File(context.filesDir, fileName)
    val contentList = mutableListOf<Pair<String, Int>>()
    if (file.exists()) {
        file.forEachLine { line ->
            val parts = line.split(":")
            if (parts.size == 2) {
                val name = parts[0].trim()
                val number = parts[1].trim().toIntOrNull() ?: 0
                contentList.add(Pair(name, number))
            }
        }
    }
    return contentList
}

// Функция getUpdates с передачей username и callback
fun getUpdates(tgUsername: String, callback: () -> Unit) {
    val url = "https://api.telegram.org/bot7349774648:AAEQBtzD1vidVPRjAbc3eKnsDrc8fofbBRo/getUpdates"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("TelegramBot", "Ошибка получения обновлений: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseData = response.body?.string()
            if (!response.isSuccessful) {
                Log.e("TelegramBot", "Ошибка получения обновлений: $responseData")
            } else {
                Log.i("TelegramBot", "Обновления: $responseData")

                // Парсинг JSON-ответа
                val jsonResponse = JSONObject(responseData.toString())
                val resultArray: JSONArray = jsonResponse.getJSONArray("result")

                // Проверяем, есть ли обновления
                if (resultArray.length() > 0) {
                    for (i in 0 until resultArray.length()) {
                        val update = resultArray.getJSONObject(i)
                        val message = update.optJSONObject("message") ?: continue
                        val chat = message.getJSONObject("chat")

                        // Извлекаем chat_id и username
                        val chatId = chat.getString("id")
                        val username = chat.optString("username", "").removePrefix("@") // Убираем @

                        // Проверяем, совпадает ли username с tgUsername
                        if (username.equals(tgUsername, ignoreCase = true)) {
                            // Сохраняем chatId в глобальную переменную
                            chatIdGlobal = chatId
                            Log.i("TelegramBot", "chat_id: $chatIdGlobal")
                            Log.i("TelegramBot", "username: $username")

                            // Вызываем callback после получения chatId
                            callback()
                            return  // Выходим, чтобы избежать лишних итераций
                        }
                    }
                } else {
                    Log.i("TelegramBot", "Нет новых обновлений")
                }
            }
        }
    })
}
/*
fun getChatIdByUsername(username: String, onChatIdRetrieved: (String) -> Unit) {
    val url = "https://api.telegram.org/bot7349774648:AAEQBtzD1vidVPRjAbc3eKnsDrc8fofbBRo/getChat?chat_id=@$username"
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("TelegramBot", "Ошибка получения chat_id: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseData = response.body?.string()
            if (!response.isSuccessful) {
                Log.e("TelegramBot", "Ошибка получения chat_id: $responseData")
            } else {
                try {
                    val jsonObject = JSONObject(responseData.toString())
                    val chatId = jsonObject.getJSONObject("result").getString("id")
                    Log.i("TelegramBot", "chat_id для пользователя $username: $chatId")
                    onChatIdRetrieved(chatId)
                } catch (e: JSONException) {
                    Log.e("TelegramBot", "Ошибка парсинга JSON: ${e.message}")
                }
            }
        }
    })
}
*/

// Функция для отправки сообщения через Telegram
fun sendTelegramMessage(chatId: String, message: String) {
    val url = "https://api.telegram.org/bot7349774648:AAEQBtzD1vidVPRjAbc3eKnsDrc8fofbBRo/sendMessage"
    val client = OkHttpClient()

    // Параметры запроса
    val jsonBody = JSONObject()
    jsonBody.put("chat_id", chatId)
    jsonBody.put("text", message)

    val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

    // Создаем запрос
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    // Выполняем запрос
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("TelegramBot", "Ошибка отправки сообщения: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val responseData = response.body?.string()
            if (!response.isSuccessful) {
                Log.e("TelegramBot", "Ошибка отправки сообщения: $responseData")
            } else {
                Log.i("TelegramBot", "Сообщение успешно отправлено: $responseData")
            }
        }
    })
}

fun getChatIdByTg(tgUsername: String, context: Context): String? {
    val users = readUsersFromFile(context)

    for (user in users) {
        val (_, tg, chatId) = user.split(":")
        if (tg == tgUsername) {
            return chatId
        }
    }
    return null // Если не найдено соответствие
}

fun sendMessageToUser(context: Context, tgUsername: String, message: String) {
    // Получаем chatId по Telegram-логину
    val chatId = getChatIdByTg(tgUsername, context)

    if (chatId != null) {
        sendTelegramMessage(chatId, message)
    } else {
        Log.e("TelegramBot", "Не удалось найти chatId для пользователя с ТГ: $tgUsername")
    }
}
