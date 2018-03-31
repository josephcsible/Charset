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

package pl.asie.charset.module.decoration.stacks;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import pl.asie.charset.lib.block.BlockBase;
import pl.asie.charset.lib.material.ItemMaterial;
import pl.asie.charset.lib.utils.UnlistedPropertyGeneric;

import javax.annotation.Nullable;
import java.util.WeakHashMap;

public class BlockStacks extends BlockBase implements ITileEntityProvider {
	protected static final UnlistedPropertyGeneric<TileEntityStacks> PROPERTY_TILE = new UnlistedPropertyGeneric<>("tile", TileEntityStacks.class);

	public BlockStacks() {
		super(Material.IRON);
		setFullCube(false);
		setOpaqueCube(false);
		setSoundType(SoundType.METAL);
		setHardness(0.0F);
		setUnlocalizedName("charset.stacks");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public float getAmbientOcclusionLightValue(IBlockState state) {
		return 1.0f;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isTranslucent(IBlockState state) {
		return true;
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		TileEntity tile = source.getTileEntity(pos);
		if (tile instanceof TileEntityStacks) {
			int count = ((TileEntityStacks) tile).stacks.size();
			float height = ((count + 7) / 8) * 0.125f;
			return new AxisAlignedBB(
					0, 0, 0,
					1, height, 1
			);
		} else {
			return FULL_BLOCK_AABB;
		}
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, @Nullable TileEntity te, int fortune, boolean silkTouch) {
		if (te instanceof TileEntityStacks) {
			TObjectIntMap<ItemMaterial> materials = new TObjectIntHashMap<>();
			for (ItemMaterial material : ((TileEntityStacks) te).stacks) {
				materials.adjustOrPutValue(material, 1, 1);
			}

			for (ItemMaterial material : materials.keySet()) {
				ItemStack stack = material.getStack();
				if (!stack.isEmpty()) {
					int count = materials.get(material);
					for (int i = 0; i < count; i += stack.getMaxStackSize()) {
						stack = stack.copy();
						stack.setCount(Math.min(count - i, stack.getMaxStackSize()));
						drops.add(stack);
					}
				}
			}
		}
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World worldIn, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		this.onBlockHarvested(worldIn, pos, state, player);
		if (player.isCreative()) {
			NonNullList<ItemStack> drops = NonNullList.create();
			getDrops(drops, worldIn, pos, state, worldIn.getTileEntity(pos), 0, false);
			for (ItemStack s : drops) {
				spawnAsEntity(worldIn, pos, s);
			}
			worldIn.setBlockToAir(pos);
			return true;
		} else {
			return !(cooldownMap.containsKey(player) && cooldownMap.get(player) >= worldIn.getTotalWorldTime());
		}
	}

	@Override
	public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return true;
	}

	private final WeakHashMap<EntityPlayer, Long> cooldownMap = new WeakHashMap<>();

	@Override
	public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
		if (te instanceof TileEntityStacks) {
			if (cooldownMap.containsKey(player) && cooldownMap.get(player) >= worldIn.getTotalWorldTime()) {
				return;
			}

			if (player.isSneaking()) {
				NonNullList<ItemStack> drops = NonNullList.create();
				getDrops(drops, worldIn, pos, state, te, 0, false);
				for (ItemStack s : drops) {
					spawnAsEntity(worldIn, pos, s);
				}
				worldIn.setBlockToAir(pos);
			} else {
				cooldownMap.put(player, worldIn.getTotalWorldTime() + 1);

				ItemStack stackRemoved = ((TileEntityStacks) te).removeStack(false);
				if (!stackRemoved.isEmpty()) {
					if (stackRemoved.getCount() > 1) {
						stackRemoved = stackRemoved.copy();
						stackRemoved.setCount(1);
					}
					spawnAsEntity(worldIn, pos, stackRemoved);
				}
				if (((TileEntityStacks) te).stacks.isEmpty()) {
					worldIn.setBlockToAir(pos);
				}
			}
		}
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity tile = world.getTileEntity(pos);
		if (tile instanceof TileEntityStacks) {
			return ((IExtendedBlockState) state).withProperty(PROPERTY_TILE, (TileEntityStacks) tile);
		} else {
			return state;
		}
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[0], new IUnlistedProperty[]{PROPERTY_TILE});
	}

	@Nullable
	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileEntityStacks();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean addDestroyEffects(World world, BlockPos pos, ParticleManager manager) {
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean addHitEffects(IBlockState state, World world, RayTraceResult target, ParticleManager manager) {
		return true;
	}
}
