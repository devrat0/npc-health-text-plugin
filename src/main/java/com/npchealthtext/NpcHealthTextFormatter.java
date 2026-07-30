package com.npchealthtext;

import java.awt.Color;

/**
 * Formats health values and percentage strings using presets or custom template placeholders
 * ({curr}, {max}, {pct}) and calculates dynamic color gradients based on health ratios.
 */
public class NpcHealthTextFormatter
{
	/**
	 * Formats NPC health text according to the selected DisplayMode preset or custom template pattern.
	 *
	 * @param currentHp Current calculated or overridden HP
	 * @param maxHp Max HP of the NPC (0 if unknown)
	 * @param ratio Client health ratio
	 * @param scale Client health scale
	 * @param overrideCurrentHpActive Whether an exact Boss Bar widget HP override is active
	 * @param mode DisplayMode format option
	 * @param showDecimalPercentage Whether to include 1 decimal place in percentage outputs
	 * @param hidePercentageSymbol Whether to omit the '%' percentage symbol
	 * @param customHpFormat Custom format template string (e.g. "{pct} ({curr} HP)")
	 * @return Formatted overlay text string
	 */
	public String formatHealthText(
		int currentHp,
		int maxHp,
		int ratio,
		int scale,
		boolean overrideCurrentHpActive,
		DisplayMode mode,
		boolean showDecimalPercentage,
		boolean hidePercentageSymbol,
		String customHpFormat)
	{
		if (mode == null)
		{
			mode = DisplayMode.BOTH_PERCENT_FIRST_HP_SUFFIX;
		}

		String pctSuffix = hidePercentageSymbol ? "" : "%";
		String pctStr;

		double hpFraction = (overrideCurrentHpActive && maxHp > 0)
			? ((double) currentHp / maxHp)
			: ((double) ratio / scale);

		if (showDecimalPercentage)
		{
			pctStr = String.format("%.1f%s", hpFraction * 100.0, pctSuffix);
		}
		else
		{
			int pctInt = (int) Math.round(hpFraction * 100.0);
			if (pctInt == 0 && ratio > 0)
			{
				pctInt = 1;
			}
			pctStr = String.format("%d%s", pctInt, pctSuffix);
		}

		String currStr = String.valueOf(currentHp);
		String maxStr = maxHp > 0 ? String.valueOf(maxHp) : "";

		// Unknown Max HP path fallback
		if (maxHp <= 0 && mode != DisplayMode.CUSTOM)
		{
			return pctStr;
		}

		switch (mode)
		{
			case BOTH:
				return String.format("%s / %s (%s)", currStr, maxStr, pctStr);
			case BOTH_VALUE_ONLY:
				return String.format("%s (%s)", currStr, pctStr);
			case BOTH_PERCENT_FIRST_VALUE:
				return String.format("%s (%s)", pctStr, currStr);
			case BOTH_PERCENT_FIRST_HP_SUFFIX:
				return String.format("%s (%s HP)", pctStr, currStr);
			case BOTH_PERCENT_FIRST_MAX:
				return String.format("%s (%s / %s)", pctStr, currStr, maxStr);
			case HP_VALUE:
				return String.format("%s / %s", currStr, maxStr);
			case HP_VALUE_ONLY:
				return currStr;
			case HP_PERCENTAGE:
				return pctStr;
			case CUSTOM:
				if (customHpFormat == null || customHpFormat.trim().isEmpty())
				{
					customHpFormat = "{pct} ({curr} HP)";
				}
				String output = customHpFormat;
				output = output.replace("{curr}", currStr);
				output = output.replace("{max}", maxHp > 0 ? maxStr : "?");
				output = output.replace("{pct}", pctStr);
				return output;
			default:
				return String.format("%s (%s HP)", pctStr, currStr);
		}
	}

	public String formatHealthText(
		int currentHp,
		int maxHp,
		int ratio,
		int scale,
		boolean overrideCurrentHpActive,
		DisplayMode mode,
		boolean showDecimalPercentage,
		boolean hidePercentageSymbol)
	{
		return formatHealthText(
			currentHp, maxHp, ratio, scale, overrideCurrentHpActive,
			mode, showDecimalPercentage, hidePercentageSymbol, "{pct} ({curr} HP)"
		);
	}

	public String formatHealthText(
		int currentHp,
		int maxHp,
		int ratio,
		int scale,
		boolean overrideCurrentHpActive,
		DisplayMode mode,
		boolean showDecimalPercentage)
	{
		return formatHealthText(
			currentHp, maxHp, ratio, scale, overrideCurrentHpActive,
			mode, showDecimalPercentage, false, "{pct} ({curr} HP)"
		);
	}

	/**
	 * Calculates a smooth color gradient from low HP color (0% HP) to yellow (50% HP) to high HP color (100% HP).
	 */
	public Color getHpGradientColor(Color lowColor, Color highColor, double ratio)
	{
		ratio = Math.max(0.0, Math.min(1.0, ratio));

		int r, g, b, a;
		// Upper half gradient: 50% HP (Yellow) -> 100% HP (High Color)
		if (ratio >= 0.5)
		{
			double factor = (ratio - 0.5) * 2.0;
			int midR = 255;
			int midG = 255;
			int midB = 0;

			r = (int) (midR + factor * (highColor.getRed() - midR));
			g = (int) (midG + factor * (highColor.getGreen() - midG));
			b = (int) (midB + factor * (highColor.getBlue() - midB));
			a = highColor.getAlpha();
		}
		// Lower half gradient: 0% HP (Low Color) -> 50% HP (Yellow)
		else
		{
			double factor = ratio * 2.0;
			int midR = 255;
			int midG = 255;
			int midB = 0;

			r = (int) (lowColor.getRed() + factor * (midR - lowColor.getRed()));
			g = (int) (lowColor.getGreen() + factor * (midG - lowColor.getGreen()));
			b = (int) (lowColor.getBlue() + factor * (midB - lowColor.getBlue()));
			a = lowColor.getAlpha();
		}

		return new Color(
			Math.max(0, Math.min(255, r)),
			Math.max(0, Math.min(255, g)),
			Math.max(0, Math.min(255, b)),
			Math.max(0, Math.min(255, a))
		);
	}
}
