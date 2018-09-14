package com.minecolonies.blockout.element.advanced;

import com.google.common.collect.Lists;
import com.minecolonies.blockout.BlockOut;
import com.minecolonies.blockout.binding.dependency.DependencyObjectHelper;
import com.minecolonies.blockout.binding.dependency.IDependencyObject;
import com.minecolonies.blockout.binding.dependency.injection.DependencyObjectInjector;
import com.minecolonies.blockout.binding.dependency.injection.IDependencyDataProvider;
import com.minecolonies.blockout.binding.property.PropertyCreationHelper;
import com.minecolonies.blockout.builder.core.IBlockOutGuiConstructionData;
import com.minecolonies.blockout.builder.core.builder.IBlockOutGuiConstructionDataBuilder;
import com.minecolonies.blockout.builder.data.BlockOutGuiConstructionData;
import com.minecolonies.blockout.core.element.IUIElement;
import com.minecolonies.blockout.core.element.IUIElementHost;
import com.minecolonies.blockout.core.element.drawable.IChildDrawableUIElement;
import com.minecolonies.blockout.core.element.drawable.IDrawableUIElement;
import com.minecolonies.blockout.core.element.input.IClickAcceptingUIElement;
import com.minecolonies.blockout.core.element.input.IScrollAcceptingUIElement;
import com.minecolonies.blockout.core.element.values.*;
import com.minecolonies.blockout.core.factory.IUIElementFactory;
import com.minecolonies.blockout.core.management.render.IRenderManager;
import com.minecolonies.blockout.core.management.update.IUpdateManager;
import com.minecolonies.blockout.element.core.AbstractChildrenContainingUIElement;
import com.minecolonies.blockout.element.simple.Region;
import com.minecolonies.blockout.event.injector.EventHandlerInjector;
import com.minecolonies.blockout.loader.IUIElementData;
import com.minecolonies.blockout.loader.IUIElementDataBuilder;
import com.minecolonies.blockout.render.core.IRenderingController;
import com.minecolonies.blockout.style.resources.ImageResource;
import com.minecolonies.blockout.util.Log;
import com.minecolonies.blockout.util.math.BoundingBox;
import com.minecolonies.blockout.util.math.Clamp;
import com.minecolonies.blockout.util.math.Vector2d;
import com.minecolonies.blockout.util.mouse.MouseButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;

import static com.minecolonies.blockout.util.Constants.Controls.General.*;
import static com.minecolonies.blockout.util.Constants.Controls.List.*;

public class List extends AbstractChildrenContainingUIElement implements IScrollAcceptingUIElement, IClickAcceptingUIElement, IDrawableUIElement, IChildDrawableUIElement
{
    private static final int CONST_SCROLLBAR_SIZE = 5;

    //The current scroll state is not bindable. It is exclusively controlled by the control it self.
    private       double                    scrollOffset;

    //Bindable resource binding.
    private IDependencyObject<ResourceLocation>             templateResource;
    private IDependencyObject<IBlockOutGuiConstructionData> templateConstructionData;

    private IDependencyObject<Object> source;
    private IDependencyObject<ResourceLocation> scrollBarBackgroundResource;
    private IDependencyObject<ResourceLocation> scrollBarForegroundResource;
    private boolean                             dataBoundMode;

    private IDependencyObject<Boolean> showScrollbar;
    private IDependencyObject<ControlDirection> orientation;

    public List(
      @NotNull final IDependencyObject<ResourceLocation> style, @NotNull final String id, @Nullable final IUIElementHost parent)
    {
        super(KEY_LIST, style, id, parent);
        this.dataBoundMode = false;
        this.source = DependencyObjectHelper.createFromValue(Lists.newArrayList());
        this.templateResource = DependencyObjectHelper.createFromValue(new ResourceLocation(""));
        this.scrollBarBackgroundResource = DependencyObjectHelper.createFromValue(new ResourceLocation(""));
        this.scrollBarForegroundResource = DependencyObjectHelper.createFromValue(new ResourceLocation(""));
        this.templateConstructionData = DependencyObjectHelper.createFromValue(new BlockOutGuiConstructionData());
        this.orientation = DependencyObjectHelper.createFromValue(ControlDirection.TOP_BOTTOM);
        this.showScrollbar = DependencyObjectHelper.createFromValue(true);
    }

