package universalelectricity.core.asm.template;

import net.minecraftforge.common.ForgeDirection;
import universalelectricity.api.IEnergyContainer;
import universalelectricity.api.IEnergyInterface;

/**
 * @author Calclavia
 * 
 */
public class StaticForwarder
{
	/**
	 * Adds electricity to an block. Returns the quantity of electricity that was accepted. This
	 * should always return 0 if the block cannot be externally charged.
	 * 
	 * @param from Orientation the electricity is sent in from.
	 * @param receive Maximum amount of electricity (joules) to be sent into the block.
	 * @param doReceive If false, the charge will only be simulated.
	 * @return Amount of energy that was accepted by the block.
	 */
	public static int receiveElectricity(IEnergyInterface handler, ForgeDirection from, int receive, boolean doReceive)
	{
		return handler.onReceiveEnergy(from, receive, doReceive);
	}

	/**
	 * Adds electricity to an block. Returns the ElectricityPack, the electricity provided. This
	 * should always return null if the block cannot be externally discharged.
	 * 
	 * @param from Orientation the electricity is requested from.
	 * @param energy Maximum amount of energy to be sent into the block.
	 * @param doReceive If false, the charge will only be simulated.
	 * @return Amount of energy that was given out by the block.
	 */
	public static int extractElectricity(IEnergyInterface handler, ForgeDirection from, int request, boolean doProvide)
	{
		return handler.onExtractEnergy(from, request, doProvide);
	}
	
	/**
	 * Gets the voltage of this TileEntity.
	 * 
	 * @return The amount of volts. E.g 120v or 240v
	 */
	public static int getVoltage(IEnergyInterface handler, ForgeDirection direction)
	{
		return handler.getVoltage(direction);
	}

	public static int getElectricityStored(IEnergyContainer handler, ForgeDirection from)
	{
		return handler.getEnergy(from);
	}

	public static float getMaxElectricity(IEnergyContainer handler, ForgeDirection from)
	{
		return handler.getEnergyCapacity(from);
	}

	public static boolean canConnect(IEnergyInterface handler, ForgeDirection from)
	{
		return handler.canConnect(from);
	}
}
