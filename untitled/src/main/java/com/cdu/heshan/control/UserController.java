package com.cdu.heshan.control;

import com.cdu.heshan.dao.UserDAO;
import com.cdu.heshan.dataobj.UserDO;
import com.cdu.heshan.model.Paging;
import com.cdu.heshan.model.Result;
import com.cdu.heshan.model.User;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class UserController {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RedisTemplate<String, UserDO> redisTemplate;

    @GetMapping(path = "/user/login")
    public String loginPage(Model model) {
        return "login";
    }

    @GetMapping(path = "/user/reg")
    public String regPage(Model model) {
        return "reg";
    }

    @GetMapping("/main")
    public String mainPage(HttpSession session, Model model) {
        // 从session获取用户信息
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/user/login";
        }

        // 将用户信息添加到模型
        model.addAttribute("user", user);
        return "/main";
    }

    @GetMapping("/users")
    @ResponseBody
    public Result<Paging<UserDO>> getAll(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        Result<Paging<UserDO>> result = new Result();

        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 15;
        }
        // 设置当前页数为1，以及每页3条记录
        Page<UserDO> page = PageHelper.startPage(pageNum, pageSize).doSelectPage(() -> userDAO.findAll());

        result.setSuccess(true);
        result.setData(
                new Paging<>(page.getPageNum(), page.getPageSize(), page.getPages(), page.getTotal(), page.getResult()));
        return result;
    }

    @PostMapping("/user/add")
    @ResponseBody
    public UserDO save(@RequestBody UserDO userDO) {
        if (userDO == null) {
            return null;
        }

        userDAO.add(userDO);
        redisTemplate.opsForValue().set(userDO.getUserName(), userDO);
        return userDO;
    }

    @PostMapping("/user/batchAdd")
    @ResponseBody
    public List<UserDO> batchAdd(@RequestBody List<UserDO> userDOs) {
        if (CollectionUtils.isEmpty(userDOs)) {
            return userDOs;
        }

        userDAO.batchAdd(userDOs);

        for (UserDO userDO : userDOs) {
            redisTemplate.opsForValue().set(userDO.getUserName(), userDO);
        }

        return userDOs;
    }

    @PostMapping("/user/update")
    @ResponseBody
    public UserDO update(@RequestBody UserDO userDO) {
        if (userDO == null) {
            return null;
        }

        userDAO.update(userDO);
        redisTemplate.opsForValue().set(userDO.getUserName(), userDO);
        return userDO;
    }

    @GetMapping("/user/del")
    @ResponseBody
    public boolean delete(@RequestParam("id") Long id) {
        if (id == null || id < 0) {
            return false;
        }

        List<Long> ids = new ArrayList<>();
        ids.add(id);
        List<UserDO> users = findByIds(ids);
        if (!CollectionUtils.isEmpty(users)) {
            UserDO userDO = users.get(0);
            redisTemplate.delete(userDO.getUserName());
        }

        return userDAO.delete(id) > 0;
    }

    @GetMapping("/user/findByUserName")
    @ResponseBody
    public UserDO findByUserName(@RequestParam("userName") String userName) {
        if (StringUtils.isBlank(userName)) {
            return null;
        }

        UserDO userDO = redisTemplate.opsForValue().get(userName);

        if (userDO == null) {
            userDO = userDAO.findByUserName(userName);
        }

        return userDO;
    }

    @GetMapping("/user/search")
    @ResponseBody
    public List<UserDO> search(@RequestParam("keyWord") String keyWord,
                               @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime startTime,
                               @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                               LocalDateTime endTime) {
        return userDAO.search(keyWord, startTime, endTime);
    }

    @GetMapping("/user/findByIds")
    @ResponseBody
    public List<UserDO> findByIds(@RequestParam("ids") List<Long> ids) {
        return userDAO.findByIds(ids);
    }

    @GetMapping("/info")
    public String userInfo(HttpSession session, Model model) {
        // 从session中获取存储的用户对象
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        UserDO userDO = userDAO.findByUserName(user.getUserName());
        if (userDO == null) {
            model.addAttribute("error", "用户信息不存在");
            return "error";
        }
        User freshUser = userDO.toModel();
        model.addAttribute("user", freshUser);
        return "info";
    }
}