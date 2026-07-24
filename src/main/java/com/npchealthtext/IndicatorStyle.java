package com.npchealthtext;

public enum IndicatorStyle
{
	TILE("Tile Glow"),
	HULL("Hull Glow"),
	BORDER("Border Glow"),
	ICON("Text Icon"),
	ALL("All Indicators");

	private final String name;

	IndicatorStyle(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
