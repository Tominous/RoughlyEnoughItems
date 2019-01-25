package me.shedaniel.rei.plugin;

import com.mojang.blaze3d.platform.GlStateManager;
import me.shedaniel.rei.api.IRecipeCategory;
import me.shedaniel.rei.gui.widget.IWidget;
import me.shedaniel.rei.gui.widget.ItemSlotWidget;
import me.shedaniel.rei.gui.widget.RecipeBaseWidget;
import me.shedaniel.rei.listeners.IMixinContainerGui;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DefaultCampfireCategory implements IRecipeCategory<DefaultCampfireDisplay> {
    
    private static final Identifier DISPLAY_TEXTURE = new Identifier("roughlyenoughitems", "textures/gui/display.png");
    
    @Override
    public Identifier getIdentifier() {
        return DefaultPlugin.CAMPFIRE;
    }
    
    @Override
    public ItemStack getCategoryIcon() {
        return new ItemStack(Blocks.CAMPFIRE);
    }
    
    @Override
    public String getCategoryName() {
        return I18n.translate("category.rei.campfire");
    }
    
    @Override
    public List<IWidget> setupDisplay(IMixinContainerGui containerGui, DefaultCampfireDisplay recipeDisplay, Rectangle bounds) {
        Point startPoint = new Point((int) bounds.getCenterX() - 41, (int) bounds.getCenterY() - 27);
        List<IWidget> widgets = new LinkedList<>(Arrays.asList(new RecipeBaseWidget(bounds) {
            @Override
            public void draw(int mouseX, int mouseY, float partialTicks) {
                super.draw(mouseX, mouseY, partialTicks);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                GuiLighting.disable();
                MinecraftClient.getInstance().getTextureManager().bindTexture(DISPLAY_TEXTURE);
                drawTexturedRect(startPoint.x, startPoint.y, 0, 167, 82, 54);
                int height = MathHelper.ceil((System.currentTimeMillis() / 250 % 14d) / 1f);
                drawTexturedRect(startPoint.x + 2, startPoint.y + 31 + (14 - height), 82, 77 + (14 - height), 14, height);
                int width = MathHelper.ceil((System.currentTimeMillis() / 250 % 24d) / 1f);
                drawTexturedRect(startPoint.x + 24, startPoint.y + 19, 82, 92, width, 17);
                String text = I18n.translate("category.rei.campfire.time", MathHelper.floor(recipeDisplay.getCookTime() / 20d));
                int length = MinecraftClient.getInstance().fontRenderer.getStringWidth(text);
                MinecraftClient.getInstance().fontRenderer.drawWithShadow(text, bounds.x + bounds.width - length - 4, startPoint.y + 54 - 8, -1);
            }
        }));
        widgets.add(new ItemSlotWidget(startPoint.x + 1, startPoint.y + 11, recipeDisplay.getInput().get(0), true, true, containerGui, true));
        widgets.add(new ItemSlotWidget(startPoint.x + 61, startPoint.y + 19, recipeDisplay.getOutput(), false, true, containerGui, true));
        return widgets;
    }
    
}