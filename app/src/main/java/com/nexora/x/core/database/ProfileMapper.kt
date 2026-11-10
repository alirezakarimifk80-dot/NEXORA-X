package com.nexora.x.core.database

import com.nexora.x.core.model.Profile
import com.nexora.x.core.model.TlsConfig
import com.nexora.x.core.model.TransportConfig

/**
 * Bidirectional mapper between [ProfileEntity] (Room persistence) and [Profile] (Domain model).
 */
object ProfileMapper {

    private const val WARNING_DELIMITER = "\u001F"

    fun toDomain(
        entity: ProfileEntity,
        selectedProfileId: String? = null
    ): Profile {
        val warnings = if (entity.unsupportedWarningsSerialized.isBlank()) {
            emptyList()
        } else {
            entity.unsupportedWarningsSerialized
                .split(WARNING_DELIMITER)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        return Profile(
            id = entity.id,
            displayName = entity.displayName,
            protocol = entity.protocol,
            serverAddress = entity.serverAddress,
            port = entity.port,
            credential = entity.credential,
            securityMethod = entity.securityMethod,
            alterId = entity.alterId,
            tlsConfig = TlsConfig(
                enabled = entity.tlsEnabled,
                securityMode = entity.tlsSecurityMode,
                sni = entity.sni,
                alpn = entity.alpn,
                fingerprint = entity.fingerprint,
                allowInsecure = entity.allowInsecure,
                realityPublicKey = entity.realityPublicKey,
                realityShortId = entity.realityShortId
            ),
            transportConfig = TransportConfig(
                networkType = entity.networkType,
                host = entity.transportHost,
                path = entity.transportPath,
                serviceName = entity.grpcServiceName,
                headerType = entity.headerType,
                flow = entity.flow
            ),
            unsupportedWarnings = warnings,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            isSelected = entity.id == selectedProfileId
        )
    }

    fun toEntity(domain: Profile): ProfileEntity {
        val serializedWarnings = domain.unsupportedWarnings
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(WARNING_DELIMITER)

        return ProfileEntity(
            id = domain.id,
            displayName = domain.displayName,
            protocol = domain.protocol,
            serverAddress = domain.serverAddress,
            port = domain.port,
            credential = domain.credential,
            securityMethod = domain.securityMethod,
            alterId = domain.alterId,
            tlsEnabled = domain.tlsConfig.enabled,
            tlsSecurityMode = domain.tlsConfig.securityMode,
            sni = domain.tlsConfig.sni,
            alpn = domain.tlsConfig.alpn,
            fingerprint = domain.tlsConfig.fingerprint,
            allowInsecure = domain.tlsConfig.allowInsecure,
            realityPublicKey = domain.tlsConfig.realityPublicKey,
            realityShortId = domain.tlsConfig.realityShortId,
            networkType = domain.transportConfig.networkType,
            transportHost = domain.transportConfig.host,
            transportPath = domain.transportConfig.path,
            grpcServiceName = domain.transportConfig.serviceName,
            headerType = domain.transportConfig.headerType,
            flow = domain.transportConfig.flow,
            unsupportedWarningsSerialized = serializedWarnings,
            createdAtEpochMillis = domain.createdAtEpochMillis,
            updatedAtEpochMillis = domain.updatedAtEpochMillis
        )
    }
}
