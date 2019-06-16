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
package p455w0rd.wpt.init;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.item.Item;
import net.minecraftforge.registries.IForgeRegistry;
import p455w0rd.wpt.items.ItemWPT;
import p455w0rd.wpt.items.ItemWPTCreative;

/**
 * @author p455w0rd
 *
 */
public class ModItems {

	public static final ItemWPT WPT = new ItemWPT();
	public static final ItemWPTCreative CREATIVE_WPT = new ItemWPTCreative();
	private static final Item[] ITEM_ARRAY = new Item[] {
			WPT, CREATIVE_WPT
	};
	private static final List<Item> ITEM_LIST = Lists.newArrayList(ITEM_ARRAY);

	public static final List<Item> getList() {
		return ITEM_LIST;
	}

	public static final void register(final IForgeRegistry<Item> registry) {
		registry.registerAll(ITEM_ARRAY);
	}

	/*
	@SideOnly(Side.CLIENT)
	public static final void initModels(final ModelBakeEvent event) {
		for (final Item item : getList()) {
			if (item instanceof IModelHolder) {
				final IModelHolder holder = (IModelHolder) item;
				holder.initModel();
				if (holder.shouldUseInternalTEISR()) {
					final IBakedModel wtModel = event.getModelRegistry().getObject(holder.getModelResource(item));
					holder.setWrappedModel(new ItemLayerWrapper(wtModel).setRenderer(WTItemRenderer.getRendererForItem(item)));
					event.getModelRegistry().putObject(holder.getModelResource(item), holder.getWrappedModel());
				}
			}
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static final void registerTEISRs(final ModelRegistryEvent event) {
		for (final Item item : getList()) {
			if (item instanceof IModelHolder) {
				final IModelHolder holder = (IModelHolder) item;
				if (holder.shouldUseInternalTEISR()) {
					item.setTileEntityItemStackRenderer((TileEntityItemStackRenderer) WTItemRenderer.getRendererForItem(item));
				}
			}
		}
	}
	*/

}
