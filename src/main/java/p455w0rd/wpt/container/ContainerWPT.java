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
package p455w0rd.wpt.container;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.*;

import appeng.api.AEApi;
import appeng.api.config.*;
import appeng.api.definitions.IDefinitions;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.*;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.client.me.InternalSlotME;
import appeng.client.me.SlotME;
import appeng.container.ContainerNull;
import appeng.container.guisync.GuiSync;
import appeng.container.slot.*;
import appeng.container.slot.SlotRestrictedInput.PlacableItemType;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ItemViewCell;
import appeng.me.helpers.ChannelPowerSrc;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.*;
import appeng.util.inv.*;
import appeng.util.item.AEItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.*;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import p455w0rd.ae2wtlib.api.ICustomWirelessTerminalItem;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.ae2wtlib.api.container.ContainerWT;
import p455w0rd.ae2wtlib.api.container.slot.OptionalSlotFake;
import p455w0rd.ae2wtlib.api.container.slot.SlotPatternOutputs;
import p455w0rd.wpt.api.IWPTContainer;
import p455w0rd.wpt.api.IWirelessPatternTerminalItem;
import p455w0rd.wpt.container.slot.SlotCraftingOutput;
import p455w0rd.wpt.container.slot.SlotPatternTerm;
import p455w0rd.wpt.init.ModNetworking;
import p455w0rd.wpt.sync.packets.*;
import p455w0rd.wpt.util.WPTUtils;

/**
 * @author p455w0rd
 *
 */
public class ContainerWPT extends ContainerWT implements IWPTContainer {

