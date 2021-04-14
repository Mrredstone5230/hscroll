package me.polishkrowa.hscroll.mixin;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    private Minecraft client;
    private double eventDeltaWheel;
    private boolean mixinConfigured;

    @Inject(at = @At("RETURN"), method = "displayGuiScreen")
    private void init(CallbackInfo info) {
        if (!mixinConfigured) {
            this.client = Minecraft.getInstance();
            this.eventDeltaWheel = 0.0D;
            // Override MC scroll callback with our own
            GLFW.glfwSetScrollCallback(this.client.getMainWindow().getHandle(), this::onMouseScroll);
        }
        mixinConfigured = true;
    }

    private void onMouseScroll(long window, double xoffset, double yoffset) {
        if (window == Minecraft.getInstance().getMainWindow().getHandle()) {

            if (Minecraft.IS_RUNNING_ON_MAC && yoffset == 0) {
                yoffset = xoffset;
            }

            double delta = (this.client.gameSettings.discreteMouseScroll ? Math.signum(yoffset) : yoffset)
                    * this.client.gameSettings.mouseWheelSensitivity;

            if (this.client.loadingGui == null) {
                MainWindow clientWindow = Minecraft.getInstance().getMainWindow();

                if (this.client.currentScreen != null) {
                    double xposition = this.client.mouseHelper.getMouseX() * (double) clientWindow.getScaledWidth()
                            / (double) clientWindow.getWidth();
                    double yposition = this.client.mouseHelper.getMouseY() * (double) clientWindow.getScaledHeight()
                            / (double) clientWindow.getHeight();
                    this.client.currentScreen.mouseScrolled(xposition, yposition, delta);
                } else if (this.client.player != null) {
                    if (this.eventDeltaWheel != 0.0D && Math.signum(delta) != Math.signum(this.eventDeltaWheel)) {
                        this.eventDeltaWheel = 0.0D;
                    }

                    this.eventDeltaWheel += delta;
                    float currentWheelDelta = (float) ((int) this.eventDeltaWheel);
                    if (currentWheelDelta == 0.0F) {
                        return;
                    }

                    this.eventDeltaWheel -= (double) currentWheelDelta;
                    if (this.client.player.isSpectator()) {
                        if (this.client.ingameGUI.getSpectatorGui().isMenuActive()) {
                            this.client.ingameGUI.getSpectatorGui().onMouseScroll((double) (-currentWheelDelta));
                        } else {
                            float j = MathHelper.clamp(
                                    this.client.player.abilities.getFlySpeed() + currentWheelDelta * 0.005F, 0.0F,
                                    0.2F);
                            this.client.player.abilities.setFlySpeed(j);
                        }
                    } else {
                        this.client.player.inventory.changeCurrentItem((double) currentWheelDelta);
                    }
                }
            }
        }
    }
}