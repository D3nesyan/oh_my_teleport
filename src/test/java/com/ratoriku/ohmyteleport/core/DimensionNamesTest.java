package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DimensionNamesTest {

	@Test
	void titleCasesTheBuiltInDimensions() {
		assertEquals("Overworld", DimensionNames.fallback("minecraft:overworld"));
		assertEquals("The Nether", DimensionNames.fallback("minecraft:the_nether"));
		assertEquals("The End", DimensionNames.fallback("minecraft:the_end"));
	}

	@Test
	void keepsTheNamespaceOfOtherModsDimensions() {
		assertEquals("twilightforest:twilight_forest", DimensionNames.fallback("twilightforest:twilight_forest"));
	}

	@Test
	void handlesAnIdWithoutANamespaceAsVanilla() {
		assertEquals("Overworld", DimensionNames.fallback("overworld"));
	}

	@Test
	void handlesMissingOrEmptyInput() {
		assertEquals("unknown", DimensionNames.fallback(null));
		assertEquals("unknown", DimensionNames.fallback(""));
	}

	@Test
	void doesNotChokeOnAwkwardPaths() {
		assertEquals("A B", DimensionNames.fallback("minecraft:a__b"));
		assertEquals("minecraft:", DimensionNames.fallback("minecraft:"));
	}
}
