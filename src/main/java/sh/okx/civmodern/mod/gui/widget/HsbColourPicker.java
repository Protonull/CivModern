package sh.okx.civmodern.mod.gui.widget;

import com.mojang.blaze3d.platform.GlUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GLUtil;
import sh.okx.civmodern.mod.gui.Texture;
import sh.okx.civmodern.mod.gui.screen.ScreenCloseable;

public class HsbColourPicker extends AbstractWidget {

    private static final ResourceLocation COLOUR_PICKER_ICON = ResourceLocation.tryBuild("civmodern", "gui/colour.png");

    private final Texture hueSelector;
    private final Texture saturationBrightnessTexture;
    private final Consumer<Integer> colourConsumer;
    private final Consumer<Integer> previewConsumer;

    private int hue = 0; // [0, 360]
    //private int saturation = 0; // [0, 100]
    //private int brightness = 0; // [0, 100]

    private boolean mouseOverGrid = false;

    private boolean showPalette = false;
    private boolean updateTexture = true;
    private boolean hueMouseDown = false;

    private final ScreenCloseable closeable;

    private int renderY;

    public HsbColourPicker(int x, int y, int width, int height, int colour, Consumer<Integer> colourConsumer, Consumer<Integer> previewConsumer, ScreenCloseable closeable) {
        super(x, y, width, height, Component.literal("HSB Colour Picker"));

        this.hue = getHue(colour);

        this.closeable = closeable;
        this.hueSelector = getHueSelector();
        this.saturationBrightnessTexture = new Texture(128, 128);
        this.colourConsumer = colourConsumer;
        this.previewConsumer = previewConsumer;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Render colour picker above button if it would exceed the screen height otherwise
        this.renderY = (this.getX() + 101 > Minecraft.getInstance().getWindow().getGuiScaledHeight()) ? this.getY() - 101 - this.height : this.getY();

        RenderSystem.setShaderTexture(0, COLOUR_PICKER_ICON);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(COLOUR_PICKER_ICON, this.getX(), this.getY(), 10, 0, isHoveredOrFocused() ? 20 : 0, this.width,
            this.height, 20, 40);

        if (showPalette) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            if (this.updateTexture) {
                updateTexture(this.saturationBrightnessTexture, this.hue);
                this.updateTexture = false;
            }

            this.saturationBrightnessTexture.bind();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 1000);
            // Saturation and brightness selector
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            bufferBuilder.addVertex(matrix4f, this.getX(), renderY + height, 0).setUv(0, 0);
            bufferBuilder.addVertex(matrix4f, this.getX(), renderY + height + 128, 0).setUv(0, 1);
            bufferBuilder.addVertex(matrix4f, this.getX() + 128, renderY + height + 128, 0).setUv(1, 1);
            bufferBuilder.addVertex(matrix4f, this.getX() + 128, renderY + height, 0).setUv(1, 0);
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
//      guiGraphics.blit(this.saturationBrightnessTexture, this.getX(), renderY + height, 0, 0, 0, 101, 101, 128, 128);

            // Hue selector
            hueSelector.bind();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(matrix4f, this.getX() + 106, renderY + height, 0).setUv(0, 0);
            bufferBuilder.addVertex(matrix4f, this.getX() + 106, renderY + height + 101, 0).setUv(0, 1);
            bufferBuilder.addVertex(matrix4f, this.getX() + 106 + 10, renderY + height + 101, 0).setUv(1, 1);
            bufferBuilder.addVertex(matrix4f, this.getX() + 106 + 10, renderY + height, 0).setUv(1, 0);
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
//      Gui.blit(matrixStack, this.x + 106, renderY + height, 10, 101, 0, 0, 1, 360, 1, 360);

            RenderSystem.disableBlend();

