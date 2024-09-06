package sh.okx.civmodern.mod.gui.screen;

import java.text.DecimalFormat;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import sh.okx.civmodern.mod.CivModernConfig;
import sh.okx.civmodern.mod.ColourProvider;
import sh.okx.civmodern.mod.gui.widget.HsbColourPicker;
import sh.okx.civmodern.mod.gui.widget.ImageButton;

public class CompactedConfigScreen extends Screen {
    private static final DecimalFormat FORMAT = new DecimalFormat("##%");

    private static final ItemStack ITEM; static {
        CompoundTag item = new CompoundTag();
        item.putString("id", "stone");
        item.putInt("Count", 64);
        CompoundTag tag = new CompoundTag();
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf("\"Compacted Item\""));
        CompoundTag display = new CompoundTag();
        display.put("Lore", lore);
        tag.put("display", display);
        item.put("tag", tag);
        ITEM = ItemStack.parse(Minecraft.getInstance().level.registryAccess(), item).get();
    }

    private int itemX;
    private int itemY;

    private final Screen parent;
    private HsbColourPicker picker;

    public CompactedConfigScreen(Screen parent) {
        super(Component.translatable("civmodern.screen.compacted.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        itemX = this.width / 2 - 16 / 2;
        itemY = this.height / 6 - 24;

        int leftWidth = width / 2 - (60 + 8 + 20 + 8 + 20) / 2;

        EditBox widget = new EditBox(font, leftWidth, height / 6, 60, 20, Component.empty());
        widget.setValue("#" + String.format("%06X", CivModernConfig.compactedItemColour));
        widget.setMaxLength(7);
        Pattern pattern = Pattern.compile("^(#[0-9A-F]{0,6})?$", Pattern.CASE_INSENSITIVE);
        widget.setFilter(string -> pattern.matcher(string).matches());
        widget.setResponder(val -> {
            if (val.length() == 7) {
                CivModernConfig.compactedItemColour = Integer.parseInt(val.substring(1), 16);
            }
        });
        addRenderableWidget(widget);

        HsbColourPicker hsb = new HsbColourPicker(
            leftWidth + 60 + 8,
            height / 6,
            20,
            20,
            CivModernConfig.compactedItemColour,
            colour -> {
                widget.setValue("#" + String.format("%06X", colour));
                CivModernConfig.compactedItemColour = colour;
            },
            ColourProvider::setTemporaryCompactedItemColour,
            () -> {}
        );

        addRenderableWidget(new ImageButton(leftWidth + 60 + 8 + 20 + 8, height / 6, 20, 20, ResourceLocation.tryBuild("civmodern", "gui/rollback.png"), imbg -> {
            int colour = 0xffff58;
            widget.setValue("#FFFF58");
            CivModernConfig.compactedItemColour = colour;
            hsb.close();
        }));

        addRenderableWidget(picker = hsb);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            CivModernConfig.save();
            minecraft.setScreen(parent);
        }).pos(this.width / 2 - 49, this.height / 6 + 169).size(98, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        drawCentredText(guiGraphics, this.title, 0, 15, 0xffffff);

        drawItem(guiGraphics);

        if (isCursorOverItem(mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, ITEM, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(
        final double mouseX,
        final double mouseY,
        final int button
    ) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && isCursorOverItem((int) mouseX, (int) mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1 && player.isCreative()) {
            player.addItem(ITEM.copy());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
        if (picker != null) {
            picker.mouseMoved(d, e);
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        ColourProvider.setTemporaryCompactedItemColour(null);
        CivModernConfig.save();
    }

    private boolean isCursorOverItem(int mouseX, int mouseY) {
        return mouseX >= itemX - 1  && mouseX < itemX + 17 && mouseY > itemY - 1 && mouseY < itemY + 17;
    }

    private void drawItem(GuiGraphics guiGraphics) {
        guiGraphics.renderItem(ITEM, itemX, itemY);
        guiGraphics.renderItemDecorations(font, ITEM, itemX, itemY);
    }

    private void drawCentredText(GuiGraphics guiGraphics, Component text, int xOffsetCentre, int yOffsetTop, int colour) {
        int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int centre = width / 2 - font.width(text) / 2;
        guiGraphics.drawString(this.font, text, centre + xOffsetCentre, yOffsetTop, colour);
    }
}
