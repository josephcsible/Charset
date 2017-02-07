/*
 * Copyright (c) 2015-2016 Adrian Siekierka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2014 copygirl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.asie.charset.tweaks;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.charset.lib.utils.ItemUtils;

import javax.annotation.Nonnull;
import java.util.Set;

public class TweakFiniteWater extends Tweak {
	public TweakFiniteWater() {
		super("tweaks", "finiteWater", "Prevents water sources from being created in areas other than *water biomes below or at sea level*.", false);
	}

	@Override
	public void enable() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void disable() {
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	private boolean isWater(IBlockState state) {
		return state.getMaterial() == Material.WATER;
	}

	@SubscribeEvent
	public void onFluidSource(BlockEvent.CreateFluidSourceEvent event) {
		if (isWater(event.getState())) {
			World world = event.getWorld();

			if (event.getPos().getY() <= world.getSeaLevel()) {
				Biome b = event.getWorld().getBiome(event.getPos());
				if (b instanceof BiomeOcean || b instanceof BiomeRiver) {
					boolean isAir = false;

					for (int i = event.getPos().getY() + 1; i <= world.getSeaLevel(); i++) {
						BlockPos pos = new BlockPos(event.getPos().getX(), i, event.getPos().getZ());
						IBlockState state = world.getBlockState(pos);
						if (isAir) {
							if (!state.getBlock().isAir(state, world, pos)) {
								// disconnection, cancel
								event.setResult(Event.Result.DENY);
								return;
							}
						} else {
							if (state.getBlock().isAir(state, world, pos)) {
								isAir = true;
							} else if (!isWater(state)) {
								// disconnection, cancel
								event.setResult(Event.Result.DENY);
								return;
							}
						}
					}

					// connection found, do not cancel
					return;
				}
			}

			// has not returned, cancel
			event.setResult(Event.Result.DENY);
			return;
		}
	}
}