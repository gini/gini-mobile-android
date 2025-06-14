@file:OptIn(ExperimentalMaterial3Api::class)

package net.gini.android.capture.ui.components.picker.date

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.gini.android.capture.R
import net.gini.android.capture.ui.theme.GiniTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun GiniDatePickerDialog(
    onDismissRequest: () -> Unit,
    onSaved: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate.now(),
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
    colors: GiniDatePickerDialogColors = GiniDatePickerDialogColors.colors(),
    isLandScape : Boolean = false
) {

    val dateState = rememberDatePickerState(
        selectableDates = selectableDates,
        initialSelectedDateMillis =
        date
            .atTime(12, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli(),
    )

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismissRequest
    ) {
        Card(
            modifier = if (isLandScape) modifier
                .fillMaxWidth(0.65f)
                .fillMaxHeight(0.90f)
                .verticalScroll(rememberScrollState())
            else modifier.fillMaxWidth(0.95f),
        ) {
            DatePicker(
                state = dateState,
                showModeToggle = false,
                colors = with(colors) {
                    DatePickerDefaults.colors(
                        titleContentColor = textSupporting,
                        headlineContentColor = textHeadline,
                        weekdayContentColor = textDateEnabled,
                        navigationContentColor = textDateEnabled,
                        dayContentColor = textDateEnabled,
                        selectedDayContentColor = textDateSelected,
                        selectedDayContainerColor = dateSelected,
                        todayContentColor = textDateToday,
                        todayDateBorderColor = dateSelected,
                        dividerColor = divider,
                    )
                }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = { onDismissRequest() }) {
                    Text(
                        text = stringResource(id = R.string.gc_date_picker_cancel),
                        style = GiniTheme.typography.body1
                    )
                }
                TextButton(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = {
                        dateState.selectedDateMillis?.let {
                            onSaved(
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                    }) {
                    Text(
                        text = stringResource(id = R.string.gc_date_picker_select),
                        style = GiniTheme.typography.body1
                    )
                }
            }
        }

    }
}

@Preview
@Composable
private fun DatePickerDialogPreviewLight() {
    DatePickerDialogPreview()
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun DatePickerDialogPreviewDark() {
    DatePickerDialogPreview()
}

@Composable
private fun DatePickerDialogPreview() {
    GiniTheme {
        GiniDatePickerDialog(onDismissRequest = {}, onSaved = {})
    }
}