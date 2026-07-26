package com.npchealthtext;

import net.runelite.api.NPC;
import net.runelite.client.util.WildcardMatcher;

/**
 * Handles target interaction detection, NPC display mode filtering (SHOW_TARGET_NPC,
 * SHOW_WHITELISTED, etc.), and wildcard matching against whitelists and blacklists.
 */
public class NpcFilterManager
{
	private final BossHealthManager bossHealthManager;

	public NpcFilterManager()
	{
		this(new BossHealthManager());
	}

	public NpcFilterManager(BossHealthManager bossHealthManager)
	{
		this.bossHealthManager = bossHealthManager != null ? bossHealthManager : new BossHealthManager();
	}

	/**
	 * Evaluates whether an NPC is considered an active target.
	 * Checks direct player interaction, recent combat history, and boss widget correlation.
	 *
	 * @param npc Target NPC to evaluate
	 * @param context Frame-level targeting state
	 * @return true if the NPC is an active target, false otherwise
	 */
	public boolean isTarget(NPC npc, TargetContext context)
	{
		if (npc == null || context == null)
		{
			return false;
		}

		// Player is directly attacking / interacting with this NPC
		if (context.getCurrentInteracting() == npc)
		{
			return true;
		}

		// NPC is directly attacking / targeting the local player
		if (context.getLocalPlayer() != null && npc.getInteracting() == context.getLocalPlayer())
		{
			return true;
		}

		// NPC was recently targeted within the last 15 seconds (or active boss bar)
		if (context.getLastTargetNpc() == npc && !npc.isDead())
		{
			if (context.isBossBarActive() || System.currentTimeMillis() - context.getLastTargetTime() < 15000)
			{
				return true;
			}
		}

		// Active boss bar widget fallback for single boss or matching ratio NPC
		if (context.isBossBarActive() && !npc.isDead() && context.getLastTargetNpc() == null)
		{
			if (context.getMatchingBossCount() <= 1
				|| bossHealthManager.isNpcRatioMatchingBossWidget(
					npc,
					npc.getHealthRatio(),
					npc.getHealthScale(),
					context.getBossWidgetData(),
					context.getLastHpMap()))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Determines whether an NPC overlay should be rendered based on NpcDisplayMode,
	 * target status, whitelist configuration, and blacklist configuration.
	 */
	public boolean shouldRenderForDisplayMode(String npcName, boolean isTarget, NpcDisplayMode mode, String nameList, String blacklist)
	{
		if (npcName == null)
		{
			return false;
		}

		if (mode == null)
		{
			mode = NpcDisplayMode.SHOW_TARGET_NPC;
		}

		switch (mode)
		{
			case SHOW_TARGET_NPC:
				if (!isTarget)
				{
					return false;
				}
				break;
			case SHOW_WHITELISTED:
				if (!isNameInList(npcName, nameList))
				{
					return false;
				}
				break;
			case SHOW_WHITELIST_TARGET:
				if (!isTarget || !isNameInList(npcName, nameList))
				{
					return false;
				}
				break;
			case SHOW_ALL:
			default:
				break;
		}

		// Enforce blacklist filtering if blacklist is configured
		if (blacklist != null && !blacklist.trim().isEmpty())
		{
			if (isNameInList(npcName, blacklist))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if an NPC name matches any item in a comma-separated wildcard pattern list.
	 */
	public boolean isNameInList(String npcName, String rawList)
	{
		if (rawList == null || rawList.trim().isEmpty() || npcName == null)
		{
			return false;
		}

		String lowerName = npcName.trim().toLowerCase();
		String[] parts = rawList.split(",");
		for (String part : parts)
		{
			String pattern = part.trim().toLowerCase();
			if (!pattern.isEmpty() && WildcardMatcher.matches(pattern, lowerName))
			{
				return true;
			}
		}
		return false;
	}
}
