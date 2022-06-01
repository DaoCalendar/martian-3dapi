package org.jzy3d.plot3d.rendering.canvas;

import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jzy3d.awt.AWTHelper;
import org.jzy3d.chart.IAnimator;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.chart.factories.NativePainterFactory;
import org.jzy3d.maths.Coord2d;
import org.jzy3d.maths.Dimension;
import org.jzy3d.painters.IPainter;
import org.jzy3d.plot3d.GPUInfo;
import org.jzy3d.plot3d.rendering.scene.Scene;
import org.jzy3d.plot3d.rendering.view.Renderer3d;
import org.jzy3d.plot3d.rendering.view.View;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * {@link CanvasAWT} is a base implementation that primarily allows to integrate a Jzy3d chart in an
 * AWT application.
 * 
 * Relying on JOGL's {@link GLPanel}, this canvas can actually be used in AWT, Swing, as well as SWT
 * through <code>org.jzy3d.bridge.swt.Bridge.adapt(swt,awt)</code>.
 * 
 * 
 * @author Martin Pernollet
 */
public class CanvasAWT extends GLCanvas implements IScreenCanvas, INativeCanvas {
  private static final long serialVersionUID = 980088854683562436L;

  protected double pixelScaleX;
  protected double pixelScaleY;
  protected View view;
  protected Renderer3d renderer;
  protected IAnimator animator;
  protected List<ICanvasListener> canvasListeners = new ArrayList<>();

  protected ScheduledExecutorService exec = new ScheduledThreadPoolExecutor(1);

  /**
   * Initialize a {@link CanvasAWT} attached to a {@link Scene}, with a given rendering
   * {@link Quality}.
   */
  public CanvasAWT(IChartFactory factory, Scene scene, Quality quality,
      GLCapabilitiesImmutable glci) {
    super(glci);

    view = scene.newView(this, quality);
    view.getPainter().setCanvas(this);

    renderer = newRenderer(factory);
    addGLEventListener(renderer);

    setAutoSwapBufferMode(quality.isAutoSwapBuffer());

    animator = factory.getPainterFactory().newAnimator(this);
    if (quality.isAnimated()) {
      animator.start();
    } else {
      animator.stop();
    }

    if(ALLOW_WATCH_PIXEL_SCALE)
      watchPixelScale();
    
    if (quality.isPreserveViewportSize())
      setPixelScale(newPixelScaleIdentity());
  }
  
  protected void watchPixelScale() {
    exec.schedule(new PixelScaleWatch() {
      @Override
      public double getPixelScaleY() {
        return CanvasAWT.this.getPixelScaleY();
      }
      @Override
      public double getPixelScaleX() {
        return CanvasAWT.this.getPixelScaleX();
      }
      @Override
      protected void firePixelScaleChanged(double pixelScaleX, double pixelScaleY) {
        CanvasAWT.this.firePixelScaleChanged(pixelScaleX, pixelScaleY);
      }
    }, 0, TimeUnit.SECONDS);
  }

  protected float[] newPixelScaleIdentity() {
    return new float[] {ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE};
  }

  protected Renderer3d newRenderer(IChartFactory factory) {
    return ((NativePainterFactory) factory.getPainterFactory()).newRenderer3D(view);
  }

  @Override
  public double getLastRenderingTimeMs() {
    return renderer.getLastRenderingTimeMs();
  }

  @Override
  public IAnimator getAnimation() {
    return animator;
  }

  @Override
  public void setPixelScale(float[] scale) {
    if (scale != null)
      setSurfaceScale(scale);
    else
      setSurfaceScale(new float[] {1f, 1f});
  }

  public void setPixelScale(Coord2d scale) {
	  setPixelScale(scale.toArray());
  }
  
  /**
   * Pixel scale is used to model the pixel ratio thay may be introduced by HiDPI or Retina
   * displays.
   */
  @Override
  public Coord2d getPixelScale() {
    return new Coord2d(getPixelScaleX(), getPixelScaleY());
  }
  
  @Override
  public Coord2d getPixelScaleJVM() {
    return new Coord2d(AWTHelper.getPixelScaleX(this), AWTHelper.getPixelScaleY(this));
  }

  public double getPixelScaleX() {
	double scale = getSurfaceWidth() / (double) getWidth();
	return scale;
	//double scale2 = AWTHelper.getPixelScaleX(this);
	//return Math.max(scale, scale2);
  }

  public double getPixelScaleY() {
    double scale = getSurfaceHeight() / (double) getHeight();
	return scale;
    //double scale2 = AWTHelper.getPixelScaleY(this);
	//return Math.max(scale, scale2);
  }


  /** Reset pixel scale to (1,1) */
  protected void resetPixelScale() {
    pixelScaleX = 1;
    pixelScaleY = 1;
  }

