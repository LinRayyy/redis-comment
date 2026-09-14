package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * session 中验证码的 key
     */
    private static final String CODE_SESSION_KEY = "code";

    /**
     * session 中登录用户的 key
     */
    private static final String USER_SESSION_KEY = "user";

    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号格式，不合法直接返回
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        // 2. 生成 6 位随机数字验证码
        String code = RandomUtil.randomNumbers(6);
        // 3. 将验证码保存到 session 中，登录时比对
        session.setAttribute(CODE_SESSION_KEY, code);
        // 4. 模拟发送短信：真实环境应调用短信服务商接口，这里只打印日志
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 5. 返回成功
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号格式，不合法直接返回
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误！");
        }
        // 2. 取出 session 中缓存的验证码，与前端传入的验证码比对
        Object cacheCode = session.getAttribute(CODE_SESSION_KEY);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 验证码不存在或输入错误
            return Result.fail("验证码错误");
        }
        // 3. 验证码校验通过，根据手机号查询用户
        User user = query().eq("phone", phone).one();
        // 4. 用户不存在说明是第一次登录，自动注册
        if (user == null) {
            user = createUserWithPhone(phone);
        }
        // 5. 数据脱敏：只把 id、昵称、头像等非敏感字段写入 session
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        session.setAttribute(USER_SESSION_KEY, userDTO);
        // 6. 返回成功
        return Result.ok();
    }

    /**
     * 根据手机号创建新用户并保存入库
     *
     * @param phone 手机号
     * @return 保存后的用户（已回填自增主键）
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        // 昵称使用 "user_" + 10 位随机字符串
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
