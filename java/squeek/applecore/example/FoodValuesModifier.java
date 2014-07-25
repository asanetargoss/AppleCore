package squeek.applecore.example;

import net.minecraft.init.Items;
import squeek.applecore.api.food.FoodEvent;
import squeek.applecore.api.food.FoodValues;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class FoodValuesModifier
{
	@SubscribeEvent
	public void getFoodValues(FoodEvent.GetFoodValues event)
	{
		if (event.food.getItem() == Items.apple)
			event.foodValues = new FoodValues(0, 0);
	}

	@SubscribeEvent
	public void getPlayerSpecificFoodValues(FoodEvent.GetFoodValues event)
	{
		if (event.player != null)
		{
			event.foodValues = new FoodValues((20 - event.player.getFoodStats().getFoodLevel()) / 2, 0);
		}
	}
}