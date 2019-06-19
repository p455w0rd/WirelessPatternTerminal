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
package p455w0rd.wpt.integration;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import appeng.util.Platform;
import mezz.jei.api.*;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.config.Constants;
import mezz.jei.transfer.RecipeTransferErrorInternal;
import mezz.jei.transfer.RecipeTransferErrorTooltip;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import p455w0rd.ae2wtlib.api.WTApi;
import p455w0rd.wpt.container.ContainerWPT;
import p455w0rd.wpt.init.ModItems;
import p455w0rd.wpt.init.ModNetworking;
import p455w0rd.wpt.sync.packets.PacketJEIPatternRecipe;
import p455w0rdslib.LibGlobals.Mods;

/**
 * @author p455w0rd
 *
 */
@SuppressWarnings("deprecation")
@JEIPlugin
public class JEI implements IModPlugin {

	private static final IRecipeTransferError NEEDED_MODE_CRAFTING = new IncorrectTerminalModeError(true);
	private static final IRecipeTransferError NEEDED_MODE_PROCESSING = new IncorrectTerminalModeError(false);

	@Override
	public void register(@Nonnull final IModRegistry registry) {
		final String wptBaublesDescKey = Mods.BAUBLES.isLoaded() ? WTApi.instance().getConstants().getTooltips().jeiCanBeWorn() : "";
		registry.addIngredientInfo(Lists.newArrayList(new ItemStack(ModItems.WPT)), VanillaTypes.ITEM, "jei.wpt.desc", wptBaublesDescKey);
		registry.getRecipeTransferRegistry().addRecipeTransferHandler(new RecipeTransferHandler(), Constants.UNIVERSAL_RECIPE_TRANSFER_UID);
	}

	public static class RecipeTransferHandler implements IRecipeTransferHandler<ContainerWPT> {

		public static final String OUTPUTS_KEY = "Outputs";

		@Override
		public Class<ContainerWPT> getContainerClass() {
			return ContainerWPT.class;
		}

		@Override
		@Nullable
		public IRecipeTransferError transferRecipe(final ContainerWPT container, final IRecipeLayout recipeLayout, final EntityPlayer player, final boolean maxTransfer, final boolean doTransfer) {
			final String recipeType = recipeLayout.getRecipeCategory().getUid();
			if (doTransfer) {
				final Map<Integer, ? extends IGuiIngredient<ItemStack>> ingredients = recipeLayout.getItemStacks().getGuiIngredients();
				final NBTTagCompound recipeInputs = new NBTTagCompound();
				NBTTagCompound recipeOutputs = null;
				final NBTTagList outputList = new NBTTagList();
				int inputIndex = 0;
				int outputIndex = 0;
				for (final Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> ingredientEntry : ingredients.entrySet()) {
					final IGuiIngredient<ItemStack> guiIngredient = ingredientEntry.getValue();
					if (guiIngredient != null) {
						ItemStack ingredient = ItemStack.EMPTY;
						if (guiIngredient.getDisplayedIngredient() != null) {
							ingredient = guiIngredient.getDisplayedIngredient().copy();
						}
						if (guiIngredient.isInput()) {
							final List<ItemStack> currentList = guiIngredient.getAllIngredients();
							ItemStack stack = currentList.isEmpty() ? ItemStack.EMPTY : currentList.get(0);
							for (final ItemStack currentStack : currentList) {
								if (Platform.isRecipePrioritized(currentStack)) {
									stack = currentStack.copy();
								}
							}
							if (stack == null) {
								stack = ItemStack.EMPTY;
							}
							recipeInputs.setTag("#" + inputIndex, stack.writeToNBT(new NBTTagCompound()));
							inputIndex++;
						}
						else {
							if (outputIndex >= 3 || ingredient.isEmpty() || container.isCraftingMode()) {
								continue;
							}
							outputList.appendTag(ingredient.writeToNBT(new NBTTagCompound()));
							++outputIndex;
							continue;
						}
					}
				}
				if (!outputList.hasNoTags()) {
					recipeOutputs = new NBTTagCompound();
					recipeOutputs.setTag(OUTPUTS_KEY, outputList);
				}
				ModNetworking.instance().sendToServer(new PacketJEIPatternRecipe(recipeInputs, recipeOutputs));
			}
			if (!recipeType.equals(VanillaRecipeCategoryUid.INFORMATION) && !recipeType.equals(VanillaRecipeCategoryUid.FUEL)) {
				if (!container.isCraftingMode()) {
					if (recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
						return NEEDED_MODE_CRAFTING;
					}
				}
				else if (!recipeType.equals(VanillaRecipeCategoryUid.CRAFTING)) {
					return NEEDED_MODE_PROCESSING;
				}
			}
			else {
				return RecipeTransferErrorInternal.INSTANCE;
			}
			return null;
		}

	}

	private static class IncorrectTerminalModeError extends RecipeTransferErrorTooltip {

		private static final String CRAFTING = I18n.translateToLocalFormatted("jei.crafting.desc", new Object[0]);
		private static final String PROCESSING = I18n.translateToLocalFormatted("jei.processing.desc", new Object[0]);

		public IncorrectTerminalModeError(final boolean needsCrafting) {
			super(I18n.translateToLocalFormatted("jei.errormsg.desc", TextFormatting.BOLD + (needsCrafting ? CRAFTING : PROCESSING) + TextFormatting.RESET + "" + TextFormatting.RED));
		}

	}

}
