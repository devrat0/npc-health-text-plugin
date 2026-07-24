package com.npchealthtext;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.util.WildcardMatcher;

public class HpThresholdManager
{
	private static final Map<String, Color> COLOR_MAP = new HashMap<>();

	static
	{
		COLOR_MAP.put("red", Color.RED);
		COLOR_MAP.put("yellow", Color.YELLOW);
		COLOR_MAP.put("orange", new Color(255, 140, 0));
		COLOR_MAP.put("green", Color.GREEN);
		COLOR_MAP.put("lime", new Color(50, 205, 50));
		COLOR_MAP.put("cyan", Color.CYAN);
		COLOR_MAP.put("blue", Color.BLUE);
		COLOR_MAP.put("magenta", Color.MAGENTA);
		COLOR_MAP.put("purple", new Color(128, 0, 128));
		COLOR_MAP.put("pink", Color.PINK);
		COLOR_MAP.put("white", Color.WHITE);
		COLOR_MAP.put("black", Color.BLACK);
		COLOR_MAP.put("gray", Color.GRAY);
		COLOR_MAP.put("grey", Color.GRAY);
	}

	public List<HpThresholdRule> parseRules(String rawConfig)
	{
		List<HpThresholdRule> rules = new ArrayList<>();
		if (rawConfig == null || rawConfig.trim().isEmpty())
		{
			return rules;
		}

		String[] entries = rawConfig.split("[,;\\n]+");
		for (String entry : entries)
		{
			String trimmed = entry.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}

			String[] parts = trimmed.split(":");
			if (parts.length < 2)
			{
				continue;
			}

			String npcPattern;
			double percent;
			Color color;
			String iconSymbol = null;
			Set<IndicatorStyle> styles = null;

			try
			{
				if (isNumeric(parts[0]))
				{
					// Global format: THRESHOLD:COLOR[:ICON][:STYLES] (e.g. 10:red or 10:red:⚠)
					npcPattern = "*";
					percent = Double.parseDouble(parts[0].trim());
					color = parseColor(parts[1].trim());

					for (int i = 2; i < parts.length; i++)
					{
						String tok = parts[i].trim();
						if (isStyleToken(tok))
						{
							styles = parseStyles(tok);
						}
						else if (iconSymbol == null && !tok.isEmpty())
						{
							iconSymbol = tok;
						}
					}
				}
				else
				{
					// NPC format: NPC:THRESHOLD:COLOR[:ICON][:STYLES] (e.g. Ba-Ba:66:yellow:⚡)
					npcPattern = parts[0].trim();
					percent = Double.parseDouble(parts[1].trim());
					color = (parts.length >= 3) ? parseColor(parts[2].trim()) : Color.RED;

					for (int i = 3; i < parts.length; i++)
					{
						String tok = parts[i].trim();
						if (isStyleToken(tok))
						{
							styles = parseStyles(tok);
						}
						else if (iconSymbol == null && !tok.isEmpty())
						{
							iconSymbol = tok;
						}
					}
				}

				rules.add(new HpThresholdRule(npcPattern, percent, color, iconSymbol, styles));
			}
			catch (Exception ignored)
			{
				// Skip invalid entries gracefully
			}
		}
		return rules;
	}

	public String serializeRules(List<HpThresholdRule> rules)
	{
		if (rules == null || rules.isEmpty())
		{
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < rules.size(); i++)
		{
			HpThresholdRule rule = rules.get(i);
			if (i > 0)
			{
				sb.append(", ");
			}
			sb.append(rule.getNpcPattern()).append(":")
				.append(formatPercent(rule.getThresholdPercent())).append(":")
				.append(toHex(rule.getColor()));

			if (rule.getIconSymbol() != null && !rule.getIconSymbol().isEmpty())
			{
				sb.append(":").append(rule.getIconSymbol());
			}

			if (rule.hasSpecificStyles())
			{
				sb.append(":");
				List<String> styleNames = new ArrayList<>();
				for (IndicatorStyle s : rule.getIndicatorStyles())
				{
					styleNames.add(s.name().toLowerCase());
				}
				sb.append(String.join("|", styleNames));
			}
		}
		return sb.toString();
	}

	public HpThresholdRule getMatchingThreshold(String npcName, double currentHpPct, String overridesConfig, String globalConfig)
	{
		if (npcName == null)
		{
			npcName = "";
		}

		// 1. Check NPC Overrides first
		List<HpThresholdRule> overrideRules = parseRules(overridesConfig);
		List<HpThresholdRule> matchingOverrides = new ArrayList<>();
		String cleanNpcName = npcName.trim().toLowerCase();

		for (HpThresholdRule rule : overrideRules)
		{
			String pattern = rule.getNpcPattern().toLowerCase();
			boolean matchesName = pattern.equals("*") || WildcardMatcher.matches(pattern, cleanNpcName);
			if (matchesName && currentHpPct <= rule.getThresholdPercent())
			{
				matchingOverrides.add(rule);
			}
		}

		if (!matchingOverrides.isEmpty())
		{
			// Pick rule with the SMALLEST threshold percent (most critical)
			matchingOverrides.sort(Comparator.comparingDouble(HpThresholdRule::getThresholdPercent));
			return matchingOverrides.get(0);
		}

		// 2. Fall back to Global Rules
		List<HpThresholdRule> globalRules = parseRules(globalConfig);
		List<HpThresholdRule> matchingGlobals = new ArrayList<>();

		for (HpThresholdRule rule : globalRules)
		{
			if (currentHpPct <= rule.getThresholdPercent())
			{
				matchingGlobals.add(rule);
			}
		}

		if (!matchingGlobals.isEmpty())
		{
			matchingGlobals.sort(Comparator.comparingDouble(HpThresholdRule::getThresholdPercent));
			return matchingGlobals.get(0);
		}

		return null;
	}

	public Color parseColor(String colorStr)
	{
		if (colorStr == null || colorStr.trim().isEmpty())
		{
			return Color.RED;
		}

		String clean = colorStr.trim().toLowerCase();
		if (COLOR_MAP.containsKey(clean))
		{
			return COLOR_MAP.get(clean);
		}

		if (!clean.startsWith("#"))
		{
			clean = "#" + clean;
		}

		try
		{
			if (clean.length() == 7) // #RRGGBB
			{
				return new Color(Integer.parseInt(clean.substring(1), 16));
			}
			else if (clean.length() == 9) // #AARRGGBB
			{
				int argb = (int) Long.parseLong(clean.substring(1), 16);
				return new Color(argb, true);
			}
		}
		catch (Exception ignored) {}

		return Color.RED;
	}

	public String toHex(Color color)
	{
		if (color == null)
		{
			return "#FF0000";
		}
		for (Map.Entry<String, Color> entry : COLOR_MAP.entrySet())
		{
			if (entry.getValue().equals(color))
			{
				return entry.getKey();
			}
		}
		if (color.getAlpha() < 255)
		{
			return String.format("#%02X%02X%02X%02X", color.getAlpha(), color.getRed(), color.getGreen(), color.getBlue());
		}
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	private boolean isNumeric(String str)
	{
		if (str == null)
		{
			return false;
		}
		try
		{
			Double.parseDouble(str.trim());
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	private boolean isStyleToken(String str)
	{
		if (str == null || str.trim().isEmpty())
		{
			return false;
		}
		String clean = str.trim().toUpperCase();
		if (clean.contains("|") || clean.contains("&"))
		{
			return true;
		}
		for (IndicatorStyle style : IndicatorStyle.values())
		{
			if (style.name().equals(clean))
			{
				return true;
			}
		}
		return false;
	}

	private Set<IndicatorStyle> parseStyles(String stylesStr)
	{
		Set<IndicatorStyle> set = EnumSet.noneOf(IndicatorStyle.class);
		String[] tokens = stylesStr.split("[|&]+");
		for (String tok : tokens)
		{
			String clean = tok.trim().toUpperCase();
			try
			{
				set.add(IndicatorStyle.valueOf(clean));
			}
			catch (IllegalArgumentException ignored) {}
		}
		return set;
	}

	private String formatPercent(double val)
	{
		if (val == (long) val)
		{
			return String.format("%d", (long) val);
		}
		return String.format("%.1f", val);
	}
}