    public List(
      @NotNull final IDependencyObject<ResourceLocation> style,
      @NotNull final String id,
      @Nullable final IUIElementHost parent,
      @NotNull final IDependencyObject<EnumSet<Alignment>> alignments,
      @NotNull final IDependencyObject<Dock> dock,
      @NotNull final IDependencyObject<AxisDistance> margin,
      @NotNull final IDependencyObject<Vector2d> elementSize,
      @NotNull final IDependencyObject<AxisDistance> padding,
      @NotNull final IDependencyObject<Object> dataContext,
      @NotNull final IDependencyObject<Boolean> visible,
      @NotNull final IDependencyObject<Boolean> enabled,
      @NotNull final boolean dataBoundMode,
      @NotNull final IDependencyObject<ResourceLocation> templateResource,
      @NotNull final IDependencyObject<ResourceLocation> scrollBarBackgroundResource,
      @NotNull final IDependencyObject<ResourceLocation> scrollBarForegroundResource,
      @NotNull final double scrollOffset,
      @NotNull final IDependencyObject<ControlDirection> orientation,
      @NotNull final IDependencyObject<Boolean> showScrollbar,
      @NotNull final IDependencyObject<Object> source)
    {
        super(KEY_LIST, style, id, parent, alignments, dock, margin, elementSize, padding, dataContext, visible, enabled);
        this.dataBoundMode = dataBoundMode;
        this.templateResource = templateResource;
        this.templateConstructionData = DependencyObjectHelper.createFromValue(new BlockOutGuiConstructionData());
        this.scrollBarBackgroundResource = scrollBarBackgroundResource;
        this.scrollBarForegroundResource = scrollBarForegroundResource;
        this.scrollOffset = scrollOffset;
        this.orientation = orientation;
        this.showScrollbar = showScrollbar;
        this.source = source;
    }

    @Override
    public void update(@NotNull final IUpdateManager updateManager)
    {
        super.update(updateManager);

        if (!dataBoundMode)
        {
            return;
        }

        updateChildrenInDataBoundMode(updateManager);
    }

    @Override
    public void onMouseScroll(final int localX, final int localY, final int deltaWheel)
    {
        scroll(deltaWheel / getTotalContentLength() / -10);
    }

    @Override
    public boolean canAcceptMouseInput(final int localX, final int localY, final MouseButton button)
    {
        final BoundingBox localBox = getLocalBoundingBox();
        if (!localBox.includes(new Vector2d(localX, localY)))
        {
            return false;
        }

        if (getOrientation() == ControlDirection.TOP_BOTTOM)
        {
            final double offset = localBox.getSize().getX() - localX;
            return offset <= CONST_SCROLLBAR_SIZE;
        }
        else {
            final double offset = localBox.getSize().getY() - localY;
            return offset <= CONST_SCROLLBAR_SIZE;
        }
    }

    @Override
    public void onMouseClickBegin(final int localX, final int localY, final MouseButton button)
    {
        onScrollBarClick(localX, localY);
    }

    @Override
    public void onMouseClickEnd(final int localX, final int localY, final MouseButton button)
    {
        onScrollBarClick(localX, localY);
    }

    @Override
    public void onMouseClickMove(final int localX, final int localY, final MouseButton button, final float timeElapsed)
    {
        onScrollBarClick(localX, localY);
    }

    /**
     * Called by the rendering manager before the drawing of the background of our children starts.
     *
     * @param manager The manager.
     */
    @Override
    public void preBackgroundDrawOfChildren(@NotNull final IRenderManager manager)
    {
        manager.getRenderingController().getScissoringController().push(getAbsoluteInternalBoundingBox());
    }

    /**
     * Called by the rendering manager after the drawing of the background of our children ended.
     *
     * @param manager The manager.
     */
    @Override
    public void postBackgroundDrawOfChildren(@NotNull final IRenderManager manager)
    {
        manager.getRenderingController().getScissoringController().pop();
    }

    /**
     * Called by the rendering manager before the drawing of the foreground of our children starts.
     *
     * @param manager The manager.
     */
    @Override
    public void preForegroundDrawOfChildren(@NotNull final IRenderManager manager)
    {
        manager.getRenderingController().getScissoringController().push(getAbsoluteInternalBoundingBox());
    }

    /**
     * Called by the rendering manager after the drawing of the foreground of our children ended.
     *
     * @param manager The manager.
     */
    @Override
    public void postForegroundDrawOfChildren(@NotNull final IRenderManager manager)
    {
        manager.getRenderingController().getScissoringController().pop();
    }

