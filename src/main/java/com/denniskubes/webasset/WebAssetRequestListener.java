package com.denniskubes.webasset;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

public class WebAssetRequestListener
  implements ServletRequestListener {

  @Override
  public void requestInitialized(ServletRequestEvent sre) {
    // do nothing
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {
    // cleanup the ids when the request is destroyed
    WebAssetRequest.cleanup();
  }

}
