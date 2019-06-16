package p455w0rd.wpt.sync.packets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.helpers.IContainerCraftingPacket;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.WrapperInvItemHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.ae2wtlib.api.networking.INetworkInfo;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.integration.JEI.RecipeTransferHandler;
import p455w0rd.wpt.sync.WPTPacket;

/**
 * @author p455w0rd
 *
 */
public class PacketJEIPatternRecipe extends WPTPacket {

	NBTTagCompound input;
	NBTTagCompound output;

	public PacketJEIPatternRecipe(final ByteBuf stream) {
		input = ByteBufUtils.readTag(stream);
		if (stream.readBoolean()) {
			output = ByteBufUtils.readTag(stream);
		}
	}

	public PacketJEIPatternRecipe(@Nonnull NBTTagCompound input, @Nullable NBTTagCompound output) {
		this.input = input;
		this.output = output;
		final ByteBuf data = Unpooled.buffer();
		data.writeInt(getPacketID());
		ByteBufUtils.writeTag(data, input);
		if (output != null) {
			data.writeBoolean(true);
			ByteBufUtils.writeTag(data, output);
		}
		else {
			data.writeBoolean(false);
		}
		configureWrite(data);
	}

	@Override
	public void serverPacketData(final INetworkInfo manager, final WPTPacket packet, final EntityPlayer player) {
		NBTTagCompound currentStack;
		Container con = player.openContainer;
		ItemStack[] recipe = new ItemStack[9];
		ItemStack[] recipeOutput = null;
		for (int i = 0; i < recipe.length; ++i) {
			currentStack = (NBTTagCompound) input.getTag("#" + i);
			recipe[i] = currentStack == null ? ItemStack.EMPTY : new ItemStack(currentStack);
		}
		if (output != null) {
			recipeOutput = new ItemStack[3];
			NBTTagList outputList = output.getTagList(RecipeTransferHandler.OUTPUTS_KEY, 10);
			for (int i = 0; i < recipeOutput.length; ++i) {
				recipeOutput[i] = new ItemStack(outputList.getCompoundTagAt(i));
			}
		}
		if (con instanceof IContainerCraftingPacket && con instanceof ContainerWPT) {
			IActionHost obj;
			IContainerCraftingPacket cct = (IContainerCraftingPacket) con;
			IGridNode node = cct.getNetworkNode();
			if (node == null && (obj = cct.getActionSource().machine().get()) != null) {
				node = obj.getActionableNode();
			}
			if (node != null) {
				IGrid grid = node.getGrid();
				if (grid == null) {
					return;
				}
				IStorageGrid inv = (IStorageGrid) grid.getCache(IStorageGrid.class);
				ISecurityGrid security = (ISecurityGrid) grid.getCache(ISecurityGrid.class);
				IItemHandler craftMatrix = cct.getInventoryByName("crafting");
				if (inv != null && recipe != null && security != null) {
					for (int i = 0; i < craftMatrix.getSlots(); ++i) {
						ItemStack currentItem = ItemStack.EMPTY;
						if (recipe[i] != null) {
							currentItem = recipe[i].copy();
						}
						ItemHandlerUtil.setStackInSlot(craftMatrix, i, currentItem);
					}
					if (recipeOutput == null) {
						con.onCraftMatrixChanged(new WrapperInvItemHandler(craftMatrix));
					}
				}
				if (recipeOutput != null && !((ContainerWPT) con).isCraftingMode()) {
					IItemHandler outputInv = cct.getInventoryByName("output");
					for (int i = 0; i < recipeOutput.length; ++i) {
						ItemHandlerUtil.setStackInSlot(outputInv, i, recipeOutput[i]);
					}
				}
			}
		}
	}

}
