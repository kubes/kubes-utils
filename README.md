Utility classes for spring web developement.  This includes the web asset 
management framework.  See unit tests.

Example configuring the PropertyWebappContextInitializer

 ```
  <listener>
    <listener-class>org.springframework.web.util.WebAppRootListener</listener-class>
  </listener>

  <servlet>
    <servlet-name>springmvc</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
      <param-name>detectAllViewResolvers</param-name>
      <param-value>true</param-value>
    </init-param>
    <init-param>
      <param-name>contextConfigLocation</param-name>
      <param-value>/WEB-INF/conf/*-context.xml</param-value>
    </init-param>
    <init-param>
      <param-name>contextInitializerClasses</param-name>
      <param-value>com.denniskubes.spring.PropertyWebappContextInitializer</param-value>
    </init-param>    
    <load-on-startup>1</load-on-startup> 
  </servlet>
```

Example configuring the in PatternReloadableResourceBundleMessageSource in webapp-context.xml

 ```
  <bean id="messageSource"
    class="com.denniskubes.spring.PatternReloadableResourceBundleMessageSource">
    <property name="cacheSeconds" value="5" />
    <property name="resources">
      <list>
        <value>/WEB-INF/pages/**/*_i18n.xml</value>
      </list>
    </property>
  </bean>
```

Example configuring the web asset manager in webapp-context.xml

 ```
  <bean id="webAssetManager" class="com.denniskubes.webasset.WebAssetManager"
    init-method="startup" destroy-method="shutdown">
    <property name="rootDirectory" value="${webapp.root}" />
    <property name="configDirectory" value="/WEB-INF/pages/" />
    <property name="removeExistingCacheFiles" value="true" />
    <property name="filters" ref="webAssetFilters" />
    <property name="typeToFilters" ref="typeToFilters" />
  </bean>
  
  <bean id="cssCompressorFilter" class="com.denniskubes.webasset.CssCompressorFilter" />
  <bean id="jsCompressorFilter" class="com.denniskubes.webasset.JavascriptCompressorFilter" />
  
  <util:map id="webAssetFilters">
    <entry key="cssCompressor" value-ref="cssCompressorFilter" />
    <entry key="jsCompressor" value-ref="jsCompressorFilter" />
  </util:map>
  
  <util:map id="typeToFilters">
    <entry key="javascript" value="jsCompressor" />
    <entry key="stylesheet" value="cssCompressor" />
  </util:map>
```
