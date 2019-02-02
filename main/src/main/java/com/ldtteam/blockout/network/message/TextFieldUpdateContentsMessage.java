package com.ldtteam.blockout.network.message;

import com.ldtteam.blockout.BlockOut;
import com.ldtteam.blockout.connector.core.IGuiKey;
import com.ldtteam.blockout.element.root.RootGuiElement;
import com.ldtteam.blockout.element.simple.TextField;
import com.ldtteam.blockout.network.message.core.IBlockOutClientToServerMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class TextFieldUpdateContentsMessage implements IBlockOutClientToServerMessage
{
    private String controlId;
    private String newContent;

    public TextFieldUpdateContentsMessage(final String controlId, final String newContent)
    {
        this.controlId = controlId;
        this.newContent = newContent;
    }

    public TextFieldUpdateContentsMessage()
    {
    }

    @Override
    public void onMessageArrivalAtServer(@NotNull final MessageContext ctx)
    {
        final EntityPlayerMP playerMP = ctx.getServerHandler().player;
        final IGuiKey guiKey = BlockOut.getBlockOut().getProxy().getGuiController().getOpenUI(playerMP);

        if (guiKey == null)
        {
            return;
        }

        final RootGuiElement rootGuiElement = (RootGuiElement) BlockOut.getBlockOut().getProxy().getGuiController().getRoot(guiKey);
        final Optional<TextField> optionalTextBox = rootGuiElement.searchExactElementById(controlId, TextField.class);
        optionalTextBox.ifPresent(textBox -> {
            textBox.setContents(newContent);
        });
    }
}