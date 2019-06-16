package p455w0rd.wpt.api;

import appeng.api.implementations.tiles.IViewCellStorage;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.IContainerCraftingPacket;
import p455w0rd.ae2wtlib.api.container.IWTContainer;
import p455w0rd.ae2wtlib.api.container.slot.IOptionalSlotHost;

/**
 * @author p455w0rd
 *
 */
public interface IWPTContainer extends IWTContainer, IContainerCraftingPacket, IOptionalSlotHost, IMEMonitorHandlerReceiver<IAEItemStack>, IViewCellStorage {

}
