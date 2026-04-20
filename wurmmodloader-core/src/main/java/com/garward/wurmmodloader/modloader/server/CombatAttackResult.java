package com.garward.wurmmodloader.modloader.server;

/**
 * Result holder for CombatAttackEvent dispatch.
 *
 * <p>Carries both cancellation status and the attack result back to the bytecode
 * patch that invokes {@code ProxyServerHook.fireCombatAttackEvent}.
 *
 * <p><strong>Top-level, not nested:</strong> this type must stay at the top level
 * so mod classloaders can resolve it via reflection on ProxyServerHook's method
 * table. Making it a nested class of ServerHook trips
 * {@code Class.getDeclaredMethods0} under mod classloader isolation (see
 * {@code LootManager.onCreatureDeath}'s {@code Class.forName} path).
 */
public final class CombatAttackResult {
    public final boolean cancelled;
    public final boolean attackResult;

    public CombatAttackResult(boolean cancelled, boolean attackResult) {
        this.cancelled = cancelled;
        this.attackResult = attackResult;
    }
}
