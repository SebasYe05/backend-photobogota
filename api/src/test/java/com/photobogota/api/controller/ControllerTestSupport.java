package com.photobogota.api.controller;

import com.photobogota.api.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.AfterEach;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Base para tests de controllers con MockMvc standalone:
 * valida cuerpos con Jakarta Validation, centraliza el manejo de
 * excepciones en {@link GlobalExceptionHandler} y resuelve
 * {@code @AuthenticationPrincipal} usando el SecurityContextHolder.
 *
 * Nota: al usar standaloneSetup, los @PreAuthorize NO se aplican (a falta de
 * proxys de seguridad); esos ya se cubren en SecurityConfigTest.
 */
public abstract class ControllerTestSupport {

    private final org.springframework.validation.Validator validator;

    protected ControllerTestSupport() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        this.validator = factory;
    }

    protected MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    protected RequestPostProcessor autenticado(String username, String... roles) {
        return request -> {
            UserDetails userDetails = User.withUsername(username)
                    .password("clave")
                    .roles(roles)
                    .build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
            return request;
        };
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }
}