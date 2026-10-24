package com.example.rickandmortymvvm.presentation.character.mapper

import com.example.rickandmortymvvm.support.characterFixture
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies display formatting stays outside domain values and does not assume a timestamp length.
 *
 * @author Stefan Bayne
 * @since 2026-10-24
 */
class CharacterUiMapperTest {
    @Test
    fun `display labels and date do not change domain values`() {
        val character = characterFixture()
        val ui = character.toUiModel()
        assertEquals(character.id, ui.id)
        assertEquals(character.name, ui.name)
        assertEquals(character.image, ui.imageUrl)
        assertEquals("Unknown", ui.gender)
        assertEquals("Unknown", ui.status)
        assertEquals("Alien", ui.species)
        assertEquals("2017-11-04", ui.firstSeenDate)
        assertEquals("unknown", character.status)
        assertEquals("2017-11-04T18:48:46.250Z", character.created)
    }

    @Test
    fun `date without a time is displayed without truncation`() {
        assertEquals(
            "2026-10-24",
            characterFixture().copy(created = "2026-10-24").toUiModel().firstSeenDate,
        )
    }
}
