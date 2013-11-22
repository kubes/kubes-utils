package com.denniskubes.webasset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class TestWebAssetParser {

  private String parserFolder = "/webasset/parser";

  @Test(expected = IOException.class)
  public void testNonExistingConfig()
    throws Exception { 
    WebAssetParser parser = new WebAssetParser();
    File nonExistingFile = new File("idontexist.waf");
    parser.parseConfig(nonExistingFile);
  }

  @Test(expected = IOException.class)
  public void testBadJsonConfig()
    throws Exception {
    WebAssetParser parser = new WebAssetParser();
    Resource baseJsonRes =
      new ClassPathResource(parserFolder + "/pabadjson.waf");
    File badJsonFile = baseJsonRes.getFile();
    parser.parseConfig(badJsonFile);
  }

  @Test(expected = IOException.class)
  public void testBlankConfig()
    throws Exception {
    WebAssetParser parser = new WebAssetParser();
    Resource blankRes = new ClassPathResource(parserFolder + "/blank.waf");
    File blankFile = blankRes.getFile();
    parser.parseConfig(blankFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoIdsConfig()
    throws Exception {
    WebAssetParser parser = new WebAssetParser();
    Resource noIdsRes = new ClassPathResource(parserFolder + "/noids.waf");
    File noIdsFile = noIdsRes.getFile();
    parser.parseConfig(noIdsFile);
  }

  @Test
  public void testParseGlobalWebAssetConfig()
    throws Exception {

    WebAssetParser parser = new WebAssetParser();
    Resource globalRes = new ClassPathResource(parserFolder + "/global.waf");
    File globalFile = globalRes.getFile();
    WebAssetConfig globalConfig = parser.parseConfig(globalFile);

    // aliases
    Map<String, String> aliases = globalConfig.getAliases();
    Assert.assertEquals(aliases.get("alias1"), "/WEB-INF/js/global1.js");
    Assert.assertEquals(aliases.get("alias2"), "/WEB-INF/js/global2.js");
    Assert.assertEquals(aliases.get("alias3"), "/WEB-INF/css/global1.css");
    Assert.assertEquals(aliases.get("alias4"), "/WEB-INF/css/global2.css");

    // title
    Assert.assertEquals(globalConfig.getTitle(), "[[default.title]]");

    // meta
    List<Map<String, String>> metas = globalConfig.getMetas();
    Assert.assertTrue(metas.size() == 1);
    Map<String, String> contentType = metas.get(0);
    Assert.assertEquals(contentType.get("http-equiv"), "Content-Type");
    Assert.assertEquals(contentType.get("content"), "text/html; charset=utf-8");

    // scripts
    List<Map<String, String>> scripts = globalConfig.getScripts();
    Assert.assertTrue(scripts.size() == 3);
    Map<String, String> script1 = scripts.get(0);
    Assert.assertEquals(script1.get("type"), "text/javascript");
    Assert.assertEquals(script1.get("path"), "{{alias1}}");
    Map<String, String> script3 = scripts.get(2);
    Assert.assertEquals(script3.get("type"), "text/javascript");
    Assert.assertEquals(script3.get("path"), "/WEB-INF/js/global3.js");

    // links
    List<Map<String, String>> links = globalConfig.getLinks();
    Assert.assertTrue(links.size() == 3);
    Map<String, String> link1 = links.get(0);
    Assert.assertEquals(link1.get("rel"), "stylesheet");
    Assert.assertEquals(link1.get("type"), "text/css");
    Assert.assertEquals(link1.get("path"), "{{alias3}}");
    Map<String, String> link3 = links.get(2);
    Assert.assertEquals(link3.get("rel"), "stylesheet");
    Assert.assertEquals(link3.get("type"), "text/css");
    Assert.assertEquals(link3.get("path"), "/WEB-INF/css/global3.css");
  }

  @Test
  public void testParseWebAssetConfig()
    throws Exception {

    WebAssetParser parser = new WebAssetParser();
    Resource goodRes = new ClassPathResource(parserFolder + "/good.waf");
    File configFile = goodRes.getFile();
    WebAssetConfig config = parser.parseConfig(configFile);

    // aliases
    Map<String, String> aliases = config.getAliases();
    Assert.assertTrue(aliases.size() == 0);

    // ids
    List<String> ids = config.getIds();
    Assert.assertTrue(ids.size() == 2);
    Assert.assertEquals(ids.get(0), "good");
    Assert.assertEquals(ids.get(1), "good2");

    // title
    Assert.assertEquals(config.getTitle(), "[[good.title]]");

    // meta
    List<Map<String, String>> metas = config.getMetas();
    Assert.assertTrue(metas.size() == 1);
    Map<String, String> contentType = metas.get(0);
    Assert.assertEquals(contentType.get("name"), "keywords");
    Assert.assertEquals(contentType.get("content"), "good keywords");

    // scripts
    List<Map<String, String>> scripts = config.getScripts();
    Assert.assertTrue(scripts.size() == 1);
    Map<String, String> script1 = scripts.get(0);
    Assert.assertEquals(script1.get("type"), "text/javascript");
    Assert.assertEquals(script1.get("path"), "/WEB-INF/js/good.js");

    // links
    List<Map<String, String>> links = config.getLinks();
    Assert.assertTrue(links.size() == 1);
    Map<String, String> link1 = links.get(0);
    Assert.assertEquals(link1.get("rel"), "stylesheet");
    Assert.assertEquals(link1.get("type"), "text/css");
    Assert.assertEquals(link1.get("path"), "/WEB-INF/css/good.css");
  }
}