    @Override
    public void drawBackground(@NotNull final IRenderingController controller)
    {
        if (!getShowScrollBar())
            return;

        GlStateManager.pushMatrix();

        if (getOrientation() == ControlDirection.TOP_BOTTOM)
        {
            final double localHeight = getLocalBoundingBox().getSize().getY();
            final double barLength = getScrollBarLength();
            final double maxOffset = localHeight - barLength;

            if (maxOffset < 1)
            {
                GlStateManager.popMatrix();
                return;
            }

            final Vector2d scrollBoxOrigin = getLocalBoundingBox().getLowerRightCoordinate().move(-CONST_SCROLLBAR_SIZE, 0).nullifyNegatives();
            final Vector2d scrollBoxSize = new Vector2d(CONST_SCROLLBAR_SIZE, getLocalBoundingBox().getSize().getY());

            final BoundingBox scrollBox = new BoundingBox(scrollBoxOrigin, scrollBoxSize);

            final Vector2d scrollBarOrigin = getLocalBoundingBox().getLowerRightCoordinate().move(-CONST_SCROLLBAR_SIZE, scrollOffset * maxOffset).nullifyNegatives();
            final Vector2d scrollBarSize = new Vector2d(CONST_SCROLLBAR_SIZE, barLength);

            final BoundingBox scrollBarBox = new BoundingBox(scrollBarOrigin, scrollBarSize);

            drawSrollbarBackground(controller, scrollBox);
            drawSrollbarForeground(controller, scrollBarBox);
        }
        else
        {
            final double localWidth = getLocalBoundingBox().getSize().getX();
            final double barLength = getScrollBarLength();
            final double maxOffset = localWidth - barLength;


            if (maxOffset < 1)
            {
                GlStateManager.popMatrix();
                return;
            }

            final Vector2d scrollBoxOrigin = getLocalBoundingBox().getLowerLeftCoordinate().move(0 , getLocalBoundingBox().getSize().getY()-CONST_SCROLLBAR_SIZE).nullifyNegatives();
            final Vector2d scrollBoxSize = new Vector2d(getLocalBoundingBox().getSize().getX(), CONST_SCROLLBAR_SIZE);

            final BoundingBox scrollBox = new BoundingBox(scrollBoxOrigin, scrollBoxSize);

            final Vector2d scrollBarOrigin = getLocalBoundingBox().getLowerLeftCoordinate().move(scrollOffset * maxOffset, getLocalBoundingBox().getSize().getY()-CONST_SCROLLBAR_SIZE).nullifyNegatives();
            final Vector2d scrollBarSize = new Vector2d(barLength, CONST_SCROLLBAR_SIZE);

            final BoundingBox scrollBarBox = new BoundingBox(scrollBarOrigin, scrollBarSize);

            drawSrollbarBackground(controller, scrollBox);
            drawSrollbarForeground(controller, scrollBarBox);
        }

        GlStateManager.popMatrix();
    }

    @Override
    public void drawForeground(@NotNull final IRenderingController controller)
    {

    }

    public IBlockOutGuiConstructionData getTemplateConstructionData()
    {
        return templateConstructionData.get(this);
    }

    /**
     * Updates the children when the control is in databound mode via its datacontext.
     *
     * @param updateManager The update manager to mark the ui dirty when the control changed.
     */
    private void updateChildrenInDataBoundMode(@NotNull final IUpdateManager updateManager)
    {
        if (!source.hasChanged(getDataContext())
              && !templateResource.hasChanged(getDataContext())
              && !templateConstructionData.hasChanged(getDataContext()))
        {
            //Noop needed the lists are equal so no modifications are needed.
            return;
        }

        final Object newDataContext = source.get(this);
        final Collection<?> newData;
        if (newDataContext instanceof Collection)
        {
            newData = (Collection<?>) newDataContext;
        }
        else
        {
            newData = Lists.newArrayList();
        }

        //Context has changed.
        //Mark the ui as dirty.
        updateManager.markDirty();

        //Clear all children.
        clear();

        //Create control instances from the template.
        int index = 0;
        for (final Object context :
          newData)
        {
            final IUIElement element = BlockOut.getBlockOut().getProxy().getTemplateEngine().generateFromTemplate(
              this,
              DependencyObjectHelper.createFromValue(context)
              ,
              getTemplateResource(),
              String.format("%s_%d", getId(), index++));

            if (getTemplateConstructionData() != null)
            {
                @NotNull final IBlockOutGuiConstructionData data = getTemplateConstructionData();
                DependencyObjectInjector.inject(element, data);
                EventHandlerInjector.inject(element, data);

                if (element instanceof IUIElementHost)
                {
                    IUIElementHost iuiElementHost = (IUIElementHost) element;
                    iuiElementHost.getAllCombinedChildElements().values().forEach(c ->  {
                        DependencyObjectInjector.inject(c, data);
                        EventHandlerInjector.inject(c, data);
                    });
                }
            }

            wrapNewElementAndRegister(element);
        }

        updateScrollOffset();
    }

