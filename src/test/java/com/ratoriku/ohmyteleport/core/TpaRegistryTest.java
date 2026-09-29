package com.ratoriku.ohmyteleport.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TpaRegistryTest {

	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-000000000003");

	private final MutableClock clock = new MutableClock();
	private final TpaRegistry registry = new TpaRegistry(clock, TpaRegistry.DEFAULT_TIMEOUT_MILLIS);

	@Test
	void startsEmpty() {
		assertTrue(registry.isEmpty());
		assertEquals(List.of(), registry.pendingFor(BOB));
	}

	@Test
	void requestIsVisibleToTheTarget() {
		registry.request(ALICE, BOB);

		List<TpaRegistry.Request> pending = registry.pendingFor(BOB);
		assertEquals(1, pending.size());
		assertEquals(ALICE, pending.get(0).requester());
		assertEquals(BOB, pending.get(0).target());
	}

	@Test
	void aSecondRequestFromTheSameRequesterReplacesTheFirst() {
		registry.request(ALICE, BOB);
		Optional<TpaRegistry.Request> replaced = registry.request(ALICE, CAROL);

		assertEquals(BOB, replaced.orElseThrow().target(), "the caller needs this to tell the old target");
		assertEquals(List.of(), registry.pendingFor(BOB));
		assertEquals(1, registry.pendingFor(CAROL).size());
		assertEquals(1, registry.size());
	}

	@Test
	void requestsFromDifferentRequestersCoexistForOneTarget() {
		registry.request(ALICE, BOB);
		registry.request(CAROL, BOB);

		assertEquals(2, registry.pendingFor(BOB).size());
	}

	@Test
	void requestFromSelfIsRejected() {
		registry.request(ALICE, ALICE);
		assertTrue(registry.isEmpty());
	}

	@Test
	void expiredRequestsArePrunedOnRead() {
		registry.request(ALICE, BOB);
		clock.advance(TpaRegistry.DEFAULT_TIMEOUT_MILLIS + 1);

		assertEquals(List.of(), registry.pendingFor(BOB));
		assertTrue(registry.isEmpty(), "pruning must remove the entry, not just hide it");
	}

	@Test
	void aRequestIsStillValidJustBeforeTheTimeout() {
		registry.request(ALICE, BOB);
		clock.advance(TpaRegistry.DEFAULT_TIMEOUT_MILLIS);

		assertEquals(1, registry.pendingFor(BOB).size());
	}

	@Test
	void acceptTargetedMovesOnlyThatRequest() {
		registry.request(ALICE, BOB);
		registry.request(CAROL, BOB);

		TpaRegistry.Request accepted = registry.accept(BOB, ALICE).orElseThrow();

		assertEquals(ALICE, accepted.requester());
		assertTrue(registry.pendingFor(BOB).stream().noneMatch(r -> r.requester().equals(ALICE)));
		assertEquals(1, registry.pendingFor(BOB).size());
	}

	@Test
	void acceptTargetedIgnoresRequestsAimedAtSomebodyElse() {
		registry.request(ALICE, CAROL);

		assertEquals(Optional.empty(), registry.accept(BOB, ALICE));
		assertEquals(1, registry.size());
	}

	@Test
	void acceptAnyIsEmptyWhenThereIsNothingPending() {
		assertEquals(Optional.empty(), registry.acceptAny(BOB));
	}

	@Test
	void denyTargetedRemovesTheRequest() {
		registry.request(ALICE, BOB);

		assertEquals(ALICE, registry.deny(BOB, ALICE).orElseThrow().requester());
		assertTrue(registry.isEmpty());
	}

	@Test
	void denyAnyRemovesTheOldestPendingRequest() {
		clock.set(1_000L);
		registry.request(ALICE, BOB);
		clock.set(2_000L);
		registry.request(CAROL, BOB);

		assertEquals(ALICE, registry.denyAny(BOB).orElseThrow().requester());
		assertEquals(1, registry.size());
	}

	@Test
	void cancelRemovesTheRequestersOwnRequest() {
		registry.request(ALICE, BOB);

		assertEquals(Optional.of(BOB), registry.cancel(ALICE).map(TpaRegistry.Request::target));
		assertTrue(registry.isEmpty());
		assertEquals(Optional.empty(), registry.cancel(ALICE));
	}

	@Test
	void removeForgetsEverythingARequesterHadPending() {
		registry.request(ALICE, BOB);
		registry.remove(ALICE);

		assertTrue(registry.isEmpty());
	}

	@Test
	void hasPendingIsFalseForSelfRequestsAndUnknownTargets() {
		assertFalse(registry.hasPendingFor(BOB));
		registry.request(ALICE, CAROL);
		assertFalse(registry.hasPendingFor(BOB));
	}

	private static final class MutableClock implements java.util.function.LongSupplier {
		private long now;

		void set(long value) {
			this.now = value;
		}

		void advance(long millis) {
			this.now += millis;
		}

		@Override
		public long getAsLong() {
			return now;
		}
	}
}
