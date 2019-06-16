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
package p455w0rd.wpt.util;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.ae2wtlib.api.ICustomWirelessTerminalItem;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.api.WPTApi;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.init.ModKeybindings;
import p455w0rdslib.LibGlobals.Mods;

public class WPTUtils {

	public static final String SHIFTCRAFT_NBT = "ShiftCraft";

	public static NonNullList<ItemStack> getPatternTerminals(final EntityPlayer player) {
		final NonNullList<ItemStack> terminalList = NonNullList.<ItemStack>create();
		final InventoryPlayer playerInventory = player.inventory;
		for (final ItemStack patternTerm : playerInventory.mainInventory) {
			if (isAnyWPT(patternTerm)) {
				terminalList.add(patternTerm);
			}
		}
		if (Mods.BAUBLES.isLoaded()) {
			final Set<Pair<Integer, ItemStack>> pairSet = WTApi.instance().getBaublesUtility().getAllWTBaublesByType(player, IWirelessPatternTerminalItem.class);
			for (final Pair<Integer, ItemStack> pair : pairSet) {
				terminalList.add(pair.getRight());
			}
		}
		return terminalList;
	}

	@Nonnull
	public static ItemStack getPatternTerm(final InventoryPlayer playerInv) {
		if (!playerInv.player.getHeldItemMainhand().isEmpty() && (playerInv.player.getHeldItemMainhand().getItem() instanceof IWirelessPatternTerminalItem || WTApi.instance().getWUTUtility().doesWUTSupportType(playerInv.player.getHeldItemMainhand(), IWirelessPatternTerminalItem.class))) {
			return playerInv.player.getHeldItemMainhand();
		}
		ItemStack patternTerm = ItemStack.EMPTY;
		if (Mods.BAUBLES.isLoaded()) {
			final List<Pair<Integer, ItemStack>> baubleList = Lists.newArrayList(WTApi.instance().getBaublesUtility().getAllWTBaublesByType(playerInv.player, IWirelessPatternTerminalItem.class));
			if (baubleList.size() > 0) {
				patternTerm = baubleList.get(0).getRight();
			}
		}
		if (patternTerm.isEmpty()) {
			final int invSize = playerInv.getSizeInventory();
			if (invSize <= 0) {
				return ItemStack.EMPTY;
			}
			for (int i = 0; i < invSize; ++i) {
				final ItemStack item = playerInv.getStackInSlot(i);
				if (item.isEmpty()) {
					continue;
				}
				if (item.getItem() instanceof IWirelessPatternTerminalItem || WTApi.instance().getWUTUtility().doesWUTSupportType(item, IWirelessPatternTerminalItem.class)) {
					patternTerm = item;
					break;
				}
			}
		}
		return patternTerm;
	}

	public static ItemStack getWPTBySlot(final EntityPlayer player, final int slot) {
		if (slot >= 0) {
			return WTApi.instance().getWTBySlot(player, slot, IWirelessPatternTerminalItem.class);
		}
		return ItemStack.EMPTY;
	}

	/**
	 * gets the first available Wireless Pattern Terminal
	 * the Integer of the Pair tells the slotNumber
	 * the boolean tells whether or not the Integer is a Baubles slot
	 */
	@Nonnull
	public static Pair<Boolean, Pair<Integer, ItemStack>> getFirstWirelessPatternTerminal(final InventoryPlayer playerInv) {
		boolean isBauble = false;
		int slotID = -1;
		ItemStack wirelessTerm = ItemStack.EMPTY;
		if (!playerInv.player.getHeldItemMainhand().isEmpty() && (playerInv.player.getHeldItemMainhand().getItem() instanceof IWirelessPatternTerminalItem || WTApi.instance().getWUTUtility().doesWUTSupportType(playerInv.player.getHeldItemMainhand(), IWirelessPatternTerminalItem.class))) {
			slotID = playerInv.currentItem;
			wirelessTerm = playerInv.player.getHeldItemMainhand();
		}
		else {
			if (Mods.BAUBLES.isLoaded()) {
				final Pair<Integer, ItemStack> bauble = WTApi.instance().getBaublesUtility().getFirstWTBaubleByType(playerInv.player, IWirelessPatternTerminalItem.class);
				if (!bauble.getRight().isEmpty()) {
					wirelessTerm = bauble.getRight();
					slotID = bauble.getLeft();
					if (!wirelessTerm.isEmpty()) {
						isBauble = true;
					}
				}
			}
			if (wirelessTerm.isEmpty()) {
				final int invSize = playerInv.getSizeInventory();
				if (invSize > 0) {
					for (int i = 0; i < invSize; ++i) {
						final ItemStack item = playerInv.getStackInSlot(i);
						if (item.isEmpty()) {
							continue;
						}
						if (item.getItem() instanceof IWirelessPatternTerminalItem || WTApi.instance().getWUTUtility().doesWUTSupportType(item, IWirelessPatternTerminalItem.class)) {
							wirelessTerm = item;
							slotID = i;
							break;
						}
					}
				}
			}
		}
		return Pair.of(isBauble, Pair.of(slotID, wirelessTerm));
	}