	private final AppEngInternalInventory craftingMatrixOutput = new AppEngInternalInventory(null, 1);
	private final AppEngInternalInventory craftingMatrixInv;
	private final SlotFakeCraftingMatrix[] craftingMatrixSlots = new SlotFakeCraftingMatrix[9];
	private final OptionalSlotFake[] outputSlots = new OptionalSlotFake[3];
	private final SlotPatternTerm craftSlot;
	private final SlotRestrictedInput blankPatternSlot;
	private final SlotRestrictedInput encodedPatternSlot;
	private IRecipe currentRecipe;
	@GuiSync(97)
	public boolean craftingMode = true;
	@GuiSync(96)
	public boolean substitute = false;
	private final SlotRestrictedInput[] viewCellSlots = new SlotRestrictedInput[4];
	private final IMEMonitor<IAEItemStack> monitor;
	private final IItemList<IAEItemStack> items = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
	@GuiSync(99)
	public boolean canAccessViewCells = false;
	private IGridNode networkNode;
	private IAEItemStack clientRequestedTargetItem = null;
	private final AppEngInternalInventory viewCellInv = new AppEngInternalInventory(this, 4);
	private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 2);
	private final AppEngInternalInventory processingPatternOutputInv = new AppEngInternalInventory(this, 3);

	public ContainerWPT(final EntityPlayer player, final ITerminalHost hostIn, final int slot, final boolean isBauble) {
		super(player.inventory, getActionHost(getGuiObject(isBauble ? WTApi.instance().getBaublesUtility().getWTBySlot(player, slot, IWirelessPatternTerminalItem.class) : WTApi.instance().getWTBySlot(player, slot), player)), slot, isBauble, true, 84, -24);
		setCustomName("WPTContainer");
		setTerminalHost(hostIn);
		initConfig(setClientConfigManager(new ConfigManager(this)));
		if (Platform.isServer()) {
			setServerConfigManager(getGuiObject().getConfigManager());
			monitor = getGuiObject().getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
			if (monitor != null) {
				monitor.addListener(this, null);
				setCellInventory(monitor);
				if (getGuiObject() instanceof IEnergySource) {
					setPowerSource(getGuiObject());
				}
				else if (getGuiObject() instanceof IGridHost || getGuiObject() instanceof IActionHost) {
					if (getGuiObject() instanceof IGridHost) {
						networkNode = ((IGridHost) getGuiObject()).getGridNode(AEPartLocation.INTERNAL);
					}
					else if (getGuiObject() instanceof IActionHost) {
						networkNode = ((IActionHost) getGuiObject()).getActionableNode();
					}
					else {
						networkNode = null;
					}
					if (networkNode != null) {
						final IGrid g = networkNode.getGrid();
						if (g != null) {
							setPowerSource(new ChannelPowerSrc(networkNode, (IEnergySource) g.getCache(IEnergyGrid.class)));
						}
					}
				}
			}
			else {
				getPlayer().sendMessage(PlayerMessages.CommunicationError.get());
				setValidContainer(false);
			}
		}
		else {
			monitor = null;
		}

		craftingMatrixInv = new AppEngInternalInventory(this, 9);

		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				addSlotToContainer(craftingMatrixSlots[x + y * 3] = new SlotFakeCraftingMatrix(craftingMatrixInv, x + y * 3, 8 + x * 18, -84 + y * 18));
			}
		}

		addSlotToContainer(craftSlot = new SlotPatternTerm(player, getActionSource(), getPowerSource(), getTerminalHost(), craftingMatrixInv, patternInv, craftingMatrixOutput, 92, -66, this, 2, this));
		craftSlot.setIIcon(-1);

		for (int y = 0; y < 3; y++) {
			addSlotToContainer(outputSlots[y] = new SlotPatternOutputs(processingPatternOutputInv, this, y, 92, -84 + y * 18, 0, 0, 1));
			outputSlots[y].setRenderDisabled(false);
			outputSlots[y].setIIcon(-1);
		}

		addSlotToContainer(blankPatternSlot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.BLANK_PATTERN, patternInv, 0, 147, -84, getInventoryPlayer()));
		addSlotToContainer(encodedPatternSlot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.ENCODED_PATTERN, patternInv, 1, 147, -41, getInventoryPlayer()));

		encodedPatternSlot.setStackLimit(1);

		for (int i = 0; i < getPlayerInv().getSizeInventory(); i++) {
			final ItemStack currStack = getPlayerInv().getStackInSlot(i);
			if (!currStack.isEmpty() && currStack == getWirelessTerminal()) {
				lockPlayerInventorySlot(i);
			}
		}
		/*
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				cellView[j + i * 2] = new SlotRestrictedInput(PlacableItemType.VIEW_CELL, getViewCellStorage(), j + i * 2, i * 18 - 32, j * 18 + 40, getInventoryPlayer());
				cellView[j + i * 2].setAllowEdit(true);
				addSlotToContainer(cellView[j + i * 2]);
			}
		}
		*/
		for (int i = 0; i < 4; i++) {
			addSlotToContainer(viewCellSlots[i] = new SlotRestrictedInput(PlacableItemType.VIEW_CELL, getViewCellStorage(), i, i * 18 + 8, -24, getInventoryPlayer()));
			viewCellSlots[i].setAllowEdit(true);
		}
		bindPlayerInventory(player.inventory, 8, 0);
		updateOrderOfOutputSlots();
		readNBT();
	}

	@Override
	public IItemHandler getViewCellStorage() {
		return viewCellInv;
	}

	@Override
	public IGridNode getNetworkNode() {
		return networkNode;
	}

	private void updateOrderOfOutputSlots() {
		if (!isCraftingMode()) {
			craftSlot.xPos = -9000;

			for (int y = 0; y < 3; y++) {
				outputSlots[y].xPos = outputSlots[y].getX();
			}
		}
		else {
			craftSlot.xPos = craftSlot.getX();

			for (int y = 0; y < 3; y++) {
				outputSlots[y].xPos = -9000;
			}
		}
	}

	public boolean isCraftingMode() {
		return craftingMode;
	}

	public void setCraftingMode(final boolean craftingMode) {
		this.craftingMode = craftingMode;
		updateOrderOfOutputSlots();
	}

	private boolean isSubstitute() {
		return substitute;
	}

	public void setSubstitute(final boolean substitute) {
		this.substitute = substitute;
	}

	@Override
	public void putStackInSlot(final int slotID, final ItemStack stack) {
		super.putStackInSlot(slotID, stack);
		getAndUpdateOutput();
	}

	private ItemStack getAndUpdateOutput() {
		final World world = getPlayerInv().player.world;
		final InventoryCrafting ic = new InventoryCrafting(this, 3, 3);
		for (int x = 0; x < ic.getSizeInventory(); x++) {
			ic.setInventorySlotContents(x, craftingMatrixInv.getStackInSlot(x));
		}
		if (currentRecipe == null || !currentRecipe.matches(ic, world)) {
			currentRecipe = CraftingManager.findMatchingRecipe(ic, world);
		}
		final ItemStack is;
		if (currentRecipe == null) {
			is = ItemStack.EMPTY;
		}
		else {
			is = currentRecipe.getCraftingResult(ic);
		}
		craftingMatrixOutput.setStackInSlot(0, is);
		return is;
	}

	public void encode() {
		ItemStack output = encodedPatternSlot.getStack();

		final ItemStack[] in = getInputs();
		final ItemStack[] out = getOutputs();

		// if there is no input, this would be silly.
		if (in == null || out == null) {
			return;
		}

		// first check the output slots, should either be null, or a pattern
		if (!output.isEmpty() && !isPattern(output)) {
			return;
		} // if nothing is there we should snag a new pattern.
		else if (output.isEmpty()) {
			output = blankPatternSlot.getStack();
			if (output.isEmpty() || !isPattern(output)) {
				return; // no blanks.
			}

			// remove one, and clear the input slot.
			output.setCount(output.getCount() - 1);
			if (output.getCount() == 0) {
				blankPatternSlot.putStack(ItemStack.EMPTY);
			}

			// add a new encoded pattern.
			final Optional<ItemStack> maybePattern = AEApi.instance().definitions().items().encodedPattern().maybeStack(1);
			if (maybePattern.isPresent()) {
				output = maybePattern.get();
				encodedPatternSlot.putStack(output);
			}
		}

		// encode the slot.
		final NBTTagCompound encodedValue = new NBTTagCompound();

		final NBTTagList tagIn = new NBTTagList();
		final NBTTagList tagOut = new NBTTagList();

		for (final ItemStack i : in) {
			tagIn.appendTag(createItemTag(i));
		}

		for (final ItemStack i : out) {
			tagOut.appendTag(createItemTag(i));
		}

		encodedValue.setTag("in", tagIn);
		encodedValue.setTag("out", tagOut);
		encodedValue.setBoolean("crafting", isCraftingMode());
		encodedValue.setBoolean("substitute", isSubstitute());

		output.setTagCompound(encodedValue);
	}

	private ItemStack[] getInputs() {
		final ItemStack[] input = new ItemStack[9];
		boolean hasValue = false;

		for (int x = 0; x < craftingMatrixSlots.length; x++) {
			input[x] = craftingMatrixSlots[x].getStack();
			if (!input[x].isEmpty()) {
				hasValue = true;
			}
		}

		if (hasValue) {
			return input;
		}

		return null;
	}

	private ItemStack[] getOutputs() {
		if (isCraftingMode()) {
			final ItemStack out = getAndUpdateOutput();

			if (!out.isEmpty() && out.getCount() > 0) {
				return new ItemStack[] {
						out
				};
			}
		}
		else {
			final List<ItemStack> list = new ArrayList<>(3);
			boolean hasValue = false;

			for (final OptionalSlotFake outputSlot : outputSlots) {
				final ItemStack out = outputSlot.getStack();

				if (!out.isEmpty() && out.getCount() > 0) {
					list.add(out);
					hasValue = true;
				}
			}

			if (hasValue) {
				return list.toArray(new ItemStack[list.size()]);
			}
		}

		return null;
	}

	private boolean isPattern(final ItemStack output) {
		if (output.isEmpty()) {
			return false;
		}

		final IDefinitions definitions = AEApi.instance().definitions();

		boolean isPattern = definitions.items().encodedPattern().isSameAs(output);
		isPattern |= definitions.materials().blankPattern().isSameAs(output);

		return isPattern;
	}

	private NBTBase createItemTag(final ItemStack i) {
		final NBTTagCompound c = new NBTTagCompound();

		if (!i.isEmpty()) {
			i.writeToNBT(c);
		}

		return c;
	}

	@Override
	protected void initConfig(final IConfigManager cm) {
		cm.registerSetting(Settings.SORT_BY, SortOrder.NAME);
		cm.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
		cm.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
	}

	/*
	@SuppressWarnings("unchecked")
	public static WTGuiObject<IAEItemStack> getGuiObject(final ItemStack it, final EntityPlayer player, final World w, final int x, final int y, final int z) {
		if (!it.isEmpty()) {
			final IWirelessTermHandler wh = AEApi.instance().registries().wireless().getWirelessTerminalHandler(it);
			if (wh instanceof ICustomWirelessTerminalItem) {
				return (WTGuiObject<IAEItemStack>) WTApi.instance().getGUIObject((ICustomWirelessTerminalItem) wh, it, player);
			}
		}
		return null;
	}
	*/
	@Override
	public boolean isValid(final Object verificationToken) {
		return true;
	}

	@Override
	public void postChange(final IBaseMonitor<IAEItemStack> monitor, final Iterable<IAEItemStack> change, final IActionSource actionSource) {
		for (final IAEItemStack is : change) {
			items.add(is);
		}
	}

	@Override
	public boolean isSlotEnabled(final int idx) {
		if (idx == 1) {
			return !isCraftingMode();
		}
		else if (idx == 2) {
			return isCraftingMode();
		}
		else {
			return false;
		}
	}

	public void craftOrGetItem(final PacketPatternSlot packetPatternSlot) {
		if (packetPatternSlot.slotItem != null && getCellInventory() != null) {
			final IAEItemStack out = packetPatternSlot.slotItem.copy();
			InventoryAdaptor inv = new AdaptorItemHandler(new WrapperCursorItemHandler(getPlayerInv()));
			final InventoryAdaptor playerInv = InventoryAdaptor.getAdaptor(getPlayerInv().player);

			if (packetPatternSlot.shift) {
				inv = playerInv;
			}

			if (!inv.simulateAdd(out.createItemStack()).isEmpty()) {
				return;
			}

			final IAEItemStack extracted = Platform.poweredExtraction(getPowerSource(), getCellInventory(), out, getActionSource());
			final EntityPlayer p = getPlayerInv().player;

			if (extracted != null) {
				inv.addItems(extracted.createItemStack());
				if (p instanceof EntityPlayerMP) {
					updateHeld((EntityPlayerMP) p);
				}
				detectAndSendChanges();
				return;
			}

			final InventoryCrafting ic = new InventoryCrafting(new ContainerNull(), 3, 3);
			final InventoryCrafting real = new InventoryCrafting(new ContainerNull(), 3, 3);

			for (int x = 0; x < 9; x++) {
				ic.setInventorySlotContents(x, packetPatternSlot.pattern[x] == null ? ItemStack.EMPTY : packetPatternSlot.pattern[x].createItemStack());
			}

			final IRecipe r = CraftingManager.findMatchingRecipe(ic, p.world);

			if (r == null) {
				return;
			}

			final IMEMonitor<IAEItemStack> storage = getTerminalHost().getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class));
			final IItemList<IAEItemStack> all = storage.getStorageList();

			final ItemStack is = r.getCraftingResult(ic);

			for (int x = 0; x < ic.getSizeInventory(); x++) {
				if (!ic.getStackInSlot(x).isEmpty()) {
					final ItemStack pulled = Platform.extractItemsByRecipe(getPowerSource(), getActionSource(), storage, p.world, r, is, ic, ic.getStackInSlot(x), x, all, Actionable.MODULATE, ItemViewCell.createFilter(getViewCells()));
					real.setInventorySlotContents(x, pulled);
				}
			}

			final IRecipe rr = CraftingManager.findMatchingRecipe(real, p.world);

			if (rr == r && Platform.itemComparisons().isSameItem(rr.getCraftingResult(real), is)) {
				final InventoryCraftResult craftingResult = new InventoryCraftResult();
				craftingResult.setRecipeUsed(rr);

				final SlotCrafting sc = new SlotCrafting(p, real, craftingResult, 0, 0, 0);
				sc.onTake(p, is);

				for (int x = 0; x < real.getSizeInventory(); x++) {
					final ItemStack failed = playerInv.addItems(real.getStackInSlot(x));

					if (!failed.isEmpty()) {
						p.dropItem(failed, false);
					}
				}

				inv.addItems(is);
				if (p instanceof EntityPlayerMP) {
					updateHeld((EntityPlayerMP) p);
				}
				detectAndSendChanges();
			}
			else {
				for (int x = 0; x < real.getSizeInventory(); x++) {
					final ItemStack failed = real.getStackInSlot(x);
					if (!failed.isEmpty()) {
						getCellInventory().injectItems(AEItemStack.fromItemStack(failed), Actionable.MODULATE, getActionSource());
					}
				}
			}
		}
	}

	@Override
	public void onListUpdate() {
		for (final IContainerListener c : listeners) {
			queueInventory(c);
		}
	}

	@Override
	public void addListener(final IContainerListener listener) {
		super.addListener(listener);
		queueInventory(listener);
	}

	@Override
	public void readNBT() {
		if (getWirelessTerminal().hasTagCompound()) {
			final NBTTagCompound nbt = getWirelessTerminal().getTagCompound();
			craftingMatrixInv.readFromNBT(nbt, "PatternCraftingMatrix");
			viewCellInv.readFromNBT(nbt, "PatternViewCells");
			patternInv.readFromNBT(nbt, "PatternInv");
			processingPatternOutputInv.readFromNBT(nbt, "OutputList");
			setCraftingRecipe(nbt.getBoolean("CraftingMode"));
			setSubstitution(nbt.getBoolean("Substitute"));
			super.readNBT();
		}
	}

	@Override
	public void writeToNBT() {
		if (!getWirelessTerminal().hasTagCompound()) {
			getWirelessTerminal().setTagCompound(new NBTTagCompound());
		}
		final NBTTagCompound newNBT = getWirelessTerminal().getTagCompound();
		//newNBT.setTag("CraftingMatrix", craftingGrid.serializeNBT());
		craftingMatrixInv.writeToNBT(newNBT, "PatternCraftingMatrix");
		patternInv.writeToNBT(newNBT, "PatternInv");
		viewCellInv.writeToNBT(newNBT, "PatternViewCells");
		processingPatternOutputInv.writeToNBT(newNBT, "OutputList");
		newNBT.setBoolean("CraftingMode", craftingMode);
		newNBT.setBoolean("Substitute", substitute);
		super.writeToNBT();
	}

	@Override
	public void onContainerClosed(final EntityPlayer player) {
		writeToNBT();
		super.onContainerClosed(player);
		if (monitor != null) {
			monitor.removeListener(this);
		}
	}

	@Override
	public void onUpdate(final String field, final Object oldValue, final Object newValue) {
		if (field.equals("canAccessViewCells")) {
			for (int y = 0; y < 4; y++) {
				if (viewCellSlots[y] != null) {
					viewCellSlots[y].setAllowEdit(canAccessViewCells);
				}
			}
		}
		super.onUpdate(field, oldValue, newValue);
		if (field.equals("craftingMode")) {
			getAndUpdateOutput();
			updateOrderOfOutputSlots();
		}
	}

	@Override
	public void doAction(final EntityPlayerMP player, final InventoryAction action, final int slot, final long id) {
		if (slot >= 0 && slot < inventorySlots.size()) {
			final Slot s = getSlot(slot);

			if (s instanceof SlotCraftingOutput) {
				switch (action) {
				case CRAFT_SHIFT:
				case CRAFT_ITEM:
				case CRAFT_STACK:
					((SlotCraftingOutput) s).doClick(action, player);
					updateHeld(player);
				default:
				}
			}

			if (s instanceof SlotFake) {
				final ItemStack hand = player.inventory.getItemStack();
				switch (action) {
				case PICKUP_OR_SET_DOWN:
					if (hand == null) {
						s.putStack(ItemStack.EMPTY);
					}
					else {
						s.putStack(hand.copy());
					}
					break;
				case PLACE_SINGLE:
					if (!hand.isEmpty()) {
						final ItemStack is = hand.copy();
						is.setCount(1);
						s.putStack(is);
					}
					break;
				case SPLIT_OR_PLACE_SINGLE:
					ItemStack is = s.getStack();
					if (!is.isEmpty()) {
						if (hand.isEmpty()) {
							is.setCount(Math.max(1, is.getCount() - 1));
						}
						else if (hand.isItemEqual(is)) {
							is.setCount(Math.min(is.getMaxStackSize(), is.getCount() + 1));
						}
						else {
							is = hand.copy();
							is.setCount(1);
						}
						s.putStack(is);
					}
					else if (!hand.isEmpty()) {
						is = hand.copy();
						is.setCount(1);
						s.putStack(is);
					}
					break;
				case CREATIVE_DUPLICATE:
				case MOVE_REGION:
				case SHIFT_CLICK:
				default:
					break;
				}
			}
			if (action == InventoryAction.MOVE_REGION) {
				final List<Slot> from = new LinkedList<>();

				for (final Object j : inventorySlots) {
					if (j instanceof Slot && j.getClass() == s.getClass()) {
						final Slot sl = (Slot) j;
						if (!sl.getHasStack() || sl.getHasStack() && !WPTUtils.isAnyWPT(sl.getStack())) {
							from.add(sl);
						}
					}
				}

				for (final Slot fr : from) {
					transferStackInSlot(player, fr.slotNumber);
				}
			}

			return;
		}

		// get target item.
		final IAEItemStack slotItem = getTargetStack();

		switch (action) {
		case SHIFT_CLICK:
			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			if (slotItem != null) {
				IAEItemStack ais = slotItem.copy();
				ItemStack myItem = ais.createItemStack();

				ais.setStackSize(myItem.getMaxStackSize());

				final InventoryAdaptor adp = InventoryAdaptor.getAdaptor(player);
				myItem.setCount((int) ais.getStackSize());
				myItem = adp.simulateAdd(myItem);

				if (!myItem.isEmpty()) {
					ais.setStackSize(ais.getStackSize() - myItem.getCount());
				}

				ais = Platform.poweredExtraction(getPowerSource(), getCellInventory(), ais, getActionSource());
				if (ais != null) {
					adp.addItems(ais.createItemStack());
				}
			}
			break;
		case ROLL_DOWN:
			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			final int releaseQty = 1;
			final ItemStack isg = player.inventory.getItemStack();

			if (!isg.isEmpty() && releaseQty > 0) {
				IAEItemStack ais = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(isg);
				ais.setStackSize(1);
				final IAEItemStack extracted = ais.copy();

				ais = Platform.poweredInsert(getPowerSource(), getCellInventory(), ais, getActionSource());
				if (ais == null) {
					final InventoryAdaptor ia = new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));

					final ItemStack fail = ia.removeItems(1, extracted.createItemStack(), null);
					if (fail.isEmpty()) {
						getCellInventory().extractItems(extracted, Actionable.MODULATE, getActionSource());
					}

					updateHeld(player);
				}
			}

			break;
		case ROLL_UP:
		case PICKUP_SINGLE:
			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			if (slotItem != null) {
				int liftQty = 1;
				final ItemStack item = player.inventory.getItemStack();

				if (!item.isEmpty()) {
					if (item.getCount() >= item.getMaxStackSize()) {
						liftQty = 0;
					}
					if (!Platform.itemComparisons().isSameItem(slotItem.createItemStack(), item)) {
						liftQty = 0;
					}
				}

				if (liftQty > 0) {
					IAEItemStack ais = slotItem.copy();
					ais.setStackSize(1);
					ais = Platform.poweredExtraction(getPowerSource(), getCellInventory(), ais, getActionSource());
					if (ais != null) {
						final InventoryAdaptor ia = new AdaptorItemHandler(new WrapperCursorItemHandler(player.inventory));

						final ItemStack fail = ia.addItems(ais.createItemStack());
						if (!fail.isEmpty()) {
							getCellInventory().injectItems(ais, Actionable.MODULATE, getActionSource());
						}

						updateHeld(player);
					}
				}
			}
			break;
		case PICKUP_OR_SET_DOWN:
			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			if (player.inventory.getItemStack().isEmpty()) {
				if (slotItem != null) {
					IAEItemStack ais = slotItem.copy();
					ais.setStackSize(ais.createItemStack().getMaxStackSize());
					ais = Platform.poweredExtraction(getPowerSource(), getCellInventory(), ais, getActionSource());
					if (ais != null) {
						player.inventory.setItemStack(ais.createItemStack());
					}
					else {
						player.inventory.setItemStack(ItemStack.EMPTY);
					}
					updateHeld(player);
				}
				return;
			}
			else {
				IAEItemStack ais = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(player.inventory.getItemStack());
				ais = Platform.poweredInsert(getPowerSource(), getCellInventory(), ais, getActionSource());
				if (ais != null) {
					player.inventory.setItemStack(ais.createItemStack());
				}
				else {
					player.inventory.setItemStack(ItemStack.EMPTY);
				}
				updateHeld(player);
			}

			break;
		case SPLIT_OR_PLACE_SINGLE:
			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			if (player.inventory.getItemStack().isEmpty()) {
				if (slotItem != null) {
					IAEItemStack ais = slotItem.copy();
					final long maxSize = ais.getDefinition().getMaxStackSize();
					ais.setStackSize(maxSize);
					ais = getCellInventory().extractItems(ais, Actionable.SIMULATE, getActionSource());

					if (ais != null) {
						final long stackSize = Math.min(maxSize, ais.getStackSize());
						ais.setStackSize(stackSize + 1 >> 1);
						ais = Platform.poweredExtraction(getPowerSource(), getCellInventory(), ais, getActionSource());
					}

					if (ais != null) {
						player.inventory.setItemStack(ais.createItemStack());
					}
					else {
						player.inventory.setItemStack(ItemStack.EMPTY);
					}
					updateHeld(player);
				}
			}
			else {
				IAEItemStack ais = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(player.inventory.getItemStack());
				ais.setStackSize(1);
				ais = Platform.poweredInsert(getPowerSource(), getCellInventory(), ais, getActionSource());
				if (ais == null) {
					final ItemStack is = player.inventory.getItemStack();
					is.setCount(is.getCount() - 1);
					if (is.getCount() <= 0) {
						player.inventory.setItemStack(ItemStack.EMPTY);
					}
					updateHeld(player);
				}
			}

			break;
		case CREATIVE_DUPLICATE:
			if (player.capabilities.isCreativeMode && slotItem != null) {
				final ItemStack is = slotItem.createItemStack();
				is.setCount(is.getMaxStackSize());
				player.inventory.setItemStack(is);
				updateHeld(player);
			}
			break;
		case MOVE_REGION:

			if (getPowerSource() == null || getCellInventory() == null) {
				return;
			}

			if (slotItem != null) {
				final int playerInv = 9 * 4;
				for (int slotNum = 0; slotNum < playerInv; slotNum++) {
					IAEItemStack ais = slotItem.copy();
					ItemStack myItem = ais.createItemStack();

					ais.setStackSize(myItem.getMaxStackSize());

					final InventoryAdaptor adp = InventoryAdaptor.getAdaptor(player);
					myItem.setCount((int) ais.getStackSize());
					myItem = adp.simulateAdd(myItem);

					if (!myItem.isEmpty()) {
						ais.setStackSize(ais.getStackSize() - myItem.getCount());
					}

					ais = Platform.poweredExtraction(getPowerSource(), getCellInventory(), ais, getActionSource());
					if (ais != null) {
						adp.addItems(ais.createItemStack());
					}
					else {
						return;
					}
				}
			}

			break;
		default:
			break;

		}

	}

	@Override
	public IItemHandler getInventoryByName(final String name) {
		switch (name) {
		case "crafting":
			return craftingMatrixInv;
		case "output":
			return processingPatternOutputInv;
		}
		return null;
	}

	@Override
	public IAEItemStack getTargetStack() {
		return clientRequestedTargetItem;
	}

	@Override
	public void setTargetStack(final IAEItemStack stack) {
		// client doesn't need to re-send, makes for lower overhead rapid packets.
		if (Platform.isClient()) {
			if (stack == null && clientRequestedTargetItem == null) {
				return;
			}
			if (stack != null && stack.isSameType(clientRequestedTargetItem)) {
				return;
			}
			ModNetworking.instance().sendToServer(new PacketTargetItemStack((AEItemStack) stack));
		}
		clientRequestedTargetItem = stack == null ? null : stack.copy();
	}

	@Override
	public ItemStack slotClick(final int slot, final int dragType, final ClickType clickTypeIn, final EntityPlayer player) {
		ItemStack returnStack = ItemStack.EMPTY;
		try {
			returnStack = super.slotClick(slot, dragType, clickTypeIn, player);
		}
		catch (final IndexOutOfBoundsException e) {
		}
		writeToNBT();
		detectAndSendChanges();
		return returnStack;
	}

	private void queueInventory(final IContainerListener c) {
		if (Platform.isServer() && c instanceof EntityPlayer && monitor != null) {
			try {
				PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();
				final IItemList<IAEItemStack> monitorCache = monitor.getStorageList();

				for (final IAEItemStack send : monitorCache) {
					try {
						piu.appendItem(send);
					}
					catch (final BufferOverflowException boe) {
						ModNetworking.instance().sendTo(piu, (EntityPlayerMP) c);

						piu = new PacketMEInventoryUpdate();
						piu.appendItem(send);
					}
				}

				ModNetworking.instance().sendTo(piu, (EntityPlayerMP) c);
			}
			catch (final IOException e) {
			}
		}
	}

	@Override
	public void detectAndSendChanges() {
		if (Platform.isServer()) {
			if (getGuiObject() != null) {
				if (getWirelessTerminal() != getGuiObject().getItemStack()) {
					if (!getWirelessTerminal().isEmpty()) {
						if (ItemStack.areItemsEqual(getGuiObject().getItemStack(), getWirelessTerminal())) {
							getPlayerInv().setInventorySlotContents(getPlayerInv().currentItem, getGuiObject().getItemStack());
						}
						else {
							setValidContainer(false);
						}
					}
					else {
						setValidContainer(false);
					}
				}
			}
			else {
				setValidContainer(false);
			}

			if (monitor != getTerminalHost().getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class))) {
				setValidContainer(false);
			}

			for (final Settings set : getServerConfigManager().getSettings()) {
				final Enum<?> sideLocal = getServerConfigManager().getSetting(set);
				final Enum<?> sideRemote = getClientConfigManager().getSetting(set);

				if (sideLocal != sideRemote) {
					getClientConfigManager().putSetting(set, sideLocal);
					for (final IContainerListener crafter : listeners) {
						if (crafter instanceof EntityPlayerMP) {
							try {
								ModNetworking.instance().sendTo(new PacketValueConfig(set.name(), sideLocal.name()), (EntityPlayerMP) crafter);
							}
							catch (final IOException e) {
							}
						}
					}
				}
			}

			if (!items.isEmpty()) {
				try {
					final IItemList<IAEItemStack> monitorCache = monitor.getStorageList();

					final PacketMEInventoryUpdate piu = new PacketMEInventoryUpdate();

					for (final IAEItemStack is : items) {
						final IAEItemStack send = monitorCache.findPrecise(is);
						if (send == null) {
							is.setStackSize(0);
							piu.appendItem(is);
						}
						else {
							piu.appendItem(send);
						}
					}

					if (!piu.isEmpty()) {
						items.resetStatus();

						for (final Object c : listeners) {
							if (c instanceof EntityPlayer) {
								ModNetworking.instance().sendTo(piu, (EntityPlayerMP) c);
							}
						}
					}
				}
				catch (final IOException e) {
				}
			}

			//updatePowerStatus();

			final boolean oldAccessible = canAccessViewCells;
			canAccessViewCells = hasAccess(SecurityPermissions.BUILD, false);
			if (canAccessViewCells != oldAccessible) {
				for (int y = 0; y < 4; y++) {
					if (viewCellSlots[y] != null) {
						viewCellSlots[y].setAllowEdit(canAccessViewCells);
					}
				}
			}

			super.detectAndSendChanges();

			if (!isInRange()) {
				if (!hasInfiniteRange()) {
					if (isValidContainer()) {
						getPlayer().sendMessage(PlayerMessages.OutOfRange.get());
					}
					setValidContainer(false);
				}
				if (!networkIsPowered()) {
					if (isValidContainer()) {
						getPlayer().sendMessage(PlayerMessages.MachineNotPowered.get());
					}
					setValidContainer(false);
				}
			}
			else if (!hasAccess(SecurityPermissions.CRAFT, true) || !hasAccess(SecurityPermissions.EXTRACT, true) || !hasAccess(SecurityPermissions.INJECT, true)) {
				if (isValidContainer()) {
					getPlayer().sendMessage(PlayerMessages.CommunicationError.get());
				}
				setValidContainer(false);
			}
			if (getWirelessTerminal().getItem() instanceof IWirelessPatternTerminalItem && ((IWirelessPatternTerminalItem) getWirelessTerminal().getItem()).getAECurrentPower(getWirelessTerminal()) <= 0) {
				if (isValidContainer()) {
					getPlayer().sendMessage(new TextComponentString("No Power"));
				}
				setValidContainer(false);
			}
		}
	}

	@Override
	public void onSlotChange(final Slot s) {
		if (s == encodedPatternSlot && Platform.isServer()) {
			for (final IContainerListener listener : listeners) {
				for (final Slot slot : inventorySlots) {
					if (slot instanceof OptionalSlotFake || slot instanceof SlotFakeCraftingMatrix) {
						listener.sendSlotContents(this, slot.slotNumber, slot.getStack());
					}
				}
				if (listener instanceof EntityPlayerMP) {
					((EntityPlayerMP) listener).isChangingQuantityOnly = false;
				}
			}
			detectAndSendChanges();
		}
		if (s == craftSlot && Platform.isClient()) {
			getAndUpdateOutput();
		}
	}

	public void clear() {
		for (final Slot s : craftingMatrixSlots) {
			s.putStack(ItemStack.EMPTY);
		}

		for (final Slot s : outputSlots) {
			s.putStack(ItemStack.EMPTY);
		}
		detectAndSendChanges();
		getAndUpdateOutput();
	}

	@Override
	public boolean useRealItems() {
		return false;
	}

	public void toggleSubstitute() {
		substitute = !substitute;
		detectAndSendChanges();
		getAndUpdateOutput();
	}

	@Override
	public ItemStack[] getViewCells() {
		final ItemStack[] list = new ItemStack[viewCellSlots.length];

		for (int x = 0; x < viewCellSlots.length; x++) {
			list[x] = viewCellSlots[x].getStack();
		}

		return list;
	}

	public SlotRestrictedInput getCellViewSlot(final int index) {
		return viewCellSlots[index];
	}

	@Override
	protected void updateHeld(final EntityPlayerMP p) {
		if (Platform.isServer()) {
			try {
				ModNetworking.instance().sendTo(new PacketInventoryAction(InventoryAction.UPDATE_HAND, 0, AEItemStack.fromItemStack(p.inventory.getItemStack())), p);
			}
			catch (final IOException e) {
			}
		}
	}

	public boolean isPowered() {
		final double pwr = ((ICustomWirelessTerminalItem) getWirelessTerminal().getItem()).getAECurrentPower(getWirelessTerminal());
		return pwr > 0.0;
	}

	@Override
	public void saveChanges() {
	}

	@Override
	public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removedStack, final ItemStack newStack) {
		if (inv == patternInv && slot == 1) {
			final ItemStack is = patternInv.getStackInSlot(1);
			if (!is.isEmpty() && is.getItem() instanceof ICraftingPatternItem) {
				final ICraftingPatternItem pattern = (ICraftingPatternItem) is.getItem();
				final ICraftingPatternDetails details = pattern.getPatternForItem(is, getPlayer().getEntityWorld());
				if (details != null) {
					setCraftingRecipe(details.isCraftable());
					setSubstitution(details.canSubstitute());

					for (int x = 0; x < craftingMatrixInv.getSlots() && x < details.getInputs().length; x++) {
						final IAEItemStack item = details.getInputs()[x];
						craftingMatrixInv.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
					}

					for (int x = 0; x < processingPatternOutputInv.getSlots() && x < details.getOutputs().length; x++) {
						final IAEItemStack item = details.getOutputs()[x];
						processingPatternOutputInv.setStackInSlot(x, item == null ? ItemStack.EMPTY : item.createItemStack());
					}
				}
			}
		}
		else if (inv == craftingMatrixInv) {
			fixCraftingRecipes();
		}
	}

	public void setCraftingRecipe(final boolean craftingMode) {
		this.craftingMode = craftingMode;
		fixCraftingRecipes();
	}

	private void fixCraftingRecipes() {
		if (craftingMode) {
			for (int x = 0; x < craftingMatrixInv.getSlots(); x++) {
				final ItemStack is = craftingMatrixInv.getStackInSlot(x);
				if (!is.isEmpty()) {
					is.setCount(1);
				}
			}
		}
	}

	public boolean isSubstitution() {
		return substitute;
	}

	public void setSubstitution(final boolean canSubstitute) {
		substitute = canSubstitute;
	}

	@Override
	public ItemStack transferStackInSlot(final EntityPlayer p, final int idx) {
		if (super.transferStackInSlot(p, idx) == ItemStack.EMPTY) {
			return ItemStack.EMPTY;
		}
		AppEngSlot appEngSlot = null;
		ItemStack tis = ItemStack.EMPTY;
		boolean isAppengSlot = false;
		if (inventorySlots.get(idx) instanceof AppEngSlot) {
			isAppengSlot = true;
			appEngSlot = (AppEngSlot) inventorySlots.get(idx);
			tis = appEngSlot.getStack();
		}
		if (tis.isEmpty()) {
			return ItemStack.EMPTY;
		}
		if (isAppengSlot && appEngSlot != null) {
			if (Platform.isClient()) {
				return ItemStack.EMPTY;
			}
			boolean hasMETiles = false;
			for (final Object is : inventorySlots) {
				if (is instanceof InternalSlotME) {
					hasMETiles = true;
					break;
				}
			}
			if (hasMETiles) {
				return ItemStack.EMPTY;
			}
			if (appEngSlot instanceof SlotDisabled || appEngSlot instanceof SlotInaccessible) {
				return ItemStack.EMPTY;
			}
			if (appEngSlot.getHasStack()) {
				if (isInInventory(appEngSlot) || isInHotbar(appEngSlot)) {
					/*if (tis.getItem() instanceof ItemArmor) {
						final int type = ((ItemArmor) tis.getItem()).armorType.getIndex();
						if (mergeItemStack(tis, 40 - type, 40 - type + 1, false)) {
							appEngSlot.clearStack();
							return ItemStack.EMPTY;
						}
					}
					else if (tis.getItem() == ModItems.MAGNET_CARD) {
						if (super.mergeItemStack(tis.copy(), getMagnetIndex(), getMagnetIndex() + 1, false)) {
							if (tis.getCount() > 1) {
								tis.shrink(1);
							}
							else {
								appEngSlot.clearStack();
							}
							return ItemStack.EMPTY;
						}
					}
					else if (Mods.BAUBLES.isLoaded() && WTApi.instance().getBaublesUtility().isBaubleItem(tis) && WTApi.instance().getConfig().shiftClickBaublesEnabled()) {
						final ItemStack tisCopy = tis.copy();
						tisCopy.setCount(1);
						if (mergeItemStack(tisCopy, getBaublesIndex(), getBaublesIndex() + 7, false)) {
							if (tis.getCount() > 1) {
								tis.shrink(1);
							}
							else {
								appEngSlot.clearStack();
							}
							return ItemStack.EMPTY;
						}
					}
					else if (tis.getItem() instanceof ItemShield) {
						if (mergeItemStack(tis.copy(), 53, 54, false)) {
							if (tis.getCount() > 1) {
								tis.shrink(1);
							}
							else {
								appEngSlot.clearStack();
							}
							return ItemStack.EMPTY;
						}
					}
					else */if (tis.getItem() == AEApi.instance().definitions().items().viewCell().maybeItem().get()) {
						if (mergeItemStack(tis.copy(), 54, 58, false)) {
							if (tis.getCount() > 1) {
								tis.shrink(1);
							}
							else {
								appEngSlot.clearStack();
							}
							return ItemStack.EMPTY;
						}
					}
				}

				final List<Slot> selectedSlots = new ArrayList<>();

				if (appEngSlot.isPlayerSide()) {
					tis = shiftStoreItem(tis);
					for (final Object inventorySlot : inventorySlots) {
						if (inventorySlot instanceof AppEngSlot) {
							final AppEngSlot cs = (AppEngSlot) inventorySlot;
							if (!cs.isPlayerSide() && !(cs instanceof SlotFake) && !(cs instanceof AppEngCraftingSlot)) {
								if (cs.isItemValid(tis)) {
									selectedSlots.add(cs);
								}
							}
						}
					}
				}
				else {
					for (final Object inventorySlot : inventorySlots) {
						if (inventorySlot instanceof AppEngSlot) {
							final AppEngSlot cs = (AppEngSlot) inventorySlot;

							if (cs.isPlayerSide() && !(cs instanceof SlotFake) && !(cs instanceof AppEngCraftingSlot)) {
								if (cs.isItemValid(tis)) {
									selectedSlots.add(cs);
								}
							}
						}
					}
				}

				if (selectedSlots.isEmpty() && appEngSlot.isPlayerSide()) {
					if (!tis.isEmpty()) {
						for (final Object inventorySlot : inventorySlots) {
							if (inventorySlot instanceof AppEngSlot) {
								final AppEngSlot cs = (AppEngSlot) inventorySlot;
								final ItemStack destination = cs.getStack();

								if (!cs.isPlayerSide() && cs instanceof SlotFake) {
									if (Platform.itemComparisons().isSameItem(destination, tis)) {
										return ItemStack.EMPTY;
									}
									else if (destination.isEmpty()) {
										cs.putStack(tis.copy());
										cs.onSlotChanged();
										return ItemStack.EMPTY;
									}
								}
							}
						}
					}
				}

				if (!tis.isEmpty()) {
					for (final Slot d : selectedSlots) {
						if (d instanceof SlotDisabled || d instanceof SlotME) {
							continue;
						}

						if (d.isItemValid(tis)) {
							if (d.getHasStack()) {
								final ItemStack t = d.getStack();

								if (Platform.itemComparisons().isSameItem(tis, t)) {
									int maxSize = t.getMaxStackSize();
									if (maxSize > d.getSlotStackLimit()) {
										maxSize = d.getSlotStackLimit();
									}

									int placeAble = maxSize - t.getCount();

									if (tis.getCount() < placeAble) {
										placeAble = tis.getCount();
									}

									t.grow(placeAble);
									tis.shrink(placeAble);

									if (tis.getCount() <= 0) {
										appEngSlot.putStack(ItemStack.EMPTY);
										d.onSlotChanged();
										return ItemStack.EMPTY;
									}
								}
							}
						}
					}

					for (final Slot d : selectedSlots) {
						if (d instanceof SlotDisabled || d instanceof SlotME) {
							continue;
						}

						if (d.isItemValid(tis)) {
							if (d.getHasStack()) {
								final ItemStack t = d.getStack();

								if (Platform.itemComparisons().isSameItem(t, tis)) {
									int maxSize = t.getMaxStackSize();
									if (d.getSlotStackLimit() < maxSize) {
										maxSize = d.getSlotStackLimit();
									}

									int placeAble = maxSize - t.getCount();

									if (tis.getCount() < placeAble) {
										placeAble = tis.getCount();
									}

									t.grow(placeAble);
									tis.shrink(placeAble);

									if (tis.getCount() <= 0) {
										appEngSlot.putStack(ItemStack.EMPTY);
										d.onSlotChanged();
										return ItemStack.EMPTY;
									}
								}
							}
							else {
								int maxSize = tis.getMaxStackSize();
								if (maxSize > d.getSlotStackLimit()) {
									maxSize = d.getSlotStackLimit();
								}

								final ItemStack tmp = tis.copy();
								if (tmp.getCount() > maxSize) {
									tmp.setCount(maxSize);
								}

								tis.shrink(tmp.getCount());
								d.putStack(tmp);

								if (tis.getCount() <= 0) {
									appEngSlot.putStack(ItemStack.EMPTY);
									d.onSlotChanged();
									return ItemStack.EMPTY;
								}
							}
						}
					}
				}
				appEngSlot.putStack(!tis.isEmpty() ? tis.copy() : ItemStack.EMPTY);
				appEngSlot.onSlotChanged();
			}
		}
		return ItemStack.EMPTY;
	}

	@SuppressWarnings("unchecked")
	private ItemStack shiftStoreItem(final ItemStack input) {
		if (getPowerSource() == null || getGuiObject() == null) {
			return input;
		}
		final IEnergySource pwr = getPowerSource();
		final IMEMonitor<IAEItemStack> guiobj = (IMEMonitor<IAEItemStack>) getGuiObject();
		final IActionSource act = getActionSource();
		final IItemStorageChannel chan = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
		final IAEItemStack aestack = chan.createStack(input);
		final IAEItemStack ais = Platform.poweredInsert(pwr, guiobj, aestack, act);
		if (ais == null) {
			return ItemStack.EMPTY;
		}
		return ais.createItemStack();
	}

}
