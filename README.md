# Kubes Utils

## Overview
The kubes-utils package provides various utitilies that make webapp development easier, especially Java Spring web app development.  The utilities include:

  - A flexible property configuration system for Spring webapps.
  - A reloadable pattern based message source for Spring webapps.
  - A web asset management system.

## Flexible Property Configuration

An ApplicationContextInitializer implementation for Spring MVC apps that allows easy and flexible configurations of properties used to configure the application.  First the context initializer must be configured in your spring servlet setup in the web.xml file.

    <servlet>
      <servlet-name>springmvc</servlet-name>
      <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
      ...
      <init-param>
        <param-name>contextInitializerClasses</param-name>
        <param-value>com.denniskubes.spring.PropertyWebappContextInitializer</param-value>
      </init-param>    
      <load-on-startup>1</load-on-startup> 
    </servlet>

By default the initializer looks under the application root WEB-INF for a folder structure as follows.  You can override the default location using a servlet init param called propertyRoot.  The propertyRoot path is relative from the WEB-INF folder.  If the propertyRoot folder doesn't exist or is not readable, the entire property loading process exits.

    WEB-INF/
     props/
       base/
       envs/
       hosts/
       users/

The initializer start with properties from the base folder.  By default all .properties and *props.xml files will be loaded.  You can override the property file suffixes to match using an init-param named propertyFileSuffix. The base folder is considered the core properties folder for the application that holds the default properties.

Next system properties will be searched for an "environment" property and if found properties will be loaded from the envs folder.  Properties must have the environment name as a prefix and the property file names as suffix. For example a file for an environment named prod would be prod_props.xml.

The hosts folder is then searched for properties files starting with the ip address or hostname of the server.  For example 192.168.1.1.properties.xml.

The users folder is search for properties starting with the current user 
name as defined in the user.name system property.

Each stage of property loading override any previous properties with the same name.  For instance, environment properties can be set that override the base properties for a staging and production environments and the user properties allows specific users to have development properties that over base properties.  All properties should be uniquely named.  The order in which properties are loaded within a stage is deterministic by undefined.

Each stage of loading will recurse through file system.  Folders can be nested any level deep and properties files will still be loaded as long as they match the rules for that folder.  None of the stage is required and if a folder, for instance envs or hosts, doesn't exist, that stage will simply be ignored.

A system property named property_files can be specified.  It accepts a a comma separated list of file paths to load properties from one or more external property files.  The external property files are added in the order they are specified.  Later files override early files. External property files also override properties added in the previous stages.

## Reloadable Pattern Based Message Bundles

The PatternReloadableResourceBundleMessageSource is an extension to the ReloadableResourceBundleMessageSource class that allows specifying the bundles using a pattern.  This allows us to use the same convention for resource bundles, where new bundles are automatically added.

To use the PatternReloadableResourceBundleMessageSource must be configured in spring context.xml

  <bean id="messageSource"
    class="com.denniskubes.spring.PatternReloadableResourceBundleMessageSource">
    <property name="cacheSeconds" value="5" />
    <property name="resources">
      <list>
        <value>/WEB-INF/pages/**/*_i18n.xml</value>
      </list>
    </property>
  </bean>

Then it is as simple as adding in new message bundles with the correct name.  The message bundles are loaded automatically.  The message source does handle internationalized message bundles correctly.  Using the above format the default would be _i18n.xml and french would be fr_i18n.xml.

## Web Asset Management

The WebAssetManager handler scalable inclusion and caching of web assets within a web application.  Assets such as css and js files are cached based on the content.  If the assets are changed while the application is running they are automatically recached, using a slightly differnet name, the first time they are re-requested.  There are three main parts to the web asset manager.

  - The com.denniskubes.webasset.WebAssetManager class configured in the spring webapp context.
  - The WebAssetRequest and WebAssetRequestListener classes that setup and cleanup requests.
  - A web asset config files.
  - The WebAssetTag, used on JSP pages to include web asset resources.

### The ServletRequestListener

First the WebAssetRequestListener must be configured in the web.xml file.

    <listener>
      <listener-class>com.denniskubes.webasset.WebAssetRequestListener</listener-class>
    </listener>

The WebAssetRequestListener handles cleanup on each request.

### The Spring Bean Configuration

Next the WebAssetManager must be configured in the spring webapp context.

  <!-- Web Asset Management -->
  <bean id="webAssetManager" class="com.denniskubes.webasset.WebAssetManager"
    init-method="startup" destroy-method="shutdown">
    <property name="rootDirectory" value="${your.webapp.root}" />
    <property name="configDirectory" value="/WEB-INF/pages" />
    <property name="cacheDirectory" value="/static" />
    <property name="clearCacheOnStartup" value="true" />
    <property name="clearCacheOnShutdown" value="true" />
    <property name="assetPrefixes">
      <list>
        <value>/WEB-INF/static</value>
      </list>
    </property>
  </bean>

Usually the webapp root is setup with a WebAppRootListener and that variable is used as the rootDirectory to the WebAssetManager.

### The *.waf Configuration Files

