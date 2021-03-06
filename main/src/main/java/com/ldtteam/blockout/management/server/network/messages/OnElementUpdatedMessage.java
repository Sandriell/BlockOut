package com.ldtteam.blockout.management.server.network.messages;

import com.ldtteam.blockout.element.IUIElement;
import com.ldtteam.blockout.element.IUIElementHost;
import com.ldtteam.blockout.element.root.RootGuiElement;
import com.ldtteam.blockout.gui.BlockOutContainerGui;
import com.ldtteam.blockout.gui.BlockOutGuiData;
import com.ldtteam.blockout.inventory.BlockOutContainer;
import com.ldtteam.blockout.loader.object.ObjectUIElementData;
import com.ldtteam.blockout.management.UIManager;
import com.ldtteam.blockout.network.message.core.IBlockOutServerToClientMessage;
import com.ldtteam.blockout.proxy.ProxyHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.fml.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class OnElementUpdatedMessage implements IBlockOutServerToClientMessage
{
    @NotNull
    private final ObjectUIElementData elementData;

    public OnElementUpdatedMessage(@NotNull final ObjectUIElementData elementData)
    {
        this.elementData = elementData;
    }

    @Override
    public void onMessageArrivalAtClient(@NotNull final NetworkEvent.Context ctx)
    {
        final Screen openGuiScreen = Minecraft.getInstance().currentScreen;

        if (!(openGuiScreen instanceof BlockOutContainerGui))
        {
            throw new IllegalStateException("No container open!");
        }

        final BlockOutContainerGui openContainerScreen = (BlockOutContainerGui) openGuiScreen;

        if (openContainerScreen.getInstanceData() instanceof BlockOutGuiData)
        {
            final BlockOutGuiData blockOutGui = (BlockOutGuiData) openContainerScreen.getInstanceData();
            final IUIElement containedElement = ProxyHolder.getInstance().getFactoryController().getElementFromData(elementData);
            if (!(containedElement instanceof RootGuiElement))
            {
                throw new IllegalStateException("The synced element is not a root.");
            }

            final IUIElementHost root = blockOutGui.getRoot();
            if (!(root instanceof RootGuiElement))
            {
                throw new IllegalStateException("The client root is not a root instance");
            }

            final RootGuiElement rootGuiElement = (RootGuiElement) containedElement;
            final UIManager uiManager = (UIManager) blockOutGui.getRoot().getUiManager();

            uiManager.setRootGuiElement(rootGuiElement);
            final IUIElement focusedElement = uiManager.getFocusManager().getFocusedElement();
            if (focusedElement != null)
            {
                uiManager.getFocusManager().setFocusedElement(rootGuiElement.searchExactElementById(focusedElement.getId()).orElse(null));
            }

            rootGuiElement.setUiManager(uiManager);
            blockOutGui.setRoot(rootGuiElement);
            blockOutGui.getRoot().getUiManager().getRenderManager().setGuiData(blockOutGui);
            blockOutGui.getRoot().getUiManager().getUpdateManager().updateElement(blockOutGui.getRoot());
            openContainerScreen.init(Minecraft.getInstance(), Minecraft.getInstance().mainWindow.getScaledWidth(), Minecraft.getInstance().mainWindow.getScaledHeight());

            final BlockOutContainer container = openContainerScreen.getContainer();
            container.reinitializeSlots();
        }
        else
        {
            throw new IllegalStateException("Server thinks a BlockOut guitemp was open.");
        }
    }
}
