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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.relauncher.Side;
import p455w0rd.ae2wtlib.api.ICustomWirelessTerminalItem;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.wpt.WPT;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.api.WPTApi;
import p455w0rd.wpt.sync.packets.PacketOpenGui;
import p455w0rd.wpt.util.WPTUtils;

/**
 * @author p455w0rd
 *
 */
public class ModAPIImpl extends WPTApi {

	private static ModAPIImpl INSTANCE = null;

	public static ModAPIImpl instance() {
		if (ModAPIImpl.INSTANCE == null) {
			if (!ModAPIImpl.hasFinishedPreInit()) {
				return null;
			}
			ModAPIImpl.INSTANCE = new ModAPIImpl();
		}
		return INSTANCE;
	}

	protected static boolean hasFinishedPreInit() {
		if (WPT.PROXY.getLoaderState() == LoaderState.NOINIT) {
			ModLogger.warn("API is not available until WPT finishes the PreInit phase.");
			return false;
		}

		return true;
	}

	@Override
	public void openWPTGui(final EntityPlayer player, final boolean isBauble, final int wptSlot) {
		if ((player == null) || (player instanceof FakePlayer) || (player instanceof EntityPlayerMP) || FMLCommonHandler.instance().getSide() == Side.SERVER) {
			return;
		}
		ItemStack is = isBauble ? WTApi.instance().getBaublesUtility().getWTBySlot(player, wptSlot, IWirelessPatternTerminalItem.class) : WPTUtils.getWPTBySlot(player, wptSlot);
		if (!is.isEmpty() && isTerminalLinked(is)) {
			ModNetworking.instance().sendToServer(new PacketOpenGui(ModGuiHandler.GUI_WPT, isBauble, wptSlot));
		}
	}

	@Override
	public boolean isTerminalLinked(final ItemStack wirelessTerminalItemstack) {
		String sourceKey = "";
		if (wirelessTerminalItemstack.getItem() instanceof ICustomWirelessTerminalItem && wirelessTerminalItemstack.hasTagCompound()) {
			sourceKey = ((ICustomWirelessTerminalItem) wirelessTerminalItemstack.getItem()).getEncryptionKey(wirelessTerminalItemstack);
			return (sourceKey != null) && (!sourceKey.isEmpty());
		}
		return false;
	}

}
