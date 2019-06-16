/*
 * This file is part of Wireless Pattern Terminal. Copyright (c) 2017, p455w0rd
 * (aka TheRealp455w0rd), All rights reserved unless otherwise stated.
 *
 * Wireless Pattern Terminal is free software: you can redistribute it and/or
 * modify it under the terms of the MIT License.
 *
 * Wireless Pattern Terminal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the MIT License for
 * more details.
 *
 * You should have received a copy of the MIT License along with Wireless
 * Pattern Terminal. If not, see <https://opensource.org/licenses/MIT>.
 */
package p455w0rd.wpt.init;

import org.apache.commons.lang3.tuple.Pair;

import appeng.api.AEApi;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import p455w0rd.ae2wtlib.api.*;
import p455w0rd.wpt.WPT;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.client.gui.*;
import p455w0rd.wpt.container.*;
import p455w0rd.wpt.util.WPTUtils;

/**
 * @author p455w0rd
 *
 */
public class ModGuiHandler implements IGuiHandler {

	public static final int GUI_WPT = 0;
	public static final int GUI_CRAFT_CONFIRM = 1;
	public static final int GUI_CRAFT_AMOUNT = 2;
	public static final int GUI_CRAFTING_STATUS = 3;
	private static int slot = -1;
	private static boolean isBauble = false;

	public static boolean isBauble() {
		return isBauble;
	}

	public static void setIsBauble(final boolean value) {
		isBauble = value;
	}

	public static int getSlot() {
		return slot;
	}

	public static void setSlot(final int value) {
		slot = value;
	}

	@Override
	public Object getServerGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
		final ITerminalHost patternTerminal = getPatternTerminal(player, world, new BlockPos(x, y, z), isBauble(), getSlot());
		if (patternTerminal != null) {
			switch (ID) {
			case GUI_WPT:
				return new ContainerWPT(player, patternTerminal, getSlot(), isBauble());
			case GUI_CRAFTING_STATUS:
				return new ContainerCraftingStatus(player.inventory, patternTerminal, getSlot(), isBauble());
			case GUI_CRAFT_AMOUNT:
				return new ContainerCraftAmount(player.inventory, patternTerminal, getSlot(), isBauble());
			case GUI_CRAFT_CONFIRM:
				return new ContainerCraftConfirm(player.inventory, patternTerminal, isBauble(), getSlot());
			}
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(final int ID, final EntityPlayer player, final World world, final int x, final int y, final int z) {
		final ITerminalHost patternTerminal = getPatternTerminal(player, world, new BlockPos(x, y, z), isBauble(), getSlot());
		if (patternTerminal != null) {
			switch (ID) {
			case GUI_WPT:
				return new GuiWPT(new ContainerWPT(player, patternTerminal, getSlot(), isBauble()));
			case GUI_CRAFTING_STATUS:
				return new GuiCraftingStatus(player.inventory, patternTerminal, getSlot(), isBauble());
			case GUI_CRAFT_AMOUNT:
				return new GuiCraftAmount(player.inventory, patternTerminal, getSlot(), isBauble());
			case GUI_CRAFT_CONFIRM:
				return new GuiCraftConfirm(player.inventory, patternTerminal, isBauble(), getSlot());
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private ITerminalHost getPatternTerminal(final EntityPlayer player, final World world, final BlockPos pos, final boolean isBauble, final int slot) {
		ItemStack wirelessTerminal = ItemStack.EMPTY;
		if (slot >= 0) {
			wirelessTerminal = isBauble ? WTApi.instance().getBaublesUtility().getWTBySlot(player, slot, IWirelessPatternTerminalItem.class) : WTApi.instance().getWTBySlot(player, slot);
		}
		else {
			final Pair<Boolean, Pair<Integer, ItemStack>> firstTerm = WPTUtils.getFirstWirelessPatternTerminal(player.inventory);
			wirelessTerminal = firstTerm.getRight().getRight();
			setSlot(firstTerm.getRight().getLeft());
			setIsBauble(firstTerm.getLeft());
		}
		final ICustomWirelessTerminalItem wh = (ICustomWirelessTerminalItem) AEApi.instance().registries().wireless().getWirelessTerminalHandler(wirelessTerminal);
		final WTGuiObject<IAEItemStack> terminal = wh == null ? null : (WTGuiObject<IAEItemStack>) WTApi.instance().getGUIObject(wh, wirelessTerminal, player);
		return terminal;
	}

	public static void open(final int ID, final EntityPlayer player, final World world, final BlockPos pos, final boolean isBauble, final int slot) {
		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();
		setIsBauble(isBauble);
		setSlot(slot);
		player.openGui(WPT.INSTANCE, ID, world, x, y, z);
	}

}