            // Render cursor
            float hueOffset = (this.hue / 360f) * 100;
            int cursorX = this.getX() + 106;
            int cursorY = (int) hueOffset + renderY + height;
            guiGraphics.fill(cursorX, cursorY, cursorX + 10, cursorY + 1, 0xFFFFFFFF);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (previewConsumer != null) {
            if (active && visible && showPalette && isOverGrid(mouseX, mouseY)) {
                int saturation = (int) (mouseX - this.getX());
                int brightness = (int) (mouseY - renderY - height);
                mouseOverGrid = true;
                previewConsumer.accept(toRgb(hue, saturation, brightness));
            } else if (mouseOverGrid) {
                previewConsumer.accept(null);
            }
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (!showPalette) {
            closeable.close();
        }
        showPalette = !showPalette;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        if (this.hueMouseDown) {
            setHue(mouseX, mouseY, 0, true);
        }
    }

    @Override
    public void onRelease(double d, double e) {
        this.hueMouseDown = false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return selectColour(mouseX, mouseY, button)
            || setHue(mouseX, mouseY, button, false)
            || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    private boolean selectColour(double mouseX, double mouseY, int button) {
        if (active && visible && button == 0 && showPalette && isOverGrid(mouseX, mouseY)) {
            int saturation = (int) (mouseX - this.getX());
            int brightness = (int) (mouseY - renderY - height);
            colourConsumer.accept(toRgb(hue, saturation, brightness));
            this.showPalette = false;
            return true;
        }
        return false;
    }

    private boolean setHue(double mouseX, double mouseY, int button, boolean force) {
        // Cursor selector
        if (active && visible && button == 0 && showPalette) {
            if (!force && !(mouseY >= renderY + height && mouseY <= renderY + height + 101)) {
                return false;
            }

            if (force || (mouseX >= this.getX() + 106 && mouseX <= this.getX() + 106 + 10)) {
                this.hueMouseDown = true;
                double yOffset = mouseY - (renderY + height);
                int newHue = Mth.clamp((int) ((yOffset / 102) * 360), 0, 360);
                if (newHue != this.hue) {
                    this.hue = newHue;
                    this.updateTexture = true;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isOverGrid(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + 101
            && mouseY >= renderY + height && mouseY < renderY + height + 101;
    }

    private void updateTexture(Texture texture, int hue) {
        int[] rgbaValues = new int[128 * 128];
        for (int saturation = 0; saturation <= 100; saturation++) {
            for (int brightness = 0; brightness <= 100; brightness++) {
                int rgb = toRgb(hue, saturation, brightness) & 0xFFFFFF;
                rgbaValues[(brightness * 128) + saturation] = rgb << 8 | 0xFF;
            }
        }
        texture.setPixels(rgbaValues);
        texture.update();
    }

    private int toRgb(int hue, int sat, int bright) {
        //return Color.HSBtoRGB(this.hue / 360f, this.saturation / 100f, this.brightness / 100f) & 0xFFFFFF;
        double[] rgbArr = HUSLColorConverter.hsluvToRgb(new double[] {hue, sat, bright});
        return ((int) (rgbArr[0] * 255) << 16) | ((int) (rgbArr[1] * 255) << 8) | ((int) (rgbArr[2] * 255));
    }

    private int getHue(int colour) {
        int r = colour >> 16 & 0xFF;
        int g = colour >> 8 & 0xFF;
        int b = colour & 0xFF;
        return (int) Math.round(HUSLColorConverter.rgbToHsluv(new double[] {r/255d, g/255d, b/255d})[0]);
        //return Math.round(Color.RGBtoHSB(r, g, b, null)[0] * 360);
    }

    private Texture getHueSelector() {
        Texture hueSelector = new Texture(1, 360);
        int[] rgbaValues = new int[360];
        for (int i = 0; i < 360; i++) {
            //int rgb = Color.HSBtoRGB(i / 360f, 1, 1);
            int rgb = toRgb(i, 100, 50);
            rgbaValues[i] = rgb << 8 | 0xFF;
        }
        hueSelector.setPixels(rgbaValues);
        hueSelector.update();
        return hueSelector;
    }

    public void close() {
        this.showPalette = false;
    }
}
