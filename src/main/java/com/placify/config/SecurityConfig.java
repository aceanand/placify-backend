package com.placify.config;

import com.placify.security.CustomUserDetailsService;
import com.placify.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder());
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/api/test/**").permitAll()
                .antMatchers("/api/email/webhook").permitAll()
                .antMatchers("/api/email/oauth/callback").permitAll()
                .antMatchers("/api/email/oauth/url").permitAll()
                .antMatchers(HttpMethod.GET, "/api/users/profile-picture/**").permitAll()
                .antMatchers("/h2-console/**").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/jobs/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers(HttpMethod.POST, "/api/jobs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.PUT, "/api/jobs/**").hasRole("ADMIN")
                .antMatchers(HttpMethod.DELETE, "/api/jobs/**").hasRole("ADMIN")
                .antMatchers("/api/resume/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/users/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/email-entries/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/email-sources/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/email/oauth/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/email/messages/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/parsed-jobs/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/company/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/salary/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/interview/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/chatbot/**").hasAnyRole("USER", "EMPLOYEE", "ADMIN")
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated();
        
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.headers().frameOptions().disable();
    }
}
