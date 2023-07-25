package com.nowcoder.community.config;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(){
        /*忽略静态资源的访问请求*/
        return (web) -> web.ignoring().antMatchers("/resources/**");
    }

    /**
     *授权
     * 默认对任意请求进行管理
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*登录配置，不使用formLogin，AuthenticationManager认证无法自动装配，无法使用，就无法给用户权限*/
        http.formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
                        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
                        /*登录成功后不是重定向到index，也是转发到login，在controller的login中处理登录请求，并保存登录信息*/
                        request.getRequestDispatcher("/login").forward(request,response);
                    }
                })
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
                        request.setAttribute("error", exception.getMessage());

                        /*转发到登陆页面*/
                        request.getRequestDispatcher("/login").forward(request,response);
                    }
                });

        /*授权配置*/
        http.authorizeRequests()
                /*访问xxx页面需要以下xxx，xxx中的任一权限*/
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow",
                        /*个人主页的帖子和回复*/
                        "/user/myPosts/**",
                        "/user/myReplies/**"
                ).hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**"
                ).hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll()
                /*关闭csrf防攻击凭证*/
                .and().csrf().disable();
        /*权限不足时的配置*/
        http.exceptionHandling()
                /*没有登录*/
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        /*判断时同步请求还是异步请求*/
                        String xRequestedWith = request.getHeader("x-requested-with");
                        /*XMLHttpRequest:异步请求*/
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            /*返回的是普通字符串，需要手动将其转为json*/
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer =response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你还没有登录"));
                        }else {
                            /*重定向到登录页面*/
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                /*权限不足处理器*/
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        /*判断时同步请求还是异步请求*/
                        String xRequestedWith = request.getHeader("x-requested-with");
                        /*XMLHttpRequest:异步请求*/
                        if("XMLHttpRequest".equals(xRequestedWith)){
                            /*返回的是普通字符串，需要手动将其转为json*/
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer =response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限！"));
                        }else {
                            /*重定向到登录页面*/
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        /*Security底层默认会拦截。logout请求，进行退出登录处理
        * 覆盖它默认的逻辑，才能执行我们自己的代码*/
        http.logout()
                /*传一个其他的路径让security处理，而不处理真正的/logout*/
                .logoutUrl("/securityLogout");
        return http.build();
    }


    private class CustomAuthenticationProvider implements AuthenticationProvider {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            String username = authentication.getName();
            String password = (String) authentication.getCredentials();

            User user = userService.findUserByName(username);
            if (user == null) {
                throw new UsernameNotFoundException("账号不存在!");
            }
            password = CommunityUtil.md5(password + user.getSalt());
            if (!user.getPassword().equals(password)) {
                throw new BadCredentialsException("密码不正确!");
            }
            return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        }
        @Override
        public boolean supports(Class<?> authentication) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }

    @Autowired
    void loginProvider(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(new CustomAuthenticationProvider());
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }


}
