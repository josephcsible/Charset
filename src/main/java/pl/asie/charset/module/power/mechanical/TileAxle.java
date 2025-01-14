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

package pl.asie.charset.module.power.mechanical;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import pl.asie.charset.lib.block.TileBase;
import pl.asie.charset.lib.capability.Capabilities;
import pl.asie.charset.lib.capability.CapabilityHelper;
import pl.asie.charset.lib.capability.TileCache;
import pl.asie.charset.lib.material.ItemMaterial;
import pl.asie.charset.lib.material.ItemMaterialRegistry;
import pl.asie.charset.lib.utils.ItemUtils;
import pl.asie.charset.api.experimental.mechanical.IMechanicalPowerProducer;
import pl.asie.charset.api.experimental.mechanical.IMechanicalPowerConsumer;

import javax.annotation.Nullable;

public class TileAxle extends TileBase implements ITickable {
	public static final float SPEED_MULTIPLIER = 4.5f;

	protected class AxleSide implements IMechanicalPowerProducer, IMechanicalPowerConsumer {
		protected final EnumFacing facing;
		protected final int i;
		protected final TileCache cache;
		protected double speedReceived, torqueReceived;

		public AxleSide(int i, EnumFacing facing) {
			this.i = i;
			this.facing = facing;
			this.cache = new TileCache(
					world, pos.offset(facing.getOpposite())
			);
		}

		@Override
		public boolean isAcceptingPower() {
			if (powerOutputs[i ^ 1] != null && powerOutputs[i ^ 1].torqueReceived != 0.0) return false;

			IMechanicalPowerConsumer output = CapabilityHelper.get(
					Capabilities.MECHANICAL_CONSUMER, cache.getTile(), facing
			);

			return output != null && output.isAcceptingPower();
		}

		@Override
		public void setForce(double speed, double torque) {
			IMechanicalPowerConsumer output = CapabilityHelper.get(
					Capabilities.MECHANICAL_CONSUMER, cache.getTile(), facing
			);

			if (output != null) {
				output.setForce(speed, torque);
			}

			if (speedReceived != speed || torqueReceived != torque) {
				speedReceived = speed;
				torqueReceived = torque;
				if (torqueReceived == 0.0) world.neighborChanged(pos.offset(facing), CharsetPowerMechanical.blockAxle, pos);
				markBlockForUpdate();
			}
		}

		public void onNeighborChanged(BlockPos pos) {
			cache.neighborChanged(pos);
			setForce(0.0, 0.0);
		}
	}

	public final TraitMechanicalRotation ROTATION;
	public double rotTorqueClient;
	protected AxleSide[] powerOutputs = new AxleSide[2];
	protected ItemMaterial material = ItemMaterialRegistry.INSTANCE.getDefaultMaterialByType("plank");
	protected boolean rendered;
	private double rotSpeedClient;

	public TileAxle() {
		registerTrait("rot", ROTATION = new TraitMechanicalRotation());
	}

	private boolean loadMaterialFromNBT(NBTTagCompound compound) {
		ItemMaterial nm = ItemMaterialRegistry.INSTANCE.getMaterial(compound, "material");
		if (nm != null && nm != material) {
			material = nm;
			return true;
		} else {
			return false;
		}
	}

	private void saveMaterialToNBT(NBTTagCompound compound) {
		getMaterial().writeToNBT(compound, "material");
	}

	public void onNeighborChanged(BlockPos pos) {
		for (int i = 0; i < 2; i++) {
			if (powerOutputs[i] != null) {
				powerOutputs[i].onNeighborChanged(pos);
			}
		}
	}

	protected double getRotSpeedClient() {
		if (world.isRemote) {
			return rotSpeedClient;
		} else {
			double ds0 = powerOutputs[0] != null ? powerOutputs[0].speedReceived : 0.0;
			double ds1 = powerOutputs[1] != null ? powerOutputs[1].speedReceived : 0.0;
			return Math.max(ds0, ds1);
		}
	}

	@Override
	public void update() {
		super.update();

		double oldForce = ROTATION.getForce();
		double newForce = getRotSpeedClient();

		ROTATION.tick(newForce);

		if (oldForce != newForce && !world.isRemote) {
			markBlockForUpdate();
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		if (capability == Capabilities.MECHANICAL_PRODUCER || capability == Capabilities.MECHANICAL_CONSUMER) {
			EnumFacing.Axis axis = EnumFacing.Axis.values()[getBlockMetadata()];
			return facing != null && facing.getAxis() == axis;
		}

		return super.hasCapability(capability, facing);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == Capabilities.MECHANICAL_PRODUCER || capability == Capabilities.MECHANICAL_CONSUMER) {
			EnumFacing.Axis axis = EnumFacing.Axis.values()[getBlockMetadata()];
			if (facing != null && facing.getAxis() == axis) {
				EnumFacing.AxisDirection direction = facing.getAxisDirection();
				int i = direction == EnumFacing.AxisDirection.POSITIVE ? 1 : 0;
				if (powerOutputs[i] == null) {
					powerOutputs[i] = new AxleSide(i, facing);
				}
				return (T) powerOutputs[i];
			} else {
				return null;
			}
		} else {
			return super.getCapability(capability, facing);
		}
	}

	@Override
	public void updateContainingBlockInfo() {
		super.updateContainingBlockInfo();
		powerOutputs[0] = powerOutputs[1] = null;
	}

	@Override
	public ItemStack getDroppedBlock(IBlockState state) {
		ItemStack stack = new ItemStack(CharsetPowerMechanical.itemAxle, 1, 0);
		saveMaterialToNBT(ItemUtils.getTagCompound(stack, true));
		return stack;
	}

	@Override
	public void onPlacedBy(EntityLivingBase placer, @Nullable EnumFacing face, ItemStack stack, float hitX, float hitY, float hitZ) {
		loadMaterialFromNBT(stack.getTagCompound());
	}

	@Override
	public void readNBTData(NBTTagCompound compound, boolean isClient) {
		super.readNBTData(compound, isClient);
		boolean r = loadMaterialFromNBT(compound);
		if (/*(r || !rendered) && */isClient) {
			rotSpeedClient = compound.getFloat("rs");
			rotTorqueClient = compound.getFloat("rt");
			rendered = true;
		}
	}

	@Override
	public NBTTagCompound writeNBTData(NBTTagCompound compound, boolean isClient) {
		compound = super.writeNBTData(compound, isClient);
		saveMaterialToNBT(compound);
		if (isClient) {
			double dt0 = powerOutputs[0] != null ? powerOutputs[0].torqueReceived : 0.0;
			double dt1 = powerOutputs[1] != null ? powerOutputs[1].torqueReceived : 0.0;

			compound.setFloat("rs", (float) getRotSpeedClient());
			compound.setFloat("rt", (float) Math.max(dt0, dt1));
		}
		return compound;
	}

	@Override
	public boolean hasFastRenderer() {
		return true;
	}

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
		return oldState.getBlock() != newState.getBlock();
	}

	public ItemMaterial getMaterial() {
		if (material == null) {
			material = ItemMaterialRegistry.INSTANCE.getDefaultMaterialByType("plank");
		}
		return material;
	}
}
