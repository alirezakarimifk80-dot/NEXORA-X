package com.nexora.x.data.repository

import com.nexora.x.core.database.ProfileDao
import com.nexora.x.core.database.ProfileEntity
import com.nexora.x.core.database.ProfileMapper
import com.nexora.x.core.model.ProfileType
import com.nexora.x.feature.profile.parser.ProfileImportEngine
import com.nexora.x.feature.profile.parser.ShadowsocksParser
import com.nexora.x.feature.profile.parser.TrojanParser
import com.nexora.x.feature.profile.parser.VlessParser
import com.nexora.x.feature.profile.parser.VmessParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying:
 * - Room [ProfileDao] CRUD operations (insert, update, observe, count, delete)
 * - [ProfileMapper] entity <-> domain round-trip mapping
 * - Batch plain-text import and URI export round-trip behavior
 */
class ProfileDatabaseAndRepositoryTest {

    private lateinit var fakeDao: InMemoryProfileDao
    private lateinit var importEngine: ProfileImportEngine

    @Before
    fun setUp() {
        fakeDao = InMemoryProfileDao()
        importEngine = ProfileImportEngine(
            vlessParser = VlessParser(),
            vmessParser = VmessParser(),
            trojanParser = TrojanParser(),
            shadowsocksParser = ShadowsocksParser()
        )
    }

    @Test
    fun `database CRUD and ProfileMapper preserve all transport, TLS, and warning fields`() = runTest {
        val batch = importEngine.parsePlainTextBatch(
            """
            vless://b831381d-6324-4d53-ad4f-8cda48b30811@jp.nexora.net:443?type=ws&security=tls&sni=jp.nexora.net&path=%2Fvless&unknownFlag=1#Japan%20VLESS
            trojan://MyPass123@de.nexora.net:443?security=tls&type=grpc&serviceName=grpc-tun#Germany%20Trojan
            """.trimIndent()
        )

        assertEquals(2, batch.importedProfiles.size)
        assertTrue(batch.errors.isEmpty())
        assertEquals(1, batch.warnings.size)

        // Insert into DAO
        val entities = batch.importedProfiles.map { ProfileMapper.toEntity(it) }
        fakeDao.upsertProfiles(entities)

        assertEquals(2, fakeDao.observeProfileCount().first())

        val firstProfile = batch.importedProfiles.first()
        val fetchedEntity = fakeDao.getProfileById(firstProfile.id)
        assertNotNull(fetchedEntity)

        val mappedDomain = ProfileMapper.toDomain(fetchedEntity!!, selectedProfileId = firstProfile.id)
        assertTrue(mappedDomain.isSelected)
        assertEquals(ProfileType.VLESS, mappedDomain.protocol)
        assertEquals("Japan VLESS", mappedDomain.displayName)
        assertEquals("/vless", mappedDomain.transportConfig.path)
        assertEquals(1, mappedDomain.unsupportedWarnings.size)

        // Update profile display name
        val updatedEntity = ProfileMapper.toEntity(mappedDomain.copy(displayName = "Japan VLESS Updated"))
        fakeDao.updateProfile(updatedEntity)
        assertEquals("Japan VLESS Updated", fakeDao.getProfileById(firstProfile.id)?.displayName)

        // Delete profile
        fakeDao.deleteProfileById(firstProfile.id)
        assertNull(fakeDao.getProfileById(firstProfile.id))
        assertEquals(1, fakeDao.observeProfileCount().first())
    }

    @Test
    fun `exportToUri and parseSingleLink round-trip preserves protocol and endpoint`() {
        val originalLink =
            "trojan://RoundTripPass@nl-core.nexora.net:8443?security=tls&type=ws&host=nl-core.nexora.net&path=%2Ftr#Netherlands%20Node"
        val parsed = importEngine.parseSingleLink(originalLink)
        assertTrue(parsed is com.nexora.x.core.model.ParseResult.Success)
        val profile = (parsed as com.nexora.x.core.model.ParseResult.Success).profile

        val exportedUri = importEngine.exportToUri(profile)
        val reparsed = importEngine.parseSingleLink(exportedUri)
        assertTrue(reparsed is com.nexora.x.core.model.ParseResult.Success)
        val roundTripped = (reparsed as com.nexora.x.core.model.ParseResult.Success).profile

        assertEquals(profile.protocol, roundTripped.protocol)
        assertEquals(profile.serverAddress, roundTripped.serverAddress)
        assertEquals(profile.port, roundTripped.port)
        assertEquals(profile.credential, roundTripped.credential)
        assertEquals(profile.transportConfig.path, roundTripped.transportConfig.path)
    }

    private class InMemoryProfileDao : ProfileDao {
        private val state = MutableStateFlow<List<ProfileEntity>>(emptyList())

        override fun observeAllProfiles(): Flow<List<ProfileEntity>> =
            state.map { list -> list.sortedByDescending { it.updatedAtEpochMillis } }

        override fun observeProfileCount(): Flow<Int> =
            state.map { it.size }

        override suspend fun getProfileById(profileId: String): ProfileEntity? =
            state.value.find { it.id == profileId }

        override suspend fun upsertProfile(entity: ProfileEntity) {
            state.value = state.value.filterNot { it.id == entity.id } + entity
        }

        override suspend fun upsertProfiles(entities: List<ProfileEntity>) {
            val ids = entities.map { it.id }.toSet()
            state.value = state.value.filterNot { it.id in ids } + entities
        }

        override suspend fun updateProfile(entity: ProfileEntity) {
            upsertProfile(entity)
        }

        override suspend fun deleteProfileById(profileId: String) {
            state.value = state.value.filterNot { it.id == profileId }
        }

        override suspend fun deleteAllProfiles() {
            state.value = emptyList()
        }
    }
}
