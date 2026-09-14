package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * 发送手机验证码
     * @param phone   手机号
     * @param session 用于保存验证码的会话
     * @return 发送结果
     */
    Result sendCode(String phone, HttpSession session);

    /**
     * 短信验证码登录，手机号未注册时自动注册
     * @param loginForm 登录参数，包含手机号、验证码
     * @param session   用于保存登录用户的会话
     * @return 登录结果
     */
    Result login(LoginFormDTO loginForm, HttpSession session);
}