The WebAppRootListener upon application startup looks in the configDirectory for files matching *.waf, this is configurable.  These files are the web asset configuration files.  They hold descriptions of which css, js, and other resources should be included in a web page.  There can be a global waf file and local, page specific, waf files.  The global waf file will look like this:

    {
        "global": true,
        
        "title": "Default Title",
        
        "aliases": {
          "jquery": "http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js",
          "bootstrap-css": "/WEB-INF/static/css/bootstrap.css",
          "bootstrap-js": "/WEB-INF/static/js/bootstrap.js",
          "async-js": "/WEB-INF/static/js/async.js",
          "font-awesome": "/WEB-INF/static/css/font-awesome.css"
        },
        
        "meta": [
          {
            "http-equiv": "Content-Type",
            "content": "text/html; charset=utf-8" 
          }    
        ],
        
        "scripts": [
          "${jquery}",
          "${bootstrap-js}",  
          "${async-js}"
        ],  
        
        "links": [
          "${bootstrap-css}",
          "${font-awesome}",
          "http://fonts.googleapis.com/css?family=Alegreya+Sans:400,400italic,500italic,500,700,700italic,800italic,900,900italic,300,300italic,800"
        ] 
    }

The waf file format is JSON.  The global file has a global set to true.  It sets a default title for when pages don't set a title.  It sets up aliases that can be used in any waf file.  This is useful when you want to include a script over a number of pages and the versions may change, for example jquery.  It then has three sections for meta tags, scripts, and links (stylesheets).  The meta sections takes an object with keys and values.  The scripts and links section can take shorthand versions with just the local or remote url or they can also take objects with keys and values.  Script values can be a local path starting from the rootDirectory configured on the WebAssetManager or they can be an external path.  Local files are cached, external files are not.

If the waf file is changed while the application is running, all pages are reloaded.

A page specific waf file will look like this:

    {    
        "ids": ["mypageid"],
        "title": "My WebApp - With My Title",

         "scripts": [
          "/WEB-INF/static/css/stylesheet1.css",
          "/WEB-INF/static/css/stylesheet2.css"
        ] 

        "links": [
          "/WEB-INF/static/css/page1.css",
          "/WEB-INF/static/css/page2.css"
        ] 
    }

The page waf file can also have meta, script, and link sections.  No section is required.  In fact the only required field is the ids.  The ids contain one or more comma separated ids for the page.  These ids will match up to ids setup in a controller using the WebAssetRequest static methods.  Eventually the ids will be used by the WebAssetTag to determine what scripts, stylesheets, meta tags, and titles will be placed on a final jsp page.  Page specific assets, such as title, will override global assets.

### Setup in the Spring Controller

In a controller you will like the id by calling one of the WebAssetRequest methods:

    WebAssetRequest.setup("mypageid");

This must match up to a waf file id.

### Setup on a JSP Page

The WebAssetTag is a JSTL tag that is included on a JSP page.  The tag will write out different web assets.  It is customary to have a tag write out the title, meta tags, and style sheets (links) in the head section of the webpage and another tag write out the scripts at the bottom of the web page before the closing body tag.  Here is an example.

    <!DOCTYPE html>

    <%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="wa" uri="http://denniskubes.com/taglibs/webasset.tld" %>

    <html>
      <head>
        <wa:write types="title,meta,links" includeGlobal="true" />
      </head>
      <body>
        <div id="wrapper">
          <div id="header">
          </div>
          <div id="content">
          </div>
          <div id="footer">
          </div>      
          <wa:write types="scripts" includeGlobal="true" /> 
        </div>
      </body>
    </html>

By default context paths and full hostnames are included but it is configurable.  Here is an example of what the final html page will look like:

    <!DOCTYPE html>

    <%@ page contentType="text/html;charset=UTF-8" %>
    <%@ taglib prefix="wa" uri="/WEB-INF/static/tlds/webasset.tld" %>

    <html>
      <head>
      <title>My WebApp - With My Title</title>
    <link rel="stylesheet" type="text/css" href="http://myhost/mycontext/static/css/bootstrap.cache.2563797763.css" />
    <link rel="stylesheet" type="text/css" href="http://myhost/mycontext/static/css/bootstrap-theme.cache.3633411826.css" />
    <link rel="stylesheet" type="text/css" href="http://myhost/mycontext/static/css/font-awesome.cache.2213180923.css" />
    <link rel="stylesheet" type="text/css" href="http://fonts.googleapis.com/css?family=Alegreya+Sans:400,400italic,500italic,500,700,700italic,800italic,900,900italic,300,300italic,800" />
    <link rel="stylesheet" type="text/css" href="http://myhost/mycontext/static/css/stylesheet1.2367980763.css" />
    <link rel="stylesheet" type="text/css" href="http://myhost/mycontext/static/css/stylesheet2.1478367980.css" />
      </head>
      <body>
        <div id="wrapper">
          <div id="header">
          </div>
          <div id="content">
          </div>
          <div id="footer">
          </div>      
          <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
      <script type="text/javascript" src="http://myhost/mycontext/static/js/script1.cache.3066489206.js"></script>
      <script type="text/javascript" src="http://myhost/mycontext/static/js/script2.cache.1547713098.js"></script>
        </div>
      </body>
    </html>

By default context paths and various OS file system paths are handled correctly.

## License and Bug Fixes

These works are public domain or licensed under the Apache Licene. You can do anything you want with them.  Please feel free to send any improvements or 
bug fixes.
