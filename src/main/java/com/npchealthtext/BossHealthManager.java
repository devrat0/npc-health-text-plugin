package com.npchealthtext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.WildcardMatcher;

/**
 * Manages extraction, parsing, normalization, and health ratio correlation
 * for the top OSRS Boss HP bar widget (Widget ID 303).
 */
public class BossHealthManager
{
	// Regex pattern to extract current and max HP values formatted as "current / max"
	private static final Pattern HP_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");

	// Regex pattern to remove HTML tags and commas from widget text
	private static final Pattern CLEAN_TAGS_AND_COMMAS = Pattern.compile("<[^>]*>|,");

	// Regex pattern to strip parenthetical descriptors (e.g. "(Melee)") from NPC names
	private static final Pattern PARENTHESES_PATTERN = Pattern.compile("\\(.*?\\)");

	// Regex pattern to verify string contains letters (used to find boss names in widgets)
	private static final Pattern HAS_LETTERS_PATTERN = Pattern.compile(".*[a-zA-Z].*");

	/**
	 * Immutable container holding scraped Boss HP widget details.
	 */
	public static class BossHealthData
	{
		private final String bossName;
		private final int currentHp;
		private final int maxHp;

		public BossHealthData(String bossName, int currentHp, int maxHp)
		{
			this.bossName = bossName;
			this.currentHp = currentHp;
			this.maxHp = maxHp;
		}

		public String getBossName()
		{
			return bossName;
		}

		public int getCurrentHp()
		{
			return currentHp;
		}

		public int getMaxHp()
		{
			return maxHp;
		}
	}

	/**
	 * Scrapes top-of-screen OSRS Boss HP bar widget (Widget group 303)
	 * to extract active boss name, current HP, and max HP.
	 *
	 * @param client RuneLite Client instance
	 * @return BossHealthData object if widget is active and contains valid HP, null otherwise
	 */
	public BossHealthData getBossHealthFromWidget(Client client)
	{
		if (client == null)
		{
			return null;
		}

		try
		{
			String bossName = null;
			int[] hpValues = null;

			// Inspect child widgets under group 303 (Boss Health Bar container)
			Widget bossWidgetGroup = client.getWidget(303, 0);
			if (bossWidgetGroup != null)
			{
				Widget[] children = bossWidgetGroup.getChildren();
				if (children != null)
				{
					for (Widget child : children)
					{
						if (child == null || child.isHidden() || child.getText() == null)
						{
							continue;
						}

						// Clean HTML formatting tags and commas from widget text
						String cleanText = CLEAN_TAGS_AND_COMMAS.matcher(child.getText()).replaceAll("").trim();
						if (cleanText.isEmpty())
						{
							continue;
						}

						// Parse numerical HP values ("cur / max")
						int[] parsed = parseHpString(cleanText);
						if (parsed != null && hpValues == null)
						{
							hpValues = parsed;
						}
						// Extract boss title string containing letters
						else if (bossName == null && HAS_LETTERS_PATTERN.matcher(cleanText).matches())
						{
							bossName = cleanText;
						}
					}
				}
			}

			// Fallback: check individual child widget slots 1 through 25
			for (int childId = 1; childId <= 25; childId++)
			{
				Widget w = client.getWidget(303, childId);
				if (w != null && !w.isHidden() && w.getText() != null)
				{
					String cleanText = CLEAN_TAGS_AND_COMMAS.matcher(w.getText()).replaceAll("").trim();
					if (cleanText.isEmpty())
					{
						continue;
					}

					int[] parsed = parseHpString(cleanText);
					if (parsed != null && hpValues == null)
					{
						hpValues = parsed;
					}
					else if (bossName == null && HAS_LETTERS_PATTERN.matcher(cleanText).matches())
					{
						bossName = cleanText;
					}
				}
			}

			// If valid current and max HP values were parsed, return container
			if (hpValues != null && hpValues.length >= 2)
			{
				return new BossHealthData(bossName, hpValues[0], hpValues[1]);
			}
		}
		catch (Exception ignored)
		{
		}

		return null;
	}

