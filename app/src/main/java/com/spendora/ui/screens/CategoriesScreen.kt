package com.spendora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.spendora.data.dao.CategoryDao
import com.spendora.data.entity.CategoryEntity
import com.spendora.data.model.CategoryType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    categoryDao: CategoryDao,
    onNavigateBack: () -> Unit = {}
) {
    var showArchived by remember { mutableStateOf(false) }
    val activeCategories by categoryDao.getAllActive().collectAsState(initial = emptyList())
    val archivedCategories by categoryDao.getAllArchived().collectAsState(initial = emptyList())
    val categoriesToShow = if (showArchived) archivedCategories else activeCategories

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToArchive by remember { mutableStateOf<CategoryEntity?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showArchived) "Archived Categories" else "Categories Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showArchived = !showArchived }) {
                        Text(if (showArchived) "View Active" else "View Archived")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!showArchived) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(categoriesToShow, key = { it.id }) { cat ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                if (cat.isSystem) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer) {
                                        Text("System", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text(cat.type.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        if (!showArchived && !cat.isSystem) {
                            IconButton(onClick = { categoryToArchive = cat }) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Add Custom Category Dialog
        if (showAddDialog) {
            var catName by remember { mutableStateOf("") }
            var catType by remember { mutableStateOf(CategoryType.EXPENSE) }
            var error by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Custom Category") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = catName, onValueChange = { catName = it }, label = { Text("Category Name*") }, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = catType == CategoryType.EXPENSE, onClick = { catType = CategoryType.EXPENSE }, label = { Text("Expense") })
                            FilterChip(selected = catType == CategoryType.INCOME, onClick = { catType = CategoryType.INCOME }, label = { Text("Income") })
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (catName.isBlank()) {
                            error = "Category name is required"
                            return@Button
                        }
                        coroutineScope.launch {
                            categoryDao.insert(
                                CategoryEntity(
                                    name = catName.trim(),
                                    type = catType,
                                    icon = "category",
                                    colorHex = "#9C27B0",
                                    isSystem = false,
                                    isArchived = false,
                                    sortOrder = 99
                                )
                            )
                            showAddDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Archive Dialog
        if (categoryToArchive != null) {
            AlertDialog(
                onDismissRequest = { categoryToArchive = null },
                title = { Text("Archive Category?") },
                text = {
                    Text("Archiving \"${categoryToArchive!!.name}\" hides it from future transaction pickers. All existing transactions referencing this category remain 100% intact.")
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            categoryDao.archiveCategory(categoryToArchive!!.id)
                            categoryToArchive = null
                        }
                    }) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToArchive = null }) { Text("Cancel") }
                }
            )
        }
    }
}
