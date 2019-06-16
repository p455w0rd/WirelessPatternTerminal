package p455w0rd.wpt.sync.packets;

import java.io.IOException;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.ClientHelper;
import appeng.container.ContainerOpenContext;
import appeng.helpers.InventoryAction;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import p455w0rd.ae2wtlib.api.container.ContainerWT;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.container.ContainerCraftAmount;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.init.ModGuiHandler;
import p455w0rd.wpt.sync.WPTPacket;

/**
 * @author p455w0rd
 *
 */
public class PacketInventoryAction extends WPTPacket {

	private final InventoryAction action;
	private final int slot;
	private final long id;
	private final IAEItemStack slotItem;

	// automatic.
	public PacketInventoryAction(final ByteBuf stream) throws IOException {
		action = InventoryAction.values()[stream.readInt()];
		slot = stream.readInt();
		id = stream.readLong();
		final boolean hasItem = stream.readBoolean();
		if (hasItem) {
			slotItem = AEItemStack.fromPacket(stream);
		}
		else {
			slotItem = null;
		}
	}

	// api
	public PacketInventoryAction(final InventoryAction action, final int slot, final IAEItemStack slotItem) throws IOException {

		if (Platform.isClient()) {
			throw new IllegalStateException("invalid packet, client cannot post inv actions with stacks.");
		}

		this.action = action;
		this.slot = slot;
		id = 0;
		this.slotItem = slotItem;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		data.writeInt(action.ordinal());
		data.writeInt(slot);
		data.writeLong(id);

		if (slotItem == null) {
			data.writeBoolean(false);
		}
		else {
			data.writeBoolean(true);
			slotItem.writeToPacket(data);
		}

		configureWrite(data);
	}

	// api
	public PacketInventoryAction(final InventoryAction action, final int slot, final long id) {
		this.action = action;
		this.slot = slot;
		this.id = id;
		slotItem = null;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		data.writeInt(action.ordinal());
		data.writeInt(slot);
		data.writeLong(id);
		data.writeBoolean(false);

		configureWrite(data);
	}

	@SuppressWarnings("unused")
	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		final EntityPlayerMP sender = (EntityPlayerMP) player;
		Container baseContainer = sender.openContainer;
		ContainerOpenContext context = null;
		if (sender.openContainer instanceof ContainerWPT) {
			baseContainer = sender.openContainer;
			context = ((ContainerWPT) baseContainer).getOpenContext();
		}

		if (action == InventoryAction.AUTO_CRAFT) {
			int x = (int) player.posX;
			int y = (int) player.posY;
			int z = (int) player.posZ;
			if (sender.openContainer instanceof ContainerWPT) {
				ContainerWPT wctContainer = (ContainerWPT) sender.openContainer;
				ModGuiHandler.open(ModGuiHandler.GUI_CRAFT_AMOUNT, player, player.getEntityWorld(), new BlockPos(x, y, z), wctContainer.isWTBauble(), wctContainer.getWTSlot());
			}
			/*
			else {
				ModGuiHandler.open(ModGuiHandler.GUI_CRAFT_AMOUNT, player, WTUtils.world(player), new BlockPos(x, y, z));
			}
			*/
			if (sender.openContainer instanceof ContainerCraftAmount) {
				final ContainerCraftAmount cca = (ContainerCraftAmount) sender.openContainer;

				if (baseContainer instanceof ContainerWT) {
					ContainerWT wtContainer = (ContainerWT) baseContainer;
					if (wtContainer.getTargetStack() != null) {
						cca.getCraftingItem().putStack(wtContainer.getTargetStack().asItemStackRepresentation());
						cca.setItemToCraft(wtContainer.getTargetStack());
					}
				}

				cca.detectAndSendChanges();
			}
		}
		else {
			if (baseContainer instanceof ContainerWPT) {
				((ContainerWPT) baseContainer).doAction(sender, action, slot, id);
				((ContainerWPT) baseContainer).detectAndSendChanges();
			}
		}
	}

	@Override
	public void clientPacketData(final INetworkInfo network, final WPTPacket packet, final EntityPlayer player) {
		if (action == InventoryAction.UPDATE_HAND) {
			ClientHelper ch = new ClientHelper();
			if (slotItem == null) {
				ch.getPlayers().get(0).inventory.setItemStack(ItemStack.EMPTY);
			}
			else {
				ch.getPlayers().get(0).inventory.setItemStack(slotItem.createItemStack());
			}
		}
	}

}