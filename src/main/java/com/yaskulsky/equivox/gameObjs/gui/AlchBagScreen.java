package com.yaskulsky.equivox.gameObjs.gui;

import com.yaskulsky.equivox.PECore;
import com.yaskulsky.equivox.gameObjs.container.AlchBagContainer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class AlchBagScreen extends PEContainerScreen<AlchBagContainer> {

	private static final Identifier texture = PECore.rl("textures/gui/alchchest.png");
	//Width of the grey panel left of the player inventory (x=0..47), minus a margin
	private static final int INVENTORY_LABEL_WIDTH = 40;

	public AlchBagScreen(AlchBagContainer container, Inventory invPlayer, Component title) {
		super(container, invPlayer, title, 255, 230);
	}

	@Override
	protected void extractLabels(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		//The texture has no room for labels inside the window: the 13x8 grid fills it from the top, and the only free
		// space is the side panels next to the player inventory. Draw the title above the window, white with a shadow so
		// it reads on the darkened background, and the inventory label scaled down in the left panel
		graphics.text(font, title, (imageWidth - font.width(title)) / 2, -11, 0xFFFFFFFF, true);
		float scale = Math.min(1, INVENTORY_LABEL_WIDTH / (float) font.width(playerInventoryTitle));
		graphics.pose().pushMatrix();
		graphics.pose().translate(4, 156);
		graphics.pose().scale(scale, scale);
		graphics.text(font, playerInventoryTitle, 0, 0, PEGuiGraphics.LABEL_COLOR, false);
		graphics.pose().popMatrix();
	}

	@Override
	protected void peExtractBackground(@NotNull GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
		PEGuiGraphics.blit(graphics, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}
}
