package com.example.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ExampleMenuScreen extends Screen {
	public ExampleMenuScreen() {
		super(Component.translatable("screen.modid.menu.title"));
	}

	@Override
	protected void init() {
		int buttonWidth = 100;
		int buttonHeight = 20;
		this.addRenderableWidget(Button.builder(Component.translatable("screen.modid.menu.close"), button -> this.onClose())
				.bounds(this.width / 2 - buttonWidth / 2, this.height / 2, buttonWidth, buttonHeight)
				.build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFFFF);
	}
}
