package resonant.content;

import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import resonant.content.prefab.scala.render.ISimpleItemRenderer;
import resonant.content.spatial.block.SpatialBlock;
import resonant.content.wrapper.BlockDummy;
import resonant.content.wrapper.ItemRenderHandler;
import resonant.lib.utility.LanguageUtility;

import java.util.Map.Entry;
import java.util.WeakHashMap;

/**
 * Handler to make registering all parts of a mod's objects that are loaded into the game by forge.
 *
 * @author DarkGuardsman, Calclavia
 */
public class ModManager
{
	@SidedProxy(clientSide = "resonant.content.ClientRegistryProxy", serverSide = "resonant.content.CommonRegistryProxy")
	public static CommonRegistryProxy proxy;

	public final WeakHashMap<Block, String> blocks = new WeakHashMap();
	public final WeakHashMap<Item, String> items = new WeakHashMap();
	private final Configuration config;
	private final String modID;

	private String modPrefix;
	private CreativeTabs defaultTab;

	/**
	 * Custom unique packet IDs for the mod to use.
	 */
	private int packetID = 0;

	public ModManager(Configuration config, String modID)
	{
		this.config = config;
		this.modID = modID;
	}

	public ModManager setPrefix(String modPrefix)
	{
		this.modPrefix = modPrefix;
		return this;
	}

	public ModManager setTab(CreativeTabs defaultTab)
	{
		this.defaultTab = defaultTab;
		return this;
	}

	public int getNextPacketID()
	{
		return ++packetID;
	}

	/**
	 * New SpatialBlocks system.
	 */
	public BlockDummy newBlock(Class<? extends SpatialBlock> spatialClass)
	{
		try
		{
			SpatialBlock tileBlock = spatialClass.newInstance();
			final String name = tileBlock.name();

			BlockDummy block = new BlockDummy(modPrefix, defaultTab, tileBlock);
			tileBlock.setBlock(block);

			blocks.put(block, name);
			proxy.registerBlock(block, tileBlock.itemBlock(), name, modID);

			tileBlock.onInstantiate();

			if (tileBlock instanceof ISimpleItemRenderer)
			{
				ItemRenderHandler.register(new ItemStack(block).getItem(), (ISimpleItemRenderer) tileBlock);
			}

			if (tileBlock.tile() != null)
			{
				proxy.registerTileEntity(name, tileBlock.tile().getClass());

				if (!tileBlock.normalRender())
				{
					proxy.registerDummyRenderer(tileBlock.tile().getClass());
				}
			}

			return block;
		}
		catch (Exception e)
		{
			throw new RuntimeException("Block [" + spatialClass.getSimpleName() + "] failed to be created:", e);
		}
	}
	/**
	 @Deprecated public Block createBlock(Class<? extends Block> blockClass)
	 {
	 return createBlock(blockClass, ItemBlockTooltip.class);
	 }

	 @Deprecated public Block createTile(Class<? extends Block> blockClass, Class<? extends TileEntity> tileClass)
	 {
	 return createBlock(blockClass, ItemBlockTooltip.class, tileClass);
	 }

	 @Deprecated public Block createBlock(Class<? extends Block> blockClass, Class<? extends ItemBlock> itemClass)
	 {
	 return createBlock(blockClass, itemClass, null);
	 }

	 @Deprecated public Block createBlock(Class<? extends Block> blockClass, Class<? extends ItemBlock> itemClass, Class<? extends TileEntity> tileClass)
	 {
	 return createBlock(LanguageUtility.decapitalizeFirst(blockClass.getSimpleName().replace("Block", "")), blockClass, itemClass, tileClass);
	 }

	 @Deprecated public Block createBlock(String name, Class<? extends Block> blockClass, Class<? extends ItemBlock> itemClass, Class<? extends TileEntity> tileClass)
	 {
	 return createBlock(name, blockClass, itemClass, tileClass, false);
	 }

	 /**
	  * Generates a block using reflection, and runs it threw config checks
	  *
	  * @param name       - name to register the block with
	 * @param tileClass  - the tile class to register this block to
	 * @param blockClass - class to generate the instance from
	 * @param canDisable - should we allow the player the option to disable the block
	 * @param itemClass  - item block to register with the block

	 @Deprecated public Block createBlock(String name, Class<? extends Block> blockClass, Class<? extends ItemBlock> itemClass, Class<? extends TileEntity> tileClass, boolean canDisable)
	 {
	 Block block = null;

	 if (blockClass != null && (!canDisable || (canDisable && config.get("Enabled_List", "Enabled " + name, true).getBoolean(true))))
	 {
	 try
	 {

	 //                int assignedID = idManager.getNextBlockID();
	 //                int actualID = config.getBlock(name, assignedID).getInt(assignedID);
	 block = blockClass.getConstructor().newInstance();

	 if (block != null)
	 {
	 if (modPrefix != null)
	 {
	 block.setBlockName(modPrefix + name);

	 if (ReflectionHelper.getPrivateValue(Block.class, block, "textureName", "field_111026_f") == null)
	 {
	 block.setBlockTextureName(modPrefix + name);
	 }
	 }

	 if (defaultTab != null)
	 {
	 block.setCreativeTab(defaultTab);
	 }

	 blocks.put(block, name);
	 proxy.registerBlock(block, itemClass, name, modID);
	 finishCreation(block, tileClass);
	 }
	 }
	 catch (IllegalArgumentException e)
	 {
	 throw e;
	 }
	 catch (Exception e)
	 {
	 throw new RuntimeException("Block [" + name + "] failed to be created:", e);
	 }
	 }

	 return block;
	 }

	 /**
	  * Finishes the creation of the block loading config files and tile entities
	  *
	  * @param tileClass
	 * @throws ClassNotFoundException

	 @Deprecated public void finishCreation(Block block, Class<? extends TileEntity> tileClass) throws ClassNotFoundException
	 {
	 BlockInfo blockInfo = block.getClass().getAnnotation(BlockInfo.class);

	 if (blockInfo != null)
	 {
	 for (String string : blockInfo.tileEntity())
	 {
	 Class clazz = Class.forName(string);
	 proxy.registerTileEntity(clazz.getName(), clazz);
	 }

	 }
	 // TODO Remove this and transfer to @BlockInfo
	 if (tileClass != null)
	 {
	 proxy.registerTileEntity(block.getUnlocalizedName(), tileClass);
	 }
	 }*/

