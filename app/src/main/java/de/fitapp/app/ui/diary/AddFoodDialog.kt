package de.fitapp.app.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.fitapp.app.data.entity.FoodItemEntity
import de.fitapp.app.data.entity.MealType
import de.fitapp.app.data.food.BarcodeLookupResult
import de.fitapp.app.ui.AppViewModel
import kotlinx.coroutines.launch

private enum class Tab { SUCHE, FRISCH, SCAN, EIGEN }

private val freshSuggestions = listOf(
    "Apfel", "Banane", "Tomate", "Gurke", "Kartoffel, gekocht", "Reis, gekocht",
    "Haferflocken", "Karotte", "Brokkoli, gedämpft", "Orange", "Paprika", "Zwiebel"
)

@Composable
fun AddFoodDialog(viewModel: AppViewModel, mealType: MealType, onDismiss: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.SUCHE) }
    var selectedFood by remember { mutableStateOf<FoodItemEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Lebensmittel hinzufügen", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                if (selectedFood == null) {
                    TabRow(selectedTabIndex = tab.ordinal) {
                        Tab.values().forEach { t ->
                            androidx.compose.material3.Tab(
                                selected = tab == t,
                                onClick = { tab = t },
                                text = { Text(t.tabLabel()) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    when (tab) {
                        Tab.SUCHE -> SearchTab(viewModel) { selectedFood = it }
                        Tab.FRISCH -> FreshTab(viewModel) { selectedFood = it }
                        Tab.SCAN -> BarcodeTab(viewModel) { selectedFood = it }
                        Tab.EIGEN -> CustomFoodTab(viewModel) { selectedFood = it }
                    }

                    TextButton(onClick = onDismiss, modifier = Modifier.align(androidx.compose.ui.Alignment.End)) {
                        Text("Abbrechen")
                    }
                } else {
                    AmountEntryStep(
                        food = selectedFood!!,
                        onCancel = { selectedFood = null },
                        onConfirm = { grams ->
                            viewModel.addDiaryEntry(selectedFood!!, grams, mealType)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun Tab.tabLabel() = when (this) {
    Tab.SUCHE -> "Suche"
    Tab.FRISCH -> "Frisch"
    Tab.SCAN -> "Scannen"
    Tab.EIGEN -> "Eigenes"
}

@Composable
private fun SearchTab(viewModel: AppViewModel, onSelected: (FoodItemEntity) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItemEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    OutlinedTextField(
        value = query,
        onValueChange = {
            query = it
            scope.launch { results = viewModel.searchFood(it) }
        },
        label = { Text("Lebensmittel suchen") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    LazyColumn(Modifier.heightIn(max = 260.dp)) {
        items(results) { food ->
            ListItem(
                headlineContent = { Text(food.name) },
                supportingContent = { Text("${food.kcalPer100g.toInt()} kcal / 100g") },
                modifier = Modifier.clickableRow { onSelected(food) }
            )
        }
        if (query.isNotBlank() && results.isEmpty()) {
            item { Text("Kein Treffer. Du kannst das Lebensmittel unter „Eigenes“ manuell anlegen.") }
        }
    }
}

@Composable
private fun FreshTab(viewModel: AppViewModel, onSelected: (FoodItemEntity) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItemEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Text("Frisches Obst, Gemüse & Co. ohne Barcode:", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(
        value = query,
        onValueChange = {
            query = it
            scope.launch { results = viewModel.searchFood(it) }
        },
        label = { Text("z. B. Apfel, Reis, Haferflocken …") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    if (query.isBlank()) {
        Text("Schnellauswahl:", style = MaterialTheme.typography.labelMedium)
        LazyColumn(Modifier.heightIn(max = 220.dp)) {
            items(freshSuggestions) { name ->
                ListItem(
                    headlineContent = { Text(name) },
                    modifier = Modifier.clickableRow {
                        scope.launch {
                            viewModel.searchFood(name).firstOrNull()?.let(onSelected)
                        }
                    }
                )
            }
        }
    } else {
        LazyColumn(Modifier.heightIn(max = 220.dp)) {
            items(results) { food ->
                ListItem(
                    headlineContent = { Text(food.name) },
                    supportingContent = { Text("${food.kcalPer100g.toInt()} kcal / 100g") },
                    modifier = Modifier.clickableRow { onSelected(food) }
                )
            }
        }
    }
}

@Composable
private fun BarcodeTab(viewModel: AppViewModel, onSelected: (FoodItemEntity) -> Unit) {
    // Hinweis: Die eigentliche Kamera-Scan-UI (CameraX + ML Kit) befindet sich in
    // BarcodeScannerView.kt. Hier wird das Ergebnis entgegengenommen und verarbeitet.
    var manualBarcode by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text(
        "Scanne den Barcode eines verpackten Produkts. Falls die Kamera nicht verfügbar " +
            "ist oder kein Treffer gefunden wird, kannst du die Nummer auch eingeben oder " +
            "das Produkt manuell unter „Eigenes“ anlegen."
    )
    Spacer(Modifier.height(8.dp))

    de.fitapp.app.ui.diary.barcode.BarcodeScannerView(
        onBarcodeDetected = { code ->
            scope.launch {
                status = "Suche Produkt …"
                when (val result = viewModel.lookupBarcode(code)) {
                    is BarcodeLookupResult.Found -> { status = null; onSelected(result.item) }
                    BarcodeLookupResult.NotFound -> status = "Kein Produkt zu diesem Barcode gefunden. Bitte manuell anlegen."
                    BarcodeLookupResult.NetworkError -> status = "Keine Verbindung zur Produktdatenbank. Bitte später erneut versuchen oder manuell anlegen."
                }
            }
        }
    )

    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = manualBarcode,
        onValueChange = { manualBarcode = it },
        label = { Text("Barcode manuell eingeben") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Button(
        onClick = {
            scope.launch {
                status = "Suche Produkt …"
                when (val result = viewModel.lookupBarcode(manualBarcode)) {
                    is BarcodeLookupResult.Found -> { status = null; onSelected(result.item) }
                    BarcodeLookupResult.NotFound -> status = "Kein Produkt zu diesem Barcode gefunden. Bitte manuell anlegen."
                    BarcodeLookupResult.NetworkError -> status = "Keine Verbindung zur Produktdatenbank. Bitte später erneut versuchen oder manuell anlegen."
                }
            }
        },
        enabled = manualBarcode.isNotBlank(),
        modifier = Modifier.padding(top = 8.dp)
    ) { Text("Suchen") }

    status?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun CustomFoodTab(viewModel: AppViewModel, onSelected: (FoodItemEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var sugar by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Text("Werte gelten jeweils pro 100 g / 100 ml.", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(kcal, { kcal = it }, label = { Text("kcal") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(protein, { protein = it }, label = { Text("Eiweiß g") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(carbs, { carbs = it }, label = { Text("KH g") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(sugar, { sugar = it }, label = { Text("davon Zucker g") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(fat, { fat = it }, label = { Text("Fett g") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(fiber, { fiber = it }, label = { Text("Ballaststoffe g") }, modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal))
    }

    val valid = name.isNotBlank() && kcal.toFloatOrNull() != null
    Button(
        onClick = {
            scope.launch {
                val id = viewModel.saveCustomFood(
                    name.trim(),
                    kcal.toFloatOrNull() ?: 0f,
                    protein.toFloatOrNull() ?: 0f,
                    carbs.toFloatOrNull() ?: 0f,
                    sugar.toFloatOrNull() ?: 0f,
                    fat.toFloatOrNull() ?: 0f,
                    fiber.toFloatOrNull() ?: 0f
                )
                onSelected(
                    FoodItemEntity(
                        id = id, name = name.trim(), source = de.fitapp.app.data.entity.FoodSource.CUSTOM,
                        kcalPer100g = kcal.toFloatOrNull() ?: 0f,
                        proteinPer100g = protein.toFloatOrNull() ?: 0f,
                        carbsPer100g = carbs.toFloatOrNull() ?: 0f,
                        sugarPer100g = sugar.toFloatOrNull() ?: 0f,
                        fatPer100g = fat.toFloatOrNull() ?: 0f,
                        fiberPer100g = fiber.toFloatOrNull() ?: 0f,
                        isCustom = true
                    )
                )
            }
        },
        enabled = valid,
        modifier = Modifier.padding(top = 8.dp)
    ) { Text("Anlegen & auswählen") }
}

@Composable
private fun AmountEntryStep(food: FoodItemEntity, onCancel: () -> Unit, onConfirm: (Float) -> Unit) {
    var grams by remember { mutableStateOf("100") }
    Text(food.name, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
        value = grams,
        onValueChange = { grams = it },
        label = { Text("Menge in Gramm") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
    val g = grams.toFloatOrNull()
    if (g != null && g > 0) {
        val factor = g / 100f
        Text(
            "≈ ${(food.kcalPer100g * factor).toInt()} kcal · " +
                "${(food.proteinPer100g * factor).toInt()} g Eiweiß · " +
                "${(food.carbsPer100g * factor).toInt()} g KH · " +
                "${(food.fatPer100g * factor).toInt()} g Fett",
            style = MaterialTheme.typography.bodySmall
        )
    }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onCancel) { Text("Zurück") }
        Button(onClick = { g?.let(onConfirm) }, enabled = g != null && g > 0) { Text("Hinzufügen") }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