	public static boolean isAnyWPT(@Nonnull final ItemStack patternTerm) {
		return patternTerm.getItem() instanceof IWirelessPatternTerminalItem || WTApi.instance().getWUTUtility().doesWUTSupportType(patternTerm, IWirelessPatternTerminalItem.class);
	}

	public static boolean isWPTCreative(final ItemStack patternTerm) {
		return !patternTerm.isEmpty() && ((ICustomWirelessTerminalItem) patternTerm.getItem()).isCreative();
	}

	@SideOnly(Side.CLIENT)
	public static String color(final String color) {
		switch (color) {
		case "white":
			return TextFormatting.WHITE.toString();
		case "black":
			return TextFormatting.BLACK.toString();
		case "green":
			return TextFormatting.GREEN.toString();
		case "red":
			return TextFormatting.RED.toString();
		case "yellow":
			return TextFormatting.YELLOW.toString();
		case "aqua":
			return TextFormatting.AQUA.toString();
		case "blue":
			return TextFormatting.BLUE.toString();
		case "italics":
			return TextFormatting.ITALIC.toString();
		case "bold":
			return TextFormatting.BOLD.toString();
		default:
		case "gray":
			return TextFormatting.GRAY.toString();
		}
	}

	@SideOnly(Side.CLIENT)
	public static EntityPlayer player() {
		return Minecraft.getMinecraft().player;
	}

	public static EntityPlayer player(final InventoryPlayer playerInv) {
		return playerInv.player;
	}

	@SideOnly(Side.CLIENT)
	public static World world() {
		return Minecraft.getMinecraft().world;
	}

	public static World world(final EntityPlayer player) {
		return player.getEntityWorld();
	}

	public static void chatMessage(final EntityPlayer player, final ITextComponent message) {
		player.sendMessage(message);
	}

	@SideOnly(Side.CLIENT)
	public static void handleKeybind() {
		final EntityPlayer p = WPTUtils.player();
		if (p.openContainer == null) {
			return;
		}
		if (ModKeybindings.openPatternTerminal.getKeyCode() != Keyboard.CHAR_NONE && ModKeybindings.openPatternTerminal.isPressed()) {
			final ItemStack is = WPTUtils.getPatternTerm(p.inventory);
			if (is.isEmpty()) {
				return;
			}
			final ICustomWirelessTerminalItem patternTerm = (ICustomWirelessTerminalItem) is.getItem();
			if (patternTerm != null) {
				if (!(p.openContainer instanceof ContainerWPT)) {
					final Pair<Boolean, Pair<Integer, ItemStack>> wptPair = WPTUtils.getFirstWirelessPatternTerminal(p.inventory);
					WPTApi.instance().openWPTGui(p, wptPair.getLeft(), wptPair.getRight().getLeft());
					//ModNetworking.instance().sendToServer(new PacketOpenGui(ModGuiHandler.GUI_WPT));
				}
				else {
					p.closeScreen();
				}
			}
		}
	}

	public static boolean getShiftCraftMode(@Nonnull final ItemStack wirelessTerminal) {
		if (!wirelessTerminal.isEmpty() && wirelessTerminal.hasTagCompound() && wirelessTerminal.getTagCompound().hasKey(SHIFTCRAFT_NBT, NBT.TAG_BYTE)) {
			return !wirelessTerminal.getTagCompound().getBoolean(SHIFTCRAFT_NBT);
		}
		return false;
	}

}
