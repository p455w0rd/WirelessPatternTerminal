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
package p455w0rd.wpt.items;

import java.util.List;

import appeng.api.config.*;
import appeng.api.util.IConfigManager;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.util.Platform;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.ae2wtlib.api.client.ReadableNumberConverter;
import p455w0rd.ae2wtlib.api.item.ItemWT;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.api.WPTApi;
import p455w0rd.wpt.init.ModGlobals;
import p455w0rd.wpt.util.WPTUtils;

/**
 * @author p455w0rd
 *
 */
public class ItemWPT extends ItemWT implements IWirelessPatternTerminalItem {

	public ItemWPT() {
		this(new ResourceLocation(ModGlobals.MODID, "wpt"));
	}

	public ItemWPT(final ResourceLocation registryName) {
		super(registryName);
	}

	@Override
	public IConfigManager getConfigManager(final ItemStack target) {
		final IConfigManager out = super.getConfigManager(target);
		out.registerSetting(Settings.SORT_BY, SortOrder.NAME);
		out.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
		out.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
		out.readFromNBT(Platform.openNbtData(target).copy());
		return out;
	}

	@Override
	public void openGui(final EntityPlayer player, final boolean isBauble, final int playerSlot) {
		WPTApi.instance().openWPTGui(player, isBauble, playerSlot);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(final World world, final EntityPlayer player, final EnumHand hand) {
		final ItemStack item = player.getHeldItem(hand);
		if (world.isRemote && hand == EnumHand.MAIN_HAND && !item.isEmpty() && getAECurrentPower(item) > 0) {
			openGui(player, false, player.inventory.currentItem);
		}
		else if (!world.isRemote) {

			if (getAECurrentPower(item) <= 0) {
				player.sendMessage(PlayerMessages.DeviceNotPowered.get());
				return new ActionResult<>(EnumActionResult.FAIL, item);
			}
			if (!WPTApi.instance().isTerminalLinked(item)) {
				player.sendMessage(PlayerMessages.DeviceNotLinked.get());
				return new ActionResult<>(EnumActionResult.FAIL, item);
			}
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, item);
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void addCheckedInformation(final ItemStack is, final World world, final List<String> list, final ITooltipFlag advancedTooltips) {
		if (getPlayer() == null || WTApi.instance().getGUIObject(is, getPlayer()) == null) {
			return;
		}
		final String encKey = getEncryptionKey(is);
		//String shift = I18n.format("tooltip.press_shift.desc").replace("Shift", TextFormatting.YELLOW + "" + TextFormatting.BOLD + "" + TextFormatting.ITALIC + "Shift" + TextFormatting.GRAY);
		String pctTxtColor = TextFormatting.WHITE + "";
		final double aeCurrPower = getAECurrentPower(is);
		final double aeCurrPowerPct = (int) Math.floor(aeCurrPower / getAEMaxPower(is) * 1e4) / 1e2;
		if ((int) aeCurrPowerPct >= 75) {
			pctTxtColor = TextFormatting.GREEN + "";
		}
		if ((int) aeCurrPowerPct <= 5) {
			pctTxtColor = TextFormatting.RED + "";
		}
		list.add(TextFormatting.AQUA + "==============================");
		if (WTApi.instance().isWTCreative(is)) {
			list.add(GuiText.StoredEnergy.getLocal() + ": " + TextFormatting.GREEN + "" + I18n.format(WTApi.instance().getConstants().getTooltips().infinite()));
		}
		else {
			list.add(GuiText.StoredEnergy.getLocal() + ": " + pctTxtColor + (int) aeCurrPower + " AE - " + aeCurrPowerPct + "%");
		}
		String linked = TextFormatting.RED + GuiText.Unlinked.getLocal();
		if (encKey != null && !encKey.isEmpty()) {
			linked = TextFormatting.BLUE + GuiText.Linked.getLocal();
		}
		list.add("Link Status: " + linked);

		if (WTApi.instance().getConfig().isInfinityBoosterCardEnabled()) {
			if (WTApi.instance().getConfig().isOldInfinityMechanicEnabled()) {
				list.add(I18n.format(WTApi.instance().getConstants().boosterCardUnlocalizedName()) + ": " + (hasInfiniteRange(is) ? TextFormatting.GREEN + "" : TextFormatting.RED + "" + I18n.format(WTApi.instance().getConstants().getTooltips().not())) + " " + I18n.format(WTApi.instance().getConstants().getTooltips().installed()));
			}
			else {
				final int infinityEnergyAmount = WTApi.instance().getInfinityEnergy(is);
				final String amountColor = infinityEnergyAmount < WTApi.instance().getConfig().getLowInfinityEnergyWarningAmount() ? TextFormatting.RED.toString() : TextFormatting.GREEN.toString();
				String reasonString = "";
				if (infinityEnergyAmount <= 0) {
					reasonString = "(" + I18n.format(WTApi.instance().getConstants().getTooltips().outOf()) + " " + I18n.format(WTApi.instance().getConstants().getTooltips().infinityEnergy()) + ")";
				}
				final boolean outsideOfWAPRange = !WTApi.instance().isInRange(is);
				if (!outsideOfWAPRange) {
					reasonString = I18n.format(WTApi.instance().getConstants().getTooltips().inWapRange());
				}
				final String activeString = infinityEnergyAmount > 0 && outsideOfWAPRange ? TextFormatting.GREEN + "" + I18n.format(WTApi.instance().getConstants().getTooltips().active()) : TextFormatting.GRAY + "" + I18n.format(WTApi.instance().getConstants().getTooltips().inactive()) + " " + reasonString;
				list.add(I18n.format(WTApi.instance().getConstants().getTooltips().infiniteRange()) + ": " + activeString);
				final String infinityEnergyString = WPTUtils.isWPTCreative(is) ? I18n.format(WTApi.instance().getConstants().getTooltips().infinite()) : isShiftKeyDown() ? "" + infinityEnergyAmount + "" + TextFormatting.GRAY + " " + I18n.format(WTApi.instance().getConstants().getTooltips().units()) : ReadableNumberConverter.INSTANCE.toSlimReadableForm(infinityEnergyAmount);
				list.add(I18n.format(WTApi.instance().getConstants().getTooltips().infinityEnergy()) + ": " + amountColor + "" + infinityEnergyString);
			}
		}
	}

	@Override
	public ResourceLocation getMenuIcon() {
		return new ResourceLocation(ModGlobals.MODID, "textures/items/wpt.png");
	}

	@Override
	public int getColor() {
		return 0xFFD67A0F;
	}

}
