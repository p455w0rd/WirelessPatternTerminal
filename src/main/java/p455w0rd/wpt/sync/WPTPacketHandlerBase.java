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
package p455w0rd.wpt.sync;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import p455w0rd.wpt.sync.packets.*;

public class WPTPacketHandlerBase {
	private static final Map<Class<? extends WPTPacket>, PacketTypes> REVERSE_LOOKUP = new HashMap<Class<? extends WPTPacket>, WPTPacketHandlerBase.PacketTypes>();

	public enum PacketTypes {
			PACKET_JEI_PATTERN_RECIPE(PacketJEIPatternRecipe.class),

			PACKET_CREAFT_REQUEST(PacketCraftRequest.class),

			PACKET_TARGET_ITEMSTACK(PacketTargetItemStack.class),

			PACKET_SWITCH_GUIS(PacketSwitchGuis.class),

			PACKET_INVENTORY_UPDATE(PacketMEInventoryUpdate.class),

			PACKET_PATTERN_SLOT(PacketPatternSlot.class),

			PACKET_INVENTORY_ACTION(PacketInventoryAction.class),

			PACKET_VALUE_CONFIG(PacketValueConfig.class),

			PACKET_OPENWIRELESSTERM(PacketOpenGui.class);

		private final Class<? extends WPTPacket> packetClass;
		private final Constructor<? extends WPTPacket> packetConstructor;

		PacketTypes(final Class<? extends WPTPacket> c) {
			packetClass = c;

			Constructor<? extends WPTPacket> x = null;
			try {
				x = packetClass.getConstructor(ByteBuf.class);
			}
			catch (final NoSuchMethodException ignored) {
			}
			catch (final SecurityException ignored) {
			}
			catch (final DecoderException ignored) {
			}

			packetConstructor = x;
			REVERSE_LOOKUP.put(packetClass, this);

			if (packetConstructor == null) {
				throw new IllegalStateException("Invalid Packet Class " + c + ", must be constructable on DataInputStream");
			}
		}

		public static PacketTypes getPacket(final int id) {
			return (values())[id];
		}

		static PacketTypes getID(final Class<? extends WPTPacket> c) {
			return REVERSE_LOOKUP.get(c);
		}

		public WPTPacket parsePacket(final ByteBuf in) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			return packetConstructor.newInstance(in);
		}
	}
}