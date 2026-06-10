package com.ferryapps.vitals.core.domain.usecase

import com.ferryapps.vitals.core.domain.model.VitalsSnapshot
import com.ferryapps.vitals.core.domain.repository.VitalsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetVitalsUseCase @Inject constructor(
    private val repository: VitalsRepository
) {
    operator fun invoke(): Flow<List<VitalsSnapshot>> = repository.observeSnapshots()
}
