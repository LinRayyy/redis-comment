package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器：校验 session 中是否存在登录用户，
 * 校验通过后将用户存入 ThreadLocal，供后续业务代码直接获取。
 *
 * @author 虎哥
 */
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * session 中登录用户的 key，与 UserServiceImpl 中保持一致
     */
    private static final String USER_SESSION_KEY = "user";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取当前请求的 session（false 表示不存在时不创建，避免为未登录请求生成无用会话）
        HttpSession session = request.getSession(false);
        // 2. 从 session 中取出登录用户
        Object user = session == null ? null : session.getAttribute(USER_SESSION_KEY);
        // 3. 未登录：返回 401 状态码并中断请求
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        // 4. 已登录：保存到 ThreadLocal，供 Controller / Service 使用
        UserHolder.saveUser((UserDTO) user);
        // 5. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束（无论成功或异常），必须清理 ThreadLocal，防止线程复用导致的内存泄漏与用户信息串号
        UserHolder.removeUser();
    }
}
