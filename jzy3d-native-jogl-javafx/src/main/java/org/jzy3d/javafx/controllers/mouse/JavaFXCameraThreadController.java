package org.jzy3d.javafx.controllers.mouse;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.controllers.thread.camera.CameraThreadController;
import javafx.application.Platform;

/**
 * Trying to get rid of an exception
 * https://stackoverflow.com/questions/860187/access-restriction-on-class-due-to-restriction-on-required-library-rt-jar
 * 
 * @author Martin Pernollet
 *
 */
public class JavaFXCameraThreadController extends CameraThreadController {
  protected boolean doLater = false;

  public JavaFXCameraThreadController() {
    super();
  }

  public JavaFXCameraThreadController(Chart chart) {
    super(chart);
  }

  @Override
  public void run() {
    if (doLater) {
      Platform.runLater(new Runnable() {
        @Override
        public void run() {
          JavaFXCameraThreadController.super.doRun();
        }
      });

    } else {
      doRun();
    }
  }
}
