package com.npchealthtext;

public enum OverlayPositionMode
{
	ABOVE("Above"),
	MIDDLE("Middle"),
	BOTTOM("Bottom");

	private final String name;

	OverlayPositionMode(String name)
	{
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
