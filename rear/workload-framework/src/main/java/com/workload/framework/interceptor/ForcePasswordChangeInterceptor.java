package com.workload.framework.interceptor;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import com.alibaba.fastjson2.JSON;
import com.workload.common.core.domain.AjaxResult;
import com.workload.common.core.domain.model.LoginUser;
import com.workload.common.core.text.Convert;
import com.workload.common.utils.ServletUtils;
import com.workload.system.service.ISysConfigService;

/**
 * 强制首次改密拦截器
 * <p>
 * 背景：Excel 导入新建的教师账号统一发默认初始密码（{@code wfit.default-password}），
 * 该值对全体教师公开。框架原有机制仅在 {@code /getInfo} 返回 {@code isDefaultModifyPwd}
 * 供前端弹窗提醒，而弹窗可取消、路由不设卡，等于没有强制力。
 * <p>
 * 本拦截器在服务端兜底：{@code sys_user.pwd_update_date} 为 NULL（从未改过密码）时，
 * 除白名单外的一切业务请求返回 {@link #CODE_PWD_CHANGE_REQUIRED}，前端据此跳改密页。
 * 判定条件与 {@code SysLoginController.initPasswordIsModify} 保持一致：
 * 受系统参数 {@code sys.account.initPasswordModify} 开关控制（1 开启）。
 * <p>
 * 白名单必须包含改密自身与其前置依赖，否则用户会被锁死在无法改密的状态。
 *
 * @author wflg
 */
@Component
public class ForcePasswordChangeInterceptor implements HandlerInterceptor
{
    /** 需要修改密码：区别于 401(未登录)/403(无权限)，前端据此强制跳转改密页 */
    public static final int CODE_PWD_CHANGE_REQUIRED = 602;

    /**
     * 放行清单。除改密接口自身，还须放行前端启动链路（登录/getInfo/getRouters）
     * 与个人中心页面数据，否则用户进不到能改密的界面。
     */
    private static final List<String> WHITE_LIST = List.of(
            // 登录/登出/验证码/注册
            "/login", "/logout", "/register", "/captchaImage",
            // 前端启动链路：拿不到 getInfo/getRouters 就渲染不出页面
            "/getInfo", "/getRouters",
            // 个人中心：查看资料 + 改密接口本身（放行改密是关键，否则死锁）
            "/system/user/profile", "/system/user/profile/**",
            // 静态资源与接口文档
            "/profile/**", "/common/download**", "/swagger-ui/**", "/v3/api-docs/**",
            "/favicon.ico", "/error"
    );

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    @Autowired
    private ISysConfigService configService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception
    {
        // OPTIONS 预检直接放行，避免跨域探测被拦
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            return true;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
        {
            uri = uri.substring(contextPath.length());
        }
        if (isWhiteListed(uri))
        {
            return true;
        }

        // 开关关闭时不启用强制改密
        if (!isForceEnabled())
        {
            return true;
        }

        LoginUser loginUser = currentLoginUser();
        // 未登录交由 Security 过滤链处理（返回 401），此处不越权拦截
        if (loginUser == null || loginUser.getUser() == null)
        {
            return true;
        }

        // pwd_update_date 为 NULL 表示从未修改过初始密码
        if (loginUser.getUser().getPwdUpdateDate() == null)
        {
            AjaxResult result = AjaxResult.error(CODE_PWD_CHANGE_REQUIRED,
                    "您正在使用初始密码，请先修改密码后再使用系统");
            ServletUtils.renderString(response, JSON.toJSONString(result));
            return false;
        }
        return true;
    }

    private boolean isWhiteListed(String uri)
    {
        for (String pattern : WHITE_LIST)
        {
            if (MATCHER.match(pattern, uri))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 读取系统参数 sys.account.initPasswordModify（1 开启强制改密）。
     * 该参数由 ISysConfigService 走 Redis 缓存，逐请求读取无额外 DB 开销。
     */
    private boolean isForceEnabled()
    {
        Integer flag = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return flag != null && flag == 1;
    }

    /**
     * 从 SecurityContext 取当前登录用户；未认证或主体类型不符时返回 null。
     * 不用 SecurityUtils.getLoginUser()，因其在未登录时抛异常。
     */
    private LoginUser currentLoginUser()
    {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser))
        {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }
}