    /**
     * Returns the template resource directly from the context.
     *
     * @return The template resource.
     */
    public ResourceLocation getTemplateResource()
    {
        return templateResource.get(this);
    }

    private void drawSrollbarBackground(@NotNull final IRenderingController controller, @NotNull final BoundingBox scrollBox)
    {
        final ImageResource resource = getScrollBarBackground();

        GlStateManager.pushMatrix();

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.color(1, 1, 1);

        controller.bindTexture(resource.getDiskLocation());
        controller.drawTexturedModalRect(scrollBox.getLocalOrigin(),
          scrollBox.getSize(),
          resource.getOffset(),
          resource.getSize(),
          resource.getFileSize());

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();

        GlStateManager.popMatrix();
    }

    private void drawSrollbarForeground(@NotNull final IRenderingController controller, @NotNull final BoundingBox scrollBarBox)
    {
        final ImageResource resource = getScrollBarForeground();

        GlStateManager.pushMatrix();

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        GlStateManager.color(1, 1, 1);

        controller.bindTexture(resource.getDiskLocation());
        controller.drawTexturedModalRect(scrollBarBox.getLocalOrigin(),
          scrollBarBox.getSize(),
          resource.getOffset(),
          resource.getSize(),
          resource.getFileSize());

        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();

        GlStateManager.popMatrix();
    }

    @NotNull
    public ImageResource getScrollBarBackground()
    {
        return getResource(getScrollBarBackgroundResource());
    }

    @NotNull
    public ImageResource getScrollBarForeground()
    {
        return getResource(getScrollBarForegroundResource());
    }

    @NotNull
    public ResourceLocation getScrollBarBackgroundResource()
    {
        return scrollBarBackgroundResource.get(this);
    }

    @NotNull 
    public Boolean getShowScrollBar()
    {
        return showScrollbar.get(this);
    }
    
    public void setShowScrollbar(@NotNull final Boolean showScrollbar)
    {
        this.showScrollbar.set(this,  showScrollbar);
    }
    
    @NotNull
    public ControlDirection getOrientation()
    {
        final ControlDirection controlDirection = orientation.get(this);
        if (controlDirection != ControlDirection.TOP_BOTTOM && controlDirection != ControlDirection.LEFT_RIGHT)
        {
            Log.getLogger().error("Failed to get valid ControlDirection for list: " + getId() + " current: " + controlDirection + " Valid are: TOP_BOTTOM, LEFT_RIGHT.");
            setOrientation(ControlDirection.TOP_BOTTOM);
            
            return ControlDirection.TOP_BOTTOM;
        }
        
        return controlDirection;
    }
    
    public void setOrientation(@NotNull final ControlDirection orientation)
    {
        this.orientation.set(this,  orientation);
    }
    
    private boolean updateScrollOffset()
    {
        final ControlDirection currentOrientation = getOrientation();
        boolean requiresUpdate = false;
        double maxOffset = currentOrientation == ControlDirection.TOP_BOTTOM ? (getTotalContentLength() - getLocalInternalBoundingBox().getSize().getY()) * scrollOffset : (getTotalContentLength() - getLocalInternalBoundingBox().getSize().getX()) * scrollOffset;

        if (maxOffset <= 0)
        {
            maxOffset =0;
        }

        double currentUsedOffset = 0d;
        for (IUIElement wrapper : values())
        {
            final AxisDistance newMargin;
            if (currentOrientation == ControlDirection.TOP_BOTTOM)
            {
                newMargin = new AxisDistanceBuilder().setLeft(Optional.of(0d)).setRight(Optional.of(0d)).setTop(Optional.of(-maxOffset + currentUsedOffset)).create();
            }
            else {
                newMargin =
                  new AxisDistanceBuilder().setLeft(Optional.of(-maxOffset + currentUsedOffset)).setTop(Optional.of(0d)).setBottom(Optional.of(0d)).create();
            }

            if (!newMargin.equals(wrapper.getMargin()))
            {
                requiresUpdate = true;
            }

            wrapper.setMargin(newMargin);
            wrapper.update(getParent().getUiManager().getUpdateManager());
            currentUsedOffset += (currentOrientation == ControlDirection.TOP_BOTTOM ? wrapper.getElementSize().getY() : wrapper.getElementSize().getX());
        }

        if (requiresUpdate && isVisible())
        {
            values().stream().forEach(e -> e.setVisible(
              this.getAbsoluteInternalBoundingBox().intersects(e.getAbsoluteBoundingBox())
              )
            );
        }

        return requiresUpdate;
    }

