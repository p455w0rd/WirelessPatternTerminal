package p455w0rd.wpt.sync.packets;

import java.io.IOException;

import appeng.api.AEApi;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.sync.WPTPacket;

/**
 * @author p455w0rd
 *
 */
public class PacketPatternSlot extends WPTPacket {

	public final IAEItemStack slotItem;

	public final IAEItemStack[] pattern = new IAEItemStack[9];

	public final boolean shift;

	// automatic.
	public PacketPatternSlot(final ByteBuf stream) throws IOException {

		shift = stream.readBoolean();

		slotItem = readItem(stream);

		for (int x = 0; x < 9; x++) {
			pattern[x] = readItem(stream);
		}
	}

	private IAEItemStack readItem(final ByteBuf stream) throws IOException {
		final boolean hasItem = stream.readBoolean();

		if (hasItem) {
			return AEItemStack.fromPacket(stream);
		}

		return null;
	}

	// api
	public PacketPatternSlot(final IItemHandler pat, final IAEItemStack slotItem, final boolean shift) throws IOException {

		this.slotItem = slotItem;
		this.shift = shift;

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());

		data.writeBoolean(shift);

		writeItem(slotItem, data);
		for (int x = 0; x < 9; x++) {
			pattern[x] = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(pat.getStackInSlot(x));
			writeItem(pattern[x], data);
		}

		configureWrite(data);
	}

	private void writeItem(final IAEItemStack slotItem, final ByteBuf data) throws IOException {
		if (slotItem == null) {
			data.writeBoolean(false);
		}
		else {
			data.writeBoolean(true);
			slotItem.writeToPacket(data);
		}
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		final EntityPlayerMP sender = (EntityPlayerMP) player;
		if (sender.openContainer instanceof ContainerWPT) {
			final ContainerWPT patternTerminal = (ContainerWPT) sender.openContainer;
			patternTerminal.craftOrGetItem(this);
		}
	}

}
