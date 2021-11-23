package org.jzy3d.demos.shaders;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.ChartLauncher;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.chart.factories.AWTPainterFactory;
import org.jzy3d.chart.factories.IChartFactory;
import org.jzy3d.maths.Rectangle;
import org.jzy3d.plot3d.rendering.canvas.Quality;
import org.jzy3d.plot3d.rendering.shaders.IShaderable;
import org.jzy3d.plot3d.rendering.shaders.ShaderRenderer3d;
import org.jzy3d.plot3d.rendering.shaders.Shaderable;
import org.jzy3d.plot3d.rendering.shaders.mandelbrot.MandelBrotShader;
import org.jzy3d.plot3d.rendering.shaders.mandelbrot.TexSurface;
import org.jzy3d.plot3d.rendering.view.Renderer3d;
import org.jzy3d.plot3d.rendering.view.View;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;


public class ShaderMandelbrotDemo {


  public static void main(String[] args) {
    Chart chart = initChart(new MandelBrotShader());

    chart.getScene().getGraph().add(new TexSurface());

    chart.getView().setAxisDisplayed(false);
    ChartLauncher.openChart(chart, new Rectangle(0, 0, 600, 600));
  }


  public static Chart initChart(final IShaderable s) {
    GLProfile profile = GLProfile.getMaxProgrammable(true);
    GLCapabilities capabilities = new GLCapabilities(profile);
    capabilities.setHardwareAccelerated(false);

    AWTPainterFactory painter = new AWTPainterFactory(capabilities) {
      @Override
      public Renderer3d newRenderer3D(View view) {
        ShaderRenderer3d r = new ShaderRenderer3d(view, traceGL, debugGL, new Shaderable());
        return r;
      }
    };

    IChartFactory factory = new AWTChartFactory(painter);

    Chart chart = factory.newChart(Quality.Intermediate());
    chart.getView().setSquared(false);

    // chart.getView().setCameraMode(CameraMode.PERSPECTIVE);
    return chart;
  }

  public static boolean CHART_CANVAS_AUTOSWAP = true;
}
