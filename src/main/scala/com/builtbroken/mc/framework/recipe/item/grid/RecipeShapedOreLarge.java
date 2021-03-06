package com.builtbroken.mc.framework.recipe.item.grid;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Copy of {@link ShapedOreRecipe} that allows for larger recipe sizes
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/11/2017.
 */
public class RecipeShapedOreLarge implements IRecipe
{
    public static int SIZE = 4;

    public ItemStack output = null;
    public Object[] input = null;
    public int width = 0;
    public int height = 0;

    public final String recipeString;

    public RecipeShapedOreLarge(Block result, Object... recipe)
    {
        this(new ItemStack(result), recipe);
    }

    public RecipeShapedOreLarge(Item result, Object... recipe)
    {
        this(new ItemStack(result), recipe);
    }

    public RecipeShapedOreLarge(ItemStack result, Object... recipe)
    {
        output = result.copy();

        String shape = "";
        int idx = 0;

        if (recipe[idx] instanceof String[])
        {
            String[] parts = ((String[]) recipe[idx++]);

            for (String s : parts)
            {
                width = s.length();
                shape += s;
            }

            height = parts.length;
        }
        else
        {
            while (recipe[idx] instanceof String)
            {
                String s = (String) recipe[idx++];
                shape += s;
                width = s.length();
                height++;
            }
        }

        String recipeString = "";
        for (Object tmp : recipe)
        {
            recipeString += tmp + ", ";
        }
        recipeString += output;

        this.recipeString = recipeString;

        if (width * height != shape.length())
        {
            String ret = "Invalid shaped ore large recipe:  " + recipeString;
            throw new RuntimeException(ret);
        }

        HashMap<Character, Object> itemMap = new HashMap<Character, Object>();

        for (; idx < recipe.length; idx += 2)
        {
            Character chr = (Character) recipe[idx];
            Object in = recipe[idx + 1];

            if (in instanceof ItemStack)
            {
                itemMap.put(chr, ((ItemStack) in).copy());
            }
            else if (in instanceof Item)
            {
                itemMap.put(chr, new ItemStack((Item) in));
            }
            else if (in instanceof Block)
            {
                itemMap.put(chr, new ItemStack((Block) in, 1, OreDictionary.WILDCARD_VALUE));
            }
            else if (in instanceof String)
            {
                itemMap.put(chr, OreDictionary.getOres((String) in));
            }
            else
            {
                String ret = "Invalid shaped ore recipe: ";
                for (Object tmp : recipe)
                {
                    ret += tmp + ", ";
                }
                ret += output;
                throw new RuntimeException(ret);
            }
        }

        input = new Object[width * height];
        int x = 0;
        for (char chr : shape.toCharArray())
        {
            input[x++] = itemMap.get(chr);
        }
    }

    /**
     * Returns an Item that is the result of this recipe
     */
    @Override
    public ItemStack getCraftingResult(InventoryCrafting var1)
    {
        return output.copy();
    }

    /**
     * Returns the size of the recipe area
     */
    @Override
    public int getRecipeSize()
    {
        return input.length;
    }

    @Override
    public ItemStack getRecipeOutput()
    {
        return output;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(InventoryCrafting inv, World world)
    {
        for (int x = 0; x <= SIZE - width; x++)
        {
            for (int y = 0; y <= SIZE - height; ++y)
            {
                if (checkMatch(inv, x, y, false))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean checkMatch(InventoryCrafting inv, int startX, int startY, boolean mirror)
    {
        for (int x = 0; x < SIZE; x++)
        {
            for (int y = 0; y < SIZE; y++)
            {
                int subX = x - startX;
                int subY = y - startY;
                Object target = null;

                if (subX >= 0 && subY >= 0 && subX < width && subY < height)
                {
                    if (mirror)
                    {
                        target = input[width - subX - 1 + subY * width];
                    }
                    else
                    {
                        target = input[subX + subY * width];
                    }
                }

                ItemStack slot = inv.getStackInRowAndColumn(x, y);

                if (target instanceof ItemStack)
                {
                    if (!OreDictionary.itemMatches((ItemStack) target, slot, false))
                    {
                        return false;
                    }
                }
                else if (target instanceof ArrayList)
                {
                    boolean matched = false;

                    Iterator<ItemStack> itr = ((ArrayList<ItemStack>) target).iterator();
                    while (itr.hasNext() && !matched)
                    {
                        matched = OreDictionary.itemMatches(itr.next(), slot, false);
                    }

                    if (!matched)
                    {
                        return false;
                    }
                }
                else if (target == null && slot != null)
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns the input for this recipe, any mod accessing this value should never
     * manipulate the values in this array as it will effect the recipe itself.
     *
     * @return The recipes input vales.
     */
    public Object[] getInput()
    {
        return this.input;
    }

    @Override
    public String toString()
    {
        return "RecipeShapedOreLarge[ " + recipeString + "]@" + hashCode();
    }
}
