package net.minecraft.src;

import java.util.Random;

import net.minecraft.src.forge.IGuiHandler;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.src.forge.NetworkMod;
import net.minecraft.src.universalelectricity.UniversalElectricity;
import net.minecraft.src.universalelectricity.components.GUIBatteryBox;
import net.minecraft.src.universalelectricity.components.UniversalComponents;

/**
 * This class is basically just a loader for Universal Components
 * @author Calclavia
 */

public class mod_UniversalElectricity extends NetworkMod implements IGuiHandler
{
	public static mod_UniversalElectricity instance;
	
	@Override
	public void load()
	{
		instance = this;
		MinecraftForge.setGuiHandler(this, this);
		UniversalElectricity.load();
		UniversalComponents.load();
		UniversalComponents.MachineRenderType = ModLoader.getUniqueBlockModelID(this, true);
		UniversalElectricity.registerAddon(this, this.getVersion());
	}
	
	@Override
	public String getVersion()
	{
		return UniversalElectricity.getVersion();
	}
	
	@Override
	public void generateSurface(World world, Random rand, int chunkX, int chunkZ)
    {
		UniversalElectricity.generateSurface(world, rand, chunkX, chunkZ);
    }
	
	@Override
	public void renderInvBlock(RenderBlocks renderBlocks, Block block, int metadata, int renderType)
	{
		UniversalComponents.renderInvBlock(renderBlocks, block, metadata, renderType);
	}
	
	@Override
	public Object getGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
	{
		return UniversalComponents.getGuiElement(ID, player, world, x, y, z);
	}
}