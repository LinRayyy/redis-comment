package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置：注册登录拦截器并配置放行白名单。
 *
 * @author 虎哥
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册登录拦截器，默认拦截所有请求
        registry.addInterceptor(new LoginInterceptor())
                // 以下路径无需登录即可访问
                .excludePathPatterns(
                        "/user/code",       // 发送验证码
                        "/user/login",      // 登录
                        "/blog/hot",        // 首页热门博客
                        "/shop/**",         // 商铺信息
                        "/shop-type/**",    // 商铺类型
                        "/upload/**",       // 上传的图片等静态资源
                        "/voucher/**"       // 优惠券
                );
    }
}
