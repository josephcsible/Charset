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

package pl.asie.charset.lib.capability.redstone;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import pl.asie.charset.api.wires.IRedstoneEmitter;

public class DefaultRedstoneEmitterStorage implements Capability.IStorage<IRedstoneEmitter> {
	@Override
	public NBTBase writeNBT(Capability<IRedstoneEmitter> capability, IRedstoneEmitter instance, EnumFacing side) {
		if (instance instanceof DefaultRedstoneEmitter) {
			NBTTagCompound cpd = new NBTTagCompound();
			cpd.setInteger("s", instance.getRedstoneSignal());
			return cpd;
		}
		return null;
	}

	@Override
	public void readNBT(Capability<IRedstoneEmitter> capability, IRedstoneEmitter instance, EnumFacing side, NBTBase nbt) {
		if (instance instanceof DefaultRedstoneEmitter && nbt instanceof NBTTagCompound) {
			NBTTagCompound cpd = (NBTTagCompound) nbt;
			if (cpd.hasKey("s")) {
				((DefaultRedstoneEmitter) instance).emit(cpd.getInteger("s"));
			}
		}
	}
}
