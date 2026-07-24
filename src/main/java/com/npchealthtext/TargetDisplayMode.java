package com.npchealthtext;

public enum TargetDisplayMode
{
	DEFAULT("Default (Use HP Text Format)"),
	HP_VALUE("HP Value"),
	HP_PERCENTAGE("HP Percentage"),
	BOTH("Both");

	private final String displayName;

	TargetDisplayMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
