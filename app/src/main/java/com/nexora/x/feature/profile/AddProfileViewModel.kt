package com.nexora.x.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.x.core.model.ParseResult
import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.ProfileType
import com.nexora.x.core.model.TlsConfig
import com.nexora.x.core.model.TransportConfig
import com.nexora.x.domain.repository.ProfileRepository
import com.nexora.x.domain.usecase.ImportProfileUseCase
import com.nexora.x.feature.profile.parser.UriParserUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class AddProfileEntryMode(val label: String) {
    IMPORT_URI_OR_TEXT("Paste / Clipboard Link"),
    MANUAL_FORM("Manual Configuration"),
    QR_SCANNER_ENTRY("QR Import")
}

data class AddProfileUiState(
    val editingProfileId: String? = null,
    val entryMode: AddProfileEntryMode = AddProfileEntryMode.IMPORT_URI_OR_TEXT,
    val rawImportText: String = "",
    val displayName: String = "",
    val protocol: ProfileType = ProfileType.VLESS,
    val serverAddress: String = "",
    val portText: String = "443",
    val credential: String = "",
    val securityMethod: String = "none",
    val alterIdText: String = "0",
    val networkType: String = "tcp",
    val transportHost: String = "",
    val transportPath: String = "",
    val grpcServiceName: String = "",
    val flow: String = "",
    val tlsSecurityMode: String = "tls",
    val sni: String = "",
    val alpn: String = "",
    val fingerprint: String = "chrome",
    val allowInsecure: Boolean = false,
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val validationErrors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val statusBanner: String? = null,
    val saveCompleted: Boolean = false
)