    /**
     * Returns the total content height based on all children contained in this list.
     *
     * @return The total content height.
     */
    private double getTotalContentLength()
    {
        if (getOrientation() == ControlDirection.TOP_BOTTOM)
        {
            return this.values().stream().mapToDouble(u -> u.getLocalBoundingBox().getSize().getY()).sum();    
        }

        return this.values().stream().mapToDouble(u -> u.getLocalBoundingBox().getSize().getX()).sum();        
    }

    @Override
    public boolean canAcceptMouseInput(final int localX, final int localY, final int deltaWheel)
    {
        //We accept all mouse input that is in our box.
        return true;
    }

    public void setTemplateConstructionData(final @NotNull IBlockOutGuiConstructionData templateConstructionData)
    {
        this.templateConstructionData.set(this,  templateConstructionData);
    }

    private void wrapNewElementAndRegister(@NotNull final IUIElement element)
    {
        final Region wrappingRegion = new Region(
          DependencyObjectHelper.createFromValue(getStyleId()),
          String.format("%s_wrapper", element.getId()),
          this
        );

        DependencyObjectInjector.inject(wrappingRegion, new IDependencyDataProvider()
        {
            @Override
            public boolean hasDependencyData(@NotNull final String name)
            {
                return name.contains("elementSize");
            }

            @NotNull
            @Override
            public <T> IDependencyObject<T> get(@NotNull final String name)
            {
                return (IDependencyObject<T>) DependencyObjectHelper.createFromProperty(
                  PropertyCreationHelper.create(context -> Optional.of(new Vector2d(
                    getOrientation() == ControlDirection.TOP_BOTTOM ? getLocalInternalBoundingBox().getSize().getX() : element.getMinimalInternalSizeOfParent().getX(),
                    getOrientation() == ControlDirection.LEFT_RIGHT ? getLocalInternalBoundingBox().getSize().getY() : element.getMinimalInternalSizeOfParent().getY()
                  )), (context, value) -> {
                      //Noop
                  }), new Vector2d());
            }
        });

        element.setParent(wrappingRegion);

        wrappingRegion.put(element.getId(), element);
        wrappingRegion.setParent(this);

        wrappingRegion.update(getUiManager().getUpdateManager());
        element.update(getParent().getUiManager().getUpdateManager());
        put(wrappingRegion.getId(), wrappingRegion);
    }

    public void setScrollBarBackgroundResource(@NotNull final ResourceLocation scrollBarBackground)
    {
        this.scrollBarBackgroundResource.set(this,  scrollBarBackground);
    }

    @NotNull
    public ResourceLocation getScrollBarForegroundResource()
    {
        return scrollBarForegroundResource.get(this);
    }

    public void setScrollBarForegroundResource(@NotNull final ResourceLocation scrollBarForeground)
    {
        this.scrollBarForegroundResource.set(this,  scrollBarForeground);
    }

    /**
     * Sets the template resource.
     *
     * @param templateResource The new template resource.
     */
    public void setTemplateResource(@NotNull final ResourceLocation templateResource)
    {
        this.templateResource.set(this,  templateResource);
        this.dataBoundMode = true;
        updateChildrenInDataBoundMode(getUiManager().getUpdateManager());
    }

    public boolean isDataBoundMode()
    {
        return dataBoundMode;
    }

    public void setDataBoundMode(final boolean dataBoundMode)
    {
        this.dataBoundMode = dataBoundMode;
    }

