package com.npchealthtext;

import java.awt.Color;

/**
 * Formats health values and percentage strings (HP_VALUE, HP_PERCENTAGE, BOTH, decimal percentage)
 * and calculates dynamic color gradients based on health ratios.
 */
public class NpcHealthTextFormatter
{
	/**
	 * Formats NPC health text according to the selected DisplayMode and percentage options.
	 *
	 * @param currentHp Current calculated or overridden HP
	 * @param maxHp Max HP of the NPC (0 if unknown)
	 * @param ratio Client health ratio
	 * @param scale Client health scale
	 * @param overrideCurrentHpActive Whether an exact Boss Bar widget HP override is active
	 * @param mode DisplayMode format option (HP_VALUE, HP_PERCENTAGE, BOTH)
	 * @param showDecimalPercentage Whether to include 1 decimal place in percentage outputs
	 * @param hidePercentageSymbol Whether to omit the '%' percentage symbol
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
		boolean hidePercentageSymbol)
	{
		if (mode == null)
		{
			mode = DisplayMode.BOTH;
		}

		String pctSuffix = hidePercentageSymbol ? "" : "%";

		// Known Max HP path (e.g. 325 / 900)
		if (maxHp > 0)
		{
			String valStr = String.format("%d / %d", currentHp, maxHp);

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

			switch (mode)
			{
				case HP_VALUE:
					return valStr;
				case HP_PERCENTAGE:
					return pctStr;
				case BOTH:
				default:
					return String.format("%s (%s)", valStr, pctStr);
			}
		}
		// Unknown Max HP path (percentage fallback)
		else
		{
			if (showDecimalPercentage)
			{
				return String.format("%.1f%s", ((double) ratio / scale) * 100.0, pctSuffix);
			}
			else
			{
				int pctInt = (int) Math.round(((double) ratio / scale) * 100.0);
				if (pctInt == 0 && ratio > 0)
				{
					pctInt = 1;
				}
				return String.format("%d%s", pctInt, pctSuffix);
			}
		}
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
		return formatHealthText(currentHp, maxHp, ratio, scale, overrideActive(overrideCurrentHpActive), mode, showDecimalPercentage, false);
	}

	private boolean overrideActive(boolean val)
	{
		return val;
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
