package com.denniskubes.webasset;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


/**
 * A Request object wrapping a ThreadLocal that stores one or more web asset
 * request ids.  The request ids match up to ids in the web asset configuration
 * files, *.waf.
 * 
 * The web asset ids are retrieved by the WebAssetTag and used to place correct
 * web assets onto the final JSP page.  The WebAssetTag handles cleanup of the
 * ThreadLocal.
 */
public class WebAssetRequest {

  private static ThreadLocal<String> requestIds = new ThreadLocal<String>();

  /**
   * Adds the id to the request. This id is used by the JSTL WebAssetTag
   * to display assets on JSP pages.
   * 
   * @param id The asset configuration id.
   */
  public static void setup(String id) {
    requestIds.set(id);
  }

  /**
   * Adds the ids to the request. These ids are used by the JSTL WebAssetTag to
   *  display assets on JSP pages.
   * 
   * @param ids The asset configuration ids.
   */
  public static void setup(String[] ids) {
    WebAssetRequest.setup(Arrays.asList(ids));
  }

  /**
   * Adds the ids to the request. These ids are used by the JSTL WebAssetTag to
   *  display assets on JSP pages.
   * 
   * @param ids The asset configuration ids.
   */
  public static void setup(List<String> ids) {
    Set<String> idSet = new LinkedHashSet<String>();
    for (String id : ids) {
      idSet.add(StringUtils.trim(id));
    }
    requestIds.set(StringUtils.join(idSet, ","));
  }
  
  /**
   * Returns the current web asset ids for the request.
   * 
   * @return The current request's webasset ids as a comma separated string.
   */
  public static String get() {
    return requestIds.get();
  }

  /**
   * Removes the current requests webasset ids.
   */
  public static void cleanup() {
    requestIds.remove();
  }
}
