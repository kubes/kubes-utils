package com.denniskubes.webasset;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods for passing request configuration from the Spring controller
 * to the JSTL WebAssetTag for displaying assets.
 */
public class WebAssetUtils {

  /**
   * Adds the id to the request. This id is used by the JSTL WebAssetTag
   * to display assets on JSP pages.
   * 
   * @param request The current HttpServletRequest.
   * @param id The asset configuration id.
   */
  public static void setupRequest(HttpServletRequest request, String id) {
    request.setAttribute(WebAssetConstants.IDS, id);
  }

  /**
   * Adds the ids to the request. These ids are used by the JSTL WebAssetTag to
   *  display assets on JSP pages.
   * 
   * @param request The current HttpServletRequest.
   * @param ids The asset configuration ids.
   */
  public static void setupRequest(HttpServletRequest request, String[] ids) {
    String idStr = StringUtils.join(ids, ",");
    request.setAttribute(WebAssetConstants.IDS, idStr);
  }
}