@HiltViewModel
class AddProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val importProfileUseCase: ImportProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProfileUiState())
    val uiState: StateFlow<AddProfileUiState> = _uiState.asStateFlow()

    fun loadProfileForEdit(profileId: String?) {
        if (profileId.isNullOrBlank()) {
            _uiState.value = AddProfileUiState()
            return
        }
        viewModelScope.launch {
            val existing = profileRepository.getProfileById(profileId) ?: return@launch
            _uiState.update {
                it.copy(
                    editingProfileId = existing.id,
                    entryMode = AddProfileEntryMode.MANUAL_FORM,
                    displayName = existing.displayName,
                    protocol = existing.protocol,
                    serverAddress = existing.serverAddress,
                    portText = existing.port.toString(),
                    credential = existing.credential,
                    securityMethod = existing.securityMethod,
                    alterIdText = existing.alterId.toString(),
                    networkType = existing.transportConfig.networkType,
                    transportHost = existing.transportConfig.host,
                    transportPath = existing.transportConfig.path,
                    grpcServiceName = existing.transportConfig.serviceName,
                    flow = existing.transportConfig.flow,
                    tlsSecurityMode = existing.tlsConfig.securityMode,
                    sni = existing.tlsConfig.sni,
                    alpn = existing.tlsConfig.alpn,
                    fingerprint = existing.tlsConfig.fingerprint,
                    allowInsecure = existing.tlsConfig.allowInsecure,
                    realityPublicKey = existing.tlsConfig.realityPublicKey,
                    realityShortId = existing.tlsConfig.realityShortId,
                    warnings = existing.unsupportedWarnings,
                    validationErrors = emptyList()
                )
            }
        }
    }

    fun onEntryModeChanged(mode: AddProfileEntryMode) {
        _uiState.update {
            it.copy(
                entryMode = mode,
                validationErrors = emptyList(),
                statusBanner = if (mode == AddProfileEntryMode.QR_SCANNER_ENTRY) {
                    "Camera QR scanner hardware pipeline is not yet integrated in Phase 2. Paste the decoded QR URI string below or use Clipboard import."
                } else {
                    null
                }
            )
        }
    }

    fun onRawImportTextChanged(text: String) {
        _uiState.update { it.copy(rawImportText = text, validationErrors = emptyList()) }
    }

    fun onPasteFromClipboard(clipboardText: String?) {
        if (clipboardText.isNullOrBlank()) {
            _uiState.update {
                it.copy(validationErrors = listOf("Clipboard is empty. Copy a vless://, vmess://, trojan://, or ss:// link first."))
            }
            return
        }
        _uiState.update { it.copy(rawImportText = clipboardText.trim(), validationErrors = emptyList()) }
        parseAndPopulateManualForm(clipboardText.trim())
    }

    /**
     * Parses a single link into the editable form fields so the user can inspect or customize before saving.
     */
    fun parseAndPopulateManualForm(rawInput: String = _uiState.value.rawImportText) {
        when (val result = importProfileUseCase.previewSingleLink(rawInput)) {
            is ParseResult.Success -> {
                val p = result.profile
                _uiState.update {
                    it.copy(
                        displayName = p.displayName,
                        protocol = p.protocol,
                        serverAddress = p.serverAddress,
                        portText = p.port.toString(),
                        credential = p.credential,
                        securityMethod = p.securityMethod,
                        alterIdText = p.alterId.toString(),
                        networkType = p.transportConfig.networkType,
                        transportHost = p.transportConfig.host,
                        transportPath = p.transportConfig.path,
                        grpcServiceName = p.transportConfig.serviceName,
                        flow = p.transportConfig.flow,
                        tlsSecurityMode = p.tlsConfig.securityMode,
                        sni = p.tlsConfig.sni,
                        alpn = p.tlsConfig.alpn,
                        fingerprint = p.tlsConfig.fingerprint,
                        allowInsecure = p.tlsConfig.allowInsecure,
                        realityPublicKey = p.tlsConfig.realityPublicKey,
                        realityShortId = p.tlsConfig.realityShortId,
                        warnings = result.warnings,
                        validationErrors = emptyList(),
                        statusBanner = "Parsed ${p.protocol.displayName} configuration (${p.serverAddress}:${p.port}). Review fields or tap Save."
                    )
                }
            }
            is ParseResult.Failure -> {
                _uiState.update {
                    it.copy(
                        validationErrors = listOf(result.errorMessage),
                        statusBanner = null
                    )
                }
            }
        }
    }

    fun onDisplayNameChanged(v: String) = _uiState.update { it.copy(displayName = v) }
    fun onProtocolChanged(protocol: ProfileType) {
        val defaultSecurity = when (protocol) {
            ProfileType.VLESS -> "none"
            ProfileType.VMESS -> "auto"
            ProfileType.TROJAN -> "tls"
            ProfileType.SHADOWSOCKS -> "aes-256-gcm"
        }
        val defaultTlsMode = when (protocol) {
            ProfileType.SHADOWSOCKS -> "none"
            else -> "tls"
        }
        _uiState.update {
            it.copy(
                protocol = protocol,
                securityMethod = defaultSecurity,
                tlsSecurityMode = defaultTlsMode,
                validationErrors = emptyList()
            )
        }
    }

    fun onServerAddressChanged(v: String) = _uiState.update { it.copy(serverAddress = v) }
    fun onPortChanged(v: String) = _uiState.update { it.copy(portText = v) }
    fun onCredentialChanged(v: String) = _uiState.update { it.copy(credential = v) }
    fun onSecurityMethodChanged(v: String) = _uiState.update { it.copy(securityMethod = v) }
    fun onNetworkTypeChanged(v: String) = _uiState.update { it.copy(networkType = v) }
    fun onTransportHostChanged(v: String) = _uiState.update { it.copy(transportHost = v) }
    fun onTransportPathChanged(v: String) = _uiState.update { it.copy(transportPath = v) }
    fun onGrpcServiceNameChanged(v: String) = _uiState.update { it.copy(grpcServiceName = v) }
    fun onFlowChanged(v: String) = _uiState.update { it.copy(flow = v) }
    fun onTlsSecurityModeChanged(v: String) = _uiState.update { it.copy(tlsSecurityMode = v) }
    fun onSniChanged(v: String) = _uiState.update { it.copy(sni = v) }
    fun onAlpnChanged(v: String) = _uiState.update { it.copy(alpn = v) }
    fun onFingerprintChanged(v: String) = _uiState.update { it.copy(fingerprint = v) }
    fun onAllowInsecureChanged(v: Boolean) = _uiState.update { it.copy(allowInsecure = v) }
    fun onRealityPublicKeyChanged(v: String) = _uiState.update { it.copy(realityPublicKey = v) }
    fun onRealityShortIdChanged(v: String) = _uiState.update { it.copy(realityShortId = v) }

    fun onSaveClicked(onSuccess: () -> Unit) {
        val state = _uiState.value

        // If user is in URI/Text mode and hasn't populated manual fields yet, support batch/single import directly
        if (state.entryMode == AddProfileEntryMode.IMPORT_URI_OR_TEXT &&
            state.rawImportText.isNotBlank() &&
            state.serverAddress.isBlank()
        ) {
            viewModelScope.launch {
                val batch = importProfileUseCase.importBatch(state.rawImportText)
                if (batch.importedProfiles.isEmpty()) {
                    _uiState.update {
                        it.copy(validationErrors = batch.errors.ifEmpty { listOf("Could not parse configuration input.") })
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            warnings = batch.warnings,
                            validationErrors = emptyList(),
                            saveCompleted = true
                        )
                    }
                    onSuccess()
                }
            }
            return
        }

        // Validate Manual / Populated Form Fields
        val errors = mutableListOf<String>()
        val host = state.serverAddress.trim()
        if (!UriParserUtils.isValidHost(host)) {
            errors.add("Enter a valid server hostname or IP address.")
        }

        val port = state.portText.trim().toIntOrNull()
        if (!UriParserUtils.isValidPort(port)) {
            errors.add("Port must be a valid integer between 1 and 65535.")
        }

        val cred = state.credential.trim()
        if (cred.isEmpty()) {
            val credLabel = when (state.protocol) {
                ProfileType.VLESS, ProfileType.VMESS -> "UUID"
                ProfileType.TROJAN, ProfileType.SHADOWSOCKS -> "Password"
            }
            errors.add("$credLabel cannot be empty.")
        }

        if (state.tlsSecurityMode == "reality" && state.realityPublicKey.isBlank()) {
            errors.add("Reality TLS requires a non-empty Reality Public Key (pbk).")
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }

        val now = System.currentTimeMillis()
        val resolvedName = state.displayName.trim().ifBlank {
            "${state.protocol.displayName} $host:$port"
        }

        val profile = Profile(
            id = state.editingProfileId ?: UUID.randomUUID().toString(),
            displayName = resolvedName,
            protocol = state.protocol,
            serverAddress = host,
            port = port!!,
            credential = cred,
            securityMethod = state.securityMethod.trim().ifBlank { "auto" },
            alterId = state.alterIdText.trim().toIntOrNull() ?: 0,
            tlsConfig = TlsConfig(
                enabled = state.tlsSecurityMode != "none",
                securityMode = state.tlsSecurityMode,
                sni = state.sni.trim(),
                alpn = state.alpn.trim(),
                fingerprint = state.fingerprint.trim(),
                allowInsecure = state.allowInsecure,
                realityPublicKey = state.realityPublicKey.trim(),
                realityShortId = state.realityShortId.trim()
            ),
            transportConfig = TransportConfig(
                networkType = state.networkType.trim().lowercase().ifBlank { "tcp" },
                host = state.transportHost.trim(),
                path = state.transportPath.trim(),
                serviceName = state.grpcServiceName.trim(),
                flow = state.flow.trim()
            ),
            unsupportedWarnings = state.warnings,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now
        )

        viewModelScope.launch {
            importProfileUseCase.saveValidatedProfile(profile)
            _uiState.update { it.copy(validationErrors = emptyList(), saveCompleted = true) }
            onSuccess()
        }
    }
}
