package com.nexora.x.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nexora.x.core.model.ProfileType

/**
 * Room Database Entity representing a persisted proxy profile in NEXORA X.
 *
 * Security Hardening (`MASVS-STORAGE-1` & `MASVS-STORAGE-2`):
 * - [credential], [realityPublicKey], and [realityShortId] are encrypted at rest using hardware-backed
 *   `AndroidKeyStore` AES-256-GCM (`ENCv1:...`) via [com.nexora.x.core.security.CredentialVaultCipher]
 *   inside [ProfileMapper].
 * - [toString] redacts [credential], [realityPublicKey], and [realityShortId] so secrets are never
 *   exposed in diagnostic logs or stack traces.
 * - Full raw import links are never stored to avoid duplicating sensitive tokens.
 * - Cloud backup and device transfer are explicitly disabled (`android:allowBackup="false"`,
 *   `backup_rules.xml`, and `data_extraction_rules.xml`).
 */
@Entity(
    tableName = "profiles",
    indices = [
        Index(value = ["protocol"]),
        Index(value = ["updated_at"])
    ]
)
data class ProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "protocol")
    val protocol: ProfileType,

    @ColumnInfo(name = "server_address")
    val serverAddress: String,

    @ColumnInfo(name = "port")
    val port: Int,

    @ColumnInfo(name = "credential")
    val credential: String,

    @ColumnInfo(name = "security_method")
    val securityMethod: String,

    @ColumnInfo(name = "alter_id")
    val alterId: Int = 0,

    // TLS Configuration
    @ColumnInfo(name = "tls_enabled")
    val tlsEnabled: Boolean,

    @ColumnInfo(name = "tls_security_mode")
    val tlsSecurityMode: String,

    @ColumnInfo(name = "sni")
    val sni: String,

    @ColumnInfo(name = "alpn")
    val alpn: String,

    @ColumnInfo(name = "fingerprint")
    val fingerprint: String,

    @ColumnInfo(name = "allow_insecure")
    val allowInsecure: Boolean,

    @ColumnInfo(name = "reality_public_key")
    val realityPublicKey: String,

    @ColumnInfo(name = "reality_short_id")
    val realityShortId: String,

    // Transport Configuration
    @ColumnInfo(name = "network_type")
    val networkType: String,

    @ColumnInfo(name = "transport_host")
    val transportHost: String,

    @ColumnInfo(name = "transport_path")
    val transportPath: String,

    @ColumnInfo(name = "grpc_service_name")
    val grpcServiceName: String,

    @ColumnInfo(name = "header_type")
    val headerType: String,

    @ColumnInfo(name = "flow")
    val flow: String,

    // Warnings about unsupported parameters preserved during import
    @ColumnInfo(name = "unsupported_warnings")
    val unsupportedWarningsSerialized: String,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long
) {
    override fun toString(): String {
        return "ProfileEntity(id=$id, displayName=$displayName, protocol=${protocol.name}, " +
            "server=$serverAddress:$port, credential=[REDACTED], realityPublicKey=[REDACTED], realityShortId=[REDACTED])"
    }
}
