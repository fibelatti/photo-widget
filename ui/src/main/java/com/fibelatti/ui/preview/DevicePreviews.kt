package com.fibelatti.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Regular Device",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_7,
)
@Preview(
    name = "Landscape Device",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape"
)
@Preview(
    name = "Small Device",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
)
@Preview(
    name = "Small Device (pt)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "pt",
)
@Preview(
    name = "Small Device (es)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "es",
)
@Preview(
    name = "Tablet",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_TABLET,
)
annotation class DevicePreviews
