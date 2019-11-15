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

import appeng.api.config.*;
import appeng.api.util.IConfigManager;
import appeng.util.Platform;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import p455w0rd.ae2wtlib.api.item.ItemWT;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.api.WPTApi;
import p455w0rd.wpt.init.ModGlobals;

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
	public ResourceLocation getMenuIcon() {
		return new ResourceLocation(ModGlobals.MODID, "textures/items/wpt.png");
	}

	@Override
	public int getColor() {
		return 0xFFD67A0F;
	}

}
