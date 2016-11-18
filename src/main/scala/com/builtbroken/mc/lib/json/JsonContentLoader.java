package com.builtbroken.mc.lib.json;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.core.registry.implement.IPostInit;
import com.builtbroken.mc.core.registry.implement.IRecipeContainer;
import com.builtbroken.mc.core.registry.implement.IRegistryInit;
import com.builtbroken.mc.lib.json.block.processor.JsonBlockProcessor;
import com.builtbroken.mc.lib.json.block.processor.JsonBlockSmeltingProcessor;
import com.builtbroken.mc.lib.json.block.processor.JsonBlockWorldGenProcessor;
import com.builtbroken.mc.lib.json.imp.IJsonGenObject;
import com.builtbroken.mc.lib.json.processors.JsonProcessor;
import com.builtbroken.mc.lib.mod.loadable.AbstractLoadable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import cpw.mods.fml.common.registry.GameRegistry;
import li.cil.oc.common.block.Item;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public final class JsonContentLoader extends AbstractLoadable
{
    /** Processor instance */
    public static JsonContentLoader INSTANCE = new JsonContentLoader();

    /** Internal files for loading */
    private final List<String> classPathResources = new ArrayList();
    /** External files for loading */
    private final List<File> externalFiles = new ArrayList();
    /** External jar files for loading */
    private final List<File> externalJarFiles = new ArrayList();

    /** Extensions that can be loaded by the system, defaults with .json */
    private final List<String> extensionsToLoad = new ArrayList();

    /** List of processors to handle files */
    private final List<JsonProcessor> processors = new ArrayList();
    /** List of objects generated by processors */
    private final List<IJsonGenObject> generatedObjects = new ArrayList();

    /** Block processor */
    public final JsonBlockProcessor blockProcessor;


    public File externalContentFolder;

    public JsonContentLoader()
    {
        extensionsToLoad.add("json");
        blockProcessor = new JsonBlockProcessor();
    }

    /**
     * Adds the processor to the list of processors
     *
     * @param processor
     */
    public static void registerProcessor(JsonProcessor processor)
    {
        INSTANCE.processors.add(processor);
    }

    @Override
    public void preInit()
    {
        //Init data
        externalContentFolder = new File(References.BBM_CONFIG_FOLDER, "json");
        //Validate data
        validateFilePaths();

        //Load processors
        processors.add(blockProcessor);
        blockProcessor.addSubProcessor("smeltingRecipe", new JsonBlockSmeltingProcessor());
        blockProcessor.addSubProcessor("worldGenerator", new JsonBlockWorldGenProcessor());
        //TODO add crafting recipes
        //TODO add entities
        //TODO add machine recipes
    }

    @Override
    public void init()
    {
        //Get a map of the index values for sorting loaded entries

        //Collect all entries to sort
        final ArrayList<String> processorKeys = new ArrayList();
        for (JsonProcessor processor : processors)
        {
            String jsonKey = processor.getJsonKey();
            processorKeys.add(jsonKey);
        }
        //Sort entries
        final Map<String, Integer> sortingIndexMap = sortSortingValues(processorKeys);

        //Load resources from file system
        final List<JsonEntry> jsonEntries = loadResources();

        //Sorting
        final JsonEntryComparator comparator = new JsonEntryComparator(sortingIndexMap);
        Collections.sort(jsonEntries, comparator);

        //Process all loaded elements
        for (final JsonEntry entry : jsonEntries)
        {
            try
            {
                process(entry.jsonKey, entry.element);
            }
            catch (Exception e)
            {
                //TODO figure out who made the file
                //Crash as the file may be important
                throw new RuntimeException("Failed to process entry from file " + entry.fileReadFrom + ". Make corrections to the file or contact the file's creator for the issue to be fixed.\n  Entry = " + entry, e);
            }
        }
    }

    /** Validates file paths and makes folders as needed */
    public void validateFilePaths()
    {
        if (!externalContentFolder.exists())
        {
            externalContentFolder.mkdirs();
        }
    }

    /** Loads resources from folders and class path */
    public List<JsonEntry> loadResources()
    {
        //TODO implement threading to allow other mods to load while we load content
        loadResourcesFromFolder(externalContentFolder);
        loadResourcesFromPackage("content/");

        final List<JsonEntry> elements = new ArrayList();
        //Load external files
        for (File file : externalFiles)
        {
            try
            {
                loadJsonFile(file, elements);
            }
            catch (IOException e)
            {
                //Crash as the file may be important
                throw new RuntimeException("Failed to load resource " + file, e);
            }
        }

        //Load internal files
        for (String resource : classPathResources)
        {
            try
            {
                loadJsonFileFromResources(resource, elements);
            }
            catch (IOException e)
            {
                //Crash as the file may be important
                throw new RuntimeException("Failed to load resource " + resource, e);
            }
        }
        return elements;
    }

    /**
     * Creates a map of entries used for sorting loaded files later
     *
     * @param sortingValues - values to sort, entries in list are consumed
     * @return Map of keys to sorting index values
     */
    public static Map<String, Integer> sortSortingValues(List<String> sortingValues)
    {
        //Run a basic sorter on the list to order it values, after:value, before:value:
        Collections.sort(sortingValues, new StringSortingComparator());


        final LinkedList<String> sortedValues = new LinkedList();
        while (!sortingValues.isEmpty())
        {
            //Sort out list
            sortSortingValues(sortingValues, sortedValues);

            //Exit point, prevents inf loop by removing bad entries and adding them to the end of the sorting list
            if (!sortingValues.isEmpty())
            {
                //Loop threw what entries we have left
                final Iterator<String> it = sortingValues.iterator();
                while (it.hasNext())
                {
                    final String entry = it.next();
                    if (entry.contains("@"))
                    {
                        String[] split = entry.split("@");
                        final String name = entry.split("@")[0];

                        if (split[1].contains(":"))
                        {
                            split = split[1].split(":");
                            boolean found = false;

                            //Try too see if we have a valid entry left in our sorting list that might just contain a after: or before: preventing it from adding
                            for (final String v : sortingValues)
                            {
                                if (!v.equals(entry) && v.contains(split[1]))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            for (final String v : sortedValues)
                            {
                                if (!v.equals(entry) && v.contains(split[1]))
                                {
                                    found = true;
                                    break;
                                }
                            }

                            //If we have no category for the sorting entry add it to the master list
                            if (!found)
                            {
                                Engine.logger().error("Bad sorting value for " + entry + " could not find category for " + split[1]);
                                sortedValues.add(name);
                                it.remove();
                            }
                        }
                        //If entry is invalid add it
                        else
                        {
                            Engine.logger().error("Bad sorting value for " + entry + " has no valid sorting data");
                            sortedValues.add(name);
                            it.remove();
                        }
                    }
                    //Should never happen as entries with no sorting value should be added before here
                    else
                    {
                        sortedValues.add(entry);
                        it.remove();
                    }
                }
            }
        }

        final Map<String, Integer> map = new HashMap();
        int i = 0;
        for (String s : sortedValues)
        {
            map.put(s, i);
            i++;
        }
        return map;
    }

    /**
     * Sorts the string values that will later be used as an index value
     * to sort all .json files being processed
     *
     * @param sortingValues - list of unsorted values
     * @param sortedValues  - list of sorted values and where values will be inserted into
     */
    public static void sortSortingValues(List<String> sortingValues, List<String> sortedValues)
    {
        Iterator<String> it = sortingValues.iterator();
        while (it.hasNext())
        {
            String entry = it.next();
            if (entry.contains("@"))
            {
                String[] split = entry.split("@");
                String name = split[0];
                String sortValue = split[1];
                //TODO add support for ; allowing sorting of several values

                if (sortValue.contains(":"))
                {
                    split = sortValue.split(":");
                    String prefix = split[0];
                    String cat = split[1];
                    boolean catFound = false;

                    ListIterator<String> sortedIt = sortedValues.listIterator();
                    while (sortedIt.hasNext())
                    {
                        String v = sortedIt.next();
                        if (v.equalsIgnoreCase(cat))
                        {
                            catFound = true;
                            if (prefix.equalsIgnoreCase("after"))
                            {
                                sortedIt.add(name);
                            }
                            else if (prefix.equalsIgnoreCase("before"))
                            {
                                sortedIt.previous();
                                sortedIt.add(name);
                            }
                            else
                            {
                                Engine.logger().error("Bad sorting value for " + entry + " we can only read 'after' and 'before'");
                                sortedValues.add(name);
                                it.remove();
                            }
                            break;
                        }
                    }
                    if (catFound)
                    {
                        it.remove();
                    }
                }
                else
                {
                    sortedValues.add(name);
                    it.remove();
                }
            }
            else
            {
                sortedValues.add(entry);
                it.remove();
            }
        }
    }


    public void process(String key, JsonElement element)
    {
        for (JsonProcessor processor : processors)
        {
            if (processor.canProcess(key, element))
            {
                IJsonGenObject data = processor.process(element);
                data.register();
                if (data instanceof IRegistryInit)
                {
                    ((IRegistryInit) data).onRegistered();
                }
                break;
            }
            else
            {
                //TODO add error handling
            }
        }
    }

    /**
     * Called to load json files from the folder
     *
     * @param folder
     */
    public void loadResourcesFromFolder(File folder)
    {
        for (File file : folder.listFiles())
        {
            if (file.isDirectory())
            {
                loadResourcesFromFolder(folder);
            }
            else
            {
                String extension = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length());
                if (extension.equalsIgnoreCase("jar"))
                {
                    externalJarFiles.add(file);
                }
                else if (extensionsToLoad.contains(extension))
                {
                    externalFiles.add(file);
                }
            }
        }
    }

    /**
     * Loads package
     *
     * @param folder - package your looking to load data from
     */
    public void loadResourcesFromPackage(String folder)
    {
        //http://stackoverflow.com/questions/3923129/get-a-list-of-resources-from-classpath-directory
        try
        {
            final List<String> files = IOUtils.readLines(JsonContentLoader.class.getClassLoader().getResourceAsStream(folder), Charsets.UTF_8);
            for (String name : files)
            {
                final String path = folder + (!folder.endsWith("/") ? "/" : "") + name;
                if (name.lastIndexOf(".") > 1)
                {
                    String extension = name.substring(name.lastIndexOf(".") + 1, name.length());
                    if (extensionsToLoad.contains(extension))
                    {
                        classPathResources.add(path);
                    }
                }
                else
                {
                    loadResourcesFromPackage(path + "/");
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

        }
    }

    /**
     * Loads a json file from the resource path
     *
     * @param resource - resource location
     * @return json file as a json element object
     * @throws IOException
     */
    public static void loadJsonFileFromResources(String resource, List<JsonEntry> entries) throws IOException
    {
        if (resource != null && !resource.isEmpty())
        {
            URL url = JsonContentLoader.class.getClassLoader().getResource(resource);
            if (url != null)
            {
                InputStream stream = url.openStream();
                loadJson(resource, new InputStreamReader(stream), entries);
                stream.close();
            }
        }
    }

    /**
     * Loads a json file from the resource path
     *
     * @param file - file to read from
     * @return json file as a json element object
     * @throws IOException
     */
    public static void loadJsonFile(File file, List<JsonEntry> entries) throws IOException
    {
        if (file.exists() && file.isFile())
        {
            FileReader stream = new FileReader(file);
            loadJson(file.getName(), new BufferedReader(stream), entries);
            stream.close();
        }
    }

    /**
     * Loads a json file from a reader
     *
     * @param fileName - file the reader loaded from, used only for error logs
     * @param reader   - reader with the data
     * @param entries  - place to put json entries into
     */
    public static void loadJson(String fileName, Reader reader, List<JsonEntry> entries)
    {
        JsonReader jsonReader = new JsonReader(reader);
        JsonElement element = Streams.parse(jsonReader);
        loadJsonElement(fileName, element, entries);
    }

    /**
     * Loads the data from the element passed in and creates {@link JsonEntry} for processing
     * later on.
     *
     * @param file    - file the element was read from
     * @param element - the element to process
     * @param entries - list to populate with new entries
     */
    public static void loadJsonElement(String file, JsonElement element, List<JsonEntry> entries)
    {
        if (element.isJsonObject())
        {
            JsonObject object = element.getAsJsonObject();
            String author = null;
            String helpSite = null;
            if (object.has("author"))
            {
                JsonObject authorData = object.get("author").getAsJsonObject();
                author = authorData.get("name").getAsString();
                if (authorData.has("site"))
                {
                    helpSite = authorData.get("site").getAsString();
                }
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet())
            {
                if (!entry.getKey().equalsIgnoreCase("author"))
                {
                    JsonEntry jsonEntry = new JsonEntry(entry.getKey(), file, entry.getValue());
                    jsonEntry.author = author;
                    jsonEntry.authorHelpSite = helpSite;
                    entries.add(jsonEntry);
                }
            }
        }
    }

    @Override
    public void postInit()
    {
        for (IJsonGenObject obj : generatedObjects)
        {
            if (obj instanceof IPostInit)
            {
                ((IPostInit) obj).onPostInit();
            }
            if (obj instanceof IRecipeContainer)
            {
                List<IRecipe> recipes = new ArrayList();
                ((IRecipeContainer) obj).genRecipes(recipes);
                for (IRecipe recipe : recipes)
                {
                    if (recipe != null && recipe.getRecipeOutput() != null)
                    {
                        GameRegistry.addRecipe(recipe);
                    }
                }
            }
        }
    }

    /**
     * Takes a string and attempts to convert into into a useable
     * {@link ItemStack}. Does not support NBT due to massive
     * amount of nesting and complexity that NBT can have. If
     * you want to use this try the Json version.
     *
     * @param string - simple string
     * @return ItemStack, or null
     */
    public static ItemStack fromString(String string)
    {
        //TODO move to helper as this will be reused, not just in Json
        if (string.startsWith("item[") || string.startsWith("block["))
        {
            String out = string.substring(string.indexOf("[") + 1, string.length() - 1); //ends ]
            int meta = -1;

            //Meta handling
            if (out.contains("@"))
            {
                String[] split = out.split("@");
                out = split[0];
                try
                {
                    meta = Integer.parseInt(split[1]);
                }
                catch (NumberFormatException e)
                {
                    throw new IllegalArgumentException();
                }
            }

            //Ensure domain, default to MC
            if (!out.contains(":"))
            {
                out = "minecraft:" + out;
            }
            //TODO implement short hand eg cobble - > CobbleStone

            if (string.startsWith("item["))
            {
                Object obj = Item.itemRegistry.getObject(out);
                if (obj instanceof Item)
                {
                    if (meta > -1)
                    {
                        return new ItemStack((Item) obj);
                    }
                    else
                    {
                        return new ItemStack((Item) obj, 1, meta);
                    }
                }
            }
            else
            {
                Object obj = Block.blockRegistry.getObject(out);
                if (obj instanceof Block)
                {
                    if (meta > -1)
                    {
                        return new ItemStack((Block) obj);
                    }
                    else
                    {
                        return new ItemStack((Block) obj, 1, meta);
                    }
                }
            }
        }
        //Ore Names
        else if (OreDictionary.doesOreNameExist(string))
        {
            List<ItemStack> ores = OreDictionary.getOres(string);
            for (ItemStack stack : ores)
            {
                if (stack != null)
                {
                    return stack;
                }
            }
        }
        return null;
    }

    public static ItemStack fromJson(JsonObject json)
    {
        ItemStack output = null;
        String type = json.get("type").getAsString();
        String item = json.get("item").getAsString();

        int meta = -1;
        if (json.has("meta"))
        {
            meta = json.get("meta").getAsInt();
        }

        if (type.equalsIgnoreCase("block"))
        {
            Object obj = Item.itemRegistry.getObject(item);
            if (obj instanceof Block)
            {
                if (meta > -1)
                {
                    output = new ItemStack((Block) obj);
                }
                else
                {
                    output = new ItemStack((Block) obj, 1, meta);
                }
            }
        }
        else if (type.equalsIgnoreCase("item"))
        {
            Object obj = Item.itemRegistry.getObject(item);
            if (obj instanceof Item)
            {
                if (meta > -1)
                {
                    return new ItemStack((Item) obj);
                }
                else
                {
                    return new ItemStack((Item) obj, 1, meta);
                }
            }
        }
        else if (type.equalsIgnoreCase("dict"))
        {
            List<ItemStack> ores = OreDictionary.getOres(item);
            for (ItemStack stack : ores)
            {
                if (stack != null)
                {
                    output = stack;
                    break;
                }
            }
        }

        if (output != null && json.has("nbt"))
        {
            NBTTagCompound tag = new NBTTagCompound();
            processNBTTagCompound(json.getAsJsonObject("nbt"), tag);
        }
        return output;
    }

    /**
     * Loads NBT data from a json object
     *
     * @param json - json object, converted to entry set
     * @param tag  - tag to save the data to
     */
    public static void processNBTTagCompound(JsonObject json, NBTTagCompound tag)
    {
        for (Map.Entry<String, JsonElement> entry : json.entrySet())
        {
            if (entry.getValue().isJsonPrimitive())
            {
                JsonPrimitive primitive = entry.getValue().getAsJsonPrimitive();
                if (primitive.isBoolean())
                {
                    tag.setBoolean(entry.getKey(), primitive.getAsBoolean());
                }
                else if (primitive.isNumber())
                {
                    tag.setInteger(entry.getKey(), primitive.getAsInt());
                }
                else if (primitive.isString())
                {
                    tag.setString(entry.getKey(), primitive.getAsString());
                }
            }
            else if (entry.getValue().isJsonObject())
            {
                JsonObject object = entry.getValue().getAsJsonObject();
                if (object.has("type"))
                {
                    String type = object.get("type").getAsString();
                    if (type.equalsIgnoreCase("tagCompound"))
                    {
                        NBTTagCompound nbt = new NBTTagCompound();
                        processNBTTagCompound(object, nbt);
                        tag.setTag(entry.getKey(), nbt);
                    }
                    else if (type.equalsIgnoreCase("int"))
                    {
                        tag.setInteger(entry.getKey(), entry.getValue().getAsInt());
                    }
                    else if (type.equalsIgnoreCase("double"))
                    {
                        tag.setDouble(entry.getKey(), entry.getValue().getAsDouble());
                    }
                    else if (type.equalsIgnoreCase("float"))
                    {
                        tag.setFloat(entry.getKey(), entry.getValue().getAsFloat());
                    }
                    else if (type.equalsIgnoreCase("byte"))
                    {
                        tag.setByte(entry.getKey(), entry.getValue().getAsByte());
                    }
                    else if (type.equalsIgnoreCase("short"))
                    {
                        tag.setShort(entry.getKey(), entry.getValue().getAsShort());
                    }
                    else if (type.equalsIgnoreCase("long"))
                    {
                        tag.setLong(entry.getKey(), entry.getValue().getAsLong());
                    }
                    //TODO add byte array
                    //TODO add int array
                    //TODO add tag list
                }
                else
                {
                    NBTTagCompound nbt = new NBTTagCompound();
                    processNBTTagCompound(object, nbt);
                    tag.setTag(entry.getKey(), nbt);
                }
            }
        }
    }

    /**
     * Simple pre-sorter that attempt to place tagged string near the bottom
     * so they are added after tags they depend on.
     */
    public static class StringSortingComparator implements Comparator<String>
    {
        @Override
        public int compare(String o1, String o2)
        {
            if (o1.contains("@") && !o2.contains("@"))
            {
                return 1;
            }
            else if (!o1.contains("@") && o2.contains("@"))
            {
                return -1;
            }
            //TODO attempt to sort using before & after tags
            return o1.compareTo(o2);
        }
    }

    /**
     * Used to store loaded entries during sorting
     */
    public static class JsonEntry
    {
        /** Name of the entry type, used for sorting */
        public final String jsonKey;
        /** Element entry that goes with the name key */
        public final JsonElement element;
        /** File the entry was created from */
        public final String fileReadFrom;

        /** Who create the entry in the file */
        public String author;
        /** Where the error can be reported if the file fails to read */
        public String authorHelpSite;

        public JsonEntry(String jsonKey, String fileReadFrom, JsonElement element)
        {
            this.jsonKey = jsonKey;
            this.fileReadFrom = fileReadFrom;
            this.element = element;
        }

        @Override
        public String toString()
        {
            return jsonKey + "[" + element + "]";
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof JsonEntry)
            {
                return jsonKey.equals(((JsonEntry) object).jsonKey) && element.equals(((JsonEntry) object).element);
            }
            return false;
        }
        //TODO add hashcode
    }

    /**
     * Compares two entry using a pre-defined map of sorted index values
     * <p>
     * key -> sorting value
     */
    public static class JsonEntryComparator implements Comparator<JsonEntry>
    {
        final Map<String, Integer> sortingIndexMap;

        public JsonEntryComparator(Map<String, Integer> sortingIndexMap)
        {
            this.sortingIndexMap = sortingIndexMap;
        }

        @Override
        public int compare(JsonEntry o1, JsonEntry o2)
        {
            int one = sortingIndexMap.get(o1.jsonKey);
            int two = sortingIndexMap.get(o2.jsonKey);
            return Integer.compare(one, two);
        }
    }
}
