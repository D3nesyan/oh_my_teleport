package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NameRulesTest {

	@Test
	void trimsSurroundingWhitespace() {
		assertEquals("base", NameRules.normalize("  base  "));
	}

	@Test
	void lowercasesSoLookupIsCaseInsensitive() {
		assertEquals("base", NameRules.normalize("BaSe"));
	}

	@Test
	void acceptsDigitsAndUnderscores() {
		assertEquals("base_2", NameRules.normalize("base_2"));
	}

	@Test
	void rejectsNull() {
		assertNull(NameRules.normalize(null));
	}

	@Test
	void rejectsEmptyAndBlank() {
		assertNull(NameRules.normalize(""));
		assertNull(NameRules.normalize("   "));
	}

	@Test
	void rejectsInternalWhitespace() {
		assertNull(NameRules.normalize("my base"));
		assertNull(NameRules.normalize("my\tbase"));
	}

	@Test
	void rejectsControlCharacters() {
		assertNull(NameRules.normalize("ba\u0000se"));
		assertNull(NameRules.normalize("ba\nse"));
	}

	@Test
	void rejectsNamesLongerThanTheLimit() {
		String tooLong = "a".repeat(NameRules.MAX_LENGTH + 1);
		assertNull(NameRules.normalize(tooLong));
		assertEquals(NameRules.MAX_LENGTH, NameRules.normalize("a".repeat(NameRules.MAX_LENGTH)).length());
	}

	@Test
	void isValidMatchesNormalize() {
		assertTrue(NameRules.isValid("home"));
		assertFalse(NameRules.isValid(""));
		assertFalse(NameRules.isValid(null));
	}
}