	/**
	 * Method to get block via name
	 *
	 * @param blockName
	 * @return Block requested
	 */
	public Block getBlock(String blockName)
	{
		for (Entry<Block, String> entry : blocks.entrySet())
		{
			String name = entry.getKey().getUnlocalizedName().replace("tile.", "");
			if (name.equalsIgnoreCase(blockName))
			{
				return entry.getKey();
			}
		}
		return null;
	}

	public Item newItem(Class<? extends Item> clazz)
	{
		return newItem(LanguageUtility.decapitalizeFirst(clazz.getSimpleName().replace("Item", "")), clazz);
	}

	/**
	 * Creates a new item using reflection as well runs it threw some check to activate any
	 * interface methods
	 *
	 * @param name  - name to register the item with //@param modid - mods that the item comes from
	 * @param clazz - item class
	 * @return the new item
	 */
	public <C extends Item> C newItem(String name, Class<C> clazz)
	{

		try
		{
			Item item = clazz.getConstructor().newInstance();

			if (item != null)
			{
				if (modPrefix != null)
				{
					item.setUnlocalizedName(modPrefix + name);

					if (ReflectionHelper.getPrivateValue(Item.class, item, "iconString", "field_111218_cA") == null)
					{
						item.setTextureName(modPrefix + name);
					}
				}

				if (defaultTab != null)
				{
					item.setCreativeTab(defaultTab);
				}

				items.put(item, name);
				GameRegistry.registerItem(item, name, modID);
			}

			return (C) item;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Item [" + name + "] failed to be created: " + e.getLocalizedMessage(), e.fillInStackTrace());
		}
	}
}