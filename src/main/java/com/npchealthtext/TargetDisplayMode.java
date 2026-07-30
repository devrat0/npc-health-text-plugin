package com.npchealthtext;

public enum TargetDisplayMode
{
	DEFAULT("Default"),
	BOTH("Value/Max (%)"),
	BOTH_VALUE_ONLY("Value (%)"),
	BOTH_PERCENT_FIRST_VALUE("% (Value)"),
	BOTH_PERCENT_FIRST_HP_SUFFIX("% (Value HP)"),
	BOTH_PERCENT_FIRST_MAX("% (Value/Max)"),
	HP_VALUE("Value/Max"),
	HP_VALUE_ONLY("Value Only"),
	HP_PERCENTAGE("% Only"),
	CUSTOM("Custom");

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
