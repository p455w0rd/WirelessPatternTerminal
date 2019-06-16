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
package p455w0rd.wpt.sync.packets;

import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.sync.WPTPacket;

/**
 * @author yueh
 *
 */
public class PacketTargetItemStack extends WPTPacket {

	private AEItemStack stack;

	// automatic.
	public PacketTargetItemStack(final ByteBuf stream) {
		try {
			if (stream.readableBytes() > 0) {
				stack = AEItemStack.fromPacket(stream);
			}
			else {
				stack = null;
			}
		}
		catch (Exception ex) {
			stack = null;
		}
	}

	// api
	public PacketTargetItemStack(AEItemStack stack) {

		this.stack = stack;

		final ByteBuf data = Unpooled.buffer();
		data.writeInt(getPacketID());
		if (stack != null) {
			try {
				stack.writeToPacket(data);
			}
			catch (Exception ex) {
			}
		}
		configureWrite(data);
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		if (player.openContainer instanceof ContainerWPT) {
			((ContainerWPT) player.openContainer).setTargetStack(stack);
		}
	}

}