    /**
     * Updates the scroll position with a given delta.
     *
     * @param delta The delta to scroll.
     */
    public void scroll(double delta)
    {
        final double newScrollOffset = scrollOffset + delta;
        scrollTo(newScrollOffset);
    }

    /**
     * Updates the scroll position by scrolling to a given target.
     * Given value is clamped to the minimal and maximal scroll value.
     *
     * @param target The target to scroll to.
     */
    public void scrollTo(double target)
    {
        this.scrollOffset = Clamp.Clamp(0, target, 1);
        updateScrollOffset();
        getParent().getUiManager().getUpdateManager().markDirty();
    }

    /**
     * Called when a click is made on the scrollbar.
     *
     * @param localY The localY of set of the click.
     */
    private void onScrollBarClick(int localX, int localY)
    {
        if (getOrientation() == ControlDirection.TOP_BOTTOM)
        {
            final double localHeight = getLocalBoundingBox().getSize().getY();
            final double barHeight = getScrollBarLength();
            final double maxOffset = localHeight - barHeight;

            scrollTo(localY / maxOffset);
        }
        else
        {
            final double localWidth = getLocalBoundingBox().getSize().getX();
            final double barWidth = getScrollBarLength();
            final double maxOffset = localWidth - barWidth;

            scrollTo(localX / maxOffset);

        }
    }

    /**
     * Returns the height of the scrolling element of the scrollbar.
     *
     * @return The height.
     */
    private int getScrollBarLength()
    {
        final BoundingBox localBox = getLocalBoundingBox();
        final double contentLength = getTotalContentLength();

        if (getOrientation() == ControlDirection.TOP_BOTTOM)
        {
            return (int) Math.min(localBox.getSize().getY(), (localBox.getSize().getY() / contentLength) * localBox.getSize().getY());    
        }

        return (int) Math.min(localBox.getSize().getX(), (localBox.getSize().getX() / contentLength) * localBox.getSize().getX());
    }

    public static final class ListConstructionDataBuilder extends AbstractChildrenContainingUIElement.SimpleControlConstructionDataBuilder<ListConstructionDataBuilder, List>
    {

        public ListConstructionDataBuilder(
          final String controlId,
          final IBlockOutGuiConstructionDataBuilder data)
        {
            super(controlId, data, List.class);
        }
        //templateConstructionData

        @NotNull
        public ListConstructionDataBuilder withDependentTemplateResource(@NotNull final IDependencyObject<ResourceLocation> iconResource)
        {
            return withDependency("templateResource", iconResource);
        }

        @NotNull
        public ListConstructionDataBuilder withTemplateResource(@NotNull final ResourceLocation iconResource)
        {
            return withDependency("templateResource", DependencyObjectHelper.createFromValue(iconResource));
        }

        @NotNull
        public ListConstructionDataBuilder withDependentTemplateConstructionData(@NotNull final IDependencyObject<IBlockOutGuiConstructionData> iconConstructionData)
        {
            return withDependency("templateConstructionData", iconConstructionData);
        }

        @NotNull
        public ListConstructionDataBuilder withTemplateConstructionData(@NotNull final IBlockOutGuiConstructionData iconConstructionData)
        {
            return withDependency("templateConstructionData", DependencyObjectHelper.createFromValue(iconConstructionData));
        }
    }

    public static final class Factory implements IUIElementFactory<List>
    {

        /**
         * Returns the type that this factory builds.
         *
         * @return The type.
         */
        @NotNull
        @Override
        public ResourceLocation getType()
        {
            return KEY_LIST;
        }

