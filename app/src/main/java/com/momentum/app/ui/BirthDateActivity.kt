package com.momentummm.app.ui

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import com.momentummm.app.data.UserPreferencesRepository
import com.momentummm.app.widget.LifeWeeksWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class BirthDateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BirthDateScreen(
                        onPick = { openDatePicker() },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(this, { _, y, m, d ->
            val localDate = LocalDate.of(y, m + 1, d)
            val iso = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            CoroutineScope(Dispatchers.IO).launch {
                UserPreferencesRepository.setDobIso(this@BirthDateActivity, iso)
                LifeWeeksWidgetProvider().updateAll(this@BirthDateActivity)
            }
            finish()
        }, year, month, day)

        dialog.show()
    }
}

@Composable
private fun BirthDateScreen(
    onPick: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Selecciona tu fecha de nacimiento")
        Button(onClick = onPick) { Text("Elegir fecha") }
        Button(onClick = onCancel) { Text("Cancelar") }
    }
}
