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
package p455w0rd.wpt.api;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayer;

public abstract class WPTApi {

	protected static WPTApi api = null;

	@Nullable
	public static WPTApi instance() {
		if (WPTApi.api == null) {
			try {
				final Class<?> clazz = Class.forName("p455w0rd.wpt.init.ModAPIImpl");
				final Method instanceAccessor = clazz.getMethod("instance");
				WPTApi.api = (WPTApi) instanceAccessor.invoke(null);
			}
			catch (final Throwable e) {
				return null;
			}
		}

		return WPTApi.api;
	}

	public abstract void openWPTGui(EntityPlayer player, boolean isBauble, int wptSlot);

}
