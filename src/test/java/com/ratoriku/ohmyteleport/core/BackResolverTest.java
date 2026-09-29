package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class BackResolverTest {

	private static TeleportPoint point(String dimension, double x) {
		return new TeleportPoint(dimension, x, 64.0, 0.0, 0.0f, 0.0f);
	}

	@Test
	void deathLocationWinsOverLastTeleportOrigin() {
		TeleportPoint death = point("minecraft:overworld", 1.0);
		TeleportPoint origin = point("minecraft:the_nether", 2.0);

		BackResolver.BackTarget target = BackResolver.resolve(Optional.of(death), Optional.of(origin))
				.orElseThrow();

		assertEquals(death, target.point());
		assertTrue(target.fromDeath(), "the caller must clear the death location after using it");
	}

	@Test
	void fallsBackToLastTeleportOriginWhenThereIsNoDeathLocation() {
		TeleportPoint origin = point("minecraft:the_nether", 2.0);

		BackResolver.BackTarget target = BackResolver.resolve(Optional.empty(), Optional.of(origin))
				.orElseThrow();

		assertEquals(origin, target.point());
		assertTrue(!target.fromDeath(), "the death location must not be cleared when it was not used");
	}

	@Test
	void emptyWhenNeitherIsKnown() {
		assertEquals(Optional.empty(), BackResolver.resolve(Optional.empty(), Optional.empty()));
	}
}
