package p455w0rd.wpt.sync.packets;

import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import p455w0rd.ae2wtlib.api.container.IWTContainer;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.client.gui.GuiWPT;
import p455w0rd.wpt.init.ModGuiHandler;
import p455w0rd.wpt.sync.WPTPacket;

/**
 * @author p455w0rd
 *
 */
public class PacketSwitchGuis extends WPTPacket {

	private final int newGui;

	// automatic.
	public PacketSwitchGuis(final ByteBuf stream) {
		newGui = stream.readInt();
	}

	// api
	public PacketSwitchGuis(final int newGui) {
		this.newGui = newGui;

		if (Platform.isClient()) {
			GuiWPT.setSwitchingGuis(true);
		}

		final ByteBuf data = Unpooled.buffer();

		data.writeInt(getPacketID());
		data.writeInt(newGui);

		configureWrite(data);
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		World world = player.getEntityWorld();
		int x = (int) player.posX;
		int y = (int) player.posY;
		int z = (int) player.posZ;
		Container c = player.openContainer;
		boolean isBauble = false;
		int slot = -1;
		if (c instanceof IWTContainer) {
			IWTContainer wtContainer = (IWTContainer) c;
			isBauble = wtContainer.isWTBauble();
			slot = wtContainer.getWTSlot();
		}
		ModGuiHandler.open(newGui, player, world, new BlockPos(x, y, z), isBauble, slot);
	}

	@Override
	public void clientPacketData(final INetworkInfo network, final WPTPacket packet, final EntityPlayer player) {
		GuiWPT.setSwitchingGuis(true);
	}

}
