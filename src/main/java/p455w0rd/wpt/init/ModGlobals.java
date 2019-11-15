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

import p455w0rd.ae2wtlib.api.WTApi;

public class ModGlobals {

	public static final String MODID = "wpt";
	public static final String VERSION = "1.0.2";
	public static final String NAME = "Wireless Pattern Terminal";
	public static final String SERVER_PROXY = "p455w0rd.wpt.proxy.CommonProxy";
	public static final String CLIENT_PROXY = "p455w0rd.wpt.proxy.ClientProxy";
	public static final String DEP_LIST = WTApi.BASE_DEPS_WITH_AE2WTLIB + "after:mousetweaks;after:itemscroller";
	public static final String CONFIG_FILE = WTApi.instance().getConfig().getConfigFile();

}
