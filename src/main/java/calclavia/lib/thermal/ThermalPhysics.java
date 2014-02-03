package calclavia.lib.thermal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import universalelectricity.api.vector.Vector3;

import com.builtbroken.common.science.ChemElement;

/**
 * A thermal block manager
 * 
 * @author Calclavia
 * 
 */
public class ThermalPhysics
{
	public static final ThermalPhysics INSTNACE = new ThermalPhysics();
	public static final int ROOM_TEMPERATURE = 295;

	/**
	 * Temperature: 0.5f = 22C
	 * 
	 * @return The temperature of the coordinate in the world in kelvin.
	 */
	public static int getTemperatureForCoordinate(World world, int x, int z)
	{
		int averageTemperature = 273 + (int) (world.getBiomeGenForCoords(x, z).getFloatTemperature() * 22 * 2);
		double dayNightVariance = averageTemperature * 0.05;
		return (int) (averageTemperature + (world.isDaytime() ? dayNightVariance : -dayNightVariance));
	}

	/**
	 * Q = mcT
	 * 
	 * @param mass - KG
	 * @param specificHeatCapacity - J/KG K
	 * @param temperature - K
	 * @return Q, energy in joules
	 */
	public static long getEnergyForTemperatureChange(int mass, long specificHeatCapacity, int temperature)
	{
		return mass * specificHeatCapacity * temperature;
	}

	public static int getTemperatureForEnergy(int mass, long specificHeatCapacity, long energy)
	{
		return (int) (energy / (mass * specificHeatCapacity));
	}

	public static long getRequiredBoilWaterEnergy(World world, int x, int z)
	{
		int temperatureChange = 373 - ThermalPhysics.getTemperatureForCoordinate(world, x, z);
		int mass = ThermalPhysics.getMass(1000, 1);
		return ThermalPhysics.getEnergyForTemperatureChange(mass, 4200, temperatureChange) + ThermalPhysics.getEnergyForStateChange(mass, 2257000);
	}

	/**
	 * Q = mL
	 * 
	 * @param mass - KG
	 * @param latentHeatCapacity - J/KG
	 * @return Q, energy in J
	 */
	public static long getEnergyForStateChange(int mass, long latentHeatCapacity)
	{
		return mass * latentHeatCapacity;
	}

	public static int getMass(float volume, float density)
	{
		return (int) (volume * density);
	}

	/**
	 * Mass (KG) = Volume (Cubic Meters) * Densitry (kg/m-cubed)
	 * 
	 * @param fluidStack
	 * @return The mass in KG
	 */
	public static int getMass(FluidStack fluidStack)
	{
		return (fluidStack.amount / 1000) * fluidStack.getFluid().getDensity(fluidStack);
	}

	/**
	 * A map of the temperature of the blocks
	 */
	public final HashMap<Vector3, Integer> thermalMap = new HashMap<Vector3, Integer>();

	public void update()
	{
		/**
		 * Reach thermal equilibrium
		 */
		Iterator<Entry<Vector3, Integer>> it = thermalMap.entrySet().iterator();

		while (it.hasNext())
		{
			Entry<Vector3, Integer> entry = it.next();

			for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS)
			{
				Vector3 checkPos = entry.getKey().clone().translate(dir);
				int neighbourTemp = getTemperature(checkPos);
				entry.setValue((entry.getValue() + neighbourTemp) / 2);
			}
		}
	}

	/**
	 * Adds energy to the fluid and thereby, increasing its temperature.
	 * 
	 * @param fluidStack - The fluid stack we are changing.
	 * @param specificHeatCapacity - E.g: Water: 4200. Iron: 450.
	 * @param energy - Amount of energy to put into the fluid.
	 * @return Change in temperature in Kelvin
	 */
	public int addEnergyToFluid(FluidStack fluidStack, int specificHeatCapacity, long energy)
	{
		// Mass (KG) = Volume (Cubic Meters) * Densitry (kg/m-cubed)
		int mass = (fluidStack.amount / 1000) * fluidStack.getFluid().getDensity(fluidStack);

		// c = Q/(mT); Therefore: Temperature (in Kelvin) = Q/mc
		int changeInTemperature = (int) (energy / (mass * specificHeatCapacity));

		if (fluidStack.getFluid() instanceof FluidThermal)
		{
			((FluidThermal) fluidStack.getFluid()).setTemperature(fluidStack, fluidStack.getFluid().getTemperature(fluidStack) + changeInTemperature);
		}

		return changeInTemperature;
	}

	/**
	 * Adds energy to a block in the form of heat.
	 */
	public void addEnergy(Vector3 position, ChemElement element, long energy)
	{
		// Mass (KG) = Volume (Cubic Meters) * Densitry (kg/m-cubed)
		int mass = (int) (1 * element.density);

		// c = Q/(mT); Therefore: Temperature (in Kelvin) = Q/mc
		int changeInTemperature = (int) (energy / (mass * element.heatData.specificHeat));

		setTemperature(position, getTemperature(position) + changeInTemperature);
	}

	public void setTemperature(Vector3 position, int temperature)
	{
		thermalMap.put(position, temperature);
	}

	public int getTemperature(Vector3 position)
	{
		if (thermalMap.containsKey(position))
		{
			return thermalMap.get(position);
		}

		return ROOM_TEMPERATURE;
	}
}
