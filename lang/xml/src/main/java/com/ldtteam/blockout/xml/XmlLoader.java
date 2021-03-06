package com.ldtteam.blockout.xml;

import com.ldtteam.blockout.loader.ILoader;
import com.ldtteam.blockout.loader.core.IUIElementData;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class XmlLoader implements ILoader
{
    /**
     * Creates a {@link IUIElementData} from a given {@link String}.
     *
     * @param data The data to load the UI from.
     * @return The element.
     */
    @NotNull
    @Override
    public IUIElementData loadDataFromDefinition(@NotNull final String data) throws Exception
    {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();

            final Document document = builder.parse(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));

            return new XmlUIElementData(document.getFirstChild(), null);
        } catch (Exception ex)
        {
            throw new IllegalArgumentException("Cannot parse XML data", ex);
        }
    }
}