        /**
         * Creates a new {@link List} from the given {@link IUIElementData}.
         *
         * @param elementData The {@link IUIElementData} which contains the data that is to be constructed.
         * @return The {@link List} that is stored in the {@link IUIElementData}.
         */
        @NotNull
        @Override
        public List readFromElementData(@NotNull final IUIElementData elementData)
        {
            final IDependencyObject<ResourceLocation> style = elementData.getBoundStyleId();
            final String id = elementData.getElementId();
            final IDependencyObject<EnumSet<Alignment>> alignments = elementData.getBoundAlignmentAttribute(CONST_ALIGNMENT);
            final IDependencyObject<Dock> dock = elementData.getBoundEnumAttribute(CONST_DOCK, Dock.class, Dock.NONE);
            final IDependencyObject<AxisDistance> margin = elementData.getBoundAxisDistanceAttribute(CONST_MARGIN);
            final IDependencyObject<AxisDistance> padding = elementData.getBoundAxisDistanceAttribute(CONST_PADDING);
            final IDependencyObject<Vector2d> elementSize = elementData.getBoundVector2dAttribute(CONST_ELEMENT_SIZE);
            final IDependencyObject<Object> dataContext = elementData.getBoundDataContext();
            final IDependencyObject<Boolean> visible = elementData.getBoundBooleanAttribute(CONST_VISIBLE);
            final IDependencyObject<Boolean> enabled = elementData.getBoundBooleanAttribute(CONST_ENABLED);
            final IDependencyObject<ResourceLocation> scrollBarBackgroundResource = elementData.getBoundResourceLocationAttribute(CONST_SCROLL_BACKGROUND);
            final IDependencyObject<ResourceLocation> scrollBarForegroundResource = elementData.getBoundResourceLocationAttribute(CONST_SCROLL_FOREGROUND);
            final double scrollOffset = elementData.getDoubleAttribute(CONST_SCROLLOFFSET);
            final IDependencyObject<ControlDirection> orientation = elementData.getBoundControlDirectionAttribute(CONST_ORIENTATION, ControlDirection.TOP_BOTTOM);
            final IDependencyObject<Boolean> showScrollBar = elementData.getBoundBooleanAttribute(CONST_SHOW_BAR);

            if (elementData.hasChildren())
            {
                final List list = new List(
                  style,
                  id,
                  elementData.getParentView(),
                  alignments,
                  dock,
                  margin,
                  elementSize,
                  padding,
                  dataContext,
                  visible,
                  enabled,
                  false,
                  DependencyObjectHelper.createFromValue(new ResourceLocation("")),
                  scrollBarBackgroundResource,
                  scrollBarForegroundResource,
                  scrollOffset,
                  orientation,
                  showScrollBar,
                  DependencyObjectHelper.createFromValue(Lists.newArrayList())
                );

                elementData.getChildren(list).forEach(childData -> {
                    IUIElement child = BlockOut.getBlockOut().getProxy().getFactoryController().getElementFromData(childData);
                    list.put(child.getId(), child);
                });

                return list;
            }

            final IDependencyObject<ResourceLocation> templateResource = elementData.getBoundResourceLocationAttribute(CONST_TEMPLATE);
            final IDependencyObject<Object> source = elementData.getBoundObject(CONST_SOURCE, Lists.newArrayList());

            return new List(
              style,
              id,
              elementData.getParentView(),
              alignments,
              dock,
              margin,
              elementSize,
              padding,
              dataContext,
              visible,
              enabled,
              true,
              templateResource,
              scrollBarBackgroundResource,
              scrollBarForegroundResource,
              scrollOffset,
              orientation,
              showScrollBar,
              source
            );
        }

        /**
         * Populates the given {@link IUIElementDataBuilder} with the data from {@link List} so that
         * the given {@link List} can be reconstructed with {@link #readFromElementData(IUIElementData)} created by
         * the given {@link IUIElementDataBuilder}.
         *
         * @param element The {@link List} to write into the {@link IUIElementDataBuilder}
         * @param builder The {@link IUIElementDataBuilder} to write the {@link List} into.
         */
        @Override
        public void writeToElementData(@NotNull final List element, @NotNull final IUIElementDataBuilder builder)
        {
            builder
              .addAlignment(CONST_ALIGNMENT, element.getAlignment())
              .addEnum(CONST_DOCK, element.getDock())
              .addAxisDistance(CONST_MARGIN, element.getMargin())
              .addVector2d(CONST_ELEMENT_SIZE, element.getElementSize())
              .addAxisDistance(CONST_PADDING, element.getPadding())
              .addBoolean(CONST_VISIBLE, element.isVisible())
              .addBoolean(CONST_ENABLED, element.isEnabled())
              .addResourceLocation(CONST_SCROLL_BACKGROUND, element.getScrollBarBackgroundResource())
              .addResourceLocation(CONST_SCROLL_FOREGROUND, element.getScrollBarForegroundResource())
              .addDouble(CONST_SCROLLOFFSET, element.scrollOffset)
              .addControlDirection(CONST_ORIENTATION, element.getOrientation())
              .addBoolean(CONST_SHOW_BAR, element.getShowScrollBar());

            element.values().forEach(child -> {
                builder.addChild(BlockOut.getBlockOut().getProxy().getFactoryController().getDataFromElement(child));
            });
        }
    }
}
