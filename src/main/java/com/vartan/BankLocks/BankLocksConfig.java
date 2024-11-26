package com.vartan.BankLocks;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Bank Locks")
public interface BankLocksConfig extends Config
{
	@ConfigItem(
			keyName = "preventDepositAll",
			name = "Prevent deposit-all locked item",
			description = "Whether the 'Deposit inventory' and 'Deposit worn items' buttons will be " +
					"disabled if pressing it would deposit a locked item."
	)
	default boolean preventDepositAll()
	{
		return true;
	}

	@ConfigItem(
			keyName = "playSoundWhenPrevented",
			name = "Play a sound when a deposit is prevented",
			description = "'Whether a warning sound will play when the plugin prevents a deposit."
	)
	default boolean playSoundWhenPrevented()
	{
		return true;
	}

}
