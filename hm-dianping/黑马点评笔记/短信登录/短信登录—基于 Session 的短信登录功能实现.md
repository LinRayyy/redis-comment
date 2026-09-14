1. # 短信登录—基于 Session 的短信登录功能实现

   ------

   

   ### 1. service/IUserService.java（补充方法声明）

   业务逻辑按规范落在 Service 层，在接口中暴露对应方法：

   codeJava

   

   ```
   package com.hmdp.service;
   
   import com.baomidou.mybatisplus.extension.service.IService;
   import com.hmdp.dto.LoginFormDTO;
   import com.hmdp.dto.Result;
   import com.hmdp.entity.User;
   
   import javax.servlet.http.HttpSession;
   
   public interface IUserService extends IService<User> {
       /**
        * 发送手机验证码
        */
       Result sendCode(String phone, HttpSession session);
   
       /**
        * 短信验证码登录，手机号未注册时自动注册
        */
       Result login(LoginFormDTO loginForm, HttpSession session);
   }
   ```

   ------

   

   ### 2. service/impl/UserServiceImpl.java（核心业务实现）

   codeJava

   

   ```
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
   
   @Slf4j
   @Service
   public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
   
       /** session 中验证码的 key */
       private static final String CODE_SESSION_KEY = "code";
       /** session 中登录用户的 key */
       private static final String USER_SESSION_KEY = "user";
   
       @Override
       public Result sendCode(String phone, HttpSession session) {
           // 1. 校验手机号格式，不合法直接返回
           if (RegexUtils.isPhoneInvalid(phone)) {
               return Result.fail("手机号格式错误！");
           }
           // 2. 生成 6 位随机数字验证码
           String code = RandomUtil.randomNumbers(6);
           // 3. 验证码保存到 session，登录时比对
           session.setAttribute(CODE_SESSION_KEY, code);
           // 4. 模拟发送短信：真实环境应调用短信服务商接口，这里只打印日志
           log.debug("发送短信验证码成功，验证码：{}", code);
           return Result.ok();
       }
   
       @Override
       public Result login(LoginFormDTO loginForm, HttpSession session) {
           // 1. 校验手机号格式
           String phone = loginForm.getPhone();
           if (RegexUtils.isPhoneInvalid(phone)) {
               return Result.fail("手机号格式错误！");
           }
           // 2. 取出 session 中的验证码与前端传入的比对
           Object cacheCode = session.getAttribute(CODE_SESSION_KEY);
           String code = loginForm.getCode();
           if (cacheCode == null || !cacheCode.toString().equals(code)) {
               return Result.fail("验证码错误");
           }
           // 3. 校验通过，按手机号查询用户
           User user = query().eq("phone", phone).one();
           // 4. 用户不存在说明是首次登录，自动注册
           if (user == null) {
               user = createUserWithPhone(phone);
           }
           // 5. 数据脱敏：只把非敏感字段写入 session
           UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
           session.setAttribute(USER_SESSION_KEY, userDTO);
           return Result.ok();
       }
   
       /**
        * 根据手机号创建新用户并保存入库
        */
       private User createUserWithPhone(String phone) {
           User user = new User();
           user.setPhone(phone);
           user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
           save(user);
           return user;
       }
   }
   ```

   ------

   

   ### 3. controller/UserController.java（修改接口实现体）

   将对应的处理逻辑委托给 Service 层，并从 UserHolder 获取当前用户信息：

   codeJava

   

   ```
   @PostMapping("code")
       public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
           return userService.sendCode(phone, session);
       }
   
       @PostMapping("/login")
       public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session) {
           return userService.login(loginForm, session);
       }
   
       @GetMapping("/me")
       public Result me() {
           // 由 LoginInterceptor 从 session 取出后放入 ThreadLocal
           UserDTO user = UserHolder.getUser();
           return Result.ok(user);
       }
   ```

   ------

   

   ### 4. interceptor/LoginInterceptor.java（【新建】登录拦截器）

   codeJava

   

   ```
   package com.hmdp.interceptor;
   
   import com.hmdp.dto.UserDTO;
   import com.hmdp.utils.UserHolder;
   import org.springframework.web.servlet.HandlerInterceptor;
   
   import javax.servlet.http.HttpServletRequest;
   import javax.servlet.http.HttpServletResponse;
   import javax.servlet.http.HttpSession;
   
   public class LoginInterceptor implements HandlerInterceptor {
   
       private static final String USER_SESSION_KEY = "user";
   
       @Override
       public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
           // getSession(false)：不存在时不创建，避免为未登录请求生成无用会话
           HttpSession session = request.getSession(false);
           Object user = session == null ? null : session.getAttribute(USER_SESSION_KEY);
           
           if (user == null) {
               // 未登录：返回 401 并中断请求
               response.setStatus(401);
               return false;
           }
           // 已登录：存入 ThreadLocal 供后续业务使用
           UserHolder.saveUser((UserDTO) user);
           return true;
       }
   
       @Override
       public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
           // 必须清理，防止线程复用导致内存泄漏与用户信息串号
           UserHolder.removeUser();
       }
   }
   ```

   ------

   

   ### 5. config/MvcConfig.java（【新建】MVC 配置类）

   codeJava

   

   ```
   package com.hmdp.config;
   
   import com.hmdp.interceptor.LoginInterceptor;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
   import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
   
   @Configuration
   public class MvcConfig implements WebMvcConfigurer {
   
       @Override
       public void addInterceptors(InterceptorRegistry registry) {
           registry.addInterceptor(new LoginInterceptor())
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
   ```

   ------

   

   ## 📌 设计说明与注意事项

   ### 1. 原工程资源复用说明

   - 
   - **未改动的既有类**：User、UserDTO、LoginFormDTO、Result、RegexUtils、SystemConstants、UserMapper 全部原样复用。
   - **已有工具类**：utils/UserHolder.java 原项目中已存在且 saveUser/getUser/removeUser 齐备，直接使用，未做任何侵入性改动。
   - **新增文件仅 2 个**：interceptor/LoginInterceptor.java、config/MvcConfig.java，其余均为在原骨架内填充业务逻辑。

   ### 2. 需确认的优化点

   - 
   - **/voucher/\** 放行范围偏大**：VoucherController 的 POST /voucher/seckill（新增秒杀券）也落在此通配符下，会导致管理端接口免登录即可访问。后续若需精细控制，建议调整为仅放行查询类接口（如 /voucher/list/**）。
   - **登出接口（logout）**：短信登录场景下若需实现登出，只需执行 session.invalidate() 即可。