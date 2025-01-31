/*
 * Copyright (c) 2015, 2016, 2017, 2018 Adrian Siekierka
 *
 * This file is part of Charset.
 *
 * Charset is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Charset is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Charset.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.charset.lib.wires;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IWireRenderContainer {
	final class Simple implements IWireRenderContainer {
		private final WireRenderHandler handler;

		public Simple(WireRenderHandler handler) {
			this.handler = handler;
		}

		@Override
		public int getLayerCount() {
			return 1;
		}

		@Override
		public WireRenderHandler get(int layer) {
			return handler;
		}
	}

	@SideOnly(Side.CLIENT)
	int getLayerCount();

	@SideOnly(Side.CLIENT)
	WireRenderHandler get(int layer);
}
