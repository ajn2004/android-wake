package com.example.androidwake.widget

import com.example.androidwake.domain.ApprovedNetwork
import com.example.androidwake.domain.Machine

sealed interface QuickWakeWidgetState {
    data class NonApproved(val message: String) : QuickWakeWidgetState
    data class Approved(
        val network: ApprovedNetwork,
        val machines: List<Machine>,
    ) : QuickWakeWidgetState
}
