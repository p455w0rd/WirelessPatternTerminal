package p455w0rd.wpt.client.gui;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.base.Stopwatch;

import appeng.api.config.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IConfigManager;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.me.*;
import appeng.container.slot.*;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.helpers.InventoryAction;
import appeng.util.IConfigManagerHost;
import appeng.util.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.ae2wtlib.api.client.ItemStackSizeRenderer;
import p455w0rd.ae2wtlib.api.client.ReadableNumberConverter;
import p455w0rd.ae2wtlib.api.client.gui.GuiWT;
import p455w0rd.ae2wtlib.api.client.gui.widgets.*;
import p455w0rd.ae2wtlib.api.container.ContainerWT;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.container.slot.SlotCraftingOutput;
import p455w0rd.wpt.container.slot.SlotPatternTerm;
import p455w0rd.wpt.init.*;
import p455w0rd.wpt.sync.packets.*;
import p455w0rd.wpt.util.WPTUtils;
import yalter.mousetweaks.api.MouseTweaksIgnore;

@MouseTweaksIgnore
public class GuiWPT extends GuiWT implements ISortSource, IConfigManagerHost {

	private static final ResourceLocation BACKGROUND = new ResourceLocation(ModGlobals.MODID, "textures/gui/pattern.png");

	private static final String SUBSITUTION_DISABLE = "0";
	private static final String SUBSITUTION_ENABLE = "1";

	private static final String CRAFTMODE_CRFTING = "1";
	private static final String CRAFTMODE_PROCESSING = "0";

	private GuiTabButton tabCraftButton;
	private GuiTabButton tabProcessButton;
	private GuiImgButton substitutionsEnabledBtn;
	private GuiImgButton substitutionsDisabledBtn;
	private GuiImgButton encodeBtn;
	private GuiImgButton clearBtn;

	private static int craftingGridOffsetX;
	private static int craftingGridOffsetY;

	private static String memoryText = "";
	private final ItemRepo repo;
	private final int lowerTextureOffset = 0;
	private final IConfigManager configSrc;
	//private final boolean viewCell;
	private final ItemStack[] myCurrentViewCells = new ItemStack[4];
	private GuiItemIconButton craftingStatusBtn;
	private GuiMETextField searchField;
	private int perRow = 9;
	private int reservedSpace = 0;
	private final boolean customSortOrder = true;
	private int rows = 0;
	private int maxRows = Integer.MAX_VALUE;
	private final int standardSize;
	private GuiImgButton ViewBox;
	private GuiImgButton SortByBox;
	private GuiImgButton SortDirBox;
	private GuiImgButton searchBoxSettings;
	private GuiImgButton terminalStyleBox;
	private GuiImgButtonBooster autoConsumeBoostersBox;
	private final ContainerWPT containerWPT;

	private boolean isJEIEnabled;
	//private final boolean wasTextboxFocused = false;
	//private final int screenResTicks = 0;
	EntityLivingBase entity;
	boolean isHalloween = false;

	public GuiWPT(final Container container) {
		super(container);
		xSize = 185;
		ySize = 204;
		standardSize = xSize;
		setReservedSpace(81);
		containerWPT = (ContainerWPT) container;
		setScrollBar(WTApi.instance().createScrollbar());
		subGui = switchingGuis;
		switchingGuis = false;
		repo = new ItemRepo(getScrollBar(), this);
		configSrc = containerWPT.getConfigManager();
		((ContainerWPT) inventorySlots).setGui(this);
		entity = Minecraft.getMinecraft().player;
		final Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		isHalloween = calendar.get(2) + 1 == 10 && calendar.get(5) == 31;
	}

	int getReservedSpace() {
		return reservedSpace;
	}

	void setReservedSpace(final int reservedSpace) {
		this.reservedSpace = reservedSpace;
	}

	int getMaxRows() {
		return AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.SMALL ? 6 : Integer.MAX_VALUE;
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
		memoryText = searchField.getText();
	}

