/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2. If not, see
 * <http://www.gnu.org/licenses/lgpl>.
 */

package p455w0rd.wpt.container.slot;

import java.io.IOException;

import appeng.api.AEApi;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.helpers.IContainerCraftingPacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.ae2wtlib.api.container.slot.IOptionalSlotHost;
import p455w0rd.wpt.sync.WPTPacket;
import p455w0rd.wpt.sync.packets.PacketPatternSlot;

public class SlotPatternTerm extends SlotCraftingOutput {

	private final int groupNum;
	private final IOptionalSlotHost host;

	public SlotPatternTerm(final EntityPlayer player, final IActionSource mySrc, final IEnergySource energySrc, final IStorageMonitorable storage, final IItemHandler cMatrix, final IItemHandler secondMatrix, final IItemHandler output, final int x, final int y, final IOptionalSlotHost h, final int groupNumber, final IContainerCraftingPacket c) {
		super(player, mySrc, energySrc, storage, cMatrix, secondMatrix, output, x, y, c);

		host = h;
		groupNum = groupNumber;
	}

	public WPTPacket getRequest(final boolean shift) throws IOException {
		return new PacketPatternSlot(getPattern(), AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(getStack()), shift);
	}

	@Override
	public ItemStack getStack() {
		if (!isSlotEnabled()) {
			if (!getDisplayStack().isEmpty()) {
				clearStack();
			}
		}

		return super.getStack();
	}

	@Override
	public boolean isSlotEnabled() {
		if (host == null) {
			return false;
		}

		return host.isSlotEnabled(groupNum);
	}
}