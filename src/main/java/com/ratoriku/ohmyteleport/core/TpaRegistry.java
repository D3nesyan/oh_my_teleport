package com.ratoriku.ohmyteleport.core;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Pending {@code /tpa} requests.
 *
 * <p>Rules, in order of importance:
 * <ul>
 *   <li>a requester has at most one pending request; asking again replaces their own previous one
 *       (the replaced request is returned so the caller can tell its target);
 *   <li>requests from different requesters to the same target coexist;
 *   <li>requests expire after {@link #timeoutMillis()}; expiry is applied lazily on read, so no
 *       ticking task is needed.
 * </ul>
 *
 * <p>Nothing here is persisted - a server restart dropping pending requests is intended.
 */
public final class TpaRegistry {

	public static final long DEFAULT_TIMEOUT_MILLIS = 60_000L;

	public record Request(UUID requester, UUID target, long createdAtMillis) {

		public boolean isExpired(long nowMillis, long timeoutMillis) {
			return nowMillis - createdAtMillis > timeoutMillis;
		}
	}

	private final Map<UUID, Request> byRequester = new LinkedHashMap<>();
	private final LongSupplier clock;
	private final long timeoutMillis;

	public TpaRegistry(LongSupplier clock) {
		this(clock, DEFAULT_TIMEOUT_MILLIS);
	}

	public TpaRegistry(LongSupplier clock, long timeoutMillis) {
		this.clock = clock;
		this.timeoutMillis = timeoutMillis;
	}

	public long timeoutMillis() {
		return timeoutMillis;
	}

	/**
	 * Records a request, replacing the requester's own previous one if any.
	 *
	 * @return the request that was replaced, or empty when there was none
	 */
	public Optional<Request> request(UUID requester, UUID target) {
		if (requester == null || target == null || requester.equals(target)) {
			return Optional.empty();
		}
		prune();
		Request previous = byRequester.put(requester, new Request(requester, target, clock.getAsLong()));
		return Optional.ofNullable(previous);
	}

	/** Requests waiting on this target, oldest first. */
	public List<Request> pendingFor(UUID target) {
		prune();
		return byRequester.values().stream()
				.filter(request -> request.target().equals(target))
				.sorted(Comparator.comparingLong(Request::createdAtMillis))
				.toList();
	}

	public boolean hasPendingFor(UUID target) {
		return !pendingFor(target).isEmpty();
	}

	/** Accepts the request from {@code requester} to {@code target}, removing it. */
	public Optional<Request> accept(UUID target, UUID requester) {
		return take(target, requester);
	}

	/** Accepts the oldest request aimed at {@code target}. */
	public Optional<Request> acceptAny(UUID target) {
		return pendingFor(target).stream().findFirst().flatMap(request -> take(target, request.requester()));
	}

	/** Denies the request from {@code requester} to {@code target}, removing it. */
	public Optional<Request> deny(UUID target, UUID requester) {
		return take(target, requester);
	}

	/** Denies the oldest request aimed at {@code target}. */
	public Optional<Request> denyAny(UUID target) {
		return acceptAny(target);
	}

	/** Withdraws whatever this requester had pending. */
	public Optional<Request> cancel(UUID requester) {
		prune();
		return Optional.ofNullable(byRequester.remove(requester));
	}

	/** Forgets a requester entirely, for example when they disconnect. */
	public void remove(UUID requester) {
		byRequester.remove(requester);
	}

	public boolean isEmpty() {
		prune();
		return byRequester.isEmpty();
	}

	public int size() {
		prune();
		return byRequester.size();
	}

	/** Drops every expired request. */
	public void prune() {
		long now = clock.getAsLong();
		byRequester.values().removeIf(request -> request.isExpired(now, timeoutMillis));
	}

	private Optional<Request> take(UUID target, UUID requester) {
		prune();
		Request request = byRequester.get(requester);
		if (request == null || !request.target().equals(target)) {
			return Optional.empty();
		}
		byRequester.remove(requester);
		return Optional.of(request);
	}
}
