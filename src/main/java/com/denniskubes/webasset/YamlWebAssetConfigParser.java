package com.denniskubes.webasset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

public class YamlWebAssetConfigParser
  implements WebAssetConfigParser {

  private Map<String, Object> root;

  private Map<String, String> getAttributes(Map<String, Object> node) {
    Map<String, String> attrMap = new LinkedHashMap<String, String>();
    for (String fieldname : node.keySet()) {
      String value = (String)node.get(fieldname);
      if (StringUtils.isNotBlank(value)) {
        attrMap.put(fieldname, value);
      }
    }
    return attrMap;
  }

  public YamlWebAssetConfigParser(String yamlConfig) {
    try {
      Yaml yamlParser = new Yaml();
      root = (Map<String, Object>)yamlParser.load(yamlConfig);
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Couldn't parse config yaml");
    }
  }

  @Override
  public boolean isGlobal() {
    return (Boolean)root.getOrDefault("global", false);
  }

  @Override
  public String getTitle() {
    return (String)root.get("title");
  }

  @Override
  public Map<String, String> getAliases() {
    return getAttributes((Map<String, Object>)root.get("aliases"));
  }

  @Override
  public List<String> getIds() {

    List<String> ids = null;
    if (root.containsKey("ids")) {
      List idObjs = (List)root.get("ids");
      ids = new ArrayList<String>();
      for (Object idObj : idObjs) {
        ids.add((String)idObj);
      }
    }
    return ids;
  }

  @Override
  public List<Map<String, String>> getScripts() {

    List<Map<String, String>> scripts = new ArrayList<Map<String, String>>();
    if (root.containsKey("scripts")) {
      for (Object scriptObj : (List)root.get("scripts")) {
        Map<String, String> attributes = null;
        if (scriptObj instanceof String) {

          attributes = new LinkedHashMap<String, String>();
          attributes.put("type", "text/javascript");
          attributes.put("path", (String)scriptObj);
        }
        else if (scriptObj instanceof Map) {

          attributes = new LinkedHashMap<String, String>();
          Map<String, Object> scriptMap = (Map<String, Object>)scriptObj;
          for (String key : scriptMap.keySet()) {
            attributes.put(key, (String)scriptMap.get(key));
          }
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
    if (root.containsKey("links")) {
      for (Object linkObj : (List)root.get("links")) {
        Map<String, String> attributes = null;
        if (linkObj instanceof String) {

          attributes = new LinkedHashMap<String, String>();
          attributes.put("rel", "stylesheet");
          attributes.put("type", "text/css");
          attributes.put("path", (String)linkObj);
        }
        else if (linkObj instanceof Map) {

          attributes = new LinkedHashMap<String, String>();
          Map<String, Object> linkMap = (Map<String, Object>)linkObj;
          for (String key : linkMap.keySet()) {
            attributes.put(key, (String)linkMap.get(key));
          }
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
    if (root.containsKey("meta")) {
      for (Object meta : (List)root.get("meta")) {

        Map<String, String> attributes = new LinkedHashMap<String, String>();
        Map<String, Object> metaMap = (Map<String, Object>)meta;
        for (String key : metaMap.keySet()) {
          attributes.put(key, (String)metaMap.get(key));
        }
        if (attributes.size() > 0) {
          metas.add(attributes);
        }
      }
    }
    return metas;
  }

}
