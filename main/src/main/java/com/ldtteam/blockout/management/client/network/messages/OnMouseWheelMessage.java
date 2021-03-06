package com.ldtteam.blockout.management.client.network.messages;

import com.ldtteam.blockout.connector.core.IGuiKey;
import com.ldtteam.blockout.element.root.RootGuiElement;
import com.ldtteam.blockout.network.message.core.IBlockOutClientToServerMessage;
import com.ldtteam.blockout.proxy.ProxyHolder;
import com.ldtteam.blockout.util.Log;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

public class OnMouseWheelMessage implements IBlockOutClientToServerMessage
{
    @NotNull
    private final int localX;
    @NotNull
    private final int localY;
    @NotNull
    private final int deltaWheel;

    public OnMouseWheelMessage(@NotNull final int localX, @NotNull final int localY, @NotNull final int deltaWheel)
    {
        this.localX = localX;
        this.localY = localY;
        this.deltaWheel = deltaWheel;
    }

    @Override
    public void onMessageArrivalAtServer(@NotNull final NetworkEvent.Context ctx)
    {
        final ServerPlayerEntity player = ctx.getSender();
        final IGuiKey key = ProxyHolder.getInstance().getGuiController().getOpenUI(player);
        if (key == null)
        {
            Log.getLogger().error("Player is not watching a BlockOut guitemp.");
            return;
        }

        final RootGuiElement rootGuiElement = (RootGuiElement) ProxyHolder.getInstance().getGuiController().getRoot(key);
        if (rootGuiElement == null)
        {
            Log.getLogger().error("Player seems to be watching an unknown Gui.");
            return;
        }

        rootGuiElement.getUiManager().getScrollManager().onMouseWheel(localX, localY, deltaWheel);
    }
}
