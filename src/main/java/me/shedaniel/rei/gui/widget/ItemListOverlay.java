/*
 * Roughly Enough Items by Danielshe.
 * Licensed under the MIT License.
 */

package me.shedaniel.rei.gui.widget;

import com.google.common.collect.Lists;
import me.shedaniel.cloth.api.ClientUtils;
import me.shedaniel.rei.RoughlyEnoughItemsCore;
import me.shedaniel.rei.api.ClientHelper;
import me.shedaniel.rei.api.DisplayHelper;
import me.shedaniel.rei.api.ItemCheatingMode;
import me.shedaniel.rei.api.RecipeHelper;
import me.shedaniel.rei.client.ItemListOrdering;
import me.shedaniel.rei.client.ScreenHelper;
import me.shedaniel.rei.client.SearchArgument;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ItemListOverlay extends Widget {
    
    private static final String SPACE = " ", EMPTY = "";
    private static final Comparator<ItemStack> ASCENDING_COMPARATOR;
    private static final Comparator<ItemStack> DECENDING_COMPARATOR;
    private static List<Item> searchBlacklisted = Lists.newArrayList();
    
    static {
        ASCENDING_COMPARATOR = (itemStack, t1) -> {
            if (RoughlyEnoughItemsCore.getConfigManager().getConfig().itemListOrdering.equals(ItemListOrdering.name))
                return tryGetItemStackName(itemStack).compareToIgnoreCase(tryGetItemStackName(t1));
            if (RoughlyEnoughItemsCore.getConfigManager().getConfig().itemListOrdering.equals(ItemListOrdering.item_groups)) {
                List<ItemGroup> itemGroups = Arrays.asList(ItemGroup.GROUPS);
                return itemGroups.indexOf(itemStack.getItem().getItemGroup()) - itemGroups.indexOf(t1.getItem().getItemGroup());
            }
            return 0;
        };
        DECENDING_COMPARATOR = ASCENDING_COMPARATOR.reversed();
    }
    
    private final List<ItemStack> currentDisplayed;
    private final List<SearchArgument[]> lastSearchArgument;
    private List<Widget> widgets;
    private int width, height, page;
    private Rectangle rectangle, listArea;
    
    public ItemListOverlay(int page) {
        this.currentDisplayed = Lists.newArrayList();
        this.width = 0;
        this.height = 0;
        this.page = page;
        this.lastSearchArgument = Lists.newArrayList();
    }
    
    public static List<String> tryGetItemStackToolTip(ItemStack itemStack, boolean careAboutAdvanced) {
        if (!searchBlacklisted.contains(itemStack.getItem()))
            try {
                return itemStack.getTooltipText(MinecraftClient.getInstance().player, MinecraftClient.getInstance().options.advancedItemTooltips && careAboutAdvanced ? TooltipContext.Default.ADVANCED : TooltipContext.Default.NORMAL).stream().map(Component::getFormattedText).collect(Collectors.toList());
            } catch (Throwable e) {
                e.printStackTrace();
                searchBlacklisted.add(itemStack.getItem());
            }
        return Collections.singletonList(tryGetItemStackName(itemStack));
    }
    
    public static String tryGetItemStackName(ItemStack stack) {
        if (!searchBlacklisted.contains(stack.getItem()))
            try {
                return stack.getDisplayName().getFormattedText();
            } catch (Throwable e) {
                e.printStackTrace();
                searchBlacklisted.add(stack.getItem());
            }
        try {
            return I18n.translate("item." + Registry.ITEM.getId(stack.getItem()).toString().replace(":", "."));
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "ERROR";
    }
    
    public int getFullTotalSlotsPerPage() {
        return width * height;
    }
    
    @Override
    public void render(int int_1, int int_2, float float_1) {
        GuiLighting.disable();
        widgets.forEach(widget -> widget.render(int_1, int_2, float_1));
        ClientPlayerEntity player = minecraft.player;
        if (rectangle.contains(ClientUtils.getMouseLocation()) && ClientHelper.getInstance().isCheating() && !player.inventory.getCursorStack().isEmpty() && RoughlyEnoughItemsCore.hasPermissionToUsePackets())
            ScreenHelper.getLastOverlay().addTooltip(QueuedTooltip.create(I18n.translate("text.rei.delete_items")));
    }
    
    public void updateList(DisplayHelper.DisplayBoundsHandler boundsHandler, Rectangle rectangle, int page, String searchTerm, boolean processSearchTerm) {
        this.rectangle = rectangle;
        this.page = page;
        this.widgets = Lists.newLinkedList();
        calculateListSize(rectangle);
        if (currentDisplayed.isEmpty() || processSearchTerm) {
            currentDisplayed.clear();
            currentDisplayed.addAll(processSearchTerm(searchTerm, RoughlyEnoughItemsCore.getItemRegisterer().getItemList(), ScreenHelper.inventoryStacks));
        }
        int startX = (int) rectangle.getCenterX() - width * 9;
        int startY = (int) rectangle.getCenterY() - height * 9;
        this.listArea = new Rectangle((int) startX, (int) startY, width * 18, height * 18);
        int fitSlotsPerPage = getTotalFitSlotsPerPage(startX, startY, listArea);
        int j = page * fitSlotsPerPage;
        for(int yy = 0; yy < height; yy++) {
            for(int xx = 0; xx < width; xx++) {
                int x = startX + xx * 18, y = startY + yy * 18;
                if (!canBeFit(x, y, listArea))
                    continue;
                j++;
                if (j > currentDisplayed.size())
                    break;
                widgets.add(new SlotWidget(x, y, Collections.singletonList(currentDisplayed.get(j - 1)), false, true, true) {
                    @Override
                    protected void queueTooltip(ItemStack itemStack, float delta) {
                        ClientPlayerEntity player = minecraft.player;
                        if (!ClientHelper.getInstance().isCheating() || player.inventory.getCursorStack().isEmpty())
                            super.queueTooltip(itemStack, delta);
                    }
                    
                    @Override
                    public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        if (isCurrentRendererItem() && isHighlighted(mouseX, mouseY)) {
                            if (ClientHelper.getInstance().isCheating()) {
                                if (getCurrentItemStack() != null && !getCurrentItemStack().isEmpty()) {
                                    ItemStack cheatedStack = getCurrentItemStack().copy();
                                    if (RoughlyEnoughItemsCore.getConfigManager().getConfig().itemCheatingMode == ItemCheatingMode.REI_LIKE)
                                        cheatedStack.setAmount(button != 1 ? 1 : cheatedStack.getMaxAmount());
                                    else if (RoughlyEnoughItemsCore.getConfigManager().getConfig().itemCheatingMode == ItemCheatingMode.JEI_LIKE)
                                        cheatedStack.setAmount(button != 0 ? 1 : cheatedStack.getMaxAmount());
                                    else
                                        cheatedStack.setAmount(1);
                                    return ClientHelper.getInstance().tryCheatingStack(cheatedStack);
                                }
                            } else if (button == 0)
                                return ClientHelper.getInstance().executeRecipeKeyBind(getCurrentItemStack().copy());
                            else if (button == 1)
                                return ClientHelper.getInstance().executeUsageKeyBind(getCurrentItemStack().copy());
                        }
                        return false;
                    }
                });
            }
            if (j > currentDisplayed.size())
                break;
        }
    }
    
    public int getTotalPage() {
        int fitSlotsPerPage = getTotalFitSlotsPerPage(listArea.x, listArea.y, listArea);
        if (fitSlotsPerPage > 0)
            return MathHelper.ceil(getCurrentDisplayed().size() / fitSlotsPerPage);
        return 0;
    }
    
    public int getTotalFitSlotsPerPage(int startX, int startY, Rectangle listArea) {
        int slots = 0;
        for(int x = 0; x < width; x++)
            for(int y = 0; y < height; y++)
                if (canBeFit(startX + x * 18, startY + y * 18, listArea))
                    slots++;
        return slots;
    }
    
    public boolean canBeFit(int left, int top, Rectangle listArea) {
        for(DisplayHelper.DisplayBoundsHandler sortedBoundsHandler : RoughlyEnoughItemsCore.getDisplayHelper().getSortedBoundsHandlers(minecraft.currentScreen.getClass())) {
            ActionResult fit = sortedBoundsHandler.canItemSlotWidgetFit(!RoughlyEnoughItemsCore.getConfigManager().getConfig().mirrorItemPanel, left, top, minecraft.currentScreen, listArea);
            if (fit != ActionResult.PASS)
                return fit == ActionResult.SUCCESS;
        }
        return true;
    }
    
    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        for(Widget widget : widgets)
            if (widget.keyPressed(int_1, int_2, int_3))
                return true;
        return false;
    }
    
    public Rectangle getListArea() {
        return listArea;
    }
    
    public List<ItemStack> getCurrentDisplayed() {
        return currentDisplayed;
    }
    
    private List<ItemStack> processSearchTerm(String searchTerm, List<ItemStack> ol, List<ItemStack> inventoryItems) {
        lastSearchArgument.clear();
        List<ItemStack> os = Lists.newArrayList(ol), stacks = Lists.newArrayList();
        if (RoughlyEnoughItemsCore.getConfigManager().getConfig().itemListOrdering != ItemListOrdering.registry)
            if (RoughlyEnoughItemsCore.getConfigManager().getConfig().isAscending)
                os.sort(ASCENDING_COMPARATOR);
            else
                os.sort(DECENDING_COMPARATOR);
        String[] splitSearchTerm = StringUtils.splitByWholeSeparatorPreserveAllTokens(searchTerm, "|");
        Arrays.stream(splitSearchTerm).forEachOrdered(s -> {
            String[] split = StringUtils.split(s);
            SearchArgument[] arguments = new SearchArgument[split.length];
            for(int i = 0; i < split.length; i++) {
                String s1 = split[i];
                if (s1.startsWith("@-") || s1.startsWith("-@"))
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.MOD, s1.substring(2), false);
                else if (s1.startsWith("@"))
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.MOD, s1.substring(1), true);
                else if (s1.startsWith("#-") || s1.startsWith("-#"))
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.TOOLTIP, s1.substring(2), false);
                else if (s1.startsWith("#"))
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.TOOLTIP, s1.substring(1), true);
                else if (s1.startsWith("-"))
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.TEXT, s1.substring(1), false);
                else
                    arguments[i] = new SearchArgument(SearchArgument.ArgumentType.TEXT, s1, true);
            }
            if (arguments.length > 0)
                lastSearchArgument.add(arguments);
            else
                lastSearchArgument.add(new SearchArgument[]{SearchArgument.ALWAYS});
        });
        os.stream().filter(itemStack -> filterItem(itemStack, lastSearchArgument)).forEachOrdered(stacks::add);
        List<ItemStack> workingItems = RoughlyEnoughItemsCore.getConfigManager().isCraftableOnlyEnabled() && !stacks.isEmpty() && !inventoryItems.isEmpty() ? Lists.newArrayList() : Lists.newArrayList(ol);
        if (RoughlyEnoughItemsCore.getConfigManager().isCraftableOnlyEnabled()) {
            RecipeHelper.getInstance().findCraftableByItems(inventoryItems).forEach(workingItems::add);
            workingItems.addAll(inventoryItems);
        }
        if (!RoughlyEnoughItemsCore.getConfigManager().isCraftableOnlyEnabled())
            return stacks;
        return stacks.stream().filter(itemStack -> workingItems.stream().anyMatch(stack -> stack.isEqualIgnoreTags(itemStack))).collect(Collectors.toList());
    }
    
    public List<SearchArgument[]> getLastSearchArgument() {
        return lastSearchArgument;
    }
    
    public static boolean filterItem(ItemStack itemStack, List<SearchArgument[]> arguments) {
        if (arguments.isEmpty())
            return true;
        String mod = ClientHelper.getInstance().getModFromItem(itemStack.getItem()).toLowerCase();
        String tooltips = tryGetItemStackToolTip(itemStack, true).stream().skip(1).collect(Collectors.joining("")).replace(SPACE, EMPTY).toLowerCase();
        String name = tryGetItemStackName(itemStack).replace(SPACE, EMPTY).toLowerCase();
        for(SearchArgument[] arguments1 : arguments) {
            boolean b = true;
            for(SearchArgument argument : arguments1) {
                if (argument.getArgumentType().equals(SearchArgument.ArgumentType.ALWAYS))
                    return true;
                if (argument.getArgumentType().equals(SearchArgument.ArgumentType.MOD))
                    if (SearchArgument.getFunction(!argument.isInclude()).apply(mod.indexOf(argument.getText()))) {
                        b = false;
                        break;
                    }
                if (argument.getArgumentType().equals(SearchArgument.ArgumentType.TOOLTIP))
                    if (SearchArgument.getFunction(!argument.isInclude()).apply(tooltips.indexOf(argument.getText()))) {
                        b = false;
                        break;
                    }
                if (argument.getArgumentType().equals(SearchArgument.ArgumentType.TEXT))
                    if (SearchArgument.getFunction(!argument.isInclude()).apply(name.indexOf(argument.getText()))) {
                        b = false;
                        break;
                    }
            }
            if (b)
                return true;
        }
        return false;
    }
    
    private boolean filterItem(ItemStack itemStack, SearchArgument... arguments) {
        if (arguments.length == 0)
            return true;
        String mod = ClientHelper.getInstance().getModFromItem(itemStack.getItem()).toLowerCase();
        String tooltips = tryGetItemStackToolTip(itemStack, false).stream().skip(1).collect(Collectors.joining("")).replace(SPACE, EMPTY).toLowerCase();
        String name = tryGetItemStackName(itemStack).replace(SPACE, EMPTY).toLowerCase();
        for(SearchArgument argument : arguments) {
            if (argument.getArgumentType().equals(SearchArgument.ArgumentType.MOD))
                if (SearchArgument.getFunction(!argument.isInclude()).apply(mod.indexOf(argument.getText())))
                    return false;
            if (argument.getArgumentType().equals(SearchArgument.ArgumentType.TOOLTIP))
                if (SearchArgument.getFunction(!argument.isInclude()).apply(tooltips.indexOf(argument.getText())))
                    return false;
            if (argument.getArgumentType().equals(SearchArgument.ArgumentType.TEXT))
                if (SearchArgument.getFunction(!argument.isInclude()).apply(name.indexOf(argument.getText())))
                    return false;
        }
        return true;
    }
    
    public void calculateListSize(Rectangle rect) {
        int xOffset = 0, yOffset = 0;
        width = 0;
        height = 0;
        while (true) {
            xOffset += 18;
            if (height == 0)
                width++;
            if (xOffset + 19 > rect.width) {
                xOffset = 0;
                yOffset += 18;
                height++;
            }
            if (yOffset + 19 > rect.height)
                break;
        }
    }
    
    @Override
    public boolean mouseClicked(double double_1, double double_2, int int_1) {
        if (rectangle.contains(double_1, double_2)) {
            ClientPlayerEntity player = minecraft.player;
            if (ClientHelper.getInstance().isCheating() && !player.inventory.getCursorStack().isEmpty() && RoughlyEnoughItemsCore.hasPermissionToUsePackets()) {
                ClientHelper.getInstance().sendDeletePacket();
                return true;
            }
            if (!player.inventory.getCursorStack().isEmpty() && RoughlyEnoughItemsCore.hasPermissionToUsePackets())
                return false;
            for(Widget widget : children())
                if (widget.mouseClicked(double_1, double_2, int_1))
                    return true;
        }
        return false;
    }
    
    @Override
    public List<Widget> children() {
        return widgets;
    }
    
}
