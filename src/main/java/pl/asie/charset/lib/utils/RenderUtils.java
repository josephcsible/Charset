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
 */

package pl.asie.charset.lib.utils;

import com.google.common.base.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;
import pl.asie.charset.ModCharset;
import pl.asie.charset.lib.render.CharsetFaceBakery;

import javax.annotation.Nonnull;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class RenderUtils {
	public static final CharsetFaceBakery BAKERY = new CharsetFaceBakery();
	public static final Function<ResourceLocation, TextureAtlasSprite> textureGetter = ModelLoader.defaultTextureGetter();

	public enum AveragingMode {
		FULL,
		H_EDGES_ONLY,
		V_EDGES_ONLY,
	};

	private static RenderItem renderItem;

	private RenderUtils() {

	}

	public static BakedQuad createQuad(Vector3f from, Vector3f to, @Nonnull EnumFacing facing,
									   TextureAtlasSprite sprite, int tintIndex) {
		Vector3f fFrom = new Vector3f(from);
		Vector3f fTo = new Vector3f(to);
		EnumFacing.AxisDirection facingDir = facing.getAxisDirection();
		switch (facing.getAxis()) {
			case X:
				fFrom.x = fTo.x = facingDir == EnumFacing.AxisDirection.POSITIVE ? to.x : from.x;
				break;
			case Y:
				fFrom.y = fTo.y = facingDir == EnumFacing.AxisDirection.POSITIVE ? to.y : from.y;
				break;
			case Z:
				fFrom.z = fTo.z = facingDir == EnumFacing.AxisDirection.POSITIVE ? to.z : from.z;
				break;
		}

		return BAKERY.makeBakedQuad(fFrom, fTo, tintIndex, sprite, facing, ModelRotation.X0_Y0, true);
	}

	public static int getAverageColor(TextureAtlasSprite sprite, AveragingMode mode) {
		int pixelCount = 0;
		int[] data = sprite.getFrameTextureData(0)[0];
		int[] avgColor = new int[3];
		switch (mode) {
			case FULL:
				pixelCount = sprite.getIconHeight() * sprite.getIconWidth();
				for (int j = 0; j < sprite.getIconHeight(); j++) {
					for (int i = 0; i < sprite.getIconWidth(); i++) {
						int c = data[j * sprite.getIconWidth() + i];
						avgColor[0] += (c & 0xFF);
						avgColor[1] += ((c >> 8) & 0xFF);
						avgColor[2] += ((c >> 16) & 0xFF);
					}
				}
				break;
			case H_EDGES_ONLY:
				pixelCount = sprite.getIconHeight() * 2;
				for (int j = 0; j < 2; j++) {
					for (int i = 0; i < sprite.getIconHeight(); i++) {
						int c = data[i * sprite.getIconWidth() + (j > 0 ? sprite.getIconWidth() - 1 : 0)];
						avgColor[0] += (c & 0xFF);
						avgColor[1] += ((c >> 8) & 0xFF);
						avgColor[2] += ((c >> 16) & 0xFF);
					}
				}
				break;
			case V_EDGES_ONLY:
				pixelCount = sprite.getIconWidth() * 2;
				for (int j = 0; j < 2; j++) {
					for (int i = 0; i < sprite.getIconWidth(); i++) {
						int c = data[j > 0 ? (data.length - 1 - i) : i];
						avgColor[0] += (c & 0xFF);
						avgColor[1] += ((c >> 8) & 0xFF);
						avgColor[2] += ((c >> 16) & 0xFF);
					}
				}
				break;
		}
		for (int i = 0; i < 3; i++) {
			avgColor[i] = (avgColor[i] / pixelCount) & 0xFF;
		}
		return 0xFF000000 | avgColor[0] | (avgColor[1] << 8) | (avgColor[2] << 16);
	}

	public static BufferedImage getTextureImage(ResourceLocation location) {
		Minecraft mc = Minecraft.getMinecraft();
		/* TextureAtlasSprite sprite = mc.getTextureMapBlocks().getTextureExtry(location.toString());
		if (sprite != null) {
			int[][] dataM = sprite.getFrameTextureData(0);
			if (dataM != null && dataM.length > 0) {
				int[] data = dataM[0];
				BufferedImage image = new BufferedImage(sprite.getIconWidth(), sprite.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
				image.setRGB(0, 0, image.getWidth(), image.getHeight(), data, 0, image.getWidth());
				return image;
			}
		} */

		try {
			ResourceLocation pngLocation = new ResourceLocation(location.getResourceDomain(), String.format("%s/%s%s", new Object[] {"textures", location.getResourcePath(), ".png"}));
			IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(pngLocation);
			return TextureUtil.readBufferedImage(resource.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static TextureAtlasSprite getItemSprite(ItemStack stack) {
		if (renderItem == null) {
			renderItem = Minecraft.getMinecraft().getRenderItem();
		}

		return renderItem.getItemModelWithOverrides(stack, null, null).getParticleTexture();
	}

	public static boolean isBuiltInRenderer(World world, ItemStack stack) {
		if (renderItem == null) {
			renderItem = Minecraft.getMinecraft().getRenderItem();
		}

		IBakedModel model = renderItem.getItemModelWithOverrides(stack, world, null);
		return model != null && model.isBuiltInRenderer();
	}

	public static void glColor(int color) {
		GlStateManager.color((((color >> 16) & 0xFF) / 255.0f), (((color >> 8) & 0xFF) / 255.0f), ((color & 0xFF) / 255.0f));
	}

	public static float[] calculateUV(Vector3f from, Vector3f to, EnumFacing facing1) {
		EnumFacing facing = facing1;
		if (facing == null) {
			if (from.y == to.y) {
				facing = EnumFacing.UP;
			} else if (from.x == to.x) {
				facing = EnumFacing.EAST;
			} else if (from.z == to.z) {
				facing = EnumFacing.SOUTH;
			} else {
				return null; // !?
			}
		}

		switch (facing) {
			case DOWN:
				return new float[] {from.x, 16.0F - to.z, to.x, 16.0F - from.z};
			case UP:
				return new float[] {from.x, from.z, to.x, to.z};
			case NORTH:
				return new float[] {16.0F - to.x, 16.0F - to.y, 16.0F - from.x, 16.0F - from.y};
			case SOUTH:
				return new float[] {from.x, 16.0F - to.y, to.x, 16.0F - from.y};
			case WEST:
				return new float[] {from.z, 16.0F - to.y, to.z, 16.0F - from.y};
			case EAST:
				return new float[] {16.0F - to.z, 16.0F - to.y, 16.0F - from.z, 16.0F - from.y};
		}

		return null;
	}

	public static IModel getModel(ResourceLocation location) {
		try {
			IModel model = ModelLoaderRegistry.getModel(location);
			if (model == null) {
				ModCharset.logger.error("Model " + location.toString() + " is missing! THIS WILL CAUSE A CRASH!");
			}
			return model;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static int getSelectionMask(int y, int x, int z) {
		return 1 << (y * 4 + x * 2 + z);
	}

	private static void drawLine(VertexBuffer worldrenderer, Tessellator tessellator, double x1, double y1, double z1, double x2, double y2, double z2) {
		worldrenderer.pos(x1, y1, z1).endVertex();
		worldrenderer.pos(x2, y2, z2).endVertex();
	}

	public static int getSelectionMask(EnumFacing face) {
		int lineMask = 0;
		switch (face) {
			case DOWN:
				return 0x00F;
			case UP:
				return 0xF00;
			case NORTH:
				lineMask |= getSelectionMask(1, 0, 0);
				lineMask |= getSelectionMask(1, 1, 0);
				lineMask |= getSelectionMask(0, 0, 0);
				lineMask |= getSelectionMask(2, 0, 0);
				return lineMask;
			case SOUTH:
				lineMask |= getSelectionMask(1, 0, 1);
				lineMask |= getSelectionMask(1, 1, 1);
				lineMask |= getSelectionMask(0, 0, 1);
				lineMask |= getSelectionMask(2, 0, 1);
				return lineMask;
			case WEST:
				lineMask |= getSelectionMask(1, 0, 0);
				lineMask |= getSelectionMask(1, 0, 1);
				lineMask |= getSelectionMask(0, 1, 0);
				lineMask |= getSelectionMask(2, 1, 0);
				return lineMask;
			case EAST:
				lineMask |= getSelectionMask(1, 1, 0);
				lineMask |= getSelectionMask(1, 1, 1);
				lineMask |= getSelectionMask(0, 1, 1);
				lineMask |= getSelectionMask(2, 1, 1);
				return lineMask;
		}
		return lineMask;
	}

	public static void drawSelectionBoundingBox(AxisAlignedBB box, int lineMask) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();

		double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
		double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
		double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

		AxisAlignedBB boundingBox = box.expandXyz(0.0020000000949949026D).offset(-d0, -d1, -d2);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
		GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
		GL11.glLineWidth(2.0F);
		GlStateManager.disableTexture2D();
		GlStateManager.depthMask(false);

		Tessellator tessellator = Tessellator.getInstance();
		VertexBuffer worldrenderer = tessellator.getBuffer();
		worldrenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
		if ((lineMask & getSelectionMask(0, 0, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.minY, boundingBox.minZ,
					boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
		}
		if ((lineMask & getSelectionMask(0, 0, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.minY, boundingBox.maxZ,
					boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(0, 1, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.minY, boundingBox.minZ,
					boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(0, 1, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.maxX, boundingBox.minY, boundingBox.minZ,
					boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(1, 0, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.minY, boundingBox.minZ,
					boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
		}
		if ((lineMask & getSelectionMask(1, 0, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.minY, boundingBox.maxZ,
					boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(1, 1, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.maxX, boundingBox.minY, boundingBox.minZ,
					boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		}
		if ((lineMask & getSelectionMask(1, 1, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.maxX, boundingBox.minY, boundingBox.maxZ,
					boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(2, 0, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.maxY, boundingBox.minZ,
					boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
		}
		if ((lineMask & getSelectionMask(2, 0, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.maxY, boundingBox.maxZ,
					boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(2, 1, 0)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.minX, boundingBox.maxY, boundingBox.minZ,
					boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
		}
		if ((lineMask & getSelectionMask(2, 1, 1)) != 0) {
			drawLine(worldrenderer, tessellator,
					boundingBox.maxX, boundingBox.maxY, boundingBox.minZ,
					boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
		}
		tessellator.draw();

		GlStateManager.depthMask(true);
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
	}

	public static int multiplyColor(int src, int dst) {
		int out = 0;
		for (int i = 0; i < 32; i += 8) {
			out |= ((((src >> i) & 0xFF) * ((dst >> i) & 0xFF) / 0xFF) & 0xFF) << i;
		}
		return out;
	}
}
