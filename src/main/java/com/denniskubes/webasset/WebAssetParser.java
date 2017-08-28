package com.denniskubes.webasset;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAssetParser {

  private final static Logger LOG = LoggerFactory.getLogger(WebAssetParser.class);

  /**
   * Parse a web asset config json file into a WebAssetConfig object.
   * 
   * @param configFile The config file path.
   * 
   * @return The parsed WebAssetConfig or null if no config to parse.
   * 
   * @throws IOException If the config file doen't exist or isn't readable or
   * if the config file doesn't have any data.
   * 
   * @throws IllegalArgumentException If the config doesn't contain any ids
   * and isn't a global config.
   */
  public WebAssetConfig parseConfig(File configFile)
    throws IOException {

    // ignore if the config file doesn't exist
    String configPath = configFile.getPath();
    if (!configFile.exists() || !configFile.canRead()) {
      throw new IOException("Web asset config doesn't exist or not readable: "
        + configPath);
    }

    // parse the web asset config file, if error return null
    LOG.debug("Started parsing {}", configPath);
    String configStr = FileUtils.readFileToString(new File(configPath));

    // some config files could parse to null
    if (StringUtils.isBlank(configStr)) {
      throw new IOException("Web asset config file doesn't have data: "
        + configPath);
    }

    WebAssetConfigParser parser = StringUtils.startsWith(configStr, "---")
      ? new YamlWebAssetConfigParser(configStr) : new JsonWebAssetConfigParser(
        configStr);

    WebAssetConfig webAssetConfig = new WebAssetConfig();
    webAssetConfig.setGlobalConfig(parser.isGlobal());

    // get the ids for the config, global configs don't have ids but they do
    // have aliases
    if (webAssetConfig.isGlobalConfig()) {
      Map<String, String> aliases = parser.getAliases();
      if (aliases != null && aliases.size() > 0) {
        webAssetConfig.getAliases().putAll(aliases);
      }
    }
    else {
      List<String> ids = parser.getIds();
      if (ids == null || ids.size() == 0) {
        throw new IllegalArgumentException("Web asset config must "
          + "have one or more ids: " + configPath);
      }
      webAssetConfig.getIds().addAll(ids);
    }

    // add the title
    String title = parser.getTitle();
    if (StringUtils.isNotBlank(title)) {
      webAssetConfig.setTitle(title);
    }

    // get meta tag configs
    List<Map<String, String>> metas = parser.getMetas();
    if (metas != null && metas.size() > 0) {
      webAssetConfig.getMetas().addAll(metas);
    }

    // get script tag configs
    List<Map<String, String>> scripts = parser.getScripts();
    if (scripts != null && scripts.size() > 0) {
      webAssetConfig.getScripts().addAll(scripts);
    }
    
    // get link tag configs
    List<Map<String, String>> links = parser.getLinks();
    if (links != null && links.size() > 0) {
      webAssetConfig.getLinks().addAll(links);
    }

    LOG.debug("Finished parsing {}", configPath);
    return webAssetConfig;
  }
}
