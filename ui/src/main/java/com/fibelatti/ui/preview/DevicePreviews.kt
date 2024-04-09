package com.fibelatti.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Nexus 5",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
)
@Preview(
    name = "Nexus 5 (pt)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "pt",
)
@Preview(
    name = "Nexus 5 (es)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "es",
)
@Preview(
    name = "Pixel Tablet",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_TABLET,
)
annotation class DevicePreviews
