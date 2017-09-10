package com.denniskubes.webasset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.denniskubes.utils.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonWebAssetConfigParser
  implements WebAssetConfigParser {

  private final static Logger LOG = LoggerFactory.getLogger(JsonWebAssetConfigParser.class);

  private JsonNode root;

  public JsonWebAssetConfigParser(String jsonConfig) {
    
    root = JSON.parse(jsonConfig);
    if (root == null) {
      throw new IllegalArgumentException("Couldn't parse config json");
    }
  }

  @Override
  public boolean isGlobal() {
    return JSON.getBoolean(root, "global", false);
  }

  @Override
  public String getTitle() {
    return JSON.getString(root, "title");
  }

  @Override
  public Map<String, String> getAliases() {
    return JSON.getAttributes(root.get("aliases"));
  }

  @Override
  public List<String> getIds() {
    List<String> ids = null;
    if (root.has("ids")) {
      ids = JSON.getStrings(root.get("ids"));
    }
    return ids;
  }

  @Override
  public List<Map<String, String>> getScripts() {

    List<Map<String, String>> scripts = new ArrayList<Map<String, String>>();
    if (root.has("scripts")) {
      for (JsonNode script : root.get("scripts")) {
        Map<String, String> attributes = null;
        if (script instanceof TextNode) {
          attributes = new LinkedHashMap<String, String>();
          attributes.put("type", "text/javascript");
          attributes.put("path", script.asText());
        }
        else {
          attributes = JSON.getAttributes(script);
        }
        if (attributes.size() > 0) {
          scripts.add(attributes);
        }
      }
    }

    return scripts;
  }

  @Override
  public List<Map<String, String>> getLinks() {

    List<Map<String, String>> links = new ArrayList<Map<String, String>>();
    if (root.has("links")) {
      for (JsonNode link : root.get("links")) {
        Map<String, String> attributes = null;
        if (link instanceof TextNode) {
          attributes = new LinkedHashMap<String, String>();
          attributes.put("rel", "stylesheet");
          attributes.put("type", "text/css");
          attributes.put("path", link.asText());
        }
        else {
          attributes = JSON.getAttributes(link);
        }
        if (attributes.size() > 0) {
          links.add(attributes);
        }
      }
    }

    return links;
  }

  @Override
  public List<Map<String, String>> getMetas() {

    List<Map<String, String>> metas = new ArrayList<Map<String, String>>();
    if (root.has("meta")) {
      for (JsonNode meta : root.get("meta")) {
        Map<String, String> attributes = JSON.getAttributes(meta);
        if (attributes.size() > 0) {
          metas.add(attributes);
        }
      }
    }
    return metas;
  }

}
