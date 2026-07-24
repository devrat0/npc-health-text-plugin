package com.npchealthtext;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HpThresholdRule
{
	private String npcPattern;
	private double thresholdPercent;
	private Color color;
	private String iconSymbol;
	private Set<IndicatorStyle> indicatorStyles;

	public HpThresholdRule(String npcPattern, double thresholdPercent, Color color, String iconSymbol, Set<IndicatorStyle> indicatorStyles)
	{
		this.npcPattern = npcPattern != null ? npcPattern.trim() : "*";
		this.thresholdPercent = thresholdPercent;
		this.color = color != null ? color : Color.RED;
		this.iconSymbol = iconSymbol != null ? iconSymbol.trim() : null;
		this.indicatorStyles = indicatorStyles != null && !indicatorStyles.isEmpty()
			? EnumSet.copyOf(indicatorStyles)
			: Collections.emptySet();
	}

	public HpThresholdRule(String npcPattern, double thresholdPercent, Color color, Set<IndicatorStyle> indicatorStyles)
	{
		this(npcPattern, thresholdPercent, color, null, indicatorStyles);
	}

	public boolean hasSpecificStyles()
	{
		return indicatorStyles != null && !indicatorStyles.isEmpty();
	}

	public boolean containsStyle(IndicatorStyle style)
	{
		if (indicatorStyles == null || indicatorStyles.isEmpty())
		{
			return true; // Default to allowing style if none restricted
		}
		return indicatorStyles.contains(IndicatorStyle.ALL) || indicatorStyles.contains(style);
	}
}
