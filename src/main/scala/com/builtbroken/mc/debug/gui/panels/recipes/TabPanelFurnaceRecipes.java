package com.builtbroken.mc.debug.gui.panels.recipes;

import com.builtbroken.mc.debug.data.IJsonDebugData;
import com.builtbroken.mc.debug.gui.panels.imp.PanelDataList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.StatCollector;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 9/15/2017.
 */
public class TabPanelFurnaceRecipes extends PanelDataList<TabPanelFurnaceRecipes.FurnaceData>
{
    @Override
    protected IJsonDebugData getDataEntryFor(FurnaceData object)
    {
        return object;
    }

    @Override
    protected void buildData()
    {
        data.clear();
        Map<ItemStack, ItemStack> map = FurnaceRecipes.smelting().getSmeltingList();
        for (Map.Entry<ItemStack, ItemStack> entry : map.entrySet())
        {
            data.add(new FurnaceData(entry.getKey(), entry.getValue()));
        }
        Collections.sort(data, new FurnaceDataSorter());
    }

    public static class FurnaceData implements IJsonDebugData
    {
        ItemStack input;
        ItemStack output;
        float xp;

        public FurnaceData(ItemStack in, ItemStack out)
        {
            this.input = in;
            this.output = out;
        }

        @Override
        public String buildDebugLineDisplay()
        {
            return asString(input) + "  >>>  " + asString(output); //TODO add way to highlight red if bad values exist
        }

        protected String asString(ItemStack stack)
        {
            if (stack != null)
            {
                if (stack.getItem() != null)
                {
                    final String name = Item.itemRegistry.getNameForObject(stack.getItem()).split(":")[0];
                    return "[" + name + " : " + getName(stack) + "]";
                }
                return "null item";
            }
            return "null stack";
        }

        public String getMod()
        {
            return getMod(input);
        }

        public String getMod(ItemStack stack)
        {
            if(stack != null && stack.getItem() != null)
            {
                String regName = Item.itemRegistry.getNameForObject(stack.getItem());
                if(regName != null)
                {
                    return regName.split(":")[0];
                }
            }
            return "null";
        }

        public String getName()
        {
            return getName(input);
        }

        public String getName(ItemStack stack)
        {
            final String regName = Item.itemRegistry.getNameForObject(stack.getItem());
            final String name = regName.split(":")[1];

            for (String value : new String[]{stack.getDisplayName(), stack.getUnlocalizedName(), name, StatCollector.translateToLocal(stack.getItem().getUnlocalizedName()) + " " + stack.getItemDamage()})
            {
                if (value != null)
                {
                    value = value.trim();
                    if (!value.isEmpty() && !value.toLowerCase().startsWith("null"))
                    {
                        return value;
                    }
                }
            }
            return stack.getItem().getUnlocalizedName() + "@" + stack.getItemDamage();
        }
    }

    public static class FurnaceDataSorter implements Comparator<FurnaceData>
    {
        @Override
        public int compare(FurnaceData o1, FurnaceData o2)
        {
            String regName1 = Item.itemRegistry.getNameForObject(o1.input.getItem());
            String regName2 = Item.itemRegistry.getNameForObject(o2.input.getItem());

            if (regName1 == null)
            {
                return -1;
            }
            else if (regName2 == null)
            {
                return 1;
            }

            int result = compareString(o1.getMod(), o2.getMod());
            if (result == 0)
            {
                return compareString(o1.getName(), o2.getName());
            }
            return result;
        }

        protected int compareString(String o1, String o2)
        {
            if (o1.equalsIgnoreCase(o2))
            {
                return 0;
            }
            else if (o1 == null)
            {
                return -1;
            }
            else if (o2 == null)
            {
                return 1;
            }

            int l = o1.length() < o2.length() ? o1.length() : o2.length();
            for (int i = 0; i < l; i++)
            {
                char c = o1.charAt(i);
                char c2 = o2.charAt(i);
                int result = Character.compare(c, c2);
                if (result != 0)
                {
                    return result;
                }
            }
            return Integer.compare(o1.length(), o2.length());
        }
    }
}
