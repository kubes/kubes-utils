package com.denniskubes.webasset;

import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

public class WebAssetInterceptor
  extends HandlerInterceptorAdapter {

  @Override
  public boolean preHandle(HttpServletRequest request,
    HttpServletResponse response, Object handler)
    throws Exception {

    if (handler instanceof HandlerMethod) {
      HandlerMethod hm = (HandlerMethod)handler;
      Method method = hm.getMethod();
      WebAsset waa = method.getAnnotation(WebAsset.class);
      if (waa != null) {
        WebAssetRequest.setup(waa.value());
      }
    }

    return true;
  }
}