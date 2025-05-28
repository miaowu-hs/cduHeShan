package com.cdu.heshan.api;
import com.cdu.heshan.model.Result;
import com.cdu.heshan.model.User;
import com.cdu.heshan.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author heshan
 */
@Controller
public class UserAPI {
    private static final Logger LOG = LoggerFactory.getLogger(UserAPI.class);

    @Autowired
    private UserService userService;

    @PostMapping("/api/user/reg")
    public String reg(@RequestParam("userName") String userName, @RequestParam("pwd") String pwd, Model model) {
        Result<User> result = userService.register(userName, pwd);

        LOG.warn("register result : ", result);
        if (result != null && result.isSuccess()) {
            model.addAttribute("info", "注册成功！");
        } else {
            model.addAttribute("info", "注册失败！");
        }

        return "regInfo";
    }

    @PostMapping("/api/user/login")
    public String login(@RequestParam("userName") String userName,
                        @RequestParam("pwd") String pwd,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {

        Result<User> result = userService.login(userName, pwd);
        LOG.warn("login result : {}", result);

        if (result != null && result.isSuccess()) {

            request.getSession().setAttribute("user", result.getData());

            redirectAttributes.addFlashAttribute("info", "登录成功！");
            System.out.println("登录成功，跳转主页面");
            return "redirect:/main";
        } else {
            redirectAttributes.addFlashAttribute("info", "登录失败！");
            System.out.println("登录失败");
            return "redirect:/login";
        }
    }

    @PostMapping("/api/user/logout")
    public String logout(HttpServletRequest request) {

        request.getSession().removeAttribute("user");

        return "redirect:/user/login";
    }

}