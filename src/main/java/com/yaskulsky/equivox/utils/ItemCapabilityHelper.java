package com.yaskulsky.equivox.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.Nullable;

public final class ItemCapabilityHelper {

	private ItemCapabilityHelper() {
	}

	@Nullable
	public static ResourceHandler<ItemResource> getPlayerInventory(Player player) {
		return player.getCapability(Capabilities.Item.ENTITY);
	}

	@Nullable
	public static ResourceHandler<ItemResource> of(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		//Ask the stack itself: ItemAccess.getCapability passes a copy of the stack to the provider, so a handler that keeps
		// its contents in the stack's components (the Mercurial Eye) would write them into that copy
		return stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack));
	}

	@Nullable
	public static ResourceHandler<ItemResource> of(@Nullable ResourceHandler<ItemResource> handler) {
		return handler;
	}
}