  @Override
  public void addCanvasListener(ICanvasListener listener) {
    canvasListeners.add(listener);
  }

  @Override
  public void removeCanvasListener(ICanvasListener listener) {
    canvasListeners.remove(listener);
  }

  @Override
  public List<ICanvasListener> getCanvasListeners() {
    return canvasListeners;
  }

  protected void firePixelScaleChanged(double pixelScaleX, double pixelScaleY) {
    for (ICanvasListener listener : canvasListeners) {
      listener.pixelScaleChanged(pixelScaleX, pixelScaleY);
    }
  }

  @Override
  public void dispose() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        if (animator != null)
          animator.stop();
        renderer = null;
        view = null;
      }
    }).start();
  }

  @Override
  public String getDebugInfo() {
    IPainter painter = getView().getPainter();
    
    GLCapabilitiesImmutable caps = getChosenGLCapabilities();
    
    GL gl = (GL) painter.acquireGL();
    GPUInfo info = GPUInfo.load(gl);
    painter.releaseGL();
    
    return "Capabilities  : " + caps + "\n" + info.toString();
  }

  @Override
  public void forceRepaint() {
    if (true) {
      // -- Method1 --
      // Display() is required to use the GLCanvas procedure and to ensure
      // that GL2 rendering occurs in the
      // GUI thread.
      // Actually it seems to be a bad idea, because this call implies a
      // rendering out of the excepted GL2 thread,
      // which:
      // - is slower than rendering in GL2 Thread
      // - throws java.lang.InterruptedException when rendering occurs
      // while closing the window
      display(); // JOGL
    } else {
      // -- Method2 --
      // Composite.repaint() is required with post/pre rendering, for
      // triggering PostRenderer rendering
      // at each frame (instead of ). The counterpart is that OpenGL2
      // rendering will occurs in the caller thread
      // and thus in the thread where the shoot() method was invoked (such
      // as AWT if shoot() is triggered
      // by a mouse event.
      //
      // Implies blinking with some JRE version (6.18, 6.17) but not with
      // some other (6.5)
      repaint(); // AWT
    }
  }

  @Override
  public void screenshot(File file) throws IOException {
    if (!file.getParentFile().exists())
      file.getParentFile().mkdirs();
    TextureData screen = screenshot();
    TextureIO.write(screen, file);
  }

  @Override
  public TextureData screenshot() {
    renderer.nextDisplayUpdateScreenshot();
    display();
    return renderer.getLastScreenshot();
  }

  public void triggerMouseEvent(java.awt.event.MouseEvent e) {
    processMouseEvent(e);
  }

  public void triggerMouseMotionEvent(java.awt.event.MouseEvent e) {
    processMouseMotionEvent(e);
  }

  public void triggerMouseWheelEvent(java.awt.event.MouseWheelEvent e) {
    processMouseWheelEvent(e);
  }

  @Override
  public void addMouseController(Object o) {
    addMouseListener((java.awt.event.MouseListener) o);
    if (o instanceof MouseWheelListener)
      addMouseWheelListener((MouseWheelListener) o);
    if (o instanceof MouseMotionListener)
      addMouseMotionListener((MouseMotionListener) o);
  }

  @Override
  public void addKeyController(Object o) {
    addKeyListener((java.awt.event.KeyListener) o);
  }

  @Override
  public void removeMouseController(Object o) {
    removeMouseListener((java.awt.event.MouseListener) o);
    if (o instanceof MouseWheelListener)
      removeMouseWheelListener((MouseWheelListener) o);
    if (o instanceof MouseMotionListener)
      removeMouseMotionListener((MouseMotionListener) o);
  }

  @Override
  public void removeKeyController(Object o) {
    removeKeyListener((java.awt.event.KeyListener) o);
  }

  @Override
  public GLAutoDrawable getDrawable() {
    return this;
  }

  @Override
  public Renderer3d getRenderer() {
    return renderer;
  }

  /** Provide a reference to the View that renders into this canvas. */
  @Override
  public View getView() {
    return view;
  }

  /**
   * Provide the actual renderer width for the open gl camera settings, which is obtained after a
   * resize event.
   */
  @Override
  @Deprecated // use getDimension() instead
  public int getRendererWidth() {
    return (renderer != null ? renderer.getWidth() : 0);
  }

  /**
   * Provide the actual renderer height for the open gl camera settings, which is obtained after a
   * resize event.
   */
  @Override
  @Deprecated // use getDimension() instead
  public int getRendererHeight() {
    return (renderer != null ? renderer.getHeight() : 0);
  }
  
  @Override
  public Dimension getDimension() {
    if(renderer!=null) {
      return new Dimension(renderer.getWidth(), renderer.getHeight());
    }
    else {
      return new Dimension(0, 0);
    }
  }
  
  @Override
  public boolean isNative() {
    return true;
  }


}
