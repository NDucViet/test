package com.KDLST.Manager.Config;

import java.io.IOException;

import java.util.Arrays;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class RoleFilter implements Filter {

    private static final List<String> PUBLIC_PATH = Arrays.asList("/user/login", "/user/register", "/user/showRegister",
            "/user/showLogin", "/user/about", "/user/contact", "/user/changePassword", "/user/toChangePass",
            "/user/changePass", "/user/logout",
            "/blog", "/css", "/images", "/js");

    private static final List<String> USER_PATH = Arrays.asList("/login", "/register", "/showRegister",
            "/showLogin", "/about", "/contact", "/changePassword", "/toChangePass", "/changePass");

    private static final List<String> EMPLOY_PATH = Arrays.asList("/login", "/register", "/showRegister",
            "/showLogin", "/about", "/contact", "/changePassword", "/toChangePass", "/changePass");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // TODO Auto-generated method stub
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getServletPath();
        if (PUBLIC_PATH.stream().anyMatch(path::startsWith) || path.equals("/")) {
            System.out.println("cc");
            chain.doFilter(request, response);
            return;
        }
        String userRole = (String) httpRequest.getSession().getAttribute("userRole");
        if (userRole == null) {
            // Nếu người dùng không đăng nhập hoặc không có vai trò, chuyển hướng đến trang
            // đăng nhập
            httpResponse.sendRedirect("/user/showLogin");
        } else if ("ADMIN".equals(userRole)) {
            chain.doFilter(request, response);
        } else if ("USER".equals(userRole)) {
            if ("/index".equals(path)) {
                // Cho phép USER truy cập các URL cụ thể
                chain.doFilter(request, response);
            } else {
                // Chặn truy cập và điều hướng đến trang lỗi hoặc đăng nhập
                httpResponse.sendRedirect("/showLogin");
            }
        } else {
            // Chặn truy cập và điều hướng đến trang đăng nhập nếu không có vai trò
            httpResponse.sendRedirect("/showLogin");
        }
    }

}
