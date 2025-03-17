
package com.ecommerce.kafkahighconcurrencyproject.security;

import com.ecommerce.kafkahighconcurrencyproject.config.ConfigProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring security with gateway filter configuration and 1 in memory user for
 * debugging.
 *
 */
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    GatewayFilter gatewayFilter;

    @Autowired
    ConfigProperty configProperty;

    public SecurityConfig() {
        super();
        // store security context in case of async methods
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().and()// for postman basic auth
                .formLogin().and()// for browser form login
                .csrf().disable().authorizeRequests().antMatchers(configProperty.getWhitelistUrl()).permitAll()
                .anyRequest().authenticated().and()// all request authenticated
                .addFilterBefore(gatewayFilter, UsernamePasswordAuthenticationFilter.class);// gateway filter config
    }

    /**
     * Default user with all authorities could be used for debugging
     *
     * @param auth
     * @throws Exception
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        if (!configProperty.getDefaultPassword().equalsIgnoreCase(AppConstant.EMPTY)) {
            auth.inMemoryAuthentication().withUser(configProperty.getDefaultUser())
                    .password(configProperty.getDefaultPassword()).authorities(configProperty.getDefaultAuthority());
        }
    }
}
