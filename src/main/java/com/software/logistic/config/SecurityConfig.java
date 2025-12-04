package com.software.logistic.config;

import com.software.logistic.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 允许访问所有API端点
                .requestMatchers("/api/**").permitAll()
                // 允许访问所有静态资源
                .requestMatchers("/", "/*.html", "/*.js", "/*.css", "/*.ico", "/images/**", "/admin/**", "/customer/**", "/delivery/**", "/finance/**", "/manager/**", "/warehouse/**", "/common.js").permitAll()
                // 允许所有请求访问，临时禁用认证
                .anyRequest().permitAll()
            )
            // 禁用所有认证方式
            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable())
            // 禁用会话管理，使用无状态认证
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 添加匿名认证，确保所有请求都有认证信息
            .anonymous(anonymous -> anonymous
                .principal("anonymousUser")
                .authorities("ROLE_ANONYMOUS")
            )
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}