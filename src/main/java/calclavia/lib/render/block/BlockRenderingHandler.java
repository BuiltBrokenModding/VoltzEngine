package calclavia.lib.render.block;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Maps;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class BlockRenderingHandler implements ISimpleBlockRenderingHandler
{
	/**
	 * Maps fake TileEntities
	 */
	public static final Map<Block, TileEntity> inventoryTileEntities = Maps.newIdentityHashMap();

	public TileEntity getTileEntityForBlock(Block block)
	{
		TileEntity te = inventoryTileEntities.get(block);
		if (te == null)
		{
			te = block.createTileEntity(Minecraft.getMinecraft().theWorld, 0);
			inventoryTileEntities.put(block, te);
		}
		return te;
	}

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer)
	{
		if (block instanceof ICustomBlockRender)
		{
			((ICustomBlockRender) block).renderInventory(block, metadata, modelID, renderer);
			return;
		}

		TileEntity renderTile = null;

		if (block.hasTileEntity(metadata))
		{
			renderTile = getTileEntityForBlock(block);
		}

		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);

		if (renderTile != null)
		{
			GL11.glPushAttrib(GL11.GL_TEXTURE_BIT);
			GL11.glPushMatrix();
			GL11.glTranslated(-0.5, -0.5, -0.5);
			TileEntityRenderer.instance.renderTileEntityAt(renderTile, 0, 0, 0, 0);
			GL11.glPopMatrix();
			GL11.glPopAttrib();
		}

	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer)
	{
		if (block instanceof ICustomBlockRender)
		{
			return ((ICustomBlockRender) block).renderStatic(world, x, y, z, block, modelId, renderer);
		}

		return true;
	}

	@Override
	public boolean shouldRender3DInInventory()
	{
		return true;
	}
}