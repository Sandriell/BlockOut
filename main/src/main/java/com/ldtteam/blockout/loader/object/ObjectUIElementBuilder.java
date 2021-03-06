package com.ldtteam.blockout.loader.object;

import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.internal.MoreTypes;
import com.ldtteam.blockout.element.IUIElement;
import com.ldtteam.blockout.loader.core.IUIElementData;
import com.ldtteam.blockout.loader.core.IUIElementDataBuilder;
import com.ldtteam.blockout.loader.core.IUIElementMetaDataBuilder;
import com.ldtteam.blockout.loader.core.component.IUIElementDataComponent;
import com.ldtteam.blockout.loader.factory.core.IUIElementDataComponentConverter;
import com.ldtteam.blockout.proxy.ProxyHolder;
import com.ldtteam.blockout.util.Constants;
import com.ldtteam.blockout.util.stream.CollectorHelper;
import com.ldtteam.blockout.util.stream.FunctionHelper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ObjectUIElementBuilder implements IUIElementDataBuilder<ObjectUIElementData>
{
    private final List<ObjectUIElementData>                 children   = Lists.newArrayList();
    private       ObjectUIElementMetaData                   metaData;
    private       Map<String, ObjectUIElementDataComponent> attributes = new HashMap<>();

    public ObjectUIElementBuilder()
    {
    }

    @Override
    public IUIElementDataBuilder<ObjectUIElementData> copyFrom(final IUIElementData<?> elementData)
    {
        final ObjectUIElementDataComponent elementDataComponent = elementData.toDataComponent(new ObjectUIElementDataComponent());

        this.attributes = elementDataComponent.getAsMap()
                            .entrySet()
                            .stream()
                            .map(FunctionHelper::<String, IUIElementDataComponent, ObjectUIElementDataComponent>castMapValue)
                            .collect(CollectorHelper.entryToMapCollector());
        this.metaData = new ObjectUIElementMetaData(attributes, null);

        this.children.clear();

        return this;
    }

    @Override
    public IUIElementDataBuilder<ObjectUIElementData> withMetaData(final Consumer<IUIElementMetaDataBuilder<?>> builder)
    {
        final ObjectUIElementMetaDataBuilder metaDataBuilder = new ObjectUIElementMetaDataBuilder();
        builder.accept(metaDataBuilder);

        this.metaData = metaDataBuilder.build();

        return this;
    }

    @Override
    public IUIElementDataBuilder<ObjectUIElementData>
    addChild(final IUIElement element)
    {
        final ObjectUIElementData data = ProxyHolder.getInstance().getFactoryController().getDataFromElementWithBuilder(element, new ObjectUIElementBuilder());
        this.children.add(data);
        return this;
    }

    @Override
    public <T> IUIElementDataBuilder<ObjectUIElementData> addComponent(final String componentName, final T value, final Type typeToken)
    {
        final ParameterizedType type = new MoreTypes.ParameterizedTypeImpl(null, IUIElementDataComponentConverter.class, typeToken);
        final IUIElementDataComponentConverter<T> componentConverter = (IUIElementDataComponentConverter<T>) ProxyHolder.getInstance().getInjector().getInstance(Key.get(type));
        final ObjectUIElementDataComponent component = componentConverter.writeToElement(value, c -> new ObjectUIElementDataComponent());

        attributes.put(componentName, component);

        return this;
    }

    @Override
    public ObjectUIElementData build()
    {
        final ObjectUIElementDataComponent childrenComponent = new ObjectUIElementDataComponent();
        final List<IUIElementDataComponent> childrenData =
          children.stream().map(objectUIElementData -> objectUIElementData.toDataComponent(new ObjectUIElementDataComponent())).collect(Collectors.toList());

        if (attributes.containsKey(Constants.Controls.General.CONST_CHILDREN))
        {
            childrenData.addAll(attributes.get(Constants.Controls.General.CONST_CHILDREN).getAsList());
        }

        childrenComponent.setList(childrenData);
        attributes.put(Constants.Controls.General.CONST_CHILDREN, childrenComponent);

        final Map<String, ObjectUIElementDataComponent> combinedAttributes = metaData.mergeData(attributes);

        return new ObjectUIElementData(combinedAttributes, new ObjectUIElementMetaData(combinedAttributes, metaData.getParent().orElse(null)));
    }
}