package com.fibelatti.photowidget.configure

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fibelatti.photowidget.R
import com.fibelatti.photowidget.model.PhotoWidget
import com.fibelatti.photowidget.preferences.DefaultPicker
import com.fibelatti.photowidget.preferences.DefaultPickerFooterButtons
import com.fibelatti.photowidget.ui.NumberSpinner
import com.fibelatti.photowidget.ui.WidgetPositionViewer

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun PhotoWidgetPaddingPicker(
    currentValue: Int,
    onApplyClick: (newValue: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DefaultPicker(
        title = stringResource(id = R.string.photo_widget_configure_padding),
        modifier = modifier,
    ) {
        var value: Int by remember(currentValue) { mutableIntStateOf(currentValue) }

        WidgetPositionViewer(
            photoWidget = PhotoWidget(
                currentPhoto = LocalSamplePhoto.current,
                padding = value,
            ),
            modifier = Modifier
                .width(200.dp)
                .aspectRatio(.75f),
        )

        NumberSpinner(
            value = value,
            onIncreaseClick = { value++ },
            onDecreaseClick = { value-- },
            modifier = Modifier.padding(horizontal = 24.dp),
            upperBound = 20,
            lowerBound = 0,
        )

        DefaultPickerFooterButtons(
            onApplyClick = { onApplyClick(value) },
            onResetClick = { value = 0 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
    }
}
