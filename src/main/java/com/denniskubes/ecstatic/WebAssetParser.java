package com.denniskubes.ecstatic;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denniskubes.utils.JSON;

public class WebAssetParser {

  private final static Logger LOG =
    LoggerFactory.getLogger(WebAssetParser.class);

  /**
   * Returns a key value Map from a JSON node. This is used in custom asset
   * configuration, such as meta tags.
   * 
   * @param node The node to extract key value pairs from.
   * 
   * @return A Map containing the key value attribute pairs.
   */
  private Map<String, String> getAttributes(JsonNode node) {
    Map<String, String> attrMap = new LinkedHashMap<String, String>();
    for (String fieldname : JSON.getFieldnames(node)) {
      String value = JSON.getString(node, fieldname);
      if (StringUtils.isNotBlank(value)) {
        attrMap.put(fieldname, value);
      }
    }
    return attrMap;
  }

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
    JsonNode asset = null;
    try {
      ObjectMapper mapper = new ObjectMapper();
      asset = mapper.readValue(configFile, JsonNode.class);
    }
    catch (Exception e) {
      throw new IOException("Cannot parse web asset config:" + configPath);
    }

    // some config files could parse to null
    if (asset == null) {
      throw new IOException("Web asset config file doesn't have data: "
        + configPath);
    }

    WebAssetConfig webAssetConfig = new WebAssetConfig();
    webAssetConfig.setGlobalConfig(JSON.getBoolean(asset, "global", false));

    // get the ids for the config, global configs don't have ids but they do
    // have aliases
    if (webAssetConfig.isGlobalConfig()) {
      Map<String, String> aliases = getAttributes(asset.get("aliases"));
      if (aliases != null && aliases.size() > 0) {
        webAssetConfig.getAliases().putAll(aliases);
      }
    }
    else {
      List<String> ids = null;
      if (asset.has("ids")) {
        JsonNode idNode = asset.get("ids");
        ids = JSON.getStrings(idNode);
      }
      if (ids == null || ids.size() == 0) {
        throw new IllegalArgumentException("Web asset config must "
          + "have one or more ids: " + configPath);
      }
      webAssetConfig.getIds().addAll(ids);
    }

    // add the title
    String title = JSON.getString(asset, "title");
    if (StringUtils.isNotBlank(title)) {
      webAssetConfig.setTitle(title);
    }

    // loop through the meta tag configurations
    if (asset.has("meta")) {
      List<Map<String, String>> metas = webAssetConfig.getMetas();
      for (JsonNode meta : asset.get("meta")) {
        Map<String, String> attributes = getAttributes(meta);
        if (attributes.size() > 0) {
          metas.add(attributes);
        }
      }
    }

    // loop through the script tag configurations
    if (asset.has("scripts")) {
      List<Map<String, String>> scripts = webAssetConfig.getScripts();
      for (JsonNode script : asset.get("scripts")) {
        Map<String, String> attributes = null;
        if (script instanceof TextNode) {
          attributes = new LinkedHashMap<String, String>();
          attributes.put("type", "text/javascript");
          attributes.put("path", script.asText());
        }
        else {
          attributes = getAttributes(script);
        }
        if (attributes.size() > 0) {
          scripts.add(attributes);
        }
      }
    }

    // loop through the link tag configurations
    if (asset.has("links")) {
      List<Map<String, String>> links = webAssetConfig.getLinks();
      for (JsonNode link : asset.get("links")) {
        Map<String, String> attributes = null;
        if (link instanceof TextNode) {
          attributes = new LinkedHashMap<String, String>();
          attributes.put("rel", "stylesheet");
          attributes.put("type", "text/css");
          attributes.put("path", link.asText());
        }
        else {
          attributes = getAttributes(link);
        }
        if (attributes.size() > 0) {
          links.add(attributes);
        }
      }
    }

    LOG.debug("Finished parsing {}", configPath);
    return webAssetConfig;
  }
}
