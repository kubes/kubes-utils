package com.denniskubes.webasset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TestWebAssetManager {

  private String rootDirectory = "/webasset/test-webapp";
  private String cacheDirectory = "/_webasset_cache_";

  private boolean matches(String path, String start, String end) {
    boolean matchesStart = StringUtils.startsWith(path, start);
    boolean matchesEnd = StringUtils.endsWith(path, end);
    return matchesStart && matchesEnd;
  }

  @Test(expected = IOException.class)
  public void testBadRootDirectory()
    throws IOException {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/idontexist");
    wam.startup();
    wam.shutdown();
  }

  @Test
  public void testInitializeAndShutdown()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    File rootFile = rootResource.getFile();
    String fullRootPath = rootFile.getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();
    Assert.assertTrue(wam.getConfigForId("good1") != null);
    Assert.assertTrue(wam.getConfigForId("good2") != null);
    Assert.assertTrue(wam.getConfigForId(WebAssetManager.GLOBAL) != null);
    wam.shutdown();

    File cacheFile = new File(rootFile, cacheDirectory);
    Assert.assertFalse(cacheFile.exists());
  }

  @Test
  public void testGlobalScripts()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/global1.js"));
    List<Map<String, String>> scripts = wam.getGlobalScripts(Locale.US);
    Assert.assertTrue(scripts != null && scripts.size() == 1);
    Map<String, String> globalScriptAttrs = scripts.get(0);
    String cachedPath = globalScriptAttrs.get("path");
    Assert.assertTrue(matches(cachedPath, cacheDirectory + "/js/global1", "js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/global1.js"));

    wam.shutdown();
  }

  @Test
  public void testScriptsForId()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/global2.js"));
    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/global3.js"));

    List<Map<String, String>> scripts = wam.getScriptsForId("good1", Locale.US,
      null);
    Assert.assertTrue(scripts != null && scripts.size() == 2);

    Map<String, String> script2Attrs = scripts.get(0);
    String script2Path = script2Attrs.get("path");
    Assert.assertTrue(matches(script2Path, cacheDirectory + "/js/global2", "js"));

    Map<String, String> script3Attrs = scripts.get(1);
    String script3Path = script3Attrs.get("path");
    Assert.assertTrue(matches(script3Path, cacheDirectory + "/js/global3", "js"));

    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/global2.js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/global3.js"));

    wam.shutdown();
  }

  @Test
  public void testExternal()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("http://localhost/script1.js"));
    Assert.assertNull(wam.getCachedPath("http://localhost/script2.js"));
    Assert.assertNull(wam.getCachedPath("http://localhost/stylesheet1.css"));
    Assert.assertNull(wam.getCachedPath("http://localhost/stylesheet2.css"));

    List<Map<String, String>> scripts = wam.getScriptsForId("external1",
      Locale.US, null);
    Assert.assertTrue(scripts != null && scripts.size() == 2);
    Assert.assertEquals(scripts.get(0).get("path"),
      "http://localhost/script1.js");
    Assert.assertEquals(scripts.get(1).get("path"),
      "http://localhost/script2.js");

    List<Map<String, String>> links = wam.getLinksForId("external1", Locale.US,
      null);
    Assert.assertTrue(links != null && links.size() == 2);
    Assert.assertEquals(links.get(0).get("path"),
      "http://localhost/stylesheet1.css");
    Assert.assertEquals(links.get(1).get("path"),
      "http://localhost/stylesheet2.css");

    Assert.assertNull(wam.getCachedPath("http://localhost/script1.js"));
    Assert.assertNull(wam.getCachedPath("http://localhost/script2.js"));
    Assert.assertNull(wam.getCachedPath("http://localhost/stylesheet1.css"));
    Assert.assertNull(wam.getCachedPath("http://localhost/stylesheet2.css"));

    wam.shutdown();
  }

  @Test
  public void testGlobalLinks()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/css/global1.css"));
    List<Map<String, String>> links = wam.getGlobalLinks(Locale.US);
    Assert.assertTrue(links != null && links.size() == 1);
    Map<String, String> globalLinkAttrs = links.get(0);
    String cachedPath = globalLinkAttrs.get("path");
    Assert.assertTrue(matches(cachedPath, cacheDirectory + "/css/global1",
      "css"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/css/global1.css"));

    wam.shutdown();
  }

  @Test
  public void testLinksForId()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/css/global2.css"));
    Assert.assertNull(wam.getCachedPath("/WEB-INF/css/global3.css"));

    List<Map<String, String>> links = wam.getLinksForId("good1", Locale.US,
      null);
    Assert.assertTrue(links != null && links.size() == 2);

    Map<String, String> link2Attrs = links.get(0);
    String link2Path = link2Attrs.get("path");
    Assert.assertTrue(matches(link2Path, cacheDirectory + "/css/global2", "css"));

    Map<String, String> link3Attrs = links.get(1);
    String link3Path = link3Attrs.get("path");
    Assert.assertTrue(matches(link3Path, cacheDirectory + "/css/global3", "css"));

    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/css/global2.css"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/css/global3.css"));

    wam.shutdown();
  }

  @Test
  public void testGlobalMetas()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    List<Map<String, String>> metas = wam.getGlobalMetas(Locale.US);
    Assert.assertTrue(metas != null && metas.size() == 1);
    Map<String, String> contentType = metas.get(0);
    Assert.assertEquals(contentType.get("http-equiv"), "Content-Type");
    Assert.assertEquals(contentType.get("content"), "text/html; charset=utf-8");

    wam.shutdown();
  }

  @Test
  public void testMetasForId()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    List<Map<String, String>> metas = wam.getMetasForId("good1", Locale.US,
      null);
    Assert.assertTrue(metas != null && metas.size() == 2);

    Map<String, String> keywords = metas.get(0);
    Assert.assertEquals(keywords.get("name"), "keywords");
    Assert.assertEquals(keywords.get("content"), "good1 keywords");

    Map<String, String> description = metas.get(1);
    Assert.assertEquals(description.get("name"), "description");
    Assert.assertEquals(description.get("content"), "good1 description");

    wam.shutdown();
  }

  @Test
  public void testGlobalTitle()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("webasset/test-webapp/WEB-INF/config/webasset");

    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.setMessageSource(messageSource);
    wam.startup();

    String title = wam.getGlobalTitle(Locale.US);
    Assert.assertEquals(title, "default title");

    wam.shutdown();
  }

  @Test
  public void testTitleForId()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("webasset/test-webapp/WEB-INF/config/webasset");

    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.setMessageSource(messageSource);
    wam.startup();

    String title = wam.getTitleForId("good1", Locale.US, null);
    Assert.assertEquals(title, "good1 title");

    wam.shutdown();
  }

  @Test
  public void testSingleAssetReloading()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/global2.js"));
    List<Map<String, String>> scripts = wam.getScriptsForId("good1", Locale.US,
      null);
    Assert.assertTrue(scripts != null && scripts.size() == 2);
    Map<String, String> script2Attrs = scripts.get(0);
    String script2Path = script2Attrs.get("path");
    Assert.assertTrue(matches(script2Path, cacheDirectory + "/js/global2", "js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/global2.js"));

    // simulate reloading the
    String origCached = wam.getCachedPath("/WEB-INF/js/global2.js");
    File global2File = new File(fullRootPath, "/WEB-INF/js/global2.js");
    File renamedFile = new File(fullRootPath, "/WEB-INF/js/global2.js.old");
    File reloadFile = new File(fullRootPath, "/WEB-INF/js/reload1");
    FileUtils.copyFile(global2File, renamedFile);
    FileUtils.copyFile(reloadFile, global2File);
    // must increase last modified or else some times it is the same as the
    // system current millis and won't reload and test fails
    global2File.setLastModified(System.currentTimeMillis() + 1000);
    wam.getScriptsForId("good1", Locale.US, null);
    String newCached = wam.getCachedPath("/WEB-INF/js/global2.js");
    Assert.assertFalse(origCached.equals(newCached));
    FileUtils.copyFile(renamedFile, global2File);

    wam.shutdown();
  }

  @Test
  public void testLocaleSwitching()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("webasset/test-webapp/WEB-INF/config/webasset");

    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.setMessageSource(messageSource);
    wam.startup();

    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/locale_specific.js"));
    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/locale_specific_es.js"));

    List<Map<String, String>> scripts = wam.getScriptsForId("locale1",
      Locale.US, null);
    Assert.assertTrue(scripts != null && scripts.size() == 2);
    Map<String, String> scriptAttrs = scripts.get(1);
    String scriptPath = scriptAttrs.get("path");
    Assert.assertTrue(matches(scriptPath, cacheDirectory
      + "/js/locale_specific.", "js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/locale_specific.js"));
    Assert.assertNull(wam.getCachedPath("/WEB-INF/js/locale_specific_es.js"));

    List<Map<String, String>> scripts2 = wam.getScriptsForId("locale1",
      new Locale("es", "ES"), null);
    Assert.assertTrue(scripts2 != null && scripts2.size() == 2);
    Map<String, String> script2Attrs = scripts2.get(1);
    String script2Path = script2Attrs.get("path");
    Assert.assertTrue(matches(script2Path, cacheDirectory
      + "/js/locale_specific_es.", "js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/locale_specific.js"));
    Assert.assertNotNull(wam.getCachedPath("/WEB-INF/js/locale_specific_es.js"));

    wam.shutdown();
  }

  @Test
  public void testNoAliasOrMessage()
    throws Exception {

    Resource rootResource = new ClassPathResource(rootDirectory);
    String fullRootPath = rootResource.getFile().getPath();
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("webasset/test-webapp/WEB-INF/config/webasset");

    WebAssetManager wam = new WebAssetManager();
    wam.setRootDirectory(fullRootPath);
    wam.setConfigDirectory("/WEB-INF/config");
    wam.setClearCacheOnShutdown(true);
    wam.setMessageSource(messageSource);
    wam.startup();

    String title = wam.getTitleForId("nomessage", Locale.US, null);
    Assert.assertEquals(title, "${no.message}");

    wam.shutdown();
  }

}
