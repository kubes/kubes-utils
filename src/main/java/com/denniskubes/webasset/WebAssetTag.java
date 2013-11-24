package com.denniskubes.webasset;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.tags.RequestContextAwareTag;

/**
 * <p>JSTL Tag that writes out web assets including script tags, link tags,
 * meta tags, and titles.</p>
 * 
 * <p>Options can be set to include global assets and to include dynamic assets.
 * Global assets are configured in the global webasset config file. Dynamic
 * assets are setup in the request by the Spring controller.</p>
 * 
 * <p>There are four different asset type that can be written. The are script,
 * meta, link, and title. Script writes out script tags, usually javascript.
 * Meta writes out meta tags. Link writes out link tags, usually stylesheets.
 * And title writes out the page title.</p>
 * 
 * <p>If ids are specified on the tag they override any ids setup in the Spring
 * controller. Usually one or more ids are specified by the controller and put
 * into the request. Ids are only specified on the tag in special cases, such as
 * when you have specific scripts that run in a specific location on a page. An
 * example of this would be analytics or advertisements.</p>
 */
public class WebAssetTag
  extends RequestContextAwareTag {

  private String types;
  private String ids;
  private boolean includeGlobal = false;
  private boolean includeDynamic = false;

  public void setTypes(String types) {
    this.types = types;
  }

  public void setIds(String ids) {
    this.ids = ids;
  }

  public void setIncludeGlobal(boolean includeGlobal) {
    this.includeGlobal = includeGlobal;
  }

  public void setIncludeDynamic(boolean includeDynamic) {
    this.includeDynamic = includeDynamic;
  }

  private void writeTitleTag(Set<String> ids)
    throws IOException {

    HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
    WebApplicationContext context = RequestContextUtils.getWebApplicationContext(request);
    WebAssetManager wam = (WebAssetManager)context.getBean("webAssetManager");
    JspWriter out = pageContext.getOut();
    Locale locale = request.getLocale();

    // start out with the global title
    String title = wam.getGlobalTitle(locale);

    // use only the first title found for an id, can't have multiple
    for (String id : ids) {
      String idTitle = wam.getTitleForId(id, locale);
      if (StringUtils.isNotBlank(idTitle)) {
        title = idTitle;
        break;
      }
    }

    // dynamic title overrides
    if (includeDynamic) {
      String dynTitle = (String)request.getAttribute(WebAssetConstants.REQUEST_TITLE);
      if (StringUtils.isNotBlank(dynTitle)) {
        title = dynTitle;
      }
    }

    // write out the title
    if (StringUtils.isNotBlank(title)) {
      StringBuilder titleTagBuilder = new StringBuilder();
      titleTagBuilder.append("<title>");
      titleTagBuilder.append(title);
      titleTagBuilder.append("</title>");
      out.print(titleTagBuilder.toString() + "\n");
      request.setAttribute(WebAssetConstants.TITLE, title);
    }
  }

  private void writeScriptTags(Set<String> ids)
    throws IOException {

    HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
    WebApplicationContext context = RequestContextUtils.getWebApplicationContext(request);
    WebAssetManager wam = (WebAssetManager)context.getBean("webAssetManager");
    JspWriter out = pageContext.getOut();
    Locale locale = request.getLocale();

    Set<Map<String, String>> allScripts = new LinkedHashSet<Map<String, String>>();
    if (includeGlobal) {
      List<Map<String, String>> globalScripts = wam.getGlobalScripts(locale);
      if (globalScripts != null && globalScripts.size() > 0) {
        allScripts.addAll(globalScripts);
      }
    }

    for (String id : ids) {
      List<Map<String, String>> idScripts = wam.getScriptsForId(id, locale);
      if (idScripts != null && idScripts.size() > 0) {
        allScripts.addAll(idScripts);
      }
    }

    // include dynamic script tags
    if (includeDynamic) {
      List<Map<String, String>> requestScripts = (List<Map<String, String>>)request.getAttribute(WebAssetConstants.REQUEST_SCRIPTS);
      if (requestScripts != null && requestScripts.size() > 0) {
        allScripts.addAll(requestScripts);
      }
    }

    // write out the scripts
    if (allScripts.size() > 0) {
      for (Map<String, String> scriptAttrs : allScripts) {
        StringBuilder scriptTagBuilder = new StringBuilder();
        String type = scriptAttrs.get("type");
        String src = scriptAttrs.get("path");
        scriptTagBuilder.append("<script");
        if (StringUtils.isNotBlank(type)) {
          scriptTagBuilder.append(" type=\"" + type + "\"");
        }
        if (StringUtils.isNotBlank(src)) {
          scriptTagBuilder.append(" src=\"" + src + "\"");
        }
        scriptTagBuilder.append(">");
        scriptTagBuilder.append("</script>");
        out.print(scriptTagBuilder.toString() + "\n");
      }
      request.setAttribute(WebAssetConstants.SCRIPTS, allScripts);
    }
  }

  private void writeLinkTags(Set<String> ids)
    throws IOException {

    HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
    WebApplicationContext context = RequestContextUtils.getWebApplicationContext(request);
    WebAssetManager wam = (WebAssetManager)context.getBean("webAssetManager");
    JspWriter out = pageContext.getOut();
    Locale locale = request.getLocale();

    Set<Map<String, String>> allLinks = new LinkedHashSet<Map<String, String>>();
    if (includeGlobal) {
      List<Map<String, String>> globalLinks = wam.getGlobalLinks(locale);
      if (globalLinks != null && globalLinks.size() > 0) {
        allLinks.addAll(globalLinks);
      }
    }

    for (String id : ids) {
      List<Map<String, String>> idLinks = wam.getLinksForId(id, locale);
      if (idLinks != null && idLinks.size() > 0) {
        allLinks.addAll(idLinks);
      }
    }

    // include dynamic link tags
    if (includeDynamic) {
      List<Map<String, String>> requestLinks = (List<Map<String, String>>)request.getAttribute(WebAssetConstants.REQUEST_LINKS);
      if (requestLinks != null && requestLinks.size() > 0) {
        allLinks.addAll(requestLinks);
      }
    }

    // write out the links
    if (allLinks.size() > 0) {
      for (Map<String, String> linkAttrs : allLinks) {
        StringBuilder linkTagBuilder = new StringBuilder();
        String type = linkAttrs.get("type");
        String href = linkAttrs.get("path");
        linkTagBuilder.append("<link rel=\"stylesheet\"");
        if (StringUtils.isNotBlank(type)) {
          linkTagBuilder.append(" type=\"" + type + "\"");
        }
        if (StringUtils.isNotBlank(href)) {
          linkTagBuilder.append(" href=\"" + href + "\"");
        }
        linkTagBuilder.append(" />");
        out.print(linkTagBuilder.toString() + "\n");
      }
      request.setAttribute(WebAssetConstants.LINKS, allLinks);
    }
  }

  private void writeMetaTags(Set<String> ids)
    throws IOException {

    HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
    WebApplicationContext context = RequestContextUtils.getWebApplicationContext(request);
    WebAssetManager wam = (WebAssetManager)context.getBean("webAssetManager");
    JspWriter out = pageContext.getOut();
    Locale locale = request.getLocale();

    Set<Map<String, String>> allMetas = new LinkedHashSet<Map<String, String>>();
    if (includeGlobal) {
      List<Map<String, String>> globalMetas = wam.getGlobalMetas(locale);
      if (globalMetas != null && globalMetas.size() > 0) {
        allMetas.addAll(globalMetas);
      }
    }

    for (String id : ids) {
      List<Map<String, String>> idMetas = wam.getMetasForId(id, locale);
      if (idMetas != null && idMetas.size() > 0) {
        allMetas.addAll(idMetas);
      }
    }

    // include dynamic meta tags
    if (includeDynamic) {
      List<Map<String, String>> requestLinks = (List<Map<String, String>>)request.getAttribute(WebAssetConstants.REQUEST_METAS);
      if (requestLinks != null && requestLinks.size() > 0) {
        allMetas.addAll(requestLinks);
      }
    }

    // write out the meta tags
    if (allMetas.size() > 0) {
      for (Map<String, String> metaAttrs : allMetas) {
        StringBuilder metaTagBuilder = new StringBuilder();
        metaTagBuilder.append("<meta");
        for (Entry<String, String> metaAttr : metaAttrs.entrySet()) {
          String key = metaAttr.getKey();
          String value = metaAttr.getValue();
          if (StringUtils.isNotBlank(key)) {
            metaTagBuilder.append(" " + key + "=\"");
          }
          metaTagBuilder.append(value + "\"");
        }
        metaTagBuilder.append(" />");
        out.print(metaTagBuilder.toString() + "\n");
      }
      request.setAttribute(WebAssetConstants.LINKS, allMetas);
    }
  }

  @Override
  protected final int doStartTagInternal()
    throws JspException, IOException {

    try {

      HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

      // get the types of assets for this tag
      Set<String> assetTypes = new HashSet<String>();
      String[] includeTypes = StringUtils.split(types, ",");
      if (includeTypes != null) {
        for (String includeType : includeTypes) {
          includeType = StringUtils.trim(StringUtils.lowerCase(includeType));
          assetTypes.add(includeType);
        }
      }

      // are ids hardcoded on the tag itself, overrides anything specified
      // in the request. an id must be specified either on the tag or in the
      // request, even though they don't have to exist in the configuration
      boolean tagSpecifiedIds = (this.ids != null);
      String idStr = (tagSpecifiedIds) ? this.ids
        : (String)request.getAttribute(WebAssetConstants.IDS);

      // dedup tag ids, keep in order
      Set<String> ids = new LinkedHashSet<String>();
      if (StringUtils.isNotBlank(idStr)) {
        for (String id : StringUtils.split(idStr, ",")) {
          ids.add(StringUtils.trim(id));
        }
      }

      // write out any title tag
      if (assetTypes.contains("title")) {
        writeTitleTag(ids);
      }

      // write out any meta tags
      if (assetTypes.contains("metas")) {
        writeMetaTags(ids);
      }

      // write out any link tag
      if (assetTypes.contains("links")) {
        writeLinkTags(ids);
      }

      // write out any script tag
      if (assetTypes.contains("scripts")) {
        writeScriptTags(ids);
      }

    }
    catch (IOException e) {
      throw new JspException(e);
    }

    return SKIP_BODY;
  }

  public int doEndTag() {
    return EVAL_PAGE;
  }
}
