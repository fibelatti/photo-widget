package com.fibelatti.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Phone",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_7,
)
@Preview(
    name = "Phone — Landscape",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape",
)
@Preview(
    name = "Phone — Small",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
)
@Preview(
    name = "Phone — Small (pt)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "pt",
)
@Preview(
    name = "Phone — Small (es)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "es",
)
@Preview(
    name = "Phone — Small (fr)",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.NEXUS_5,
    locale = "fr",
)
@Preview(
    name = "Tablet",
    group = "Device",
    showSystemUi = true,
    showBackground = true,
    device = Devices.PIXEL_TABLET,
)
annotation class DevicePreviews