	@Override
	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		maxRows = getMaxRows();
		mc.player.openContainer = inventorySlots;
		guiLeft = (width - xSize) / 2;
		guiTop = (height - ySize) / 2;

		maxRows = getMaxRows();
		perRow = AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL ? 9 : 9 + (width - standardSize) / 18;
		isJEIEnabled = Loader.isModLoaded("JEI");
		final int top = isJEIEnabled ? 22 : 0;
		final int magicNumber = 114 + 1;
		final int extraSpace = height - magicNumber - 0 - top - reservedSpace;
		rows = (int) Math.floor(extraSpace / 18);
		if (rows > maxRows) {
			rows = maxRows;
		}
		if (isJEIEnabled) {
			rows--;
		}
		if (rows < 3) {
			rows = 3;
		}

		getMeSlots().clear();
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < perRow; x++) {
				getMeSlots().add(new InternalSlotME(repo, x + y * perRow, 8 + x * 18, 18 + y * 18));
			}
		}

		if (AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.FULL) {
			xSize = standardSize + (perRow - 9) * 18;
		}
		else {
			xSize = standardSize;
		}
		ySize = magicNumber + rows * 18 + reservedSpace;
		final int unusedSpace = height - ySize;
		guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));
		super.initGui();

		buttonList.add(tabCraftButton = new GuiTabButton(guiLeft + 173, guiTop + ySize - 177, new ItemStack(Blocks.CRAFTING_TABLE), GuiText.CraftingPattern.getLocal(), itemRender));
		buttonList.add(tabProcessButton = new GuiTabButton(guiLeft + 173, guiTop + ySize - 177, new ItemStack(Blocks.FURNACE), GuiText.ProcessingPattern.getLocal(), itemRender));
		buttonList.add(substitutionsEnabledBtn = new GuiImgButton(guiLeft + 74, guiTop + ySize - 168, Settings.ACTIONS, ItemSubstitution.ENABLED));
		substitutionsEnabledBtn.setHalfSize(true);
		buttonList.add(substitutionsDisabledBtn = new GuiImgButton(guiLeft + 74, guiTop + ySize - 168, Settings.ACTIONS, ItemSubstitution.DISABLED));
		substitutionsDisabledBtn.setHalfSize(true);
		buttonList.add(clearBtn = new GuiImgButton(guiLeft + 64, guiTop + ySize - 168, Settings.ACTIONS, ActionItems.CLOSE));
		clearBtn.setHalfSize(true);
		buttonList.add(encodeBtn = new GuiImgButton(guiLeft + 147, guiTop + ySize - 148, Settings.ACTIONS, ActionItems.ENCODE));
		if (customSortOrder) {
			getButtonPanel().addButton(SortByBox = new GuiImgButton(getButtonPanelXOffset(), getButtonPanelYOffset(), Settings.SORT_BY, configSrc.getSetting(Settings.SORT_BY)));
		}
		getButtonPanel().addButton(ViewBox = new GuiImgButton(getButtonPanelXOffset(), getButtonPanelYOffset(), Settings.VIEW_MODE, configSrc.getSetting(Settings.VIEW_MODE)));
		getButtonPanel().addButton(SortDirBox = new GuiImgButton(getButtonPanelXOffset(), getButtonPanelYOffset(), Settings.SORT_DIRECTION, configSrc.getSetting(Settings.SORT_DIRECTION)));
		getButtonPanel().addButton(searchBoxSettings = new GuiImgButton(getButtonPanelXOffset(), getButtonPanelYOffset(), Settings.SEARCH_MODE, AEConfig.instance().getConfigManager().getSetting(Settings.SEARCH_MODE)));
		getButtonPanel().addButton(terminalStyleBox = new GuiImgButton(getButtonPanelXOffset(), getButtonPanelYOffset(), Settings.TERMINAL_STYLE, AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE)));
		if (!WTApi.instance().getConfig().isOldInfinityMechanicEnabled() && !WTApi.instance().isWTCreative(getWirelessTerminal())) {
			getButtonPanel().addButton(autoConsumeBoostersBox = new GuiImgButtonBooster(getButtonPanelXOffset(), getButtonPanelYOffset(), containerWPT.getWirelessTerminal()));
		}
		getButtonPanel().init(this);
		searchField = new GuiMETextField(fontRenderer, guiLeft + 79, guiTop + 4, 90, 12);
		searchField.setEnableBackgroundDrawing(false);
		searchField.setMaxStringLength(25);
		searchField.setTextColor(0xFFFFFF);
		searchField.setSelectionColor(0xFF99FF99);
		searchField.setVisible(true);
		buttonList.add(craftingStatusBtn = new GuiItemIconButton(guiLeft + 170, guiTop - 4, 2 + 11 * 16, GuiText.CraftingStatus.getLocal(), itemRender));
		craftingStatusBtn.setHideEdge(13);
		final Enum<?> setting = AEConfig.instance().getConfigManager().getSetting(Settings.SEARCH_MODE);
		searchField.setFocused(SearchBoxMode.AUTOSEARCH == setting || SearchBoxMode.JEI_AUTOSEARCH == setting);
		if (isSubGui()) {
			searchField.setText(memoryText);
			repo.setSearchString(memoryText);
			repo.updateView();
			setScrollBar();
		}
		craftingGridOffsetX = Integer.MAX_VALUE;
		craftingGridOffsetY = Integer.MAX_VALUE;
		for (final Object s : inventorySlots.inventorySlots) {
			if (s instanceof AppEngSlot) {
				if (((Slot) s).xPos < 197) {
					repositionSlot((AppEngSlot) s);
				}
			}
			if (s instanceof SlotCraftingMatrix || s instanceof SlotFakeCraftingMatrix) {
				final Slot g = (Slot) s;
				if (g.xPos > 0 && g.yPos > 0) {
					craftingGridOffsetX = Math.min(craftingGridOffsetX, g.xPos);
					craftingGridOffsetY = Math.min(craftingGridOffsetY, g.yPos);
				}
			}
		}
		craftingGridOffsetX -= 25;
		craftingGridOffsetY -= 6;
		setScrollBar();
	}

	protected void repositionSlot(final AppEngSlot s) {
		s.yPos = s.getY() + ySize - 78 - 5;
	}

	@Override
	public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		if (containerWPT.isCraftingMode()) {
			tabCraftButton.visible = true;
			tabProcessButton.visible = false;

			if (containerWPT.substitute) {
				substitutionsEnabledBtn.visible = true;
				substitutionsDisabledBtn.visible = false;
			}
			else {
				substitutionsEnabledBtn.visible = false;
				substitutionsDisabledBtn.visible = true;
			}
		}
		else {
			tabCraftButton.visible = false;
			tabProcessButton.visible = true;
			substitutionsEnabledBtn.visible = false;
			substitutionsDisabledBtn.visible = false;
		}
		fontRenderer.drawString("Terminal", 8, 6, 4210752);

		String warning = "";
		if (WTApi.instance().getConfig().isInfinityBoosterCardEnabled() && !WTApi.instance().getConfig().isOldInfinityMechanicEnabled()) {
			final int infinityEnergyAmount = WTApi.instance().getInfinityEnergy(getWirelessTerminal());
			if (WTApi.instance().hasInfiniteRange(getWirelessTerminal())) {
				if (!WTApi.instance().isInRangeOfWAP(getWirelessTerminal(), WPTUtils.player())) {
					if (infinityEnergyAmount < WTApi.instance().getConfig().getLowInfinityEnergyWarningAmount()) {
						warning = TextFormatting.RED + "" + I18n.format(WTApi.instance().getConstants().getTooltips().infinityEnergyLow());
					}
				}
			}
			if (!WTApi.instance().isWTCreative(getWirelessTerminal()) && isPointInRegion(containerWPT.getBoosterSlot().xPos, containerWPT.getBoosterSlot().yPos, 16, 16, mouseX, mouseY) && mc.player.inventory.getItemStack().isEmpty()) {
				final String amountColor = infinityEnergyAmount < WTApi.instance().getConfig().getLowInfinityEnergyWarningAmount() ? TextFormatting.RED.toString() : TextFormatting.GREEN.toString();
				final String infinityEnergy = I18n.format(WTApi.instance().getConstants().getTooltips().infinityEnergy()) + ": " + amountColor + "" + (isShiftKeyDown() ? infinityEnergyAmount : ReadableNumberConverter.INSTANCE.toSlimReadableForm(infinityEnergyAmount)) + "" + TextFormatting.GRAY + " " + I18n.format(WTApi.instance().getConstants().getTooltips().units());
				drawTooltip(mouseX - offsetX, mouseY - offsetY, infinityEnergy);
			}
		}
		//if (!warning.isEmpty()) {
		//	GlStateManager.enableBlend();
		//	GlStateManager.color(1, 1, 1, 1);
		fontRenderer.drawString(GuiText.inventory.getLocal(), 8, ySize - 177, 4210752);
		mc.fontRenderer.drawString(" " + warning, 8 + fontRenderer.getStringWidth(GuiText.inventory.getLocal()), ySize - 177, 4210752);
		//}
	}

	@Override
	public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
		mc.getTextureManager().bindTexture(BACKGROUND);
		final int x_width = 197;
		this.drawTexturedModalRect(offsetX, offsetY, 0, 0, x_width, 18);

		//this.drawTexturedModalRect(offsetX + x_width, offsetY, x_width, 0, 46, 100);

		for (int x = 0; x < rows; x++) {
			this.drawTexturedModalRect(offsetX, offsetY + 18 + x * 18, 0, 18, x_width, 18);
		}

		this.drawTexturedModalRect(offsetX, offsetY + 16 + rows * 18 + lowerTextureOffset, 0, 106 - 18 - 18, x_width, 99 + reservedSpace - lowerTextureOffset);

		if (containerWPT.isCraftingMode()) {
			drawTexturedModalRect(offsetX + 88, offsetY + 43 + rows * 18 + lowerTextureOffset, 256 - 24, 256 - 24, 24, 24);
		}
		else {
			mc.getTextureManager().bindTexture(WTApi.instance().getConstants().getStatesTexture());
			for (int i = 0; i < 3; i++) {
				drawTexturedModalRect(offsetX + 91, offsetY + 28 + rows * 18 + lowerTextureOffset + 18 * i, 0, 256 - 18, 18, 18);
			}
		}
		for (int i = 0; i < 4; i++) {
			mc.getTextureManager().bindTexture(WTApi.instance().getConstants().getStatesTexture());
			drawTexturedModalRect(offsetX + 7 + 18 * i, offsetY + 88 + rows * 18 + lowerTextureOffset, 0, 256 - 18, 18, 18);
		}

		boolean update = false;

		for (int i = 0; i < 4; i++) {
			if (myCurrentViewCells[i] == null || myCurrentViewCells[i] != containerWPT.getCellViewSlot(i).getStack()) {
				update = true;
				myCurrentViewCells[i] = containerWPT.getCellViewSlot(i).getStack();
			}
		}

		if (update) {
			repo.setViewCell(myCurrentViewCells);
		}

		if (WTApi.instance().getConfig().isInfinityBoosterCardEnabled() && !WTApi.instance().isWTCreative(getWirelessTerminal())) {
			//final Slot boosterSlot = containerWPT.getBoosterSlot();
			//final int bx = boosterSlot.xPos;
			//final int by = boosterSlot.yPos;

			//drawTexturedModalRect(guiLeft + (bx - 2), guiTop + (by - 2), 237, 237, 19, 19);
		}
		if (searchField != null) {
			searchField.drawTextBox();
		}
	}

	@Override
	public void drawSlot(final Slot s) {
		if (s instanceof SlotME) {
			try {
				zLevel = 100.0F;
				itemRender.zLevel = 100.0F;
				if (!isPowered()) {
					drawRect(s.xPos, s.yPos, 16 + s.xPos, 16 + s.yPos, 0x66111111);
				}
				zLevel = 0.0F;
				itemRender.zLevel = 0.0F;
				super.drawSlot(new Size1Slot((SlotME) s));
				ItemStackSizeRenderer.getInstance().renderStackSize(fontRenderer, ((SlotME) s).getAEStack(), s.xPos, s.yPos);
			}
			catch (final Exception err) {
			}
			return;
		}
		else {
			super.drawSlot(s);
		}
	}

	@Override
	public void updateScreen() {
		repo.setPower(containerWPT.isPowered());
		super.updateScreen();
		if (isHalloween && entity != Minecraft.getMinecraft().player) {
			if (!entity.getHeldItemMainhand().isItemEqual(Minecraft.getMinecraft().player.getHeldItemMainhand())) {
				entity.setHeldItem(EnumHand.MAIN_HAND, Minecraft.getMinecraft().player.getHeldItemMainhand());
			}
			if (!entity.getHeldItemOffhand().isItemEqual(Minecraft.getMinecraft().player.getHeldItemOffhand())) {
				entity.setHeldItem(EnumHand.OFF_HAND, Minecraft.getMinecraft().player.getHeldItemOffhand());
			}
		}
	}

	@Override
	protected void actionPerformed(final GuiButton btn) throws IOException {
		if (btn == craftingStatusBtn) {
			ModNetworking.instance().sendToServer(new PacketSwitchGuis(ModGuiHandler.GUI_CRAFTING_STATUS));
			return;
		}
		if (btn instanceof GuiImgButton) {
			final boolean backwards = Mouse.isButtonDown(1);
			final GuiImgButton iBtn = (GuiImgButton) btn;

			if (iBtn.getSetting() != Settings.ACTIONS) {
				final Enum<?> cv = iBtn.getCurrentValue();
				final Enum<?> next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());
				if (btn == terminalStyleBox || btn == searchBoxSettings) {
					AEConfig.instance().getConfigManager().putSetting(iBtn.getSetting(), next);
				}
				else {
					ModNetworking.instance().sendToServer(new PacketValueConfig(iBtn.getSetting().name(), next.name()));
				}
				iBtn.set(next);
				if (next.getClass() == SearchBoxMode.class || next.getClass() == TerminalStyle.class) {
					reinitalize();
				}
			}
		}
		if (btn == autoConsumeBoostersBox) {
			autoConsumeBoostersBox.cycleValue();
		}
		if (tabCraftButton == btn || tabProcessButton == btn) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("PatternTerminal.CraftMode", tabProcessButton == btn ? CRAFTMODE_CRFTING : CRAFTMODE_PROCESSING));
		}

		if (encodeBtn == btn) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("PatternTerminal.Encode", "1"));
		}

		if (clearBtn == btn) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("PatternTerminal.Clear", "1"));
		}

		if (substitutionsEnabledBtn == btn || substitutionsDisabledBtn == btn) {
			ModNetworking.instance().sendToServer(new PacketValueConfig("PatternTerminal.Substitute", substitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
		}
		super.actionPerformed(btn);
	}

	private void reinitalize() {
		buttonList.clear();
		initGui();
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();

		final int i = Mouse.getEventDWheel();
		if (i != 0 && isShiftKeyDown()) {
			final int x = Mouse.getEventX() * width / mc.displayWidth;
			final int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
			mouseWheelEvent(x, y, i / Math.abs(i));
		}
		else if (i != 0 && getScrollBar() != null) {
			getScrollBar().wheel(i);
		}
	}

	private void mouseWheelEvent(final int x, final int y, final int wheel) {
		final Slot slot = getSlot(x, y);
		if (slot instanceof SlotME) {
			final IAEItemStack item = ((SlotME) slot).getAEStack();
			if (item != null) {
				if (inventorySlots instanceof ContainerWPT) {
					((ContainerWPT) inventorySlots).setTargetStack(item);
				}
				final InventoryAction direction = wheel > 0 ? InventoryAction.ROLL_DOWN : InventoryAction.ROLL_UP;
				final int times = Math.abs(wheel);
				final int inventorySize = getInventorySlots().size();
				for (int h = 0; h < times; h++) {
					final PacketInventoryAction p = new PacketInventoryAction(direction, inventorySize, 0);
					ModNetworking.instance().sendToServer(p);
				}
			}
		}
	}

	@Override
	protected void handleMouseClick(final Slot slot, final int slotIdx, final int mouseButton, final ClickType clickType) {
		final EntityPlayer player = Minecraft.getMinecraft().player;
		if (slot instanceof SlotFake) {
			final InventoryAction action = clickType == ClickType.QUICK_CRAFT ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
			if (drag_click.size() > 1) {
				return;
			}
			final PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0);
			ModNetworking.instance().sendToServer(p);
			return;
		}
		if (slot instanceof SlotPatternTerm) {
			if (mouseButton == 6) {
				return; // prevent weird double clicks..
			}
			try {
				ModNetworking.instance().sendToServer(((SlotPatternTerm) slot).getRequest(isShiftKeyDown()));
			}
			catch (final IOException e) {
			}
		}
		else if (slot instanceof SlotCraftingOutput) {
			if (mouseButton == 6) {
				return; // prevent weird double clicks..
			}
			InventoryAction action = null;
			if (isShiftKeyDown()) {
				action = InventoryAction.CRAFT_SHIFT;
			}
			else {
				// Craft stack on right-click, craft single on left-click
				action = mouseButton == 1 ? InventoryAction.CRAFT_STACK : InventoryAction.CRAFT_ITEM;
			}
			final PacketInventoryAction p = new PacketInventoryAction(action, slotIdx, 0);
			ModNetworking.instance().sendToServer(p);

			return;
		}

		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			if (enableSpaceClicking()) {
				IAEItemStack stack = null;
				if (slot instanceof SlotME) {
					stack = ((SlotME) slot).getAEStack();
				}

				int slotNum = getInventorySlots().size();

				if (!(slot instanceof SlotME) && slot != null) {
					slotNum = slot.slotNumber;
				}

				((ContainerWT) inventorySlots).setTargetStack(stack);

				final PacketInventoryAction p = new PacketInventoryAction(InventoryAction.MOVE_REGION, slotNum, 0);
				ModNetworking.instance().sendToServer(p);
				return;
			}
		}

		if (slot instanceof SlotDisconnected) {
			InventoryAction action = null;
			switch (clickType) {
			case PICKUP: // pickup / set-down.
				action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
				break;
			case QUICK_MOVE:
				action = mouseButton == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
				break;

			case CLONE: // creative dupe:

				if (player.capabilities.isCreativeMode) {
					action = InventoryAction.CREATIVE_DUPLICATE;
				}

				break;
			default:
			case THROW: // drop item:
			}

			if (action != null) {
				final PacketInventoryAction p = new PacketInventoryAction(action, slot.getSlotIndex(), ((SlotDisconnected) slot).getSlot().getId());
				ModNetworking.instance().sendToServer(p);
			}
			return;
		}

		if (slot instanceof SlotME) {
			InventoryAction action = null;
			IAEItemStack stack = null;

			switch (clickType) {
			case PICKUP: // pickup / set-down.
				action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
				stack = ((SlotME) slot).getAEStack();

				if (stack != null && action == InventoryAction.PICKUP_OR_SET_DOWN && player.inventory.getItemStack().isEmpty() && (stack.getStackSize() == 0 || stack.getStackSize() > 0 && GuiScreen.isAltKeyDown())) {
					action = InventoryAction.AUTO_CRAFT;
				}

				break;
			case QUICK_MOVE:
				action = mouseButton == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
				stack = ((SlotME) slot).getAEStack();
				break;

			case CLONE: // creative dupe:
				stack = ((SlotME) slot).getAEStack();
				if (stack != null && stack.isCraftable()) {
					action = InventoryAction.AUTO_CRAFT;
				}
				else if (player.capabilities.isCreativeMode) {
					final IAEItemStack slotItem = ((SlotME) slot).getAEStack();
					if (slotItem != null) {
						action = InventoryAction.CREATIVE_DUPLICATE;
					}
				}
				break;

			default:
			case THROW: // drop item:
			}
			if (action != null) {
				if (inventorySlots instanceof ContainerWPT) {
					((ContainerWPT) inventorySlots).setTargetStack(stack);
					final PacketInventoryAction p = new PacketInventoryAction(action, getInventorySlots().size(), 0);
					ModNetworking.instance().sendToServer(p);
				}
			}
			return;
		}

		if (!disableShiftClick && isShiftKeyDown()) {
			disableShiftClick = true;

			if (dbl_whichItem == null || bl_clicked != slot || dbl_clickTimer.elapsed(TimeUnit.MILLISECONDS) > 150) {
				// some simple double click logic.
				bl_clicked = slot;
				dbl_clickTimer = Stopwatch.createStarted();
				if (slot != null) {
					dbl_whichItem = slot.getHasStack() ? slot.getStack().copy() : ItemStack.EMPTY;
				}
				else {
					dbl_whichItem = ItemStack.EMPTY;
				}
			}
			else if (!dbl_whichItem.isEmpty()) {
				// a replica of the weird broken vanilla feature.

				final List<Slot> slots = getInventorySlots();
				for (final Slot inventorySlot : slots) {
					if (inventorySlot != null && inventorySlot.canTakeStack(Minecraft.getMinecraft().player) && inventorySlot.getHasStack() && inventorySlot.inventory == slot.inventory && Container.canAddItemToSlot(inventorySlot, dbl_whichItem, true)) {
						handleMouseClick(inventorySlot, inventorySlot.slotNumber, 1, clickType);
					}
				}
			}

			disableShiftClick = false;
		}
		super.handleMouseClick(slot, slotIdx, mouseButton, clickType);
	}

	@Override
	protected void keyTyped(final char character, final int key) throws IOException {
		if (!checkHotbarKeys(key)) {
			if (character == ' ' && searchField.getText().isEmpty()) {
				return;
			}

			if (searchField.textboxKeyTyped(character, key)) {
				repo.setSearchString(searchField.getText());
				repo.updateView();
				this.setScrollBar();
			}
			else {
				super.keyTyped(character, key);
			}
		}
	}

	@Override
	protected void mouseClicked(final int xCoord, final int yCoord, final int btn) throws IOException {
		searchField.mouseClicked(xCoord, yCoord, btn);

		if (btn == 1 && searchField.isMouseIn(xCoord, yCoord)) {
			searchField.setText("");
			repo.setSearchString("");
			repo.updateView();
			this.setScrollBar();
		}

		super.mouseClicked(xCoord, yCoord, btn);
	}

	public void postUpdate(final List<IAEItemStack> list) {
		for (final IAEItemStack is : list) {
			repo.postUpdate(is);
		}
		repo.updateView();
		setScrollBar();
	}

	private void setScrollBar() {
		getScrollBar().setTop(18).setLeft(175).setHeight(rows * 18 - 2);
		getScrollBar().setRange(0, (repo.size() + perRow - 1) / perRow - rows, Math.max(1, rows / 6));
	}

	@Override
	public Enum getSortBy() {
		return configSrc.getSetting(Settings.SORT_BY);
	}

	@Override
	public Enum getSortDir() {
		return configSrc.getSetting(Settings.SORT_DIRECTION);
	}

	@Override
	public Enum getSortDisplay() {
		return configSrc.getSetting(Settings.VIEW_MODE);
	}

	@Override
	public void updateSetting(final IConfigManager manager, final Enum settingName, final Enum newValue) {
		if (SortByBox != null) {
			SortByBox.set(configSrc.getSetting(Settings.SORT_BY));
		}
		if (SortDirBox != null) {
			SortDirBox.set(configSrc.getSetting(Settings.SORT_DIRECTION));
		}
		if (ViewBox != null) {
			ViewBox.set(configSrc.getSetting(Settings.VIEW_MODE));
		}
		repo.updateView();
	}

	@Override
	protected boolean isPowered() {
		return repo.hasPower();
	}

}