	/**
	 * Parses a string containing "current / max" health numbers into an integer array [current, max].
	 */
	public int[] parseHpString(String text)
	{
		if (text == null || text.trim().isEmpty())
		{
			return null;
		}
		try
		{
			String cleanText = CLEAN_TAGS_AND_COMMAS.matcher(text).replaceAll("").trim();
			Matcher matcher = HP_PATTERN.matcher(cleanText);
			if (matcher.find())
			{
				int cur = Integer.parseInt(matcher.group(1));
				int max = Integer.parseInt(matcher.group(2));
				if (max > 0 && cur >= 0)
				{
					return new int[]{cur, max};
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return null;
	}

	/**
	 * Strips parenthetical suffixes, dashes, and colons to extract base boss name.
	 * E.g., "Vanguard (Melee)" -> "vanguard", "Olm's Hand - Left" -> "olm's hand".
	 */
	public String getNormalizedBaseName(String name)
	{
		if (name == null)
		{
			return "";
		}
		String cleaned = PARENTHESES_PATTERN.matcher(name).replaceAll("").trim().toLowerCase();
		if (cleaned.contains("-"))
		{
			cleaned = cleaned.split("-")[0].trim();
		}
		if (cleaned.contains(":"))
		{
			cleaned = cleaned.split(":")[0].trim();
		}
		return cleaned;
	}

	/**
	 * Determines whether an in-game NPC name matches a Boss Bar widget name using exact matching,
	 * normalized base name comparison, prefix matching, or wildcard patterns.
	 */
	public boolean bossNameMatches(String npcName, String widgetBossName)
	{
		if (npcName == null || widgetBossName == null)
		{
			return false;
		}

		String cleanNpc = npcName.trim().toLowerCase();
		String cleanWidget = widgetBossName.trim().toLowerCase();

		if (cleanNpc.isEmpty() || cleanWidget.isEmpty())
		{
			return false;
		}

		// Direct exact match check
		if (cleanNpc.equals(cleanWidget))
		{
			return true;
		}

		// Normalized base name comparison (stripping suffixes/prefixes)
		String baseNpc = getNormalizedBaseName(npcName);
		String baseWidget = getNormalizedBaseName(widgetBossName);

		if (!baseNpc.isEmpty() && baseNpc.equals(baseWidget))
		{
			return true;
		}

		// Prefix matching for base names of length >= 4
		if (baseNpc.length() >= 4 && baseWidget.length() >= 4)
		{
			if (baseNpc.startsWith(baseWidget) || baseWidget.startsWith(baseNpc))
			{
				return true;
			}
		}

		// Wildcard pattern matching fallback
		return WildcardMatcher.matches(cleanWidget, cleanNpc) || WildcardMatcher.matches(cleanNpc, cleanWidget);
	}

	/**
	 * Counts active (non-dead) NPCs in the current room matching the specified boss widget name.
	 */
	public int countMatchingNpcs(Iterable<NPC> npcs, String bossName)
	{
		if (npcs == null || bossName == null || bossName.trim().isEmpty())
		{
			return 0;
		}
		int count = 0;
		for (NPC n : npcs)
		{
			if (n != null && !n.isDead() && n.getName() != null && bossNameMatches(n.getName(), bossName))
			{
				count++;
			}
		}
		return count;
	}

	/**
	 * Checks if an NPC's client health bar ratio (ratio / scale) matches the top Boss Bar widget ratio
	 * (currentHp / maxHp) within a quantization tolerance.
	 */
	public boolean isNpcRatioMatchingBossWidget(NPC npc, int ratio, int scale, BossHealthData bossWidgetData, Map<Integer, int[]> lastHpMap)
	{
		if (npc == null || bossWidgetData == null || bossWidgetData.getMaxHp() <= 0 || bossWidgetData.getCurrentHp() < 0)
		{
			return false;
		}

		// Fallback to cached health ratio if current NPC ratio/scale is invalid (-1)
		if (ratio < 0 || scale <= 0)
		{
			int index = npc.getIndex();
			if (lastHpMap != null && lastHpMap.containsKey(index))
			{
				int[] cached = lastHpMap.get(index);
				ratio = cached[0];
				scale = cached[1];
			}
		}

		if (ratio < 0 || scale <= 0)
		{
			return false;
		}

		// Calculate client health fraction and boss widget health fraction
		double npcFraction = (double) ratio / scale;
		double bossFraction = (double) bossWidgetData.getCurrentHp() / bossWidgetData.getMaxHp();

		// Calculate ratio quantization tolerance based on health scale
		double tolerance = (1.5 / scale) + 0.01;
		return Math.abs(npcFraction - bossFraction) <= tolerance;
	}
}
