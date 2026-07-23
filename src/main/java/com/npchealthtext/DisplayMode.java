package com.npchealthtext;

public enum DisplayMode
{
	HP_VALUE("HP Value"),
	HP_PERCENTAGE("HP Percentage"),
	BOTH("Both");

	private final String displayName;

	DisplayMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